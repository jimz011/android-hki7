"""Generate complete Android locale resources from the English source files.

Uses Google's public translation endpoint, caches every result, protects Android format
arguments, and fails if any locale has missing keys or changed placeholders.
"""

from __future__ import annotations

import argparse
import concurrent.futures
import json
import re
import time
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app/src/main/res"
CACHE_FILE = Path(__file__).with_name("translation_cache.json")
LOCALES = (
    "nl", "de", "fr", "es", "it", "tr", "ja", "ko", "nb", "sv", "fi", "ar",
    "pl", "iw", "ru", "th", "ro", "hu", "bg", "el", "cs", "sk", "lt", "da", "et", "lv", "hr",
)
# Android resource qualifier -> the code the translation endpoint expects, for the few that differ.
# Norwegian is the odd one: Android names the Bokmal resource folder `nb`, the endpoint calls the
# language `no`. Everything absent from this map is passed through unchanged.
# Hebrew is the other historical mismatch: Android names the folder `values-iw` (the pre-1989
# ISO code), and the endpoint answers to the same "iw".
TRANSLATE_TARGETS = {"nb": "no", "iw": "iw"}
# de-rCH, de-rAT and es-rMX are excluded for a different reason: the endpoint has no target for a
# regional variant of German or Spanish, so translating them would just re-fetch the parent
# language. derive_regional_variants.py copies the parent locale and applies what actually
# differs instead.
# pt-rBR, b+es+419, and zh-rCN/zh-rTW are deliberately excluded: their Android resource-folder
# qualifiers aren't valid Google Translate target codes (need pt-BR/es-419/zh-CN/zh-TW), and unlike
# ja/ko their <plurals> live inside strings.xml itself, which this script fully overwrites — running
# it for them silently mistranslates (falls back to English) and drops their plurals. Add new keys to
# those locales with a small standalone insert-only script instead (see git history for an example).
SOURCE_FILES = tuple(sorted((RES / "values").glob("strings*.xml")))
# Names that are the same in every language. Ordered longest-first: each is taken out of the text
# before the shorter ones, so a compound name is never broken up by a substring of itself.
BRAND_TERMS = (
    "HKI 7 Cloud",
    "Home Assistant",
    "Google Drive",
    "Nabu Casa",
    "Valetudo",
    "HKI 7",
    "HKI7",
)
FORMAT_ARGUMENT = re.compile(r"%\d+\$[a-zA-Z]")
NONPOSITIONAL_FORMAT = re.compile(r"%(?!\d+\$)[.\d]*[a-zA-Z]")
# Every runtime-substituted argument, positional or not: %1$s, %d, %.1f, %,2f, %-5s.
# `%%` matches as a conversion of '%' purely so an escaped percent is consumed here rather than
# being read as the opening of a real argument; [any_format_arguments] then drops it.
ANY_FORMAT = re.compile(r"%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z%]")


def any_format_arguments(value: str) -> list[str]:
    """Format specifiers in order of appearance, escaped percent signs excluded."""
    return [m.group() for m in ANY_FORMAT.finditer(value) if m.group() != "%%"]
ZERO_WIDTH = re.compile("[\u200b-\u200d\ufeff]")


def load_sources() -> dict[str, str]:
    strings: dict[str, str] = {}
    for path in SOURCE_FILES:
        root = ET.parse(path).getroot()
        for element in root.findall("string"):
            if element.get("translatable") == "false":
                continue
            name = element.attrib["name"]
            value = (
                "".join(element.itertext())
                .replace("\\'", "'")
                .replace("\\n", "\n")
            )
            if name in strings and strings[name] != value:
                raise RuntimeError(f"Conflicting English resource: {name}")
            strings[name] = value
    return strings


def load_locale_strings(locale: str) -> dict[str, str]:
    """Load every string already supplied by a locale, regardless of its semantic XML file."""
    strings: dict[str, str] = {}
    for path in sorted((RES / f"values-{locale}").glob("*.xml")):
        for element in ET.parse(path).getroot().findall("string"):
            if element.get("translatable") == "false":
                continue
            name = element.attrib["name"]
            value = "".join(element.itertext()).replace("\\'", "'").replace("\\n", "\n")
            if name in strings:
                raise RuntimeError(f"Duplicate locale resource {locale}/{name}")
            strings[name] = value
    return strings


