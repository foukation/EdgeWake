"""Piper sample-generator command construction and execution."""

from __future__ import annotations

from collections.abc import Callable, Sequence
import logging
from pathlib import Path
import subprocess
import sys
import tempfile
from typing import Any

from edgewake.tts.config import TtsConfig, Voice


LOGGER = logging.getLogger(__name__)

CommandRunner = Callable[..., Any]


class GenerationError(RuntimeError):
    """Raised when positive TTS samples cannot be generated safely."""


def build_generation_command(
    config: TtsConfig,
    voice: Voice,
    *,
    sample_count: int,
    output_dir: Path,
    python_executable: str | Path | None = None,
) -> list[str]:
    """Build the official ``piper_sample_generator`` CLI command."""

    if sample_count <= 0:
        raise GenerationError("每个音色的样本数量必须大于0")

    executable = str(python_executable or sys.executable)
    command = [
        executable,
        "-m",
        "piper_sample_generator",
        config.wake_word,
        "--model",
        str(config.voice_model_path(voice)),
        "--max-samples",
        str(sample_count),
        "--length-scales",
        *(_format_numbers(config.generation.length_scales)),
        "--noise-scales",
        *(_format_numbers(config.generation.noise_scales)),
        "--noise-scale-ws",
        *(_format_numbers(config.generation.noise_scale_ws)),
        "--output-dir",
        str(output_dir),
    ]
    return command


def generate_samples(
    config: TtsConfig,
    *,
    samples_per_voice: int | None = None,
    overwrite: bool = False,
    dry_run: bool = False,
    runner: CommandRunner = subprocess.run,
    python_executable: str | Path | None = None,
) -> tuple[tuple[str, ...], ...]:
    """Generate or resume positive WAV samples for every configured voice.

    Existing WAV files count toward the target, so an interrupted run can be
    resumed without deleting valid output. New files are first generated in a
    temporary directory and moved into the final directory only after the
    upstream command succeeds.
    """

    target_count = (
        samples_per_voice
        if samples_per_voice is not None
        else config.samples_per_voice
    )
    if target_count <= 0:
        raise GenerationError("每个音色的样本数量必须大于0")

    commands: list[tuple[str, ...]] = []
    for voice in config.voices:
        output_dir = config.voice_output_dir(voice)
        existing_count = _count_wav_files(output_dir)
        if overwrite:
            existing_count = 0

        missing_count = max(0, target_count - existing_count)
        if missing_count == 0:
            LOGGER.info(
                "音色 %s 已有 %d 条样本，达到目标数量",
                voice.name,
                _count_wav_files(output_dir),
            )
            continue

        if dry_run:
            pending_dir = output_dir / ".edgewake-pending"
            command = build_generation_command(
                config,
                voice,
                sample_count=missing_count,
                output_dir=pending_dir,
                python_executable=python_executable,
            )
            commands.append(tuple(command))
            LOGGER.info("将执行：%s", subprocess.list2cmdline(command))
            continue

        _require_voice_files(config, voice)
        output_dir.mkdir(parents=True, exist_ok=True)
        if overwrite:
            _remove_wav_files(output_dir)

        with tempfile.TemporaryDirectory(
            prefix=".edgewake-pending-",
            dir=output_dir,
        ) as pending_dir_text:
            pending_dir = Path(pending_dir_text)
            command = build_generation_command(
                config,
                voice,
                sample_count=missing_count,
                output_dir=pending_dir,
                python_executable=python_executable,
            )
            commands.append(tuple(command))
            LOGGER.info(
                "生成音色 %s：已有%d条，本次生成%d条",
                voice.name,
                existing_count,
                missing_count,
            )
            try:
                runner(command, check=True)
            except (OSError, subprocess.CalledProcessError) as error:
                raise GenerationError(
                    f"音色 {voice.name} 的TTS生成失败"
                ) from error

            generated_files = _sorted_wav_files(pending_dir)
            if len(generated_files) != missing_count:
                raise GenerationError(
                    f"音色 {voice.name} 预期生成{missing_count}条，"
                    f"实际生成{len(generated_files)}条"
                )

            _move_into_dataset(generated_files, output_dir)

        final_count = _count_wav_files(output_dir)
        LOGGER.info("音色 %s 当前共有 %d 条样本", voice.name, final_count)

    return tuple(commands)


def _require_voice_files(config: TtsConfig, voice: Voice) -> None:
    """Require both Piper files before starting an expensive generation run."""

    missing = [
        path
        for path in (
            config.voice_model_path(voice),
            config.voice_metadata_path(voice),
        )
        if not path.is_file()
    ]
    if missing:
        missing_text = "、".join(str(path) for path in missing)
        raise GenerationError(f"音色 {voice.name} 缺少模型文件：{missing_text}")


def _move_into_dataset(files: Sequence[Path], output_dir: Path) -> None:
    """Move completed WAV files into collision-free numeric filenames."""

    next_index = _next_available_index(output_dir)
    for source in files:
        while True:
            destination = output_dir / f"{next_index:06d}.wav"
            next_index += 1
            if not destination.exists():
                break
        source.replace(destination)


def _next_available_index(output_dir: Path) -> int:
    """Return one more than the largest numeric WAV stem."""

    indices = [
        int(path.stem)
        for path in output_dir.glob("*.wav")
        if path.stem.isdigit()
    ]
    return max(indices, default=-1) + 1


def _count_wav_files(output_dir: Path) -> int:
    """Count direct child WAV files without traversing unrelated directories."""

    return sum(1 for path in output_dir.glob("*.wav") if path.is_file())


def _remove_wav_files(output_dir: Path) -> None:
    """Remove only direct child WAV files after explicit overwrite approval."""

    for path in output_dir.glob("*.wav"):
        if path.is_file():
            path.unlink()


def _sorted_wav_files(directory: Path) -> list[Path]:
    """Sort upstream numeric filenames naturally instead of lexicographically."""

    def sort_key(path: Path) -> tuple[int, int | str]:
        if path.stem.isdigit():
            return (0, int(path.stem))
        return (1, path.name)

    return sorted(
        (path for path in directory.glob("*.wav") if path.is_file()),
        key=sort_key,
    )


def _format_numbers(values: Sequence[float]) -> list[str]:
    """Format numeric CLI arguments without unnecessary trailing zeros."""

    return [format(value, "g") for value in values]
