# ASR模块WS对外接口文档

# ASR 模块协议详细说明

## 1\. 接口路径：

```Plaintext
ws://36.140.170.160:15700/ws/asr/realtime
```

---

## 2\. 统一上行协议（客户端 → ASR）

### 2\.1 消息类型

|type|说明|
|---|---|
|`start`|开始识别，可带参数|
||音频文件|
|`finish`|音频发送结束|
|`cancel`|取消本次识别|

### 2\.2 JSON 结构

**开始（start）**

```JSON
{
  "type": "start",
  "data": {
    "vendor": "baidu",
    "format": "pcm",
    "sample": 16000,
    "cuid": "device-xxx",
    "dialog_request_id": "optional",
    "language": "zh-CN"
  }
}
```

|字段|类型|必填|说明|
|---|---|---|---|
|type|string|是|固定 `"start"`|
|data|object|否|扩展参数|
|data\.vendor|string|是|厂商："baidu", "tencent", "aliyun", "volc"|
|data\.format|string|是|音频格式，如 `pcm`/ `opus`|
|data\.sample|int|是|采样率，如 16000|
|data\.cuid|string|否|设备/用户唯一标识|
|data\.dialog\_request\_id|string|否|会话请求 ID|
|data\.language|string|否|语种|

**结束（finish）**

```JSON
{ "type": "finish" }
```

**取消（cancel）**

```JSON
{ "type": "cancel" }
```

### 2\.3 二进制帧

- 在 `start` 之后、`finish` 或 `cancel` 之前，客户端发送**二进制 PCM 音频帧**。

- 建议：16k 单声道 16bit，每帧 160ms（约 5120 字节）或按厂商要求分包。

---

## 3\. 统一下行协议（ASR → 客户端）

### 3\.1 消息类型

|type|说明|
|---|---|
|`ready`|识别就绪，可带 trace\_id，**暂无此次下发，需要可以放开**|
|`RESULT`|识别结果|
|`heartbeat`|心跳，可选|
|`error`|错误|

### 3\.2 JSON 结构说明

**就绪（ready）**

```JSON
{
  "type": "ready",
  "trace_id": "dd9358c6-bc37-4f3d-a254-9bd333f25cda"
}
```

|字段|类型|说明|
|---|---|---|
|type|string|固定 `"ready"`|
|trace\_id|string|会话/连接唯一标识|

**中间结果（data\.is\_final = false）**

```JSON
{
  "type": "RESULT",
  "trace_id": "b5a0fb92-8caf-401c-80e7-37a074573480",
  "data": {
    "text": "上海天气怎么样",
    "is_final": false,
    "vendor": "baidu"
  }
}
```

|字段|类型|说明|
|---|---|---|
|type|string|固定 `"RESULT"`|
|trace\_id|string|会话标识|
|data\.text|string|当前句识别中的文本|
|data\.is\_final|string|false 表示中间识别结果|
|data\.vendor|string|当前选择的供应商|

**最终结果（data\.is\_final = true）**

```JSON
{
  "type": "RESULT",
  "trace_id": "b5a0fb92-8caf-401c-80e7-37a074573480",
  "data": {
    "text": "上海天气怎么样啊？",
    "is_final": true,
    "vendor": "baidu"
  }
}
```

|字段|类型|说明|
|---|---|---|
|type|string|固定 `"RESULT"`|
|trace\_id|string|会话标识|
|data\.text|string|当前句识别中的文本|
|data\.is\_final|string|false 表示中间识别结果|
|data\.vendor|string|当前选择的供应商|
|data\.start\_time|int|句首时间（毫秒），可选|
|data\.end\_time|int|句尾时间（毫秒），可选|
|data\.index|int|句序号（从 0 递增），可选|

**心跳（heartbeat）**

```JSON
{ "type": "heartbeat" }
```

**错误（error）**

```JSON
{
  "type": "error",
  "err_no": -3004,
  "err_msg": "asr authentication failed",
  "trace_id": "b5a0fb92-8caf-401c-80e7-37a074573480",
  "data": {
  }
}
```

|字段|类型|说明|
|---|---|---|
|type|string|固定 `"error"`|
|err\_no|int|错误码|
|err\_msg|string|错误信息|
|sn|string|会话标识，可选|

---

## 4\. 接入示例

### 4\.1 python（pcm格式）

