"""Insert missing keys into the locales generate_translations.py deliberately skips.

pt, pt-rBR, b+es+419, zh-rCN and zh-rTW are excluded from the main generator because their
Android resource-folder qualifiers aren't valid Google Translate target codes, and because their
<plurals> live inside strings.xml — which that script fully overwrites. This one only ever
*appends* the keys a locale is missing, so existing wording and plurals are left untouched.

Run after adding new English strings:

    python tools/localization/insert_missing_translations.py

Verify with verify_translations.py, which is what actually decides whether a locale is complete.
"""

from __future__ import annotations

import json
import xml.etree.ElementTree as ET
from pathlib import Path

from generate_translations import (
    CACHE_FILE,
    RES,
    android_escape,
    load_locale_strings,
    load_sources,
    translate_one,
)

# Android resource qualifier -> the target code the translation endpoint actually accepts.
LOCALE_TARGETS = {
    "pt": "pt-PT",
    "pt-rBR": "pt-BR",
    "b+es+419": "es",
    "zh-rCN": "zh-CN",
    "zh-rTW": "zh-TW",
}


def target_file(locale: str) -> Path:
    """Where appended strings go. Every one of these locales keeps a single strings.xml."""
    return RES / f"values-{locale}" / "strings.xml"


def append_strings(path: Path, additions: dict[str, str]) -> None:
    """Append <string> elements just before </resources>, preserving the rest of the file byte
    for byte. Rewriting via ElementTree would reformat the whole file and drop its comments."""
    text = path.read_text("utf-8")
    closing = text.rindex("</resources>")
    block = "".join(
        f'    <string name="{name}">{android_escape(value)}</string>\n'
        for name, value in sorted(additions.items())
    )
    path.write_text(text[:closing] + block + text[closing:], "utf-8")


def main() -> None:
    source = load_sources()
    cache: dict[str, str] = json.loads(CACHE_FILE.read_text("utf-8")) if CACHE_FILE.exists() else {}
    total = 0

    for locale, target in LOCALE_TARGETS.items():
        existing = load_locale_strings(locale)
        missing = {name: value for name, value in source.items() if name not in existing}
        if not missing:
            print(f"{locale}: nothing missing")
            continue

        translated: dict[str, str] = {}
        for name, english in missing.items():
            key = f"{target}\0{english}"
            if key not in cache:
                cache[key] = translate_one(target, english)
            translated[name] = cache[key]

        path = target_file(locale)
        if not path.exists():
            raise SystemExit(f"No strings.xml for {locale} at {path}")
        append_strings(path, translated)
        total += len(translated)
        print(f"{locale}: added {len(translated)} strings")

    CACHE_FILE.write_text(json.dumps(cache, ensure_ascii=False, indent=2, sort_keys=True), "utf-8")
    print(f"Done: {total} strings inserted")


if __name__ == "__main__":
    main()
