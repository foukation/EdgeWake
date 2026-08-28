# 持续语音识别（Persistent ASR）弱网下接收回调延迟分析与优化方案

> 创建日期：2026-05-20  
> 模块：`components/ai_sdk/src/speech_recognition_persistent.cc` + `asr_websocket.cc`  
> 现象：弱网环境下，ASR 接收回调延迟约 **3 秒** 才有消息

---

## 1. 问题背景

在持续语音识别（`SpeechRecognitionPersistent`）模式下，用户反馈：

> 弱网环境下，接收回调有些慢，3 秒左右才有消息回调。

这一延迟在正常网络下不可见，仅在弱网（高丢包 / 高 RTT / 带宽抖动）场景出现。

---

## 2. 接收链路全景

```
[Server] 
   │  TCP / TLS（弱网受影响）
   ▼
esp_websocket_client task  (栈 8192，prio 默认)
   │
   ▼  事件回调（同步，运行在 ws_task 上下文）
AsrWebsocket::EventHandler  (case WEBSOCKET_EVENT_DATA)
   │  message_callback_(data, len, op_code)  ← 同步
   ▼
SpeechRecognitionPersistent::Impl 注册的 lambda
   │  parseMessage(data, len)  ← JSON 反序列化（同步）
   ▼
result_callback_(r)            ← 用户回调（同步）⚠️
```

**关键事实**：从 socket 读取到用户回调全部在 **ws_task 同一栈、同步串行**。

---

## 3. 可能瓶颈与定位优先级

| # | 瓶颈点 | 可能性 | SDK 可优化 | 备注 |
|---|---|---|---|---|
| **A** | TCP Nagle 算法（默认开启，攒 200ms 才发小包） | ⭐⭐⭐⭐ | ✅ 开启 `TCP_NODELAY` | ASR 中间结果包通常 <200B，命中 Nagle 触发条件 |
| **B** | 服务端弱网下批量回包 | ⭐⭐⭐⭐ | ❌ | 服务端可能为节省带宽合并多次中间结果 |
| **C** | TLS / TCP 重传 | ⭐⭐⭐ | ❌ | 物理 / 链路层 |
| **D** | `ping_interval_ms=30000` 检测网络异常太慢 | ⭐⭐ | ✅ 降到 5-10s | 不直接影响回调延迟，但影响断网感知 |
| **E** | 回调链同步执行阻塞 ws_task | ⭐⭐ | ✅ 解耦到独立任务 | 用户回调中若做重活会拖慢下一条消息接收 |
| **F** | WS task 优先级被高优先级任务长期抢占 | ⭐ | ✅ 提升 prio | 需具体测才能确认 |

---

## 4. 关键配置现状（speech_recognition_persistent.cc）

```cpp
// 第 135-137 行附近
config.network_timeout_ms = 30000;   // 30 秒
config.ping_interval_ms   = 30000;   // 30 秒
config.buffer_size        = 8192;
```

`asr_websocket.cc` 中：

```cpp
ws_config.buffer_size        = config.buffer_size;       // 8192
ws_config.network_timeout_ms = config.network_timeout_ms;// 30000
ws_config.task_stack         = 8192;
ws_config.disable_auto_reconnect = true;
ws_config.ping_interval_sec  = config.ping_interval_ms / 1000;  // 30
ws_config.crt_bundle_attach  = esp_crt_bundle_attach;
ws_config.skip_cert_common_name_check = false;
```

**注意**：当前未显式开启 `TCP_NODELAY`，依赖 ESP-IDF 底层 transport 默认配置。

---

## 5. 优化方案（按收益 / 成本排序）

### ✅ 方案 1：开启 `TCP_NODELAY`（必做，低风险）

**做法**：在 `AsrWebsocket::connect()` 中，`esp_websocket_client_start` 之后通过 ESP-IDF transport API 设置底层 socket 选项；或在 `esp_websocket_client_config_t` 中检查 v5.4.3 是否提供直接字段。

