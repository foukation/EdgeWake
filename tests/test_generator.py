"""Tests for Piper command construction and resumable generation."""

from __future__ import annotations

from pathlib import Path
from typing import Any

from edgewake.tts.config import TtsConfig
from edgewake.tts.generator import (
    build_generation_command,
    clean_samples,
    generate_samples,
)


def test_build_command_contains_approved_text_and_parameters(
    tts_config: TtsConfig,
) -> None:
    """The upstream CLI receives the approved wake word and model path."""

    voice = tts_config.voices[0]
    command = build_generation_command(
        tts_config,
        voice,
        sample_count=30,
        output_dir=Path("pending"),
        python_executable="python-test",
    )

    assert command[:4] == [
        "python-test",
        "-m",
        "edgewake.tts.piper_runner",
        "灵犀灵犀",
    ]
    assert command[command.index("--max-samples") + 1] == "30"
    assert command[command.index("--model") + 1] == str(
        tts_config.voice_model_path(voice)
    )
    assert command[command.index("--length-scales") + 1 :][:3] == [
        "0.8",
        "1",
        "1.2",
    ]


def test_generation_resumes_from_existing_wav_files(
    tts_config: TtsConfig,
) -> None:
    """Only the missing number of samples is generated on a resumed run."""

    voice = tts_config.voices[0]
    _create_model_files(tts_config)
    output_dir = tts_config.voice_output_dir(voice)
    output_dir.mkdir(parents=True)
    (output_dir / "000000.wav").write_bytes(b"existing-0")
    (output_dir / "000001.wav").write_bytes(b"existing-1")

    requested_counts: list[int] = []

    def runner(command: list[str], **_: Any) -> None:
        count = int(command[command.index("--max-samples") + 1])
        pending_dir = Path(command[command.index("--output-dir") + 1])
        requested_counts.append(count)
        for index in range(count):
            (pending_dir / f"{index}.wav").write_bytes(
                f"new-{index}".encode()
            )

    generate_samples(tts_config, runner=runner, cleaner=_copy_cleaner)

    assert requested_counts == [3]
    assert len(list(output_dir.glob("*.wav"))) == 3
    assert (output_dir / "000000.wav").read_bytes() == b"existing-0"
    assert (output_dir / "000002.wav").read_bytes() == b"new-2"
    assert (
        tts_config.voice_cleaned_dir(voice) / "000002.wav"
    ).read_bytes() == b"cleaned:new-2"


def test_dry_run_does_not_require_models_or_create_output(
    tts_config: TtsConfig,
) -> None:
    """Dry-run constructs commands while keeping the workspace unchanged."""

    commands = generate_samples(
        tts_config,
        samples_per_voice=30,
        dry_run=True,
        python_executable="python-test",
    )

    assert len(commands) == 1
    assert "--max-samples" in commands[0]
    assert commands[0][commands[0].index("--max-samples") + 1] == "30"
    assert not tts_config.dataset_root.exists()


def test_overwrite_replaces_only_wav_files(tts_config: TtsConfig) -> None:
    """Explicit overwrite removes WAVs while preserving unrelated files."""

    voice = tts_config.voices[0]
    _create_model_files(tts_config)
    output_dir = tts_config.voice_output_dir(voice)
    output_dir.mkdir(parents=True)
    (output_dir / "000000.wav").write_bytes(b"old")
    marker = output_dir / "notes.txt"
    marker.write_text("keep", encoding="utf-8")

    def runner(command: list[str], **_: Any) -> None:
        count = int(command[command.index("--max-samples") + 1])
        pending_dir = Path(command[command.index("--output-dir") + 1])
        for index in range(count):
            (pending_dir / f"{index}.wav").write_bytes(b"new")

    generate_samples(
        tts_config,
        samples_per_voice=2,
        overwrite=True,
        runner=runner,
        cleaner=_copy_cleaner,
    )

    assert len(list(output_dir.glob("*.wav"))) == 2
    assert marker.read_text(encoding="utf-8") == "keep"


def test_clean_samples_preserves_raw_and_skips_existing_cleaned(
    tts_config: TtsConfig,
) -> None:
    """Cleanup mirrors raw names without changing an existing clean result."""

    voice = tts_config.voices[0]
    raw_dir = tts_config.voice_output_dir(voice)
    cleaned_dir = tts_config.voice_cleaned_dir(voice)
    raw_dir.mkdir(parents=True)
    cleaned_dir.mkdir(parents=True)
    (raw_dir / "000000.wav").write_bytes(b"raw-0")
    (raw_dir / "000001.wav").write_bytes(b"raw-1")
    (cleaned_dir / "000000.wav").write_bytes(b"keep")

    created = clean_samples(tts_config, cleaner=_copy_cleaner)

    assert created == (cleaned_dir / "000001.wav",)
    assert (raw_dir / "000001.wav").read_bytes() == b"raw-1"
    assert (cleaned_dir / "000000.wav").read_bytes() == b"keep"
    assert (cleaned_dir / "000001.wav").read_bytes() == b"cleaned:raw-1"


def _create_model_files(config: TtsConfig) -> None:
    """Create minimal placeholders; synthesis itself is mocked in these tests."""

    voice = config.voices[0]
    model_path = config.voice_model_path(voice)
    metadata_path = config.voice_metadata_path(voice)
    model_path.parent.mkdir(parents=True)
    model_path.write_bytes(b"onnx")
    metadata_path.write_text("{}", encoding="utf-8")


def _copy_cleaner(source: Path, destination: Path) -> None:
    """Write a recognizable cleaned fixture without parsing fake WAV bytes."""

    destination.write_bytes(b"cleaned:" + source.read_bytes())
