"""Merge a translations/*.csv file back into app/src/main/res/values-<locale>/.

This is the other half of export_translations.py, and the thing that makes a contributed CSV
usable without hand-copying thousands of strings:

    python tools/localization/import_translations.py translations/nl.csv
    python tools/localization/import_translations.py translations/nl.csv --dry-run

It validates before it writes anything — format arguments have to survive, keys have to exist,
plural quantities have to be real CLDR categories — and reports every problem at once rather
than stopping at the first. A row with an empty translation is skipped, never used to blank an
existing string, so a partially filled file is safe to import.

Existing files are edited in place: only the elements whose text actually changed are touched,
so the diff shows the wording that moved and nothing else. Keys the locale does not have yet are
appended to the file matching the English source's layout, which is also how a brand-new
language gets its whole directory created.
"""

from __future__ import annotations

import argparse
import csv
import re
import sys
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path

from translation_csv import (
    COLUMNS,
    LOCALES,
    PLURAL_QUANTITIES,
    RES,
    ROOT,
    ZERO_WIDTH,
    Resource,
    format_arguments,
    is_quote_wrapped,
    load_resources,
    locale_path,
    to_xml_text,
)

XML_HEADER = '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'


@dataclass
class Change:
    key: str
    item: str
    value: str


def locale_from(path: Path) -> str:
    locale = path.stem
    if locale not in LOCALES:
        raise SystemExit(
            f"{path.name}: '{locale}' is not a locale this app ships.\n"
            f"Rename the file to one of: {', '.join(LOCALES)}\n"
            "(new languages need their qualifier adding to the localization tools first)"
        )
    return locale


def read_csv(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle)
        missing = [column for column in COLUMNS if column not in (reader.fieldnames or [])]
        if missing:
            raise SystemExit(
                f"{path.name}: missing column(s) {', '.join(missing)}.\n"
                f"The header must be: {','.join(COLUMNS)}"
            )
        # key/item are structural and safe to tidy. english/translation are not: some strings are
        # layout glue whose leading or trailing space is load-bearing (" is", " (required)"), and
        # stripping them silently closes up the gap in the UI.
        structural = {"key", "item"}
        return [
            {
                column: (row.get(column) or "").strip() if column in structural
                else (row.get(column) or "")
                for column in COLUMNS
            }
            for row in reader
        ]


def validate(
    rows: list[dict[str, str]],
    source: dict[str, Resource],
    existing: dict[str, Resource],
) -> tuple[list[Change], list[str], list[str]]:
    changes: list[Change] = []
    problems: list[str] = []
    warnings: list[str] = []
    seen: set[tuple[str, str]] = set()

    for number, row in enumerate(rows, 2):  # row 1 is the header
        key, item, english, translation = row["key"], row["item"], row["english"], row["translation"]
        where = f"row {number} ({key or '?'})"

        if not key:
            problems.append(f"{where}: no key")
            continue
        resource = source.get(key)
        if resource is None:
            problems.append(f"{where}: no such string in the English source — was the key edited?")
            continue
        if (key, item) in seen:
            problems.append(f"{where}: duplicate row for this key and item")
            continue
        seen.add((key, item))

        if resource.type == "plural" and item not in PLURAL_QUANTITIES:
            problems.append(
                f"{where}: '{item}' is not a plural category "
                f"(use one of: {', '.join(PLURAL_QUANTITIES)})"
            )
            continue
        if resource.type == "array" and not item.isdigit():
            problems.append(f"{where}: array rows need a position number in the item column")
            continue
        if resource.type == "string" and item:
            problems.append(f"{where}: plain strings must leave the item column empty")
            continue

        if not translation:
            continue  # untranslated, or deliberately left for someone else

        # The English shown in the CSV is what the translator worked from. If it no longer matches
        # the source, the file predates a wording change and importing it would silently reinstate
        # the old English meaning under a new key.
        expected = resource.items.get(item if resource.type != "string" else "")
        if expected is not None and english and english != expected:
            problems.append(
                f"{where}: the english column is out of date (source now reads {expected!r}) — "
                "re-export and re-apply the translation"
            )
            continue

        reference = expected if expected is not None else next(iter(resource.items.values()), "")
        # Compared as sets: a translation may legitimately repeat an argument that English uses
        # once, where grammatical agreement calls for naming the same value twice.
        wanted = sorted(set(format_arguments(reference)))
        got = sorted(set(format_arguments(translation)))
        # Only checked when the English actually takes arguments. A string that takes none is
        # never handed to String.format, so a stray "% d" that a translation happens to form —
        # Hungarian writes "50% felett" for "Above 50%" — is ordinary text, not a broken argument.
        if wanted and wanted != got:
            problems.append(
                f"{where}: format arguments {got or 'none'} should be {wanted} — "
                "these are substituted at runtime and must survive translation"
            )
            continue
        if ZERO_WIDTH.search(translation):
            problems.append(f"{where}: contains an invisible zero-width character")
            continue

        # Not fatal, but worth saying out loud. Both checks compare against what the locale
        # already shipped rather than against the English: several locales have long-standing,
        # deliberate spacing of their own, and warning about those on every import would bury
        # the case that actually matters — a spreadsheet having trimmed the edge whitespace off
        # a string whose space is what separates it from the text it is concatenated with.
        was = existing.get(key)
        previous = was.items.get(item if resource.type != "string" else "") if was else None
        if previous is not None:
            for edge, label in ((str.startswith, "leading"), (str.endswith, "trailing")):
                if edge(previous, " ") and not edge(translation, " "):
                    warnings.append(f"{where}: the {label} space was removed")
        if is_quote_wrapped(translation) and not is_quote_wrapped(reference):
            warnings.append(
                f"{where}: wrapped in double quotes, which Android strips — "
                "escape them as \\\" if they should show"
            )

        changes.append(Change(key=key, item=item, value=translation))

    return changes, problems, warnings


