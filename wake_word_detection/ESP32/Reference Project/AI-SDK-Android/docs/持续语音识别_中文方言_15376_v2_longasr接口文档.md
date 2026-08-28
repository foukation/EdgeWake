# 百度公有云实时语音识别接口文档

> 平台 WebSocket 路径：`/app-ws/v2/long-asr`  
> 上游：百度公有云实时语音识别 WebSocket API

---

## 1. 概述

实时语音识别采用 WebSocket 全双工连接，边上传音频边获取识别结果。适用于长句语音输入、音视频字幕、直播质检、会议记录等场景。

---

## 2. 连接

### 2.1 WebSocket URI

```
wss://{platform-host}/app-ws/v2/long-asr?deviceNo={deviceNo}&deviceId={deviceId}&productId={productId}&productKey={productKey}&sign={sign}&ts={ts}&sn={sn}
```

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| deviceNo | 是 | 设备号 |
| deviceId | 是 | 设备 ID |
| productId | 是 | 产品 ID |
| productKey | 是 | 产品 Key |
| ts | 是 | 毫秒时间戳，用于签名校验 |
| sign | 是 | 签名，算法与 ASR 其它 WebSocket 接口一致 |
| sn | 否 | 请求追踪 ID，建议 UUID；未传时平台自动生成 |

**握手校验：**

1. `sign` + `ts` 签名校验失败 → 拒绝连接  
2. 产品 `apiList` 未包含 `publicCloudAsr` → 拒绝连接  
3. 校验通过后，平台连接百度上游并建立双向转发


## 3. 客户端协议

连接成功后，客户端按以下顺序发送/接收数据（与百度官方协议一致，经平台透明转发）。

### 3.1 发送开始参数帧（Text / JSON）

```json
{
  "type": "START",
  "data": {
    "dev_pid": 15376,
    "format": "pcm",
    "sample": 16000
  }
}
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| type | string | 是 | 固定 `START` |
| data.dev_pid | int | 否 | 识别模型，默认 15376 |
| data.format | string | 否 | 固定 `pcm`，默认由平台补齐 |
| data.sample | int | 否 | 采样率，固定 16000 |

### 3.2 发送音频数据帧（Binary）

- 内容为 PCM 二进制音频  
- 除最后一帧外，每帧建议 20–200 ms  
- 离线文件建议按实时速度发送（发一帧后 sleep 对应时长）

音频格式：**16 kHz / 单声道 / 16-bit 小端 PCM**。

### 3.3 发送结束帧（Text / JSON）

```json
{
  "type": "FINISH"
}
```

### 3.4 发送取消帧（Text / JSON，可选）

```json
{
  "type": "CANCEL"
}
```

取消与结束不同：取消表示不再需要识别结果，上游会迅速关闭连接。

---

## 4. 接收识别结果

服务端返回 **Text / JSON** 帧（平台原样转发）。

### 4.1 临时结果（MID_TEXT）

```json
{
  "err_no": 0,
  "err_msg": "OK",
  "type": "MID_TEXT",
  "result": "北京天气怎",
  "log_id": 45677785,
  "sn": "399427ce-e999-11e9-94c8-fa163e4e6064_ws_2"
}
```

### 4.2 最终结果（FIN_TEXT）

```json
{
  "type": "FIN_TEXT",
  "result": "北京天气怎么样",
  "start_time": 53220,
  "end_time": 73340,
  "err_no": 0,
  "err_msg": "OK",
  "log_id": 45677785,
  "sn": "399427ce-e999-11e9-94c8-fa163e4e6064_ws_2"
}
```

| 字段 | 说明 |
| --- | --- |
| type | `MID_TEXT` 临时结果；`FIN_TEXT` 最终结果或单句报错 |
| result | 识别文本 |
| start_time / end_time | 一句话起止时间（毫秒），仅 `FIN_TEXT` 有 |
| err_no | 0 表示成功，非 0 见百度错误码文档 |
| err_msg | 错误描述 |
| log_id / sn | 排查日志用 |

**说明：**

- 单句报错（如 `err_no=-3005`）不影响其它句子继续识别  
- 整次请求是否结束，以服务端是否关闭 WebSocket 为准  
- 建议单句不超过 30 秒