```Python
import asyncio
import json
import logging

import pytest
import websockets

@pytest.mark.asyncio
async def test_realtime_asr():
    uri = "ws://36.140.170.160:15700/ws/asr/realtime"

    async with websockets.connect(uri) as websocket:
        # 发送配置
        await websocket.send(json.dumps({
            "type": "start",
            "data": {
                "vendor": "baidu",
                "format": "pcm",
                "sample": 16000,
                "cuid": "device-xxx",
                "dialog_request_id": "optional",
                "language": "zh-CN"
            }
        }))
        # 用于存储接收到的所有回复
        received_responses = []
        # 创建一个事件，用于通知何时可以关闭连接
        all_responses_received = asyncio.Event()

        # 启动接收回复的协程
        async def receive_responses():
            try:
                print("\n")
                while True:
                    response = await websocket.recv()
                    received_responses.append(response)
                    response = json.loads(response)  # 格式化结果
                    # 如果需要打印响应内容，取消下面的注释
                    print(f"收到的回复: {response}")
                    # 假设收到某些标志消息时，表示消息接收完毕，可以结束
                    data = response.get("data") or {}
                    is_final = data.get("is_final", False)

                    if is_final:
                        print("接收到所有消息，准备结束接收。")
                        all_responses_received.set()  # 设置事件，通知关闭连接
            except websockets.exceptions.ConnectionClosed:
                print("连接已关闭，结束接收.")

        # 启动接收响应的任务
        asyncio.create_task(receive_responses())
        pcm_path = "./1773334911403_asrchat.wav"
        # 发送音频数据
        with open(pcm_path, "rb") as f:
            while chunk := f.read(1024):
                await websocket.send(chunk)
                await asyncio.sleep(0.01)  # 模拟实时传输,稍微增加一点儿延迟

        # 发送结束信号
        await websocket.send(json.dumps({"type": "finish"}))
        # 等待接收到所有消息的事件
        await all_responses_received.wait()
        # 发送结束信号
        await websocket.send(json.dumps({"type": "cancel"}))
```

### 4\.2 python（pcm \-\> 裸opus帧）

```Python

@pytest.mark.asyncio
async def test_realtime_asr_opus_stream111():
    uri = "ws://36.140.170.160:15700/ws/asr/realtime"

    async with websockets.connect(uri) as websocket:
        pcm_path = "./1773334911403_asrchat.pcm" 

        logger.info(f"使用 PCM 文件: {pcm_path}，将通过 opuslib 实时转为裸 Opus 流")

        # 告诉服务端：我要发 opus 格式
        await websocket.send(json.dumps({
            "type": "start",
            "sample_rate": 16000,
            "data": {
                "vendor": "volc",
                "format": "opus",
                "sample": 16000,
                "cuid": "device-xxx",
                "dialog_request_id": "test-opus-001",
                "language": "zh-CN",
            }
        }))

        received_responses = []
        all_responses_received = asyncio.Event()
        timeout_seconds = 30

        async def receive_responses():
            try:
                while True:
                    response = await asyncio.wait_for(websocket.recv(), timeout=timeout_seconds)
                    received_responses.append(response)
                    response = json.loads(response)
                    logger.info(f"收到的回复：{response}")

                    msg_type = response.get("type", "")

                    if msg_type == "RESULT":
                        if (response.get("data") or {}).get("is_final"):
                            all_responses_received.set()
                            break
                    elif msg_type in ("END", "error"):
                        all_responses_received.set()
                        break

            except Exception as e:
                logger.warning(f"接收异常: {e}")
                import traceback
                traceback.print_exc()
            finally:
                all_responses_received.set()

        receive_task = asyncio.create_task(receive_responses())

        # 使用 opuslib 编码 PCM → 裸 Opus 帧 
        encoder = opuslib.Encoder(16000, 1, opuslib.APPLICATION_VOIP)
        frame_size_samples = 320  # 20ms @ 16kHz

        chunk_count = 0
        with open(pcm_path, "rb") as f:
            while True:
                # 读取一帧 PCM (640字节)
                pcm_frame = f.read(frame_size_samples * 2)
                if not pcm_frame:
                    break

                # 如果最后一帧不足，补零
                if len(pcm_frame) < frame_size_samples * 2:
                    pcm_frame += b'\x00' * (frame_size_samples * 2 - len(pcm_frame))

                # 编码为裸 Opus 帧
                opus_frame = encoder.encode(pcm_frame, frame_size_samples)

                # 发送裸 Opus 帧
                await websocket.send(opus_frame)
                chunk_count += 1

                if chunk_count % 50 == 0:
                    logger.debug(f"已发送 {chunk_count} 个 Opus 帧")

                await asyncio.sleep(0.02)  # 模拟实时（20ms）

        logger.info(f"Opus发送完成，共 {chunk_count} 个块")

        await websocket.send(json.dumps({"type": "finish"}))

        try:
            await asyncio.wait_for(all_responses_received.wait(), timeout=timeout_seconds + 5)
        except asyncio.TimeoutError:
            logger.warning("等待响应超时")

        logger.info(f"测试完成，共收到 {len(received_responses)} 条消息")
```

