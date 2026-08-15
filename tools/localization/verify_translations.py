"""Verify that every shipped locale completely matches the default Android resources."""

from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app/src/main/res"
LOCALES = (
    "nl", "de", "fr", "es", "it", "tr",
    "pt", "pt-rBR", "b+es+419", "ja", "ko", "zh-rCN", "zh-rTW",
    "nb", "sv", "fi", "ar",
)
# CLDR defines only the "other" plural category for these languages, so a plurals resource with
# just that one item is complete, not missing "one"/"few"/etc.
NO_GRAMMATICAL_PLURAL_LOCALES = {"ja", "ko", "zh-rCN", "zh-rTW"}
PLACEHOLDER = re.compile(r"%\d+\$[a-zA-Z]")
ZERO_WIDTH = re.compile("[\u200b-\u200d\ufeff]")
LOCALIZED_COMPARISON = re.compile(
    r"(?:==|!=)\s*stringResource\s*\(|"
    r"stringResource\s*\([^)\n]+\)\s*(?:==|!=)"
)


@dataclass(frozen=True)
class Resource:
    kind: str
    values: tuple[str, ...]

    @property
    def placeholders(self) -> tuple[frozenset[str], ...]:
        # A positional placeholder (%1$s, %2$d, ...) may legitimately be repeated in a translation
        # (e.g. grammatical agreement requiring the same argument twice), so compare the set of
        # distinct placeholders used, not how many times each appears.
        return tuple(frozenset(PLACEHOLDER.findall(value)) for value in self.values)


def element_text(element: ET.Element) -> str:
    return "".join(element.itertext()).replace("\\n", "\n").replace("\\'", "'")


def load_directory(directory: Path) -> dict[str, Resource]:
    resources: dict[str, Resource] = {}
    for path in sorted(directory.glob("*.xml")):
        root = ET.parse(path).getroot()
        for element in root:
            if element.tag not in {"string", "plurals"}:
                continue
            if element.get("translatable") == "false":
                continue
            name = element.attrib["name"]
            if element.tag == "string":
                resource = Resource("string", (element_text(element),))
            else:
                items = tuple(
                    f"{item.attrib.get('quantity', '')}\0{element_text(item)}"
                    for item in element.findall("item")
                )
                resource = Resource("plurals", items)
            if name in resources:
                raise RuntimeError(f"Duplicate resource {name!r} in {directory}")
            resources[name] = resource
    return resources


def main() -> int:
    source = load_directory(RES / "values")
    failures: list[str] = []
    for locale in LOCALES:
        translated = load_directory(RES / f"values-{locale}")
        missing = sorted(source.keys() - translated.keys())
        extra = sorted(translated.keys() - source.keys())
        if missing:
            failures.append(f"{locale}: missing {len(missing)}: {', '.join(missing)}")
        if extra:
            failures.append(f"{locale}: extra {len(extra)}: {', '.join(extra)}")
        for name in sorted(source.keys() & translated.keys()):
            expected = source[name]
            actual = translated[name]
            if actual.kind != expected.kind:
                failures.append(f"{locale}/{name}: {actual.kind} should be {expected.kind}")
            elif actual.kind == "plurals" and locale in NO_GRAMMATICAL_PLURAL_LOCALES:
                other = next((v for v in actual.values if v.startswith("other\0")), None)
                expected_other = next((v for v in expected.values if v.startswith("other\0")), None)
                if other is None:
                    failures.append(f"{locale}/{name}: missing 'other' quantity")
                elif frozenset(PLACEHOLDER.findall(other)) != frozenset(PLACEHOLDER.findall(expected_other or "")):
                    failures.append(f"{locale}/{name}: placeholders in 'other' item don't match")
            elif actual.kind == "plurals":
                # A locale legitimately has a different number of quantity items from English —
                # Arabic uses all six CLDR categories where English uses two — so the items cannot
                # be compared pairwise. What must hold is that every item carries the placeholders
                # the English plural uses.
                required = frozenset().union(*expected.placeholders) if expected.placeholders else frozenset()
                for item, present in zip(actual.values, actual.placeholders):
                    if present != required:
                        quantity = item.split("\0", 1)[0]
                        failures.append(
                            f"{locale}/{name}: '{quantity}' has placeholders {set(present)}, "
                            f"expected {set(required)}"
                        )
            elif actual.placeholders != expected.placeholders:
                failures.append(
                    f"{locale}/{name}: placeholders {actual.placeholders} "
                    f"should be {expected.placeholders}"
                )
            if any(ZERO_WIDTH.search(value) for value in actual.values):
                failures.append(f"{locale}/{name}: contains a zero-width character")
        print(f"{locale}: {len(translated)}/{len(source)} resources")

    kotlin_root = ROOT / "app/src/main/java"
    for path in kotlin_root.rglob("*.kt"):
        text = path.read_text(encoding="utf-8")
        for match in LOCALIZED_COMPARISON.finditer(text):
            line = text.count("\n", 0, match.start()) + 1
            failures.append(
                f"{path.relative_to(ROOT)}:{line}: compares an internal value to translated text"
            )

    if failures:
        print("\n".join(failures), file=sys.stderr)
        return 1
    print(f"All {len(source)} translatable resources are complete and valid in {len(LOCALES)} locales.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
