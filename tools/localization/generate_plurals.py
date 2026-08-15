"""Fill in <plurals> resources for locales generate_translations.py cannot cover.

Separate from generate_translations.py, which only handles <string> resources. Each locale needs
exactly the CLDR quantity categories its language actually uses: Japanese and Korean have no
grammatical plural and need only "other", the Nordic languages behave like English with "one" and
"other", and Arabic uses all six. A category Android expects but cannot find in the locale falls
back to the English resource, so a partial set shows English text at particular counts — which is
why every required category is written even where the wording has to repeat.
"""

from __future__ import annotations

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
# Locale -> the CLDR plural categories that locale requires.
PLURAL_CATEGORIES = {
    "ja": ("other",),
    "ko": ("other",),
    "nb": ("one", "other"),
    "sv": ("one", "other"),
    "fi": ("one", "other"),
    # Arabic distinguishes all six. Only "one" has a distinct English source to translate from, so
    # the remaining categories reuse the plural wording: imperfect Arabic grammar for two/few/many,
    # but always Arabic, which beats falling through to English mid-sentence.
    "ar": ("zero", "one", "two", "few", "many", "other"),
}
LOCALES = tuple(PLURAL_CATEGORIES)
# Android resource qualifier -> translation endpoint code, for the few that differ.
TRANSLATE_TARGETS = {"nb": "no"}
FORMAT_ARGUMENT = re.compile(r"%\d+\$[a-zA-Z]")
ZERO_WIDTH = re.compile("[" + chr(0x200B) + "-" + chr(0x200D) + chr(0xFEFF) + "]")


def load_source_plurals() -> dict[str, dict[str, str]]:
    plurals: dict[str, dict[str, str]] = {}
    for path in sorted((RES / "values").glob("strings*.xml")):
        root = ET.parse(path).getroot()
        for element in root.findall("plurals"):
            if element.get("translatable") == "false":
                continue
            name = element.attrib["name"]
            forms: dict[str, str] = {}
            for item in element.findall("item"):
                quantity = item.get("quantity")
                if quantity is not None:
                    forms[quantity] = (
                        "".join(item.itertext()).replace("\\'", "'").replace("\\n", "\n")
                    )
            if "other" not in forms:
                raise RuntimeError(f"{name} has no 'other' quantity")
            plurals[name] = forms
    return plurals


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
    source = load_source_plurals()
    cache: dict[str, str] = json.loads(CACHE_FILE.read_text("utf-8")) if CACHE_FILE.exists() else {}

    for locale in LOCALES:
        categories = PLURAL_CATEGORIES[locale]
        lines = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
        for name, forms in sorted(source.items()):
            lines.append(f'    <plurals name="{name}">')
            for category in categories:
                # Use the English wording for this category where one exists — in practice "one"
                # and "other" — and the plural wording for the categories English does not model.
                english = forms.get(category) or forms["other"]
                key = f"{locale}\0plurals\0{english}"
                if key not in cache:
                    cache[key] = translate_one(locale, english)
                    CACHE_FILE.write_text(
                        json.dumps(cache, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
                    )
                translated = cache[key]
                expected = frozenset(FORMAT_ARGUMENT.findall(english))
                if frozenset(FORMAT_ARGUMENT.findall(translated)) != expected:
                    raise RuntimeError(f"Placeholder mismatch for {locale}/{name}/{category}")
                lines.append(
                    f'        <item quantity="{category}">{android_escape(translated)}</item>'
                )
            lines.append("    </plurals>")
        lines.append("</resources>")
        output_path = RES / f"values-{locale}/strings_plurals.xml"
        output_path.write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")
        print(f"Wrote {locale}: {len(source)} plurals")


if __name__ == "__main__":
    main()