# Every translatable element, with the indentation it sits at. Scanned once per file: matching
# per key instead means thousands of full-file regex passes, which took ten seconds a locale.
# `string-array` has to precede `string` in the alternation. Python takes the first branch that
# matches, and `string` matches the opening tag of a string-array too — leaving the closing
# `</string>` to be found in some later element, so the span swallows everything in between.
ELEMENT = re.compile(
    r'(?P<indent>[ \t]*)<(?P<tag>string-array|plurals|string)\b[^>]*?'
    r'\bname="(?P<name>[^"]+)"[^>]*?>.*?</(?P=tag)>',
    re.S,
)


@dataclass
class Placement:
    start: int
    end: int
    indent: str
    text: str


def scan(body: str) -> dict[str, Placement]:
    return {
        match.group("name"): Placement(
            start=match.start(),
            end=match.end(),
            indent=match.group("indent"),
            text=match.group(0),
        )
        for match in ELEMENT.finditer(body)
    }


def render_element(resource: Resource, values: dict[str, str], indent: str = "    ") -> str:
    attributes = f' name="{resource.key}"'
    if resource.formatted is not None:
        attributes += f' formatted="{resource.formatted}"'

    if resource.type == "string":
        return f"{indent}<string{attributes}>{to_xml_text(values[''])}</string>"

    if resource.type == "plural":
        order = {quantity: index for index, quantity in enumerate(PLURAL_QUANTITIES)}
        items = "".join(
            f'{indent}    <item quantity="{quantity}">{to_xml_text(values[quantity])}</item>\n'
            for quantity in sorted(values, key=lambda q: order.get(q, len(order)))
            if values[quantity]
        )
        return f"{indent}<plurals{attributes}>\n{items}{indent}</plurals>"

    items = "".join(
        f"{indent}    <item>{to_xml_text(values[position])}</item>\n"
        for position in sorted(values, key=int)
        if values[position]
    )
    return f"{indent}<string-array{attributes}>\n{items}{indent}</string-array>"


