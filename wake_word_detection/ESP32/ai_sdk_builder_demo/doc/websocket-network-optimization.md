# WebSocket 网络异常优化方案

> **问题**：ASR 语音识别 WebSocket 在持续流式传输约 10 秒后出现写入失败
> **日期**：2026-03-25
> **涉及项目**：`ai_sdk_builder_demo`

---

## 1. 错误现象

```
E (15855) transport_ws: Error transport_poll_write
E (15855) websocket_client: esp_transport_write() returned 0,
          transport_error=ESP_OK, tls_error_code=0, tls_flags=0, errno=0
```

ASR 语音识别在运行约 **10 秒后**，WebSocket 写入失败，连接被异常断开。
`esp_transport_write()` 返回 0，且所有错误码为空，含义是 **TCP 发送缓冲区满**，`select()` 等待超时，socket 不可写。

---

## 2. 根因分析

### 2.1 数据流管道

```
AudioInput (5120 bytes / 160ms)
  → sendAudio() → sendBinary()
    → esp_websocket_client_send_bin() [8KB WS 缓冲区]
      → TLS 加密 [4KB 输出记录限制]  ← ⚠️ 瓶颈1
        → TCP 写入 [5760 byte 发送缓冲区]  ← ⚠️ 瓶颈2
          → WiFi TX [默认省电模式]  ← ⚠️ 瓶颈3
```

### 2.2 七个关键问题（按影响排序）

#### 问题 1：TCP 发送缓冲区太小（最关键）

- **当前值**：`TCP_SND_BUF = 5760 bytes`（仅 4 × MSS）
- **每次发送**：5120 字节音频 + WebSocket 帧头(~14 字节) + TLS 开销(~29 字节) ≈ **5200 字节**
- **问题**：单次音频帧几乎**填满整个 TCP 发送缓冲区**，如果上一个 TCP 段未被服务器 ACK，缓冲区立刻满

#### 问题 2：TLS 输出记录大小太小

- **当前值**：`MBEDTLS_SSL_OUT_CONTENT_LEN = 4096`
- **问题**：5120 字节的 WebSocket 帧需要分成 **2 个 TLS 记录**（4096 + 1024），**双倍 TCP 写入次数**，加剧缓冲区满的概率

#### 问题 3：WiFi 省电模式未关闭

- **当前**：默认 `WIFI_PS_MIN_MODEM`，WiFi 会在 beacon 间隔休眠
- **问题**：射频休眠导致 **TCP ACK 延迟到达**，发送缓冲区占满时间更长

#### 问题 4：WiFi 缓冲区参数偏小

```
CONFIG_ESP_WIFI_STATIC_RX_BUFFER_NUM=3    ← 偏小
CONFIG_ESP_WIFI_DYNAMIC_RX_BUFFER_NUM=6   ← 偏小
CONFIG_ESP_WIFI_RX_BA_WIN=3               ← 偏小
```

#### 问题 5：无背压(backpressure)机制

- `sendAudio()` **丢弃了 `sendBinary()` 的返回值**，不管写入成功失败，AudioInput 继续以固定速率发送
- 代码位置：`asr_intelligent_dialogue.cc` 第 480-499 行

#### 问题 6：`sendBinary()` 超时仅 1 秒

- 代码位置：`asr_websocket.cc` 第 186-221 行
- 1 秒超时同时用于**锁获取 + 网络写入**，如果有 ping/pong 占用锁，实际写入时间更短

#### 问题 7：错误后无恢复逻辑

- WebSocket 写入失败后，底层库直接 `abort_connection()`，会话立即结束，无法恢复

### 2.3 为什么是 ~10 秒后出错？

- 音频持续发送速率：**32 KB/s**（16kHz × 16bit × mono）
- 10 秒 ≈ 传输 **320 KB** 数据
- 网络微小波动会导致 1-2 个 TCP 段延迟 ACK
- 5760 字节的 TCP 缓冲区**只能容纳约 1 帧**的数据，完全没有余量
- 一旦有一次 ACK 延迟 > 160ms（一帧间隔），缓冲区就满了

---

## 3. 优化方案

### 3.1 sdkconfig 参数优化

修改 `sdkconfig.defaults` 或 `sdkconfig.defaults.esp32s3`：

| 参数 | 当前值 | 建议值 | 原因 |
|------|--------|--------|------|
| `CONFIG_LWIP_TCP_SND_BUF_DEFAULT` | 5760 | **11520**（8×MSS） | 增加发送缓冲区，容纳 2 帧音频 |
| `CONFIG_LWIP_TCP_WND_DEFAULT` | 5760 | **11520** | 配合发送缓冲区增大 |
| `CONFIG_MBEDTLS_SSL_OUT_CONTENT_LEN` | 4096 | **6144** | 单次 TLS 记录容纳完整音频帧 |
| `CONFIG_ESP_WIFI_STATIC_RX_BUFFER_NUM` | 3 | **6** | 增加 WiFi 接收缓冲区 |
| `CONFIG_ESP_WIFI_DYNAMIC_RX_BUFFER_NUM` | 6 | **12** | 增加动态接收缓冲区 |

