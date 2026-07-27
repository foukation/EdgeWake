"""Tests for idempotent and failure-safe model downloads."""

from __future__ import annotations

from io import BytesIO
from pathlib import Path
from urllib.request import Request

import pytest

from edgewake.tts.config import TtsConfig
from edgewake.tts.downloader import ModelDownloadError, download_models


def test_downloads_and_reuses_valid_artifacts(tts_config: TtsConfig) -> None:
    """A second run must reuse valid local files without network access."""

    payloads = {
        "https://example.test/voice.onnx": b"onnx-data",
        "https://example.test/voice.onnx.json": b"{}",
    }
    opened_urls: list[str] = []

    def opener(request: Request) -> BytesIO:
        opened_urls.append(request.full_url)
        return BytesIO(payloads[request.full_url])

    destinations = download_models(tts_config, opener=opener)
    assert len(opened_urls) == 2
    assert all(path.is_file() for path in destinations)

    def forbidden_opener(request: Request) -> BytesIO:
        raise AssertionError(f"不应再次下载：{request.full_url}")

    download_models(tts_config, opener=forbidden_opener)


def test_failed_validation_removes_partial_file(
    tts_config: TtsConfig,
) -> None:
    """An undersized download must not leave a reusable partial model."""

    voice = tts_config.voices[0]

    def opener(request: Request) -> BytesIO:
        return BytesIO(b"x")

    with pytest.raises(ModelDownloadError, match="校验失败"):
        download_models(tts_config, opener=opener)

    destination = tts_config.voice_model_path(voice)
    part_path = destination.with_name(f"{destination.name}.part")
    assert not destination.exists()
    assert not part_path.exists()


def test_dry_run_does_not_create_workspace(tts_config: TtsConfig) -> None:
    """Dry-run is strictly read-only."""

    destinations = download_models(tts_config, dry_run=True)

    assert len(destinations) == 2
    assert not tts_config.models_root.exists()