def protected_text(value: str) -> tuple[str, list[str], int]:
    # Non-positional specifiers are protected alongside positional ones. They used not to be, and
    # the translator treated them as ordinary text: %.1f came back from Greek as %.1στ (the `f`
    # conversion translated) and from Czech as %.lf (digit 1 read as letter l), both of which throw
    # when the string is formatted, while Norwegian, Swedish and Danish localized the decimal point
    # to %,1f — a grouping flag, which silently prints six decimals. Repaired in 1.1.1; protecting
    # them here is what stops it happening again on the next run.
    arguments = any_format_arguments(value)
    protected = value
    # Rebuilt by position rather than by str.replace, so a specifier that is a substring of another
    # occurrence ("%d" inside "%%d") cannot be swapped for the wrong one.
    pieces: list[str] = []
    cursor = 0
    index = 0
    for match in ANY_FORMAT.finditer(value):
        if match.group() == "%%":
            continue
        pieces.append(value[cursor:match.start()])
        pieces.append(f"__HKI_ARG_{index}__")
        cursor = match.end()
        index += 1
    pieces.append(value[cursor:])
    protected = "".join(pieces)
    # Product names ride through on the same mechanism as format arguments. A Latin-script target
    # tends to leave them alone by chance, which is why this went unnoticed; Arabic does not, and
    # rendered "Home Assistant" as "المساعد المنزلي" and the app's own name as "Hong Kong 7".
    # Longest first, so "HKI 7 Cloud" is taken before the "HKI 7" inside it.
    # One token per distinct product name, covering every occurrence, rather than one token per
    # occurrence. A name that appears twice is a name a translator may legitimately keep once,
    # reordered, or reworded around — Romanian dropped the first of two "Home Assistant" mentions
    # while keeping the second, which under one-token-per-occurrence read as a lost argument and
    # failed the run.
    format_count = len(arguments)
    for term in BRAND_TERMS:
        if term in protected:
            protected = protected.replace(term, f"__HKI_ARG_{len(arguments)}__")
            arguments.append(term)
    return protected, arguments, format_count


def restore_arguments(value: str, arguments: list[str], format_count: int) -> str:
    for index, argument in enumerate(arguments):
        # The underscore fences are matched loosely on purpose. Two format arguments with nothing
        # between them — "%2$s%3$s" in cr_count_with_average — protect to
        # "__HKI_ARG_1____HKI_ARG_2__", and a translator that collapses the run of four underscores
        # in the middle leaves the second token with no opening fence. The token body is unique, so
        # anchoring on that and treating the fences as optional restores both. (?!\d) stops index 1
        # from matching inside index 11.
        token_pattern = re.compile(rf"_*\s*HKI\s*_\s*ARG\s*_\s*{index}(?!\d)\s*_*", re.IGNORECASE)
        if index < format_count:
            # A format argument is positional: exactly one, or the string is wrong.
            value, count = token_pattern.subn(argument, value, count=1)
            if count != 1:
                raise RuntimeError(f"Translation lost format argument {argument}: {value!r}")
        else:
            # A product name only has to survive; how many times is the translation's business.
            value, count = token_pattern.subn(argument, value)
            if count == 0:
                raise RuntimeError(f"Translation lost product name {argument}: {value!r}")
    return value


def brands_intact(english: str, translated: str) -> bool:
    """Every product name present in the English must survive verbatim in the translation."""
    return all(term not in english or term in translated for term in BRAND_TERMS)


def translate_one(locale: str, value: str) -> str:
    if not re.search(r"[^\W\d_]", value, re.UNICODE):
        return value
    protected, arguments, format_count = protected_text(value)
    query = urllib.parse.urlencode(
        {
            "client": "gtx",
            "sl": "en",
            "tl": TRANSLATE_TARGETS.get(locale, locale),
            "dt": "t",
            "q": protected,
        }
    )
    request = urllib.request.Request(
        "https://translate.googleapis.com/translate_a/single?" + query,
        headers={"User-Agent": "Mozilla/5.0 HKI7-localization"},
    )
    last_error: Exception | None = None
    for attempt in range(5):
        try:
            with urllib.request.urlopen(request, timeout=25) as response:
                payload = json.loads(response.read().decode("utf-8"))
            translated = "".join(part[0] for part in payload[0] if part[0])
            return restore_arguments(translated, arguments, format_count)
        except Exception as error:
            last_error = error
            time.sleep(0.5 * (attempt + 1))
    raise RuntimeError(f"Could not translate {value!r} to {locale}") from last_error


