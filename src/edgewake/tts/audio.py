"""Lossless-format WAV post-processing for TTS training samples."""

from __future__ import annotations

import math
from pathlib import Path
import tempfile
import wave

import numpy as np


DEFAULT_FADE_MS = 50
DEFAULT_SILENCE_MS = 100


class AudioProcessingError(RuntimeError):
    """Raised when a generated WAV cannot be cleaned safely."""


def clean_wav_tail(
    source: Path,
    destination: Path,
    *,
    fade_ms: int = DEFAULT_FADE_MS,
    silence_ms: int = DEFAULT_SILENCE_MS,
) -> None:
    """Copy ``source`` to ``destination`` with fade-out and trailing silence.

    The source file is never modified. The destination is replaced atomically
    only after a complete WAV has been written.
    """

    if fade_ms <= 0:
        raise AudioProcessingError("淡出时长必须大于0毫秒")
    if silence_ms <= 0:
        raise AudioProcessingError("尾部静音时长必须大于0毫秒")

    try:
        with wave.open(str(source), "rb") as wav_file:
            channels = wav_file.getnchannels()
            sample_width = wav_file.getsampwidth()
            sample_rate = wav_file.getframerate()
            compression = wav_file.getcomptype()
            frame_count = wav_file.getnframes()
            audio_bytes = wav_file.readframes(frame_count)
    except (OSError, EOFError, wave.Error) as error:
        raise AudioProcessingError(f"无法读取WAV文件：{source}") from error

    if channels != 1:
        raise AudioProcessingError(f"WAV必须是单声道：{source}")
    if sample_width != 2:
        raise AudioProcessingError(f"WAV必须是16-bit PCM：{source}")
    if compression != "NONE":
        raise AudioProcessingError(f"WAV必须是未压缩PCM：{source}")
    if sample_rate <= 0 or frame_count <= 0:
        raise AudioProcessingError(f"WAV没有有效音频帧：{source}")

    samples = np.frombuffer(audio_bytes, dtype="<i2").copy()
    fade_frames = min(samples.size, _milliseconds_to_frames(fade_ms, sample_rate))
    if fade_frames == 1:
        fade = np.zeros(1, dtype=np.float64)
    else:
        fade = 0.5 * (
            1.0
            + np.cos(np.linspace(0.0, math.pi, fade_frames, dtype=np.float64))
        )
    faded_tail = np.rint(samples[-fade_frames:].astype(np.float64) * fade)
    samples[-fade_frames:] = np.clip(faded_tail, -32768, 32767).astype("<i2")

    silence_frames = _milliseconds_to_frames(silence_ms, sample_rate)
    cleaned_samples = np.concatenate(
        (samples, np.zeros(silence_frames, dtype="<i2"))
    )

    destination.parent.mkdir(parents=True, exist_ok=True)
    temporary_path: Path | None = None
    try:
        with tempfile.NamedTemporaryFile(
            prefix=f".{destination.name}.",
            suffix=".tmp",
            dir=destination.parent,
            delete=False,
        ) as temporary_file:
            temporary_path = Path(temporary_file.name)

        with wave.open(str(temporary_path), "wb") as wav_file:
            wav_file.setnchannels(channels)
            wav_file.setsampwidth(sample_width)
            wav_file.setframerate(sample_rate)
            wav_file.writeframes(cleaned_samples.tobytes())

        temporary_path.replace(destination)
    except (OSError, wave.Error) as error:
        raise AudioProcessingError(f"无法写入清理后的WAV：{destination}") from error
    finally:
        if temporary_path is not None and temporary_path.exists():
            temporary_path.unlink()


def _milliseconds_to_frames(milliseconds: int, sample_rate: int) -> int:
    """Convert milliseconds to at least one whole audio frame."""

    return max(1, round(sample_rate * milliseconds / 1000))
