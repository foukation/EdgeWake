# Piper TTS 正样本生成设计

## 目标

为唤醒词“灵犀灵犀”生成可供 microWakeWord 后续处理的 TTS 正样本。
首版不生成相似词负样本，也不执行模型训练。

## 上游项目

- `OHF-Voice/piper1-gpl`：底层 TTS 引擎，对应 Python 包 `piper-tts`。
- `rhasspy/piper-sample-generator`：批量 TTS 样本生成器。
- `rhasspy/piper-voices`：中文 Piper ONNX 音色模型来源。

EdgeWake 通过官方 Python 包使用上游能力，不复制或修改上游源码。

## 目录边界

代码、配置和测试保存在 Git 仓库：

```text
E:\github\EdgeWake
├── configs
├── docs
├── scripts
├── src
└── tests
```

虚拟环境保存在 `E:\github\EdgeWake\.venv`，并通过 `.gitignore` 排除。

大文件保存在工作数据目录：

```text
F:\WorkHome\China-Mobile\workSpace\EdgeWake
├── models\piper
└── datasets\tts\lingxi_lingxi\raw
```

模型、生成音频和临时文件不提交到 Git。

## 组件

- YAML 配置：定义唤醒词、工作目录、三个中文音色、下载地址和生成参数。
- 配置加载器：解析配置、校验字段并解析绝对路径。
- 模型下载器：下载 `.onnx` 和 `.onnx.json`，使用临时文件和原子替换，避免留下不完整模型。
- 样本生成器：调用 `python -m piper_sample_generator`，按音色分别输出 WAV。
- CLI 脚本：提供 `download`、`generate` 和 `all` 命令，并允许覆盖每个音色的样本数量。

## 默认参数

- 唤醒词：`灵犀灵犀`
- 音色：`chaowen-medium`、`huayan-medium`、`xiao_ya-medium`
- 每个音色：3,000 条
- 总计：9,000 条正样本
- 试听用法：通过命令行覆盖为每个音色 30 条
- 语速：`0.8`、`0.9`、`1.0`、`1.1`、`1.2`
- 随机度：`0.5`、`0.667`、`0.8`、`0.9`
- 音素时长随机度：`0.6`、`0.8`、`1.0`

## 可靠性和安全

- 下载前创建目标目录，已存在且非空的模型文件默认复用。
- 下载写入 `.part` 文件，成功后再替换正式文件。
- 已有WAV计入目标数量，任务中断后可继续生成缺少的部分。
- 显式传入 `--overwrite` 时，仅删除对应音色目录的直接子级WAV，不删除其他文件。
- CLI 支持 `--dry-run`，用于检查将要执行的命令而不下载或生成。
- 错误信息包含失败的音色、文件或子进程返回码。

## 测试

单元测试覆盖配置解析、目录推导、模型文件复用、下载失败清理以及生成命令构造。
测试使用临时目录和模拟对象，不访问网络、不下载模型、不实际合成音频。
