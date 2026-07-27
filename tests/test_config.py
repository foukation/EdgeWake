"""Tests for strict TTS YAML configuration loading."""

from __future__ import annotations

from pathlib import Path

import pytest
import yaml

from edgewake.tts.config import ConfigError, load_config


def test_repository_config_has_expected_wake_word_and_paths() -> None:
    """The committed configuration must preserve the approved data boundary."""

    repository_root = Path(__file__).resolve().parents[1]
    config = load_config(repository_root / "configs" / "tts.yaml")

    assert config.wake_word == "灵犀灵犀"
    assert config.samples_per_voice == 3000
    assert len(config.voices) == 3
    assert str(config.workspace_root) == (
        r"F:\WorkHome\China-Mobile\workSpace\EdgeWake"
    )
    assert config.models_root == config.workspace_root / "models" / "piper"


def test_relative_workspace_is_rejected(tmp_path: Path) -> None:
    """Runtime artifacts must not silently fall into the Git repository."""

    config_path = tmp_path / "tts.yaml"
    config_path.write_text(
        yaml.safe_dump(
            {
                "version": 1,
                "workspace_root": "relative-data",
                "dataset": {
                    "wake_word": "灵犀灵犀",
                    "slug": "lingxi_lingxi",
                    "samples_per_voice": 1,
                },
                "generation": {
                    "length_scales": [1.0],
                    "noise_scales": [0.667],
                    "noise_scale_ws": [0.8],
                },
                "voices": [
                    {
                        "name": "test",
                        "model": {
                            "url": "https://example.test/test.onnx",
                            "filename": "test.onnx",
                            "minimum_bytes": 1,
                        },
                        "config": {
                            "url": "https://example.test/test.onnx.json",
                            "filename": "test.onnx.json",
                            "minimum_bytes": 1,
                        },
                    }
                ],
            },
            allow_unicode=True,
        ),
        encoding="utf-8",
    )

    with pytest.raises(ConfigError, match="绝对路径"):
        load_config(config_path)


def test_unsafe_model_filename_is_rejected(tmp_path: Path) -> None:
    """Model filenames cannot escape their assigned voice directory."""

    config_path = tmp_path / "tts.yaml"
    config_path.write_text(
        f"""
version: 1
workspace_root: '{tmp_path}'
dataset:
  wake_word: '灵犀灵犀'
  slug: 'lingxi_lingxi'
  samples_per_voice: 1
generation:
  length_scales: [1.0]
  noise_scales: [0.667]
  noise_scale_ws: [0.8]
voices:
  - name: 'test'
    model:
      url: 'https://example.test/test.onnx'
      filename: '../test.onnx'
      minimum_bytes: 1
    config:
      url: 'https://example.test/test.onnx.json'
      filename: 'test.onnx.json'
      minimum_bytes: 1
""",
        encoding="utf-8",
    )

    with pytest.raises(ConfigError, match="安全"):
        load_config(config_path)

