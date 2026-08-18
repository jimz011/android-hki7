"""Make these scripts safe to print from on a Windows console.

Python picks the console's code page for stdout, and on Windows that is still a legacy one — cp1252
for a Western install, and something narrower elsewhere. Any message carrying a character outside it
raises UnicodeEncodeError *while printing*, which kills the run partway through: derive_regional
_variants.py died on its own progress line, the one whose note reads "(ß → ss)", after it had
already copied several locales.

The text these scripts handle is every language the app ships, so a non-ASCII character reaching a
progress line is normal rather than exceptional. Retargeting the stream to UTF-8 once, at import,
is cheaper than remembering to keep every message ASCII forever.
"""

from __future__ import annotations

import sys


def use_utf8_output() -> None:
    """Print UTF-8 regardless of the console's code page. A no-op where that is already true."""
    for stream in (sys.stdout, sys.stderr):
        # Absent when output is not a text stream — under a pipe that hands back bytes, say.
        reconfigure = getattr(stream, "reconfigure", None)
        if reconfigure is None:
            continue
        # errors="replace" so a stray character degrades to "?" rather than taking the run down,
        # which is the whole point of doing this.
        reconfigure(encoding="utf-8", errors="replace")
