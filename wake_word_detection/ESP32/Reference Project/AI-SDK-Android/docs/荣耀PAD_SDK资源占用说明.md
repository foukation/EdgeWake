# 荣耀 PAD SDK 资源占用说明

| 项目 | 结果 |
|---|---|
| SDK 包名 | `com.cmdc.ai.assist` |
| 核心类名 | `AIAssistantManager`、`AIAssistConfig`、`GateWay`、`AIFoundationKit`、`ASRTranslation`、`ASRIntelligentDialogue` |
| RAM 占用 | 初始化阶段实测增量约 `5.1 MB` |
| ROM 占用 | 当前全量 AAR 约 `1.61 MB`；解包后约 `3.35 MB` |
| 未来 2 年增长预估 | 不内置端侧大模型/大资源包前提下，ROM 预计控制在 `5 MB` 内；初始化 RAM 预计控制在 `10 MB` 内；|

可直接对外口径：

```text
SDK 包名为 com.cmdc.ai.assist，核心入口类为 AIAssistantManager。当前 SDK 初始化 RAM 实测增量约 5.1 MB；ROM 当前 AAR 包约 1.61 MB，解包后约 3.35 MB。未来 2 年如不内置端侧大模型和大型资源，预计 ROM 控制在 5 MB 内，初始化 RAM 控制在 10 MB 内，语音/实时对话运行态建议按 50 MB 内预留。
```