**ESP-IDF v5.4 参考**：
- 部分版本可通过 `ws_config.transport = WEBSOCKET_TRANSPORT_OVER_TCP` + 自定义 transport
- 或在连接建立后获取底层 sockfd 设置 `TCP_NODELAY`

**收益**：消除 200ms ~ 数百毫秒级的 Nagle 攒包延迟，对 ASR 中间小包尤为明显。  
**风险**：极低（仅影响下行小包合并发送策略）。

---

### ✅ 方案 2：缩短 `ping_interval_ms`（建议，低风险）

**做法**：把 `speech_recognition_persistent.cc` 中
```cpp
config.ping_interval_ms = 30000;
```
改为
```cpp
config.ping_interval_ms = 10000;  // 或 5000
```

**收益**：
- 弱网 / 断网时更快感知连接死亡（30s → 10s）
- 也能让中间路由的 NAT 映射保持活跃

**风险**：极低（每 10s 多一个 ping 帧，带宽可忽略）。

---

### 🟡 方案 3：接收路径解耦（中等改动）

**做法**：把 `parseMessage` + `result_callback_` 从 ws_task 上下文剥离，引入消息分发任务：

```
ws_task → enqueue raw msg (FreeRTOS queue) → dispatcher_task → parseMessage → user callback
```

**收益**：
- 用户回调慢不再阻塞下一条 ASR 消息接收
- 接收吞吐 / 实时性更稳定

**风险**：中等（涉及消息内存所有权、生命周期、queue 满策略）。建议先做方案 1+2 看效果，再决定是否做 3。

---

### 🟡 方案 4：缩短 `network_timeout_ms`（可选）

**做法**：从 30000 降到 10000 ms，配合应用层重连。  
**收益**：弱网卡住时更快失败 + 重连。  
**风险**：低，但需配合应用层重连策略。

---

### ⚪ 方案 5：提升 WS task 优先级（暂不建议）

**做法**：`ws_config.task_prio = ...`  
**说明**：仅在确认被其他任务抢占时才做。当前未观察到证据，**暂不优化**。

---

## 6. 待用户确认的信息

为了精准定位"3 秒延迟"来源，需要进一步信息：

1. **延迟测量口径**：
   - 服务端发送时间戳 vs 客户端 `result_callback_` 收到时间？
   - 还是"说完话 → 看到识别结果"的端到端时间？
   - 这两者差别很大，后者包含服务端推理时间（SDK 不可控）

2. **延迟分布**：
   - 仅首包慢，还是每条消息都慢？
   - 首包慢 → 多半服务端推理 + Nagle 攒包
   - 每条都慢 → 多半 Nagle / 任务调度

3. **建议抓取的日志**（弱网下持续识别一段）：
   - 每条 ASR 结果带毫秒时间戳
   - 同时记录 ESP-IDF websocket transport 层的接收时间
   - `vTaskGetRunTimeStats` 看 ws_task CPU 占用

---

## 7. 后续行动建议

**短期（已建议执行）**：
- [ ] **方案 1：TCP_NODELAY**（待实施）
- [ ] **方案 2：ping_interval 30s → 10s**（待实施）
- [ ] 弱网下抓详细时间戳日志验证收益

**中期（按需）**：
- [ ] 方案 3：接收路径解耦到独立任务
- [ ] 方案 4：network_timeout_ms 30s → 10s

**长期**：
- [ ] 与服务端确认是否存在批量回包行为，必要时协商协议优化

---

## 8. 关联文件

- [components/ai_sdk/src/speech_recognition_persistent.cc](../components/ai_sdk/src/speech_recognition_persistent.cc)
- [components/ai_sdk/src/asr_websocket.cc](../components/ai_sdk/src/asr_websocket.cc)
- [components/ai_sdk/src/include/asr_websocket.h](../components/ai_sdk/src/include/asr_websocket.h)
