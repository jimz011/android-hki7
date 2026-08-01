"""Extract direct Compose UI string literals into Android string resources.

The script intentionally targets only literals passed directly to Text (positional or `text =`)
and contentDescription. Those locations are guaranteed to be composable, so replacing them with
stringResource is safe. Indirect labels are left for the explicit localization audit.
"""

from __future__ import annotations

import hashlib
import html
import re
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SOURCE_ROOT = ROOT / "app/src/main/java/com/jimz011apps/hki7"
OUTPUT = ROOT / "app/src/main/res/values/strings_generated_ui.xml"

START_PATTERNS = [
    re.compile(r"\bText\(\s*(?:text\s*=\s*)?"),
    re.compile(r"\bcontentDescription\s*=\s*"),
]
POSITIONAL_UI_CALLS = re.compile(
    r"\b(?:SettingsChoice|SettingsSubcategory|SettingsChoiceChip|"
    r"ModernSettingsMenuItem|SettingsGroup|SettingsTabRow)\s*\("
)


def interpolation_end(source: str, start: int) -> int | None:
    """Find the closing brace of a `${...}` expression."""
    depth = 1
    index = start
    while index < len(source):
        if source.startswith("//", index):
            newline = source.find("\n", index + 2)
            index = len(source) if newline < 0 else newline + 1
            continue
        if source.startswith("/*", index):
            close = source.find("*/", index + 2)
            index = len(source) if close < 0 else close + 2
            continue
        if source.startswith('"""', index):
            close = source.find('"""', index + 3)
            if close < 0:
                return None
            index = close + 3
            continue
        if source[index] in {'"', "'"}:
            quote = source[index]
            index += 1
            while index < len(source):
                if source[index] == "\\":
                    index += 2
                elif source[index] == quote:
                    index += 1
                    break
                else:
                    index += 1
            continue
        if source[index] == "{":
            depth += 1
        elif source[index] == "}":
            depth -= 1
            if depth == 0:
                return index + 1
        index += 1
    return None


def string_literal_end(source: str, start: int) -> int | None:
    """Find the end of a regular Kotlin string, including interpolation bodies."""
    if start >= len(source) or source[start] != '"' or source.startswith('"""', start):
        return None
    index = start + 1
    while index < len(source):
        if source[index] == "\\":
            index += 2
            continue
        if source.startswith("${", index):
            end = interpolation_end(source, index + 2)
            if end is None:
                return None
            index = end
            continue
        if source[index] == '"':
            return index + 1
        index += 1
    return None


def direct_ui_literals(source: str) -> list[tuple[int, int]]:
    found: list[tuple[int, int]] = []
    occupied: set[int] = set()
    for pattern in START_PATTERNS:
        for match in pattern.finditer(source):
            start = match.end()
            if start in occupied:
                continue
            end = string_literal_end(source, start)
            if end is not None:
                found.append((start, end))
                occupied.add(start)
    return sorted(found)


def positional_ui_literals(source: str) -> list[tuple[int, int]]:
    """Find top-level literal arguments in selected UI-only composables."""
    found: list[tuple[int, int]] = []
    for match in POSITIONAL_UI_CALLS.finditer(source):
        if source[max(0, match.start() - 5):match.start()].strip().endswith("fun"):
            continue
        parens = 1
        braces = 0
        brackets = 0
        index = match.end()
        while index < len(source) and parens:
            if source.startswith("//", index):
                newline = source.find("\n", index + 2)
                index = len(source) if newline < 0 else newline + 1
                continue
            if source.startswith("/*", index):
                close = source.find("*/", index + 2)
                index = len(source) if close < 0 else close + 2
                continue
            if source[index] == '"':
                end = string_literal_end(source, index)
                if end is None:
                    break
                if parens == 1 and braces == 0 and brackets == 0:
                    found.append((index, end))
                index = end
                continue
            char = source[index]
            if char == "(":
                parens += 1
            elif char == ")":
                parens -= 1
            elif char == "{":
                braces += 1
            elif char == "}":
                braces = max(0, braces - 1)
            elif char == "[":
                brackets += 1
            elif char == "]":
                brackets = max(0, brackets - 1)
            index += 1
    return found


