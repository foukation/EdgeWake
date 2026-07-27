"""Tests for WAV tail cleanup."""

from __future__ import annotations

from pathlib import Path
import wave

import numpy as np
import pytest

from edgewake.tts.audio import AudioProcessingError, clean_wav_tail


def test_clean_wav_tail_preserves_source_and_appends_zero_silence(
    tmp_path: Path,
) -> None:
    """Cleanup fades only the tail and leaves the raw source untouched."""

    source = tmp_path / "raw.wav"
    destination = tmp_path / "cleaned.wav"
    sample_rate = 1000
    original = np.full(200, 1000, dtype="<i2")
    _write_wav(source, original, sample_rate=sample_rate)
    source_before = source.read_bytes()

    clean_wav_tail(source, destination, fade_ms=50, silence_ms=100)

    cleaned, cleaned_rate = _read_wav(destination)
    assert source.read_bytes() == source_before
    assert cleaned_rate == sample_rate
    assert cleaned.size == 300
    np.testing.assert_array_equal(cleaned[:150], original[:150])
    assert cleaned[150] == 1000
    assert cleaned[199] == 0
    np.testing.assert_array_equal(cleaned[-100:], np.zeros(100, dtype="<i2"))


def test_clean_wav_tail_handles_audio_shorter_than_fade(tmp_path: Path) -> None:
    """A very short valid clip is faded without indexing outside its frames."""

    source = tmp_path / "short.wav"
    destination = tmp_path / "cleaned.wav"
    _write_wav(source, np.full(10, 500, dtype="<i2"), sample_rate=1000)

    clean_wav_tail(source, destination, fade_ms=50, silence_ms=100)

    cleaned, _ = _read_wav(destination)
    assert cleaned.size == 110
    assert cleaned[0] == 500
    assert cleaned[9] == 0
    assert np.count_nonzero(cleaned[-100:]) == 0


def test_clean_wav_tail_rejects_stereo_audio(tmp_path: Path) -> None:
    """Unexpected channel layouts fail instead of producing corrupt output."""

    source = tmp_path / "stereo.wav"
    with wave.open(str(source), "wb") as wav_file:
        wav_file.setnchannels(2)
        wav_file.setsampwidth(2)
        wav_file.setframerate(22050)
        wav_file.writeframes(np.zeros(20, dtype="<i2").tobytes())

    with pytest.raises(AudioProcessingError, match="单声道"):
        clean_wav_tail(source, tmp_path / "cleaned.wav")


def test_clean_wav_tail_zeros_a_single_frame_clip(tmp_path: Path) -> None:
    """Even the smallest valid WAV reaches zero before appended silence."""

    source = tmp_path / "single.wav"
    destination = tmp_path / "cleaned.wav"
    _write_wav(source, np.array([500], dtype="<i2"), sample_rate=1000)

    clean_wav_tail(source, destination, fade_ms=50, silence_ms=100)

    cleaned, _ = _read_wav(destination)
    assert cleaned[0] == 0
    assert np.count_nonzero(cleaned) == 0


def _write_wav(path: Path, samples: np.ndarray, *, sample_rate: int) -> None:
    """Write a mono 16-bit PCM fixture."""

    with wave.open(str(path), "wb") as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        wav_file.setframerate(sample_rate)
        wav_file.writeframes(samples.astype("<i2").tobytes())


def _read_wav(path: Path) -> tuple[np.ndarray, int]:
    """Read samples and rate from a mono test WAV."""

    with wave.open(str(path), "rb") as wav_file:
        sample_rate = wav_file.getframerate()
        samples = np.frombuffer(
            wav_file.readframes(wav_file.getnframes()),
            dtype="<i2",
        ).copy()
    return samples, sample_rate
