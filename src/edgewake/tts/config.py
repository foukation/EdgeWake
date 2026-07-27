"""Configuration loading and validation for Piper TTS generation."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import re
from typing import Any, Mapping, Sequence
from urllib.parse import urlparse

import yaml


class ConfigError(ValueError):
    """Raised when the TTS configuration is missing or invalid."""


@dataclass(frozen=True)
class RemoteFile:
    """A downloadable model artifact and its basic integrity constraint."""

    url: str
    filename: str
    minimum_bytes: int


@dataclass(frozen=True)
class Voice:
    """One Piper voice and its required ONNX artifacts."""

    name: str
    model: RemoteFile
    metadata: RemoteFile


@dataclass(frozen=True)
class GenerationSettings:
    """Parameters passed to ``piper-sample-generator``."""

    length_scales: tuple[float, ...]
    noise_scales: tuple[float, ...]
    noise_scale_ws: tuple[float, ...]


@dataclass(frozen=True)
class TtsConfig:
    """Validated application configuration with derived runtime paths."""

    workspace_root: Path
    wake_word: str
    dataset_slug: str
    samples_per_voice: int
    generation: GenerationSettings
    voices: tuple[Voice, ...]

    @property
    def models_root(self) -> Path:
        """Return the root directory for Piper voice models."""

        return self.workspace_root / "models" / "piper"

    @property
    def dataset_root(self) -> Path:
        """Return the root directory for raw positive TTS samples."""

        return (
            self.workspace_root
            / "datasets"
            / "tts"
            / self.dataset_slug
            / "raw"
        )

    @property
    def cleaned_dataset_root(self) -> Path:
        """Return the root directory for cleaned positive TTS samples."""

        return (
            self.workspace_root
            / "datasets"
            / "tts"
            / self.dataset_slug
            / "cleaned"
        )

    def voice_model_dir(self, voice: Voice) -> Path:
        """Return the model directory assigned to ``voice``."""

        return self.models_root / voice.name

    def voice_model_path(self, voice: Voice) -> Path:
        """Return the ONNX model path assigned to ``voice``."""

        return self.voice_model_dir(voice) / voice.model.filename

    def voice_metadata_path(self, voice: Voice) -> Path:
        """Return the ONNX JSON metadata path assigned to ``voice``."""

        return self.voice_model_dir(voice) / voice.metadata.filename

    def voice_output_dir(self, voice: Voice) -> Path:
        """Return the raw WAV output directory assigned to ``voice``."""

        return self.dataset_root / voice.name

    def voice_cleaned_dir(self, voice: Voice) -> Path:
        """Return the cleaned WAV output directory assigned to ``voice``."""

        return self.cleaned_dataset_root / voice.name


def load_config(path: str | Path) -> TtsConfig:
    """Load and validate a YAML TTS configuration.

    The loader rejects relative runtime roots and unsafe filenames so generated
    artifacts cannot accidentally be written into the source repository.
    """

    config_path = Path(path)
    try:
        with config_path.open("r", encoding="utf-8") as config_file:
            raw = yaml.safe_load(config_file)
    except OSError as error:
        raise ConfigError(f"无法读取配置文件：{config_path}") from error
    except yaml.YAMLError as error:
        raise ConfigError(f"YAML格式错误：{config_path}") from error

    root = _require_mapping(raw, "配置根节点")
    version = root.get("version")
    if version != 1:
        raise ConfigError(f"不支持的配置版本：{version!r}，当前仅支持版本1")

    workspace_text = _require_string(root.get("workspace_root"), "workspace_root")
    workspace_root = Path(workspace_text).expanduser()
    if not workspace_root.is_absolute():
        raise ConfigError("workspace_root必须是绝对路径")

    dataset = _require_mapping(root.get("dataset"), "dataset")
    wake_word = _require_string(dataset.get("wake_word"), "dataset.wake_word")
    dataset_slug = _require_string(dataset.get("slug"), "dataset.slug")
    if re.fullmatch(r"[A-Za-z0-9_-]+", dataset_slug) is None:
        raise ConfigError("dataset.slug只能包含字母、数字、下划线和连字符")

    samples_per_voice = _require_positive_int(
        dataset.get("samples_per_voice"),
        "dataset.samples_per_voice",
    )

    generation_raw = _require_mapping(root.get("generation"), "generation")
    generation = GenerationSettings(
        length_scales=_require_positive_floats(
            generation_raw.get("length_scales"),
            "generation.length_scales",
        ),
        noise_scales=_require_positive_floats(
            generation_raw.get("noise_scales"),
            "generation.noise_scales",
        ),
        noise_scale_ws=_require_positive_floats(
            generation_raw.get("noise_scale_ws"),
            "generation.noise_scale_ws",
        ),
    )

    voices_raw = _require_sequence(root.get("voices"), "voices")
    if not voices_raw:
        raise ConfigError("voices至少需要配置一个音色")

    voices = tuple(
        _parse_voice(item, f"voices[{index}]")
        for index, item in enumerate(voices_raw)
    )
    names = [voice.name for voice in voices]
    if len(names) != len(set(names)):
        raise ConfigError("voices中的音色名称不能重复")

    return TtsConfig(
        workspace_root=workspace_root,
        wake_word=wake_word,
        dataset_slug=dataset_slug,
        samples_per_voice=samples_per_voice,
        generation=generation,
        voices=voices,
    )


def _parse_voice(raw: Any, field: str) -> Voice:
    """Parse one voice mapping."""

    value = _require_mapping(raw, field)
    name = _require_string(value.get("name"), f"{field}.name")
    if re.fullmatch(r"[A-Za-z0-9_-]+", name) is None:
        raise ConfigError(f"{field}.name包含不安全字符")

    return Voice(
        name=name,
        model=_parse_remote_file(value.get("model"), f"{field}.model", ".onnx"),
        metadata=_parse_remote_file(
            value.get("config"),
            f"{field}.config",
            ".onnx.json",
        ),
    )


def _parse_remote_file(raw: Any, field: str, suffix: str) -> RemoteFile:
    """Parse and validate one remote model artifact."""

    value = _require_mapping(raw, field)
    url = _require_string(value.get("url"), f"{field}.url")
    parsed_url = urlparse(url)
    if parsed_url.scheme not in {"http", "https"} or not parsed_url.netloc:
        raise ConfigError(f"{field}.url必须是有效的HTTP(S)地址")

    filename = _require_string(value.get("filename"), f"{field}.filename")
    if Path(filename).name != filename or not filename.endswith(suffix):
        raise ConfigError(f"{field}.filename必须是安全的{suffix}文件名")

    minimum_bytes = _require_positive_int(
        value.get("minimum_bytes"),
        f"{field}.minimum_bytes",
    )
    return RemoteFile(
        url=url,
        filename=filename,
        minimum_bytes=minimum_bytes,
    )


def _require_mapping(value: Any, field: str) -> Mapping[str, Any]:
    """Return ``value`` as a mapping or raise a precise configuration error."""

    if not isinstance(value, Mapping):
        raise ConfigError(f"{field}必须是对象")
    return value


def _require_sequence(value: Any, field: str) -> Sequence[Any]:
    """Return ``value`` as a non-string sequence."""

    if not isinstance(value, Sequence) or isinstance(value, (str, bytes)):
        raise ConfigError(f"{field}必须是数组")
    return value


def _require_string(value: Any, field: str) -> str:
    """Return a non-empty trimmed string."""

    if not isinstance(value, str) or not value.strip():
        raise ConfigError(f"{field}必须是非空字符串")
    return value.strip()


def _require_positive_int(value: Any, field: str) -> int:
    """Return a positive integer while rejecting booleans."""

    if isinstance(value, bool) or not isinstance(value, int) or value <= 0:
        raise ConfigError(f"{field}必须是正整数")
    return value


def _require_positive_floats(value: Any, field: str) -> tuple[float, ...]:
    """Return a non-empty tuple of finite positive numbers."""

    items = _require_sequence(value, field)
    if not items:
        raise ConfigError(f"{field}不能为空")

    numbers: list[float] = []
    for item in items:
        if isinstance(item, bool) or not isinstance(item, (int, float)):
            raise ConfigError(f"{field}只能包含数字")
        number = float(item)
        if number <= 0 or number == float("inf") or number != number:
            raise ConfigError(f"{field}只能包含有限正数")
        numbers.append(number)
    return tuple(numbers)
