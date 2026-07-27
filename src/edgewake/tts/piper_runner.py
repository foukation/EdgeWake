"""Compatibility entry point for Piper Sample Generator's ONNX path.

The upstream 3.2.0 wheel imports ``piper_train`` unconditionally even though
that module is only used by its optional English ``.pt`` generator and is not
included in the wheel. EdgeWake uses standard Chinese ONNX voices, so this
adapter supplies an empty import placeholder for that unused module and then
executes the upstream CLI unchanged.
"""

from __future__ import annotations

import importlib
from importlib.util import find_spec
import logging
from types import ModuleType
import runpy
import sys


def main() -> int:
    """Run the upstream CLI, explicitly limiting this adapter to ONNX models."""

    if any(argument.lower().endswith(".pt") for argument in sys.argv[1:]):
        print(
            "EdgeWake的兼容入口仅支持Piper ONNX音色，不支持英文.pt生成器。",
            file=sys.stderr,
        )
        return 2

    # Configure a useful default before the upstream module requests DEBUG
    # globally. This keeps normal generation output concise.
    logging.basicConfig(level=logging.INFO)
    for noisy_logger in ("filelock", "httpcore", "httpx"):
        logging.getLogger(noisy_logger).setLevel(logging.WARNING)
    _install_unused_piper_train_placeholder()
    runpy.run_module("piper_sample_generator.__main__", run_name="__main__")
    return 0


def _install_unused_piper_train_placeholder() -> None:
    """Make the upstream unused import succeed when its module is not packaged."""

    try:
        if find_spec("piper_train.vits.commons") is not None:
            importlib.import_module("piper_train.vits.commons")
            return
    except (ImportError, ModuleNotFoundError):
        pass

    piper_train = ModuleType("piper_train")
    piper_train.__path__ = []  # type: ignore[attr-defined]
    vits = ModuleType("piper_train.vits")
    vits.__path__ = []  # type: ignore[attr-defined]
    commons = ModuleType("piper_train.vits.commons")

    piper_train.vits = vits  # type: ignore[attr-defined]
    vits.commons = commons  # type: ignore[attr-defined]
    sys.modules["piper_train"] = piper_train
    sys.modules["piper_train.vits"] = vits
    sys.modules["piper_train.vits.commons"] = commons


if __name__ == "__main__":
    sys.exit(main())