> **内存增加估算**：约 +12KB RAM（TCP +5.7K, TLS +2K, WiFi +4K）

### 3.2 代码优化

#### 3.2a 关闭 WiFi 省电模式

在 `main.cc` WiFi 连接成功后添加：

```c
#include <esp_wifi.h>

// WiFi 连接成功后添加
esp_wifi_set_ps(WIFI_PS_NONE);
```

#### 3.2b `sendBinary()` 超时增大

文件：`asr_websocket.cc`

```c
// 从 1000ms 增大到 3000ms
esp_websocket_client_send_bin(client_, (const char*)data, len,
    pdMS_TO_TICKS(3000));
```

#### 3.2c `sendAudio()` 检查返回值

文件：`asr_intelligent_dialogue.cc`

```c
bool ok = websocket_.sendBinary(data, len);
if (!ok) {
    ESP_LOGW(TAG, "Audio send failed, will retry next frame");
    // 可选：短暂延迟后重试
}
```

### 3.3 WiFi 断线自动重连（另一个独立问题）

修改 `main.cc`，保留事件监听器，WiFi 断线后自动重连：

**改动 1**：删除第 109-113 行中的事件注销代码，只保留 EventGroup 清理：

```c
// 原代码（删除前两行）：
// esp_event_handler_instance_unregister(IP_EVENT, IP_EVENT_STA_GOT_IP, instance_got_ip);
// esp_event_handler_instance_unregister(WIFI_EVENT, ESP_EVENT_ANY_ID, instance_any_id);
vEventGroupDelete(s_wifi_event_group);
s_wifi_event_group = nullptr;
```

**改动 2**：修改 `wifi_event_handler` 断连处理逻辑：

```c
} else if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_DISCONNECTED) {
    if (s_wifi_event_group != nullptr) {
        // 首次连接阶段：重试 10 次
        if (s_retry_count < WIFI_MAX_RETRY) {
            esp_wifi_connect();
            s_retry_count++;
            ESP_LOGW(TAG, "WiFi disconnected, retry %d/%d ...",
                     s_retry_count, WIFI_MAX_RETRY);
        } else {
            xEventGroupSetBits(s_wifi_event_group, WIFI_FAIL_BIT);
            ESP_LOGE(TAG, "WiFi connection failed after %d retries",
                     WIFI_MAX_RETRY);
        }
    } else {
        // 运行阶段：无限重连
        s_retry_count++;
        ESP_LOGW(TAG, "WiFi lost, reconnecting (attempt %d) ...",
                 s_retry_count);
        esp_wifi_connect();
    }
}
```

同时 `IP_EVENT_STA_GOT_IP` 处理兼容运行阶段：

```c
} else if (event_base == IP_EVENT && event_id == IP_EVENT_STA_GOT_IP) {
    ip_event_got_ip_t* event = (ip_event_got_ip_t*)event_data;
    ESP_LOGI(TAG, "WiFi connected, IP: " IPSTR, IP2STR(&event->ip_info.ip));
    s_retry_count = 0;
    if (s_wifi_event_group != nullptr) {
        xEventGroupSetBits(s_wifi_event_group, WIFI_CONNECTED_BIT);
    }
}
```

### 3.4 可选 - 音频发送策略优化

减小单次发送量，降低缓冲区压力：

```
kSendThreshold: 5120 → 2560 (80ms)
kPcmChunkBytes: 5120 → 2560
kPcmChunkDelayMs: 160 → 80
```

总吞吐量不变（仍是 32KB/s），但单次写入更小，TCP 缓冲区更从容。

---

## 4. 优先级总结

| 优先级 | 改动 | 效果 |
|--------|------|------|
| **P0** | 增大 `TCP_SND_BUF` 到 11520 | 直接解决缓冲区满问题 |
| **P0** | `esp_wifi_set_ps(WIFI_PS_NONE)` | 消除 WiFi 休眠导致的 ACK 延迟 |
| **P1** | 增大 `MBEDTLS_SSL_OUT_CONTENT_LEN` 到 6144 | 减少 TLS 分片 |
| **P1** | `sendBinary()` 超时增大到 3s | 给网络更多恢复时间 |
| **P2** | 增大 WiFi 缓冲区参数 | 改善 WiFi 层吞吐 |
| **P2** | 音频帧拆小为 2560 字节 | 降低单次写入压力 |
| **P3** | `sendAudio()` 检查返回值 | 提供错误反馈能力 |
| **P3** | WiFi 断线自动重连 | 独立问题，改善长期运行稳定性 |

---

## 5. 涉及文件清单

| 文件 | 改动类型 |
|------|----------|
| `ai_sdk_builder_demo/sdkconfig.defaults` 或 `sdkconfig.defaults.esp32s3` | sdkconfig 参数 |
| `ai_sdk_builder_demo/main/main.cc` | WiFi 省电 + 断线重连 |
| `ai_sdk_builder/components/ai_sdk/src/asr_websocket.cc` | sendBinary 超时 |
| `ai_sdk_builder/components/ai_sdk/src/asr_intelligent_dialogue.cc` | sendAudio 返回值检查 |
| `ai_sdk_builder/components/ai_sdk/src/audio/audio_input.cc` | 可选：音频帧大小 |
