# EdgeWake

Edge wake-word training, optimization and deployment toolkit for Android, ESP32 and embedded Linux.

**面向端侧设备的语音唤醒模型训练、微调与部署工具链。**

EdgeWake 是一个专注于语音唤醒的开源项目，帮助开发者完成唤醒词语料生成、数据处理、模型训练、微调、评估以及端侧部署。

项目面向资源受限设备，计划支持：

- Android
- ESP32 等微控制器
- 嵌入式 Linux 设备

除了语音唤醒，EdgeWake 还将逐步加入 VAD 语音活动检测、AEC 回声消除、语音降噪和自动增益等前端处理能力，提高唤醒模型在噪声、远场和回声环境中的可靠性。

EdgeWake 专注于“小型、快速、可部署”的端侧语音唤醒能力，不以通用语音识别为目标。

## 生成“灵犀灵犀”TTS正样本

### 环境准备

在 Windows PowerShell 中执行：

```powershell
python --version
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
python -m pip install -e ".[dev]"
```

要求Python 3.10或更高版本。

Python依赖安装在仓库内的 `.venv`，Piper模型和生成的WAV保存在
`F:\WorkHome\China-Mobile\workSpace\EdgeWake`。运行路径和生成参数定义在
[`configs/tts.yaml`](configs/tts.yaml)。

### 检查执行计划

该命令不下载模型，也不生成文件：

```powershell
python scripts/generate_tts.py all --samples-per-voice 30 --dry-run
```

### 生成试听样本

下载3个中文音色，并为每个音色生成30条：

```powershell
python scripts/generate_tts.py all --samples-per-voice 30
```

试听并确认发音后，生成配置中设定的每音色3,000条。已有样本会被计入目标数量，
因此该命令可以继续未完成的任务：

```powershell
python scripts/generate_tts.py all
```

如需删除各音色目录中的已有WAV并重新生成：

```powershell
python scripts/generate_tts.py generate --overwrite
```

模型下载失败不会替换已有的有效模型。生成过程先写入临时目录，成功后才把WAV移动到
正式数据目录。

更多第三方项目、版本和许可证信息见
[`docs/third-party.md`](docs/third-party.md)。
