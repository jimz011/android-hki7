"""Fail on an unescaped apostrophe in any string resource.

Android needs `\'` inside a string; a bare one breaks the resource merge. aapt does not always say
so plainly — a bare apostrophe inside a <string-array> item surfaces as a NullPointerException
about a missing XML attribute, pointing at the merged file rather than the line that caused it,
which sends you looking for a structural fault that is not there.

Cheap to check and easy to reintroduce, since every translation pass writes thousands of strings:

    python tools/localization/check_escaping.py
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

from console import use_utf8_output

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app/src/main/res"
BACKSLASH = chr(92)

COMMENT = re.compile(r"<!--.*?-->", re.S)
TAG = re.compile(r"<[^>]*>")
ESCAPED_PAIR = re.compile(re.escape(BACKSLASH) + ".")


def main() -> int:
    use_utf8_output()
    problems: list[str] = []
    for directory in sorted(RES.glob("values*")):
        for path in sorted(directory.glob("*.xml")):
            text = COMMENT.sub("", path.read_text("utf-8"))
            for number, line in enumerate(text.splitlines(), 1):
                # Blank out tags so an apostrophe in an attribute is not mistaken for one in text,
                # then drop escaped pairs so only bare apostrophes are left.
                body = ESCAPED_PAIR.sub("", TAG.sub("\x00", line))
                if "'" in body:
                    problems.append(f"{directory.name}/{path.name}:{number}: {line.strip()[:120]}")

    if problems:
        print(f"{len(problems)} unescaped apostrophe(s):", file=sys.stderr)
        for problem in problems[:40]:
            print(f"  {problem}", file=sys.stderr)
        if len(problems) > 40:
            print(f"  ... and {len(problems) - 40} more", file=sys.stderr)
        print(f"\nWrite {BACKSLASH}' instead of ' in string resources.", file=sys.stderr)
        return 1

    print("No unescaped apostrophes.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
