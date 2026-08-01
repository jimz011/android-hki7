"""List likely user-facing Kotlin literals that are not Android resources yet."""

from __future__ import annotations

import collections
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "app/src/main/java"
LITERAL = re.compile(r'"(?:\\.|[^"\\])*"')


def main() -> None:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    occurrences: dict[str, list[str]] = collections.defaultdict(list)
    for path in SOURCE.rglob("*.kt"):
        source = path.read_text("utf-8")
        for match in LITERAL.finditer(source):
            value = match.group(0)[1:-1]
            if not re.search(r"[A-Za-z]", value):
                continue
            line = source.count("\n", 0, match.start()) + 1
            occurrences[value].append(f"{path.relative_to(ROOT)}:{line}")
    likely = [
        (value, locations)
        for value, locations in occurrences.items()
        if (
            " " in value
            or value[:1].isupper()
            or any(mark in value for mark in ("!", "?", "…"))
        )
        and not value.startswith(("http", "mdi:", "sensor.", "binary_sensor.", "com."))
    ]
    likely.sort(key=lambda item: (-len(item[1]), item[0].lower()))
    for value, locations in likely:
        print(f"{len(locations)}\t{value}\t{locations[0]}")
    print(f"\n{len(likely)} unique likely user-facing literals")


if __name__ == "__main__":
    main()