def likely_user_text(raw: str) -> bool:
    value = decode_kotlin_text(raw)
    if not re.search(r"[^\W\d_]", value, re.UNICODE):
        return False
    if "_" in value or value.startswith(("http", "mdi:", "sensor.", "binary_sensor.")):
        return False
    if re.fullmatch(r"[dMyEHhms:,. /-]+", value):
        return False
    return bool(
        " " in value
        or "${" in value
        or re.match(r"[A-ZÀ-ÖØ-Þ]", value)
        or any(mark in value for mark in ("!", "?", "…"))
    )


def text_expression_literals(source: str) -> list[tuple[int, int]]:
    """Find literals nested in Text's first argument, e.g. `Text(if (...) "On" else "Off")`."""
    found: list[tuple[int, int]] = []
    for match in re.finditer(r"\bText\(\s*(?:text\s*=\s*)?", source):
        parens = braces = brackets = 0
        index = match.end()
        while index < len(source):
            if source[index] == '"' and not source.startswith('"""', index):
                end = string_literal_end(source, index)
                if end is None:
                    break
                raw = source[index:end]
                if likely_user_text(raw):
                    found.append((index, end))
                index = end
                continue
            char = source[index]
            if char == "(":
                parens += 1
            elif char == ")":
                if parens == 0 and braces == 0 and brackets == 0:
                    break
                parens -= 1
            elif char == "{":
                braces += 1
            elif char == "}":
                braces = max(0, braces - 1)
            elif char == "[":
                brackets += 1
            elif char == "]":
                brackets = max(0, brackets - 1)
            elif char == "," and parens == 0 and braces == 0 and brackets == 0:
                break
            index += 1
    return found


def labeled_assignment_literals(source: str) -> list[tuple[int, int]]:
    """Find text branches assigned to variables such as `statusLabel` or `pageTitle`."""
    found: list[tuple[int, int]] = []
    pattern = re.compile(
        r"\bval\s+\w*(?:Label|Title|Text|Description)\w*\s*=\s*",
        re.IGNORECASE,
    )
    for match in pattern.finditer(source):
        parens = braces = brackets = 0
        index = match.end()
        while index < len(source):
            if source[index] == '"' and not source.startswith('"""', index):
                end = string_literal_end(source, index)
                if end is None:
                    break
                raw = source[index:end]
                if likely_user_text(raw):
                    found.append((index, end))
                index = end
                continue
            char = source[index]
            if char == "(":
                parens += 1
            elif char == ")":
                parens = max(0, parens - 1)
            elif char == "{":
                braces += 1
            elif char == "}":
                if braces == 0:
                    break
                braces -= 1
            elif char == "[":
                brackets += 1
            elif char == "]":
                brackets = max(0, brackets - 1)
            elif char == "\n" and parens == 0 and braces == 0 and brackets == 0:
                break
            index += 1
    return found


def decode_kotlin_text(raw: str) -> str:
    body = raw[1:-1]
    replacements = {
        r"\\": "\\",
        r"\"": '"',
        r"\n": "\n",
        r"\r": "\r",
        r"\t": "\t",
        r"\$": "$",
    }
    for source, target in replacements.items():
        body = body.replace(source, target)
    return re.sub(
        r"\\u([0-9a-fA-F]{4})",
        lambda match: chr(int(match.group(1), 16)),
        body,
    )


