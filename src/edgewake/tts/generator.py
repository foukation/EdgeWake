"""Piper sample-generator command construction and execution."""

from __future__ import annotations

from collections.abc import Callable, Sequence
import logging
import os
from pathlib import Path
import subprocess
import sys
import tempfile
from typing import Any

from edgewake.tts.audio import AudioProcessingError, clean_wav_tail
from edgewake.tts.config import TtsConfig, Voice


LOGGER = logging.getLogger(__name__)

CommandRunner = Callable[..., Any]
SampleCleaner = Callable[[Path, Path], None]


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
    """Build the command for the official generator through our ONNX adapter."""

    if sample_count <= 0:
        raise GenerationError("每个音色的样本数量必须大于0")

    executable = str(python_executable or sys.executable)
    command = [
        executable,
        "-m",
        "edgewake.tts.piper_runner",
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
    cleaner: SampleCleaner = clean_wav_tail,
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
    parameter_period = _parameter_combination_count(config)
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
            if not dry_run:
                _clean_voice_samples(
                    config,
                    voice,
                    overwrite=False,
                    cleaner=cleaner,
                )
            continue

        parameter_offset = 0 if overwrite else existing_count % parameter_period
        upstream_count = missing_count + parameter_offset
        if dry_run:
            pending_dir = output_dir / ".edgewake-pending"
            command = build_generation_command(
                config,
                voice,
                sample_count=upstream_count,
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
            _remove_wav_files(config.voice_cleaned_dir(voice))

        with tempfile.TemporaryDirectory(
            prefix=".edgewake-pending-",
            dir=output_dir,
        ) as pending_dir_text:
            pending_dir = Path(pending_dir_text)
            command = build_generation_command(
                config,
                voice,
                sample_count=upstream_count,
                output_dir=pending_dir,
                python_executable=python_executable,
            )
            commands.append(tuple(command))
            LOGGER.info(
                "生成音色 %s：已有%d条，本次保留%d条，参数偏移%d条",
                voice.name,
                existing_count,
                missing_count,
                parameter_offset,
            )
            child_environment = os.environ.copy()
            # Keep Chinese phonemizer and Hugging Face runtime assets beside the
            # configured models instead of allowing them into the Git checkout
            # or the user's global cache.
            child_environment["HF_HOME"] = str(
                config.workspace_root / "cache" / "huggingface"
            )
            child_environment["HF_HUB_DISABLE_SYMLINKS_WARNING"] = "1"
            try:
                runner(
                    command,
                    check=True,
                    cwd=config.workspace_root,
                    env=child_environment,
                )
            except (OSError, subprocess.CalledProcessError) as error:
                raise GenerationError(
                    f"音色 {voice.name} 的TTS生成失败"
                ) from error

            generated_files = _sorted_wav_files(pending_dir)
            if len(generated_files) != upstream_count:
                raise GenerationError(
                    f"音色 {voice.name} 预期生成{upstream_count}条，"
                    f"实际生成{len(generated_files)}条"
                )

            _move_into_dataset(generated_files[parameter_offset:], output_dir)

        final_count = _count_wav_files(output_dir)
        LOGGER.info("音色 %s 当前共有 %d 条样本", voice.name, final_count)
        _clean_voice_samples(
            config,
            voice,
            overwrite=False,
            cleaner=cleaner,
        )

    return tuple(commands)


def clean_samples(
    config: TtsConfig,
    *,
    overwrite: bool = False,
    dry_run: bool = False,
    cleaner: SampleCleaner = clean_wav_tail,
) -> tuple[Path, ...]:
    """Create cleaned copies for all configured raw WAV samples."""

    destinations: list[Path] = []
    for voice in config.voices:
        if overwrite and not dry_run:
            _remove_wav_files(config.voice_cleaned_dir(voice))
        destinations.extend(
            _clean_voice_samples(
                config,
                voice,
                overwrite=overwrite,
                dry_run=dry_run,
                cleaner=cleaner,
            )
        )
    return tuple(destinations)


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


def _clean_voice_samples(
    config: TtsConfig,
    voice: Voice,
    *,
    overwrite: bool,
    cleaner: SampleCleaner,
    dry_run: bool = False,
) -> list[Path]:
    """Synchronize one voice's raw WAV files into its cleaned directory."""

    raw_dir = config.voice_output_dir(voice)
    cleaned_dir = config.voice_cleaned_dir(voice)
    destinations: list[Path] = []
    for source in _sorted_wav_files(raw_dir):
        destination = cleaned_dir / source.name
        if destination.exists() and not overwrite:
            continue
        destinations.append(destination)
        if dry_run:
            LOGGER.info("将清理：%s -> %s", source, destination)
            continue
        cleaned_dir.mkdir(parents=True, exist_ok=True)
        try:
            cleaner(source, destination)
        except AudioProcessingError as error:
            raise GenerationError(f"WAV尾部清理失败：{source}") from error
        LOGGER.info("已生成清理样本：%s", destination)
    return destinations


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


def _parameter_combination_count(config: TtsConfig) -> int:
    """Return the upstream parameter cycle length for one voice."""

    return (
        len(config.generation.length_scales)
        * len(config.generation.noise_scales)
        * len(config.generation.noise_scale_ws)
    )


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
