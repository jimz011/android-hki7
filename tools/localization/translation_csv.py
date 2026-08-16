"""Shared model for the translations/*.csv exports.

export_translations.py writes these files, import_translations.py reads them back. Both agree
here on the columns, the locale list, and — the fiddly part — how Android's escaping maps onto
text a human can read in a spreadsheet.

The escaping contract, deliberately minimal so that import(export(x)) == x:

    \\'  in XML          <->  a plain apostrophe in the CSV
    &amp; and friends   <->  the bare character

Everything else rides through verbatim, backslash and all. That means a translator sees `\\n`
for a line break and `\\u0020` for a hard space rather than something prettier, which is the
right trade: those carry meaning, and silently rewriting them would corrupt the string. The
notes column says so.

Going back to XML, an apostrophe is always re-escaped — an unescaped one is the single most
common way a translation contribution breaks the Android build.

Double quotes are deliberately left alone in both directions. aapt only treats them specially
when they wrap an entire value (where they mark preserved whitespace and get stripped); the app
ships several hundred unescaped mid-string quotes and builds fine. Rewriting them here would
churn those lines for nothing, so the importer warns about the wrapping case instead.
"""

from __future__ import annotations

import re
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app/src/main/res"
OUT_DIR = ROOT / "translations"
TEMPLATE_FILE = OUT_DIR / "_new-language-template.csv"

COLUMNS = ("key", "type", "item", "english", "translation", "notes")

# Every locale the app ships, in the order verify_translations.py lists them.
LOCALES = (
    "nl", "de", "fr", "es", "it", "tr",
    "pt", "pt-rBR", "b+es+419", "ja", "ko", "zh-rCN", "zh-rTW",
    "nb", "sv", "fi", "ar",
    "pl", "iw", "ru", "th", "ro", "hu", "bg", "el", "cs", "sk", "lt", "da", "et", "lv", "hr",
    "de-rCH", "de-rAT", "es-rMX",
)

# Product names that stay in English in every language. Longest first, so "HKI 7 Cloud" is
# recognised before the "HKI 7" inside it.
BRAND_TERMS = (
    "HKI 7 Cloud",
    "Home Assistant",
    "Google Drive",
    "Nabu Casa",
    "Valetudo",
    "HKI 7",
    "HKI7",
)

# CLDR plural categories. A locale may use any subset; Arabic uses all six, Japanese only "other".
PLURAL_QUANTITIES = ("zero", "one", "two", "few", "many", "other")

# A Java/Android format specifier: %[index$][flags][width][.precision]conversion. Deliberately
# strict — a machine translator that mangles one into `%,1f`, `%.lf` or `%.1στ` produces something
# this will not match, which is exactly the signal wanted. `%%` matches as a conversion of '%' so
# that an escaped percent is consumed rather than being read as the start of a real argument.
FORMAT_ARGUMENT = re.compile(r"%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z%]")
ZERO_WIDTH = re.compile("[\u200b-\u200d\ufeff]")
TAG_KINDS = {"string": "string", "plurals": "plural", "string-array": "array"}


@dataclass
class Resource:
    key: str
    type: str
    section: str
    file: str
    items: dict[str, str] = field(default_factory=dict)
    formatted: str | None = None


@dataclass(frozen=True)
class Row:
    key: str
    type: str
    item: str
    english: str
    translation: str
    notes: str


def locale_path(locale: str) -> Path:
    return RES / f"values-{locale}"


def section_of(path: Path) -> str:
    """`strings_widgets.xml` -> `widgets`; the unprefixed `strings.xml` -> `general`."""
    stem = path.stem
    if stem == "strings":
        return "general"
    return stem.removeprefix("strings_")


def format_arguments(text: str) -> list[str]:
    """The runtime-substituted arguments in a string. `%%` is a literal percent sign, not an
    argument, so it is dropped — but only after matching, so `%%.1f` reads as an escaped percent
    followed by the text `.1f` rather than as a float argument."""
    return [spec for spec in FORMAT_ARGUMENT.findall(text) if spec != "%%"]


def to_csv_text(text: str) -> str:
    """XML element text -> what a translator reads. See the module docstring."""
    return text.replace("\\'", "'")


def to_xml_text(text: str) -> str:
    """The inverse, plus the XML entities. Never emits an unescaped apostrophe."""
    text = ZERO_WIDTH.sub("", text).replace("\r", "")
    text = text.replace("'", "\\'")
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def is_quote_wrapped(text: str) -> bool:
    """True for the one shape where aapt strips double quotes instead of showing them."""
    return len(text) > 1 and text.startswith('"') and text.endswith('"')


def load_resources(directory: Path) -> dict[str, Resource]:
    """Every translatable resource in a values directory, keyed by name, in source-file order.

    Reading goes through ElementTree; writing deliberately does not. The importer edits the
    element it is changing and leaves the rest of the file byte-for-byte alone, because a DOM
    round-trip would reflow every file and drop its comments.
    """
    resources: dict[str, Resource] = {}
    if not directory.is_dir():
        return resources
    for path in sorted(directory.glob("*.xml")):
        if path.name == "themes.xml":
            continue
        for element in ET.parse(path).getroot():
            kind = TAG_KINDS.get(str(element.tag))
            if kind is None or element.get("translatable") == "false":
                continue
            key = element.attrib["name"]
            if key in resources:
                raise RuntimeError(f"Duplicate resource {key!r} in {directory.name}")
            resource = Resource(
                key=key,
                type=kind,
                section=section_of(path),
                file=path.name,
                formatted=element.get("formatted"),
            )
            if kind == "string":
                resource.items[""] = to_csv_text("".join(element.itertext()))
            elif kind == "plural":
                for item in element.findall("item"):
                    quantity = item.attrib.get("quantity", "other")
                    resource.items[quantity] = to_csv_text("".join(item.itertext()))
            else:
                for index, item in enumerate(element.findall("item"), 1):
                    resource.items[str(index)] = to_csv_text("".join(item.itertext()))
            resources[key] = resource
    return resources
