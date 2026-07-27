# 第三方依赖

EdgeWake 的 TTS 正样本生成流程使用以下上游项目。

| 项目 | 用途 | 引入方式 | 上游许可证 |
|---|---|---|---|
| [OHF-Voice/piper1-gpl](https://github.com/OHF-Voice/piper1-gpl) | ONNX TTS 推理和音素化 | 通过 `piper-tts` Python 包间接安装 | GPL-3.0 |
| [rhasspy/piper-sample-generator](https://github.com/rhasspy/piper-sample-generator) | 批量生成唤醒词 WAV | `piper-sample-generator==3.2.0` | MIT |
| [rhasspy/piper-voices](https://huggingface.co/rhasspy/piper-voices) | 中文 ONNX 音色模型 | 运行时下载到外部数据目录 | 以仓库及各模型卡为准 |

`piper-sample-generator` 当前固定依赖 `piper-tts==1.3.0`。EdgeWake 不额外安装
其他版本，避免破坏上游已声明的兼容关系。

第三方源码不会复制到 EdgeWake 仓库。分发软件或音色模型前，应重新核对上游项目和
具体音色模型卡的许可证。
