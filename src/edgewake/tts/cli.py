"""Command-line interface for downloading Piper voices and generating TTS."""

from __future__ import annotations

import argparse
import logging
from pathlib import Path
import sys

from edgewake.tts.config import ConfigError, load_config
from edgewake.tts.downloader import ModelDownloadError, download_models
from edgewake.tts.generator import GenerationError, generate_samples


LOGGER = logging.getLogger(__name__)
DEFAULT_CONFIG = Path(__file__).resolve().parents[3] / "configs" / "tts.yaml"


def build_parser() -> argparse.ArgumentParser:
    """Create the CLI parser shared by the script and installed entry point."""

    parser = argparse.ArgumentParser(
        prog="edgewake-tts",
        description="下载Piper中文音色并生成“灵犀灵犀”TTS正样本。",
    )
    parser.add_argument(
        "--config",
        type=Path,
        default=DEFAULT_CONFIG,
        help=f"YAML配置文件（默认：{DEFAULT_CONFIG}）",
    )
    parser.add_argument(
        "--verbose",
        action="store_true",
        help="输出详细日志",
    )

    commands = parser.add_subparsers(dest="command", required=True)

    download = commands.add_parser("download", help="仅下载Piper音色模型")
    download.add_argument(
        "--force",
        action="store_true",
        help="即使模型已存在也重新下载",
    )
    download.add_argument(
        "--dry-run",
        action="store_true",
        help="只显示计划，不访问网络或写入文件",
    )

    generate = commands.add_parser("generate", help="仅生成TTS正样本")
    _add_generation_options(generate)

    all_command = commands.add_parser("all", help="先下载模型，再生成TTS")
    all_command.add_argument(
        "--force-download",
        action="store_true",
        help="即使模型已存在也重新下载",
    )
    _add_generation_options(all_command)
    return parser


def main(argv: list[str] | None = None) -> int:
    """Run the EdgeWake TTS command and return a process exit code."""

    parser = build_parser()
    args = parser.parse_args(argv)
    logging.basicConfig(
        level=logging.DEBUG if args.verbose else logging.INFO,
        format="%(levelname)s %(message)s",
    )

    try:
        config = load_config(args.config)
        LOGGER.info("唤醒词：%s", config.wake_word)
        LOGGER.info("运行数据目录：%s", config.workspace_root)

        if args.command == "download":
            download_models(
                config,
                force=args.force,
                dry_run=args.dry_run,
            )
        elif args.command == "generate":
            generate_samples(
                config,
                samples_per_voice=args.samples_per_voice,
                overwrite=args.overwrite,
                dry_run=args.dry_run,
            )
        elif args.command == "all":
            download_models(
                config,
                force=args.force_download,
                dry_run=args.dry_run,
            )
            generate_samples(
                config,
                samples_per_voice=args.samples_per_voice,
                overwrite=args.overwrite,
                dry_run=args.dry_run,
            )
        else:  # pragma: no cover - argparse restricts this branch.
            parser.error(f"未知命令：{args.command}")

    except (ConfigError, ModelDownloadError, GenerationError) as error:
        LOGGER.error("%s", error)
        return 1
    except KeyboardInterrupt:
        LOGGER.error("操作已由用户中断")
        return 130

    LOGGER.info("操作完成")
    return 0


def _add_generation_options(parser: argparse.ArgumentParser) -> None:
    """Add options shared by ``generate`` and ``all``."""

    parser.add_argument(
        "--samples-per-voice",
        type=_positive_int,
        help="覆盖配置中的每音色样本数量，例如试听时使用30",
    )
    parser.add_argument(
        "--overwrite",
        action="store_true",
        help="删除各音色目录中的已有WAV后重新生成",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="只显示计划，不下载模型或生成音频",
    )


def _positive_int(value: str) -> int:
    """Parse a strictly positive integer for argparse."""

    try:
        number = int(value)
    except ValueError as error:
        raise argparse.ArgumentTypeError("必须是正整数") from error
    if number <= 0:
        raise argparse.ArgumentTypeError("必须是正整数")
    return number


if __name__ == "__main__":
    sys.exit(main())

