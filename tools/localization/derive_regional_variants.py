"""Build the locales that are a regional variant of a language already translated.

Swiss German, Austrian German and Mexican Spanish are not separate languages, and the translation
endpoint has no target for them — asking for "de-CH" returns plain German. Translating them would
therefore burn thousands of calls to re-fetch text the parent locale already holds, and would risk
drifting away from it on the next run for no reason.

So they are derived instead: every resource file is copied from the parent locale, and only what
genuinely differs is rewritten. Today that is Swiss orthography, which has no ß — Switzerland
dropped it, and "Straße" is written "Strasse" there. Austrian and Mexican Spanish have no such
systematic written rule at the register a phone UI uses, so those two come out equal to their
parent; they exist so the language picker can name them, not because the text differs.

Android would fall back from de-AT to de and from es-MX to es-419 on its own, so none of this is
load-bearing for a device left on its system language. It matters for the in-app picker, which sets
an explicit locale and so needs resources to exist under that exact qualifier.

Run after generate_translations.py, generate_plurals.py and insert_whats_new_array.py, so the
parent locale is complete before it is copied:

    python tools/localization/derive_regional_variants.py

Verify with verify_translations.py, which counts these locales like any other.
"""

from __future__ import annotations

import re
import shutil
from pathlib import Path

from console import use_utf8_output

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app/src/main/res"

# Derived locale -> the locale it is copied from.
VARIANTS = {
    "de-rCH": "de",
    "de-rAT": "de",
    "es-rMX": "b+es+419",
}

# Swiss Standard German has no ß; it is always written ss. Applied to text only — never to a
# resource name, an attribute, or a format argument, so the substitution runs per text node.
SHARP_S = re.compile("ß")
TEXT_NODE = re.compile(r">([^<>]*)<")


def swiss_orthography(xml: str) -> str:
    return TEXT_NODE.sub(lambda m: ">" + SHARP_S.sub("ss", m.group(1)) + "<", xml)


TRANSFORMS = {"de-rCH": swiss_orthography}


def main() -> None:
    use_utf8_output()
    for variant, parent in VARIANTS.items():
        source_dir = RES / f"values-{parent}"
        if not source_dir.is_dir():
            raise SystemExit(f"Parent locale values-{parent} does not exist yet")
        target_dir = RES / f"values-{variant}"
        if target_dir.exists():
            shutil.rmtree(target_dir)
        target_dir.mkdir(parents=True)
        transform = TRANSFORMS.get(variant)
        copied = 0
        for source in sorted(source_dir.glob("*.xml")):
            text = source.read_text("utf-8")
            if transform is not None:
                text = transform(text)
            (target_dir / source.name).write_text(text, "utf-8")
            copied += 1
        note = " (ß → ss)" if transform is not None else ""
        print(f"{variant}: {copied} files from values-{parent}{note}")


if __name__ == "__main__":
    main()
