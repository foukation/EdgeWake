"""Repository-local launcher for the EdgeWake TTS CLI.

This wrapper makes the command usable before installing EdgeWake itself in
editable mode. Third-party packages must still be installed in ``.venv``.
"""

from __future__ import annotations

from pathlib import Path
import sys


REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
SOURCE_ROOT = REPOSITORY_ROOT / "src"

# Prefer the source tree that belongs to this checkout, not a globally
# installed EdgeWake package with the same name.
sys.path.insert(0, str(SOURCE_ROOT))

from edgewake.tts.cli import main  # noqa: E402


if __name__ == "__main__":
    sys.exit(main())
