"""Install the exact dependencies required by EdgeWake's ONNX TTS workflow.

Piper Sample Generator 3.2.0 declares augmentation dependencies that require a
native C++ build on Windows, although EdgeWake does not use that augmentation
path. Installing the upstream wheel with ``--no-deps`` avoids that unnecessary
compiler requirement; the actual ONNX runtime dependencies remain declared in
EdgeWake's ``pyproject.toml`` and are installed normally.
"""

from __future__ import annotations

from pathlib import Path
import subprocess
import sys


PIPER_SAMPLE_GENERATOR_VERSION = "3.2.0"
REPOSITORY_ROOT = Path(__file__).resolve().parents[1]


def main() -> int:
    """Install EdgeWake and the upstream sample generator into this interpreter."""

    _run_pip("install", "--upgrade", "pip")
    _run_pip("install", "-e", f"{REPOSITORY_ROOT}[dev]")
    _run_pip(
        "install",
        f"piper-sample-generator=={PIPER_SAMPLE_GENERATOR_VERSION}",
        "--no-deps",
    )

    print("TTS运行环境安装完成。")
    return 0


def _run_pip(*arguments: str) -> None:
    """Run pip through the active Python interpreter and stop on failure."""

    command = [sys.executable, "-m", "pip", *arguments]
    subprocess.run(command, check=True)


if __name__ == "__main__":
    sys.exit(main())

