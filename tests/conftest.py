"""Shared pytest fixtures for EdgeWake TTS tests."""

from __future__ import annotations

from pathlib import Path
import sys

import pytest


REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(REPOSITORY_ROOT / "src"))

from edgewake.tts.config import (  # noqa: E402
    GenerationSettings,
    RemoteFile,
    TtsConfig,
    Voice,
)


@pytest.fixture
def tts_config(tmp_path: Path) -> TtsConfig:
    """Return a compact valid configuration rooted in a temporary directory."""

    voice = Voice(
        name="zh_CN-test-medium",
        model=RemoteFile(
            url="https://example.test/voice.onnx",
            filename="voice.onnx",
            minimum_bytes=3,
        ),
        metadata=RemoteFile(
            url="https://example.test/voice.onnx.json",
            filename="voice.onnx.json",
            minimum_bytes=2,
        ),
    )
    return TtsConfig(
        workspace_root=tmp_path,
        wake_word="灵犀灵犀",
        dataset_slug="lingxi_lingxi",
        samples_per_voice=3,
        generation=GenerationSettings(
            length_scales=(0.8, 1.0, 1.2),
            noise_scales=(0.667, 0.8),
            noise_scale_ws=(0.8,),
        ),
        voices=(voice,),
    )