def android_escape(value: str) -> str:
    value = ZERO_WIDTH.sub("", value)
    value = value.replace("\\", "\\\\")
    value = value.replace("\n", "\\n").replace("\r", "")
    value = value.replace("'", "\\'")
    value = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    return value


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--offline",
        action="store_true",
        help="Never contact the translation endpoint; fail if checked-in locale text is incomplete.",
    )
    args = parser.parse_args()
    source = load_sources()
    cache: dict[str, str] = json.loads(CACHE_FILE.read_text("utf-8")) if CACHE_FILE.exists() else {}
    # Existing checked-in locale files are authoritative and seed the cache. This preserves
    # reviewed/manual wording and makes incremental extraction translate only newly added keys.
    existing_by_locale: dict[str, dict[str, str]] = {}
    for locale in LOCALES:
        existing = load_locale_strings(locale)
        existing_by_locale[locale] = existing
        for name, english_value in source.items():
            if name in existing:
                cache.setdefault(f"{locale}\0{english_value}", existing[name])
    jobs = [
        (locale, name, value)
        for locale in LOCALES
        for name, value in source.items()
        if f"{locale}\0{value}" not in cache
    ]
    if args.offline and jobs:
        missing = ", ".join(f"{locale}/{name}" for locale, name, _value in jobs[:25])
        suffix = "" if len(jobs) <= 25 else f", … (+{len(jobs) - 25} more)"
        raise RuntimeError(f"Offline translation cache is incomplete: {missing}{suffix}")

    def execute(job: tuple[str, str, str]) -> tuple[str, str]:
        locale, _name, value = job
        return f"{locale}\0{value}", translate_one(locale, value)

    completed = 0
    with concurrent.futures.ThreadPoolExecutor(max_workers=24) as pool:
        for key, translated in pool.map(execute, jobs):
            cache[key] = translated
            completed += 1
            if completed % 100 == 0:
                CACHE_FILE.write_text(
                    json.dumps(cache, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
                )
                print(f"Translated {completed}/{len(jobs)}", flush=True)
    CACHE_FILE.write_text(json.dumps(cache, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    # Positional placeholders (%1$s, %2$d, ...) may legitimately appear reordered or repeated once
    # translated (different word order, or the same argument needed twice for grammatical
    # agreement), so compare the set of distinct placeholders used, not order or count.
    # Checked over every specifier, not only the positional ones — a mangled %.1f is exactly the
    # failure this pass exists to catch, and it used to sail straight through.
    english_placeholders = {
        name: frozenset(any_format_arguments(value)) for name, value in source.items()
    }
    for locale in LOCALES:
        output_dir = RES / f"values-{locale}"
        output_dir.mkdir(parents=True, exist_ok=True)
        # Semantic locale files are authoritative. Do not duplicate their keys in strings.xml.
        supplied_elsewhere: set[str] = set()
        for path in sorted(output_dir.glob("*.xml")):
            if path.name == "strings.xml":
                continue
            supplied_elsewhere.update(
                element.attrib["name"]
                for element in ET.parse(path).getroot().findall("string")
                if element.get("translatable") != "false"
            )
        lines = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
        for name, value in source.items():
            if name in supplied_elsewhere:
                continue
            translated = cache[f"{locale}\0{value}"]
            expected = english_placeholders[name]
            # Only enforced where the English actually takes arguments. A string that takes none is
            # never handed to String.format, so a "% d" that a translation happens to form around a
            # literal percent sign — Spanish writes "% de unidad" for "unit_of_measurement %" — is
            # ordinary prose, not a broken specifier.
            if expected and frozenset(any_format_arguments(translated)) != expected:
                raise RuntimeError(
                    f"Placeholder mismatch for {locale}/{name}: "
                    f"{sorted(any_format_arguments(translated))} != {sorted(expected)}"
                )
            formatted = ' formatted="false"' if NONPOSITIONAL_FORMAT.search(translated) else ""
            lines.append(f'    <string name="{name}"{formatted}>{android_escape(translated)}</string>')
        lines.append("</resources>")
        (output_dir / "strings.xml").write_text(
            "\n".join(lines) + "\n", encoding="utf-8", newline="\n"
        )
        print(f"Wrote {locale}: {len(source)} strings")


if __name__ == "__main__":
    main()