def apply(locale: str, changes: list[Change], source: dict[str, Resource]) -> tuple[dict[Path, str], list[str]]:
    """Returns the new text of every file that needs rewriting, plus a human-readable log."""
    directory = locale_path(locale)
    existing = load_resources(directory)
    by_key: dict[str, dict[str, str]] = defaultdict(dict)
    for change in changes:
        by_key[change.key][change.item] = change.value

    # One scan per file up front: where each key currently lives, and exactly which span of text
    # it occupies, so an edit lands back in the file it came from.
    original: dict[Path, str] = {}
    placements: dict[Path, dict[str, Placement]] = {}
    home: dict[str, Path] = {}
    for path in sorted(directory.glob("*.xml")) if directory.is_dir() else []:
        original[path] = path.read_text("utf-8")
        placements[path] = scan(original[path])
        for name in placements[path]:
            home.setdefault(name, path)

    replacements: dict[Path, list[tuple[Placement, str]]] = defaultdict(list)
    appended: dict[Path, list[str]] = defaultdict(list)
    log: list[str] = []
    updated = added = unchanged = 0

    for key, values in by_key.items():
        resource = source[key]
        current = existing.get(key)
        merged = dict(current.items) if current else {}
        merged.update(values)
        # Carry the English layout for anything the translator did not supply, so a half-filled
        # plural still produces a valid resource rather than one with holes in it.
        for item_id in resource.items:
            merged.setdefault(item_id, "")
        merged = {item_id: text for item_id, text in merged.items() if text}
        if not merged:
            continue

        # Compare what the resource *means*, not how it is written. The locale files carry a
        # decade of incidental formatting — arrays with four items to a line, `\&apos;` where
        # this script would write `\'`, closing tags at odd indents — all of which round-trips
        # to identical text. Re-rendering those would turn a two-word fix into a thousand-line
        # diff, so an element is only ever rewritten when its content actually changed.
        if current is not None and merged == {k: v for k, v in current.items.items() if v}:
            unchanged += 1
            continue

        path = home.get(key)
        placement = placements[path].get(key) if path else None
        if placement is None:
            appended[directory / resource.file].append(render_element(resource, merged))
            added += 1
            continue

        # Re-indent to whatever the element already uses. The locale files are not uniform —
        # arrays appended by older tooling sit at column 0 — and normalising them here would
        # bury a one-word wording fix under a screenful of whitespace churn.
        rendered = render_element(resource, merged, indent=placement.indent)
        if rendered == placement.text:
            unchanged += 1
            continue
        replacements[path].append((placement, rendered))
        updated += 1

    files: dict[Path, str] = {}
    for path, edits in replacements.items():
        body = original[path]
        # Back to front, so each splice leaves the offsets of the ones before it untouched.
        for placement, rendered in sorted(edits, key=lambda edit: edit[0].start, reverse=True):
            body = body[: placement.start] + rendered + body[placement.end :]
        files[path] = body

    def text_of(path: Path) -> str:
        if path not in files:
            files[path] = original.get(path) or XML_HEADER + "</resources>\n"
        return files[path]

    for path, elements in appended.items():
        body = text_of(path)
        closing = body.rindex("</resources>")
        files[path] = body[:closing] + "".join(f"{element}\n" for element in elements) + body[closing:]

    log.append(f"  {updated} updated, {added} added, {unchanged} already matching")
    return files, log


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("csv_file", type=Path, help="e.g. translations/nl.csv")
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Validate and report, but write nothing.",
    )
    args = parser.parse_args()

    if not args.csv_file.exists():
        print(f"No such file: {args.csv_file}", file=sys.stderr)
        return 2

    locale = locale_from(args.csv_file)
    rows = read_csv(args.csv_file)
    source = load_resources(RES / "values")
    changes, problems, warnings = validate(rows, source, load_resources(locale_path(locale)))

    if problems:
        shown = problems[:40]
        print(f"{len(problems)} problem(s) in {args.csv_file.name}:", file=sys.stderr)
        print("\n".join(f"  {problem}" for problem in shown), file=sys.stderr)
        if len(problems) > len(shown):
            print(f"  ... and {len(problems) - len(shown)} more", file=sys.stderr)
        print("\nNothing was written.", file=sys.stderr)
        return 1

    files, log = apply(locale, changes, source)
    print(f"{locale}: {len(changes)} translated rows")
    print("\n".join(log))

    if warnings:
        print(f"\n{len(warnings)} warning(s) — imported anyway, but worth a look:")
        for warning in warnings[:20]:
            print(f"  {warning}")
        if len(warnings) > 20:
            print(f"  ... and {len(warnings) - 20} more")

    if not files:
        print("Nothing to write — the locale already matches the CSV.")
        return 0

    if args.dry_run:
        print(f"\nWould rewrite {len(files)} file(s):")
        for path in sorted(files):
            print(f"  {path.relative_to(ROOT)}")
        return 0

    locale_path(locale).mkdir(parents=True, exist_ok=True)
    for path, text in sorted(files.items()):
        path.write_text(text, "utf-8")
    print(f"\nWrote {len(files)} file(s). Now run:")
    print("  python tools/localization/verify_translations.py")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