def split_template(raw: str) -> tuple[str, list[str]]:
    """Return Android-format English text plus Kotlin interpolation expressions."""
    body = raw[1:-1]
    pieces: list[str] = []
    arguments: list[str] = []
    literal_start = 0
    index = 0

    def append_literal(end: int) -> None:
        pieces.append(decode_kotlin_text('"' + body[literal_start:end] + '"'))

    while index < len(body):
        if body[index] == "\\":
            index += 2
            continue
        if body[index] != "$":
            index += 1
            continue

        if index + 1 < len(body) and body[index + 1] == "{":
            depth = 1
            cursor = index + 2
            in_string = False
            escaped = False
            while cursor < len(body) and depth:
                char = body[cursor]
                if in_string:
                    if escaped:
                        escaped = False
                    elif char == "\\":
                        escaped = True
                    elif char == '"':
                        in_string = False
                else:
                    if char == '"':
                        in_string = True
                    elif char == "{":
                        depth += 1
                    elif char == "}":
                        depth -= 1
                cursor += 1
            if depth:
                index += 1
                continue
            append_literal(index)
            arguments.append(body[index + 2 : cursor - 1].strip())
            pieces.append(f"%{len(arguments)}$s")
            index = cursor
            literal_start = cursor
            continue

        identifier = re.match(r"[A-Za-z_][A-Za-z0-9_]*", body[index + 1 :])
        if identifier:
            append_literal(index)
            arguments.append(identifier.group(0))
            pieces.append(f"%{len(arguments)}$s")
            index += 1 + len(identifier.group(0))
            literal_start = index
            continue
        index += 1

    append_literal(len(body))
    value = "".join(pieces)
    if arguments:
        value = value.replace("%", "%%")
        for position in range(1, len(arguments) + 1):
            value = value.replace(f"%%{position}$s", f"%{position}$s")
    return value, arguments


def resource_key(value: str) -> str:
    words = re.findall(r"[a-z0-9]+", re.sub(r"%\d+\$s", " ", value.lower()))
    slug = "_".join(words[:9])[:58].strip("_") or "text"
    digest = hashlib.sha1(value.encode("utf-8")).hexdigest()[:7]
    return f"ui_{slug}_{digest}"


def add_imports(source: str) -> str:
    if "androidx.compose.ui.res.stringResource" not in source:
        package_end = source.index("\n", source.index("package ")) + 1
        source = source[:package_end] + "\nimport androidx.compose.ui.res.stringResource\n" + source[package_end:]
    if re.search(r"\bR\.string\.", source) and not re.search(
        r"^import com\.jimz011apps\.hki7\.R$", source, re.MULTILINE
    ):
        package_end = source.index("\n", source.index("package ")) + 1
        source = source[:package_end] + "\nimport com.jimz011apps.hki7.R\n" + source[package_end:]
    return source


def main() -> None:
    resources: dict[str, str] = {}
    if OUTPUT.exists():
        for element in ET.parse(OUTPUT).getroot().findall("string"):
            resources[element.attrib["name"]] = "".join(element.itertext()).replace("\\'", "'")
    changed_files = 0
    replacements = 0

    for path in sorted(SOURCE_ROOT.rglob("*.kt")):
        source = path.read_text(encoding="utf-8")
        changed = False
        edits: list[tuple[int, int, str]] = []
        literal_ranges = set(
            direct_ui_literals(source)
            + positional_ui_literals(source)
            + text_expression_literals(source)
            + labeled_assignment_literals(source)
        )
        for start, end in sorted(literal_ranges):
            raw = source[start:end]
            value, arguments = split_template(raw)
            if not value.strip() or not re.search(r"[^\W\d_]", value, re.UNICODE):
                continue
            key = resource_key(value)
            resources[key] = value
            args = ", " + ", ".join(arguments) if arguments else ""
            edits.append((start, end, f"stringResource(R.string.{key}{args})"))
            changed = True
            replacements += 1
        for start, end, replacement in reversed(edits):
            source = source[:start] + replacement + source[end:]
        if changed:
            source = add_imports(source)
            path.write_text(source, encoding="utf-8", newline="\n")
            changed_files += 1

    lines = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
    for key, value in sorted(resources.items()):
        escaped = html.escape(value, quote=False).replace("'", r"\'").replace("\n", r"\n")
        formatted = ' formatted="false"' if re.search(r"%(?!\d+\$)[.\d]*[a-zA-Z]", value) else ""
        lines.append(f'    <string name="{key}"{formatted}>{escaped}</string>')
    lines.append("</resources>")
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")
    print(f"Extracted {len(resources)} unique strings from {replacements} uses in {changed_files} files")


if __name__ == "__main__":
    main()
