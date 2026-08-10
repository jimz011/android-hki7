"""Translate one cr_whats_new_beta_N string-array into every shipped locale.

verify_translations.py only checks <string> and <plurals>, so the release-notes arrays are not
covered by it and are easy to forget. This translates the English array for a given release and
inserts it into each locale's file — the one that already holds the previous release's array, so
the notes stay wherever that locale happens to keep them.

    python tools/localization/insert_whats_new_array.py beta_19

Re-running is safe: a locale that already has the array is skipped rather than duplicated. Pass
--replace when the English notes have been edited since the first run — release notes get
reworded far more often than they get written once, and without it every locale would keep the
stale wording while English moved on.
"""

from __future__ import annotations

import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

from generate_translations import (
    CACHE_FILE,
    RES,
    android_escape,
    translate_one,
)

# Android resource qualifier -> the target code the translation endpoint accepts.
LOCALE_TARGETS = {
    "nl": "nl",
    "de": "de",
    "fr": "fr",
    "es": "es",
    "it": "it",
    "tr": "tr",
    "ja": "ja",
    "ko": "ko",
    "pt": "pt-PT",
    "pt-rBR": "pt-BR",
    "b+es+419": "es",
    "zh-rCN": "zh-CN",
    "zh-rTW": "zh-TW",
}


def english_items(array_name: str) -> list[str]:
    for path in sorted((RES / "values").glob("strings*.xml")):
        for element in ET.parse(path).getroot().findall("string-array"):
            if element.attrib.get("name") == array_name:
                return ["".join(item.itertext()).replace("\\'", "'") for item in element.findall("item")]
    raise SystemExit(f"No {array_name} in the English resources")


def locale_file(locale: str, previous_array: str) -> Path | None:
    """The file already holding the previous release's array — locales disagree about which
    semantic file that is, and a release note belongs beside its predecessor."""
    for path in sorted((RES / f"values-{locale}").glob("*.xml")):
        if previous_array in path.read_text("utf-8"):
            return path
    return None


def insert_array(path: Path, array_name: str, previous_array: str, items: list[str]) -> None:
    """Insert the new array immediately before the previous one, so newest stays first."""
    text = path.read_text("utf-8")
    anchor = text.index(f'<string-array name="{previous_array}">')
    # Match the anchor's own indentation rather than assuming four spaces.
    line_start = text.rindex("\n", 0, anchor) + 1
    indent = text[line_start:anchor]
    body = "".join(f"{indent}    <item>{android_escape(item)}</item>\n" for item in items)
    block = f'{indent}<string-array name="{array_name}">\n{body}{indent}</string-array>\n\n'
    path.write_text(text[:line_start] + block + text[line_start:], "utf-8")


def remove_array(path: Path, array_name: str) -> None:
    """Drop an existing array so it can be written fresh. Non-greedy to the first closing tag, so
    it can never swallow the release below it."""
    text = path.read_text("utf-8")
    pattern = re.compile(
        rf'[ \t]*<string-array name="{re.escape(array_name)}">.*?</string-array>\r?\n\s*',
        re.S,
    )
    path.write_text(pattern.sub("", text, count=1), "utf-8")


def main() -> None:
    argv = sys.argv[1:]
    replace = "--replace" in argv
    previous_override = None
    if "--previous" in argv:
        previous_override = argv[argv.index("--previous") + 1]
    args = [
        a for i, a in enumerate(argv)
        if not a.startswith("--") and (i == 0 or argv[i - 1] != "--previous")
    ]
    if len(args) != 1:
        raise SystemExit(
            "Usage: insert_whats_new_array.py <release> [--previous <array_name>] [--replace]\n"
            "  <release> is the suffix, e.g. beta_19 or 1_0_0.\n"
            "  --previous is required for a release whose predecessor cannot be derived by\n"
            "  counting down, which is any name that is not beta_N."
        )
    release = args[0]
    array_name = f"cr_whats_new_{release}"
    if previous_override:
        previous = previous_override
    elif release.startswith("beta_"):
        previous = f"cr_whats_new_beta_{int(release.rsplit('_', 1)[1]) - 1}"
    else:
        raise SystemExit(f"Cannot derive the previous array for {release!r}; pass --previous.")

    items = english_items(array_name)
    cache: dict[str, str] = json.loads(CACHE_FILE.read_text("utf-8")) if CACHE_FILE.exists() else {}

    for locale, target in LOCALE_TARGETS.items():
        path = locale_file(locale, previous)
        if path is None:
            print(f"{locale}: no file holds {previous}, skipped")
            continue
        if f'"{array_name}"' in path.read_text("utf-8"):
            if not replace:
                print(f"{locale}: already has {array_name}")
                continue
            remove_array(path, array_name)
        translated = []
        for item in items:
            key = f"{target}\0{item}"
            if key not in cache:
                cache[key] = translate_one(target, item)
            translated.append(cache[key])
        insert_array(path, array_name, previous, translated)
        print(f"{locale}: inserted {len(translated)} items into {path.name}")

    CACHE_FILE.write_text(json.dumps(cache, ensure_ascii=False, indent=2, sort_keys=True), "utf-8")


if __name__ == "__main__":
    main()
