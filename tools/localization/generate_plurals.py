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
import xml.etree.ElementTree as ET
from pathlib import Path

# One implementation of the protection scheme, shared with the other scripts: it is what keeps
# format arguments positional and product names untranslated, and a second copy here drifted
# from it — this one had neither the loosened token fences nor any product-name protection.
from generate_translations import android_escape, translate_one

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
    # CLDR cardinal categories per language. Getting these wrong is silent: a category Android wants
    # but cannot find falls through to the English resource, so the sentence switches language at
    # particular counts. Where a language's set has shrunk between CLDR releases the wider set is
    # written, since a category Android no longer asks for is merely unused.
    "bg": ("one", "other"),
    "da": ("one", "other"),
    "el": ("one", "other"),
    "et": ("one", "other"),
    "hu": ("one", "other"),
    # Thai has no grammatical plural at all, like Japanese and Korean.
    "th": ("other",),
    # Latvian uses a distinct form for zero.
    "lv": ("zero", "one", "other"),
    # One / a paucal 2-4 / everything else — the South-Slavic and Romanian shape.
    "hr": ("one", "few", "other"),
    "ro": ("one", "few", "other"),
    # The West/East-Slavic shape: one, a paucal 2-4, a "many" for 5+, and other for fractions.
    "cs": ("one", "few", "many", "other"),
    "lt": ("one", "few", "many", "other"),
    "pl": ("one", "few", "many", "other"),
    "ru": ("one", "few", "many", "other"),
    "sk": ("one", "few", "many", "other"),
    # Hebrew had a "many" for multiples of ten until CLDR dropped it; both are written.
    "iw": ("one", "two", "many", "other"),
}
LOCALES = tuple(PLURAL_CATEGORIES)
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
