"""Reliable downloads for Piper ONNX voice artifacts."""

from __future__ import annotations

from collections.abc import Callable
from contextlib import AbstractContextManager
import json
import logging
from pathlib import Path
import shutil
from typing import BinaryIO
from urllib.request import Request, urlopen

from edgewake.tts.config import RemoteFile, TtsConfig, Voice


LOGGER = logging.getLogger(__name__)

OpenUrl = Callable[[Request], AbstractContextManager[BinaryIO]]


class ModelDownloadError(RuntimeError):
    """Raised when a Piper model artifact cannot be downloaded or validated."""


def download_models(
    config: TtsConfig,
    *,
    force: bool = False,
    dry_run: bool = False,
    opener: OpenUrl | None = None,
) -> tuple[Path, ...]:
    """Download all configured Piper voice artifacts.

    Downloads are written to sibling ``.part`` files first. A completed file is
    atomically moved into place only after basic validation, so an interrupted
    request does not corrupt a previously valid model.
    """

    open_url = opener or _open_url
    destinations: list[Path] = []

    for voice in config.voices:
        artifacts = (
            (voice.model, config.voice_model_path(voice), False),
            (voice.metadata, config.voice_metadata_path(voice), True),
        )
        for artifact, destination, is_json in artifacts:
            destinations.append(destination)
            if dry_run:
                LOGGER.info("将下载 %s -> %s", artifact.url, destination)
                continue

            _download_artifact(
                voice=voice,
                artifact=artifact,
                destination=destination,
                is_json=is_json,
                force=force,
                opener=open_url,
            )

    return tuple(destinations)


def _download_artifact(
    *,
    voice: Voice,
    artifact: RemoteFile,
    destination: Path,
    is_json: bool,
    force: bool,
    opener: OpenUrl,
) -> None:
    """Download one artifact while preserving an existing valid file."""

    if not force and _is_valid_artifact(destination, artifact, is_json=is_json):
        LOGGER.info("复用已存在的模型文件：%s", destination)
        return

    destination.parent.mkdir(parents=True, exist_ok=True)
    part_path = destination.with_name(f"{destination.name}.part")
    if part_path.exists():
        # This is always a precisely derived sibling temporary file.
        part_path.unlink()

    request = Request(
        artifact.url,
        headers={"User-Agent": "EdgeWake/0.1 (+Piper model downloader)"},
    )
    LOGGER.info("下载音色 %s：%s", voice.name, artifact.filename)

    try:
        with opener(request) as response, part_path.open("wb") as output_file:
            shutil.copyfileobj(response, output_file, length=1024 * 1024)

        if not _is_valid_artifact(part_path, artifact, is_json=is_json):
            raise ModelDownloadError(
                f"下载文件校验失败：{artifact.filename}"
            )

        # Path.replace uses an atomic replace on the same filesystem.
        part_path.replace(destination)
        LOGGER.info("模型文件已保存：%s", destination)
    except Exception as error:
        if part_path.exists():
            part_path.unlink()
        if isinstance(error, ModelDownloadError):
            raise
        raise ModelDownloadError(
            f"下载音色 {voice.name} 的 {artifact.filename} 失败"
        ) from error


def _is_valid_artifact(
    path: Path,
    artifact: RemoteFile,
    *,
    is_json: bool,
) -> bool:
    """Check minimum size and, for metadata, valid JSON syntax."""

    try:
        if not path.is_file() or path.stat().st_size < artifact.minimum_bytes:
            return False
        if is_json:
            with path.open("r", encoding="utf-8") as metadata_file:
                metadata = json.load(metadata_file)
            return isinstance(metadata, dict)
    except (OSError, UnicodeError, json.JSONDecodeError):
        return False
    return True


def _open_url(request: Request) -> AbstractContextManager[BinaryIO]:
    """Open a model URL with a bounded socket timeout."""

    return urlopen(request, timeout=60)
