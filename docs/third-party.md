# 第三方依赖

EdgeWake 的 TTS 正样本生成流程使用以下上游项目。

| 项目 | 用途 | 引入方式 | 上游许可证 |
|---|---|---|---|
| [OHF-Voice/piper1-gpl](https://github.com/OHF-Voice/piper1-gpl) | ONNX TTS 推理和音素化 | 通过 `piper-tts` Python 包间接安装 | GPL-3.0 |
| [rhasspy/piper-sample-generator](https://github.com/rhasspy/piper-sample-generator) | 批量生成唤醒词 WAV | `piper-sample-generator==3.2.0` | MIT |
| [rhasspy/piper-voices](https://huggingface.co/rhasspy/piper-voices) | 中文 ONNX 音色模型 | 运行时下载到外部数据目录 | 以仓库及各模型卡为准 |

`piper-sample-generator 3.2.0` 固定声明 `piper-tts==1.3.0`，但当前中文音色使用
`phoneme_type: pinyin`，需要Piper新增的中文拼音支持。因此EdgeWake明确安装
`piper-tts[zh]==1.4.2`，并只使用生成器的标准ONNX路径。

版本3.2.0的上游wheel还存在两个与本项目相关的兼容问题：

1. 它声明了ONNX生成流程不使用的 `webrtcvad`，该包会在Windows/Python 3.12上
   触发本地C++编译。
2. 它无条件导入仅供英文 `.pt` 生成器使用、但没有包含在wheel中的 `piper_train`。
3. 它固定的 `piper-tts 1.3.0` 不支持当前中文音色使用的 `pinyin` 音素类型。

因此，`scripts/setup_tts.py` 使用 `--no-deps` 安装官方生成器wheel，并由EdgeWake
明确安装ONNX路径实际需要的依赖。`edgewake.tts.piper_runner` 只为上游缺失且未使用
的导入提供占位，然后执行上游CLI；它不修改合成算法，并明确拒绝 `.pt` 模型。

第三方源码不会复制到 EdgeWake 仓库。分发软件或音色模型前，应重新核对上游项目和
具体音色模型卡的许可证。
