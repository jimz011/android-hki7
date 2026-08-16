"""Export every locale to a single reviewable CSV under translations/.

The app's strings live in 18 semantic XML files per locale, which is fine for the codebase and
hopeless for a native speaker who just wants to read their language end to end and fix the three
sentences that are wrong. This writes one CSV per locale — every string, plural and array in one
place, English alongside the translation — plus a blank template for starting a new language.

    python tools/localization/export_translations.py            # all locales + template
    python tools/localization/export_translations.py nl de      # just these
    python tools/localization/export_translations.py --check    # fail if anything is out of date

import_translations.py reads the same files back. --check is what CI runs, so the exports can
never quietly drift behind the English source.
"""

from __future__ import annotations

import argparse
import csv
import io
import sys
from pathlib import Path

from translation_csv import (
    BRAND_TERMS,
    COLUMNS,
    LOCALES,
    OUT_DIR,
    RES,
    TEMPLATE_FILE,
    Row,
    format_arguments,
    load_resources,
    locale_path,
)


def notes_for(english: str, translation: str) -> str:
    """Constraints this row imposes on a translator. Terse on purpose — it repeats across
    thousands of rows, and anything longer bloats the file past what GitHub renders as a table."""
    notes: list[str] = []
    if not translation:
        notes.append("needs translation")
    arguments = list(dict.fromkeys(format_arguments(english)))
    if arguments:
        notes.append("keep " + " ".join(arguments))
    brands = [term for term in BRAND_TERMS if term in english]
    if brands:
        # BRAND_TERMS is longest-first, so "HKI 7 Cloud" matches before the "HKI 7" inside it.
        # Drop any term that is a substring of one already listed.
        kept = [t for t in brands if not any(t != other and t in other for other in brands)]
        notes.append("don't translate " + ", ".join(kept))
    if "\\n" in english:
        notes.append("\\n = line break")
    if english != english.strip():
        # These get concatenated with neighbouring text, so the padding is the separator.
        # Spreadsheets love to trim it; say so where it matters.
        notes.append("keep the space at the start/end")
    return "; ".join(notes)


def rows_for(locale: str | None) -> list[Row]:
    """Every translatable unit, English first, with `locale`'s current wording beside it.

    `locale` of None produces the new-language template: same rows, translation left blank.
    """
    source = load_resources(RES / "values")
    translated = load_resources(locale_path(locale)) if locale else {}

    rows: list[Row] = []
    for key, resource in source.items():
        existing = translated.get(key)
        items = dict(resource.items)
        if existing:
            # A locale legitimately carries plural quantities English does not have — Arabic uses
            # all six CLDR categories against English's two — so union the two sets rather than
            # iterating English alone, or those rows would be invisible in the export and get
            # dropped on the next import.
            for item_id in existing.items:
                items.setdefault(item_id, "")
        for item_id, english in items.items():
            translation = existing.items.get(item_id, "") if existing else ""
            rows.append(
                Row(
                    key=key,
                    type=resource.type,
                    item=item_id,
                    english=english,
                    translation=translation,
                    notes=notes_for(english, translation),
                )
            )
    return rows


def render(rows: list[Row]) -> str:
    buffer = io.StringIO(newline="")
    writer = csv.writer(buffer, lineterminator="\n")
    writer.writerow(COLUMNS)
    for row in rows:
        writer.writerow(
            [row.key, row.type, row.item, row.english, row.translation, row.notes]
        )
    return buffer.getvalue()


def write(path: Path, content: str, check: bool) -> bool:
    """Returns True when the file on disk already matches. In --check mode nothing is written."""
    current = path.read_text("utf-8") if path.exists() else None
    if current == content:
        return True
    if not check:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, "utf-8")
    return False


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("locales", nargs="*", help="Android qualifiers; default is every locale")
    parser.add_argument(
        "--check",
        action="store_true",
        help="Write nothing; exit 1 if any export is missing or out of date.",
    )
    args = parser.parse_args()

    unknown = [locale for locale in args.locales if locale not in LOCALES]
    if unknown:
        print(f"Unknown locale(s): {', '.join(unknown)}", file=sys.stderr)
        print(f"Known: {', '.join(LOCALES)}", file=sys.stderr)
        return 2

    targets: list[str | None] = list(args.locales) if args.locales else [None, *LOCALES]
    stale: list[str] = []

    for locale in targets:
        path = TEMPLATE_FILE if locale is None else OUT_DIR / f"{locale}.csv"
        rows = rows_for(locale)
        untranslated = sum(1 for row in rows if not row.translation)
        if write(path, render(rows), args.check):
            state = "up to date"
        else:
            state = "checked" if args.check else "written"
            stale.append(path.name)
        label = "template" if locale is None else locale
        print(f"{label}: {len(rows)} rows, {untranslated} untranslated — {state}")

    if args.check and stale:
        print(
            f"\n{len(stale)} export(s) out of date: {', '.join(stale)}\n"
            "Run: python tools/localization/export_translations.py",
            file=sys.stderr,
        )
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
