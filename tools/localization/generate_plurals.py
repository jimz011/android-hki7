"""Fill in <plurals> resources for locales that only need the CLDR "other" category (ja, ko).

Separate from generate_translations.py, which only handles <string> resources. Japanese and
Korean have no grammatical plural, so Android/CLDR only requires the "other" quantity for them;
the app always resolves to it since no "one"/"few"/... category exists for these locales.
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
LOCALES = ("ja", "ko")
FORMAT_ARGUMENT = re.compile(r"%\d+\$[a-zA-Z]")
ZERO_WIDTH = re.compile("[" + chr(0x200B) + "-" + chr(0x200D) + chr(0xFEFF) + "]")


def load_source_plurals() -> dict[str, str]:
    plurals: dict[str, str] = {}
    for path in sorted((RES / "values").glob("strings*.xml")):
        root = ET.parse(path).getroot()
        for element in root.findall("plurals"):
            if element.get("translatable") == "false":
                continue
            name = element.attrib["name"]
            other = next(
                (item for item in element.findall("item") if item.get("quantity") == "other"),
                None,
            )
            if other is None:
                raise RuntimeError(f"{name} has no 'other' quantity")
            value = "".join(other.itertext()).replace("\\'", "'").replace("\\n", "\n")
            plurals[name] = value
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
    source = load_source_plurals()
    cache: dict[str, str] = json.loads(CACHE_FILE.read_text("utf-8")) if CACHE_FILE.exists() else {}

    for locale in LOCALES:
        lines = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
        for name, value in sorted(source.items()):
            key = f"{locale}\0plurals\0{value}"
            if key not in cache:
                cache[key] = translate_one(locale, value)
                CACHE_FILE.write_text(
                    json.dumps(cache, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
                )
            translated = cache[key]
            expected = frozenset(FORMAT_ARGUMENT.findall(value))
            if frozenset(FORMAT_ARGUMENT.findall(translated)) != expected:
                raise RuntimeError(f"Placeholder mismatch for {locale}/{name}")
            lines.append(f'    <plurals name="{name}">')
            lines.append(f'        <item quantity="other">{android_escape(translated)}</item>')
            lines.append("    </plurals>")
        lines.append("</resources>")
        output_path = RES / f"values-{locale}/strings_plurals.xml"
        output_path.write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")
        print(f"Wrote {locale}: {len(source)} plurals")


if __name__ == "__main__":
    main()
