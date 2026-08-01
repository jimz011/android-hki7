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
LOCALES = ("ja", "ko")
SOURCE_FILES = tuple(sorted((RES / "values").glob("strings*.xml")))
FORMAT_ARGUMENT = re.compile(r"%\d+\$[a-zA-Z]")
NONPOSITIONAL_FORMAT = re.compile(r"%(?!\d+\$)[.\d]*[a-zA-Z]")
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


def protected_text(value: str) -> tuple[str, list[str]]:
    arguments = FORMAT_ARGUMENT.findall(value)
    protected = value
    for index, argument in enumerate(arguments):
        protected = protected.replace(argument, f"__HKI_ARG_{index}__", 1)
    return protected, arguments


def restore_arguments(value: str, arguments: list[str]) -> str:
    for index, argument in enumerate(arguments):
        token_pattern = re.compile(rf"__\s*HKI\s*_\s*ARG\s*_\s*{index}\s*__", re.IGNORECASE)
        value, count = token_pattern.subn(argument, value, count=1)
        if count != 1:
            raise RuntimeError(f"Translation lost format argument {argument}: {value!r}")
    return value


def translate_one(locale: str, value: str) -> str:
    if not re.search(r"[^\W\d_]", value, re.UNICODE):
        return value
    protected, arguments = protected_text(value)
    query = urllib.parse.urlencode(
        {"client": "gtx", "sl": "en", "tl": locale, "dt": "t", "q": protected}
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
            return restore_arguments(translated, arguments)
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
    english_placeholders = {
        name: frozenset(FORMAT_ARGUMENT.findall(value)) for name, value in source.items()
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
            if frozenset(FORMAT_ARGUMENT.findall(translated)) != english_placeholders[name]:
                raise RuntimeError(f"Placeholder mismatch for {locale}/{name}")
            formatted = ' formatted="false"' if NONPOSITIONAL_FORMAT.search(translated) else ""
            lines.append(f'    <string name="{name}"{formatted}>{android_escape(translated)}</string>')
        lines.append("</resources>")
        (output_dir / "strings.xml").write_text(
            "\n".join(lines) + "\n", encoding="utf-8", newline="\n"
        )
        print(f"Wrote {locale}: {len(source)} strings")


if __name__ == "__main__":
    main()
