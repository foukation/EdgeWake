# 深度分析报告：`asr.start()` 首次失败后无法再次调用

> **分析日期**：2026-05-09  
> **问题来源**：厂商反馈 — `asr.start` 第一次调用如果失败，后面就调不起来（已排除 DRAM 空间不足问题）  
> **分析范围**：`ai_sdk_builder_demo/main/test_voice_assistant_persistent.cpp` 及相关 SDK 源码  
> **结论**：发现 **3 个根本原因**，其中 2 个可独立触发该 Bug，不修改任何文件

---

## 一、问题复现路径

```
va_persistent_loop_task()
  └─ va_persistent_session()          ← 第 1 次调用
       ├─ asr.setCallbacks(...)       ← 捕获局部变量 got_close/got_error/result_count（引用）
       ├─ asr.start() → 失败          ← 触发 Bug
       ├─ vQueueDelete(asr_queue)
       └─ return                      ← 局部变量销毁 ← ★ 危险窗口开始
  vTaskDelay(2000ms)                  ← ★ 2 秒危险窗口
  └─ va_persistent_session()          ← 第 2 次调用，start() 也失败
```

`asr` 是通过 `AIAssistantManager::getInstance().speechRecognitionPersistentHelp()` 获取的**全局单例引用**，跨会话共享。因此第一次会话注册的回调（含悬空引用）在第二次会话开始之前**一直有效**。

---

## 二、根本原因分析

---

### 🔴 根本原因 1：悬空回调引用 → 堆/栈腐败（最高可能性）

#### 代码位置

`ai_sdk_builder_demo/main/test_voice_assistant_persistent.cpp`，`asr.setCallbacks(...)` 部分：

```cpp
// on_error 和 on_close 均以引用方式捕获局部变量
[&got_error](int code, const std::string& msg) {
    got_error.store(true);         // ← &got_error 是局部变量的引用
},
[&got_close, &result_count]() {
    got_close.store(true);         // ← 悬空引用！session 返回后失效
    ...result_count.load()         // ← 悬空引用！
}
```

#### 触发时序

| 步骤 | 主任务 | WebSocket 事件循环任务 |
|------|--------|----------------------|
| 1 | `asr.start()` 失败，调用 `websocket_.disconnect()` | — |
| 2 | `esp_websocket_client_close()` **阻塞最多 1 秒** | ERROR 事件已处理，**DISCONNECTED 事件排队中** |
| 3 | `esp_websocket_client_destroy()` 销毁 WebSocket 内部任务 | DISCONNECTED 已在事件循环队列，**`destroy()` 不清空事件队列！** |
| 4 | `disconnect()` 返回，`start()` 返回 `false` | — |
| 5 | `va_persistent_session()` 返回，**`got_close`、`got_error`、`result_count` 全部销毁** | — |
| 6 | `vTaskDelay(2000ms)` | **事件循环处理残留 DISCONNECTED → `close_callback_()` → `got_close.store(true)` → 写入已销毁的栈帧 → 堆/栈腐败！** |
| 7 | 第 2 次 `va_persistent_session()` 开始 | FreeRTOS 内部结构可能已腐败，`asr.start()` 失败 |

#### 原理说明

ESP-IDF 的 WebSocket 客户端通过 `esp_event_post()` 将事件投递到**独立的事件循环任务（event loop task）队列**中。`esp_websocket_client_destroy()` 销毁 WebSocket 内部任务本身，但**不会清空事件循环任务中已排队的事件**。这是 ESP-IDF 架构层面的已知行为。

一次连接失败可能产生 ERROR + DISCONNECTED 两个事件。ERROR 先被处理（唤醒主任务），DISCONNECTED 仍在队列中。`destroy()` 后，事件循环任务仍会处理 DISCONNECTED，触发此前注册的 `close_callback_`，而此时 `va_persistent_session()` 已经返回，回调中引用的局部变量已成为悬空指针。

---

### 🔴 根本原因 2：`is_connected_` 被残留事件污染 → 后续 `connect()` 永久拒绝（中高可能性）

#### 代码位置 1：`asr_websocket.cc`，`connect()` 函数

```cpp
bool AsrWebsocket::connect(const AsrWebsocketConfig& config) {
    ...
    if (is_connected_ || is_connecting_) {
        ESP_LOGW(TAG, "Already connected or connecting, disconnect first");
        xSemaphoreGive(connection_mutex_);
        return false;  // ← 直接返回，不清理 is_connected_ / is_connecting_！
    }
```

#### 代码位置 2：`speech_recognition_persistent.cc`，`start()` 函数

```cpp
if (!websocket_.connect(config)) {
    if (error_callback_) {
        error_callback_(1, "Failed to initiate WebSocket connection");
    }
    return false;  // ← connect() 失败时，没有调用 disconnect() 清理脏状态！
}
```

#### 死锁链（以超时场景为例）

```
首次 start() 等待 30 秒超时
  → disconnect() 调用 destroy()
  → TCP 握手恰好完成，CONNECTED 事件进入事件循环队列
  → destroy() 销毁内部任务，但 CONNECTED 已在队列中
  → （2 秒延迟期间）事件循环处理 CONNECTED：
       EventHandler: is_connected_ = true  ← ★ 污染！
  → 第 2 次 connect():
       is_connected_ 为 true → "Already connected or connecting" → 返回 false
  → speech_recognition_persistent::start():
       connect() 返回 false → 返回 false（无 disconnect() 调用）
       is_connected_ 永远是 true
  → 所有后续 start() 调用均失败（无限循环）
```

#### 关键缺陷

`disconnect()` 函数中有以下早返回：

```cpp
void AsrWebsocket::disconnect() {
    if (!client_) {
        return;  // ← 若 client_ 为 null，但 is_connected_ 被残留事件污染为 true，
    }            //   此处直接返回，脏状态永远无法清理！
    ...
}
```

当 `connect()` 返回 false（因为 `is_connected_` 为 true），`speech_recognition_persistent` 的 `start()` 不调用 `disconnect()`。而 `disconnect()` 本身在 `client_` 为 null 时也不清理 `is_connected_`。两处防御缺失共同导致状态永久锁死。

---

### 🟡 根本原因 3：`connection_semaphore_` 被残留事件重复触发（次要，加剧前两个问题）

```
ERROR 事件：
  → xSemaphoreGive(connection_semaphore_)  ← start() 被唤醒
  → start() 调用 disconnect()
    └─ close() 阻塞 → 触发 CLOSED/DISCONNECTED 事件（排队中）
    └─ destroy() 返回
  → （DISCONNECTED 事件处理）→ xSemaphoreGive() ← 信号量 +1（残留）
```

第 2 次 `start()` 开头虽然做了清零：

```cpp
xSemaphoreTake(connection_semaphore_, 0);  // 清除残留
```

但如果残留 DISCONNECTED 事件在清零之后、新的 `connect()` 完成之前触发，会导致信号量提前 +1，使 `start()` 提前从等待中唤醒，判断 `is_connected_` 为 false，然后对新连接调用 `disconnect()`，终止了一个本可成功的连接。

---

## 三、关键代码对照表

| 文件 | 代码位置 | 问题 |
|------|---------|------|
| `test_voice_assistant_persistent.cpp` | `setCallbacks` 中 `[&got_close]` `[&got_error]` | 引用捕获局部变量，session 结束后成为悬空引用，残留事件调用时产生 UB |
| `asr_websocket.cc` | `disconnect()` 不接受 `connection_mutex_` | 与 `connect()` 之间无锁同步保护 |
| `asr_websocket.cc` | `connect()` 的 `is_connected_ \|\| is_connecting_` 检查 | 无法区分"真实连接中"和"残留事件污染" |
| `asr_websocket.cc` | `disconnect()` 的 `if (!client_) return;` | `client_` 为 null 时不清理 `is_connected_`/`is_connecting_`，导致残留污染永久化 |
| `speech_recognition_persistent.cc` | `connect()` 返回 false 时直接 `return false` | 未调用 `disconnect()` 清理脏状态 |

---

## 四、验证方法（不修改代码）

在首次 `asr.start()` 失败后，观察以下日志：

| 日志内容 | 确认的根本原因 |
|---------|--------------|
| `"Already connected or connecting, disconnect first"` | 根本原因 2（`is_connected_` 被污染） |
| 设备 panic / `heap_caps_check_integrity` 失败 / `assert` 崩溃 | 根本原因 1（悬空回调写入已销毁变量） |
| `start()` 长时间无日志，最终超时 | 根本原因 3（信号量异常提前触发） |
| `"Connection failed"` 且紧接着没有新连接建立 | 根本原因 2 或 3 的组合 |

---

## 五、修复方向（概要，待确认后展开）

### 修复根本原因 1：使用 `shared_ptr` 替代引用捕获

```cpp
// 方案：将 got_close / got_error / result_count 包装到 shared_ptr 中
// 确保 session 返回后回调仍可安全访问
auto session_state = std::make_shared<SessionState>();
asr.setCallbacks(
    [..., session_state](...) { session_state->got_error.store(true); },
    [session_state]()         { session_state->got_close.store(true); }
);
```

### 修复根本原因 2：在 `disconnect()` 中无条件清理标志位

```cpp
void AsrWebsocket::disconnect() {
    is_connected_ = false;   // ← 无论 client_ 是否为 null，先清理标志
    is_connecting_ = false;
    if (!client_) {
        return;
    }
    // ... 其余清理逻辑
}
```

同时在 `speech_recognition_persistent::start()` 中，`connect()` 失败后也调用 `disconnect()`：

```cpp
if (!websocket_.connect(config)) {
    websocket_.disconnect();  // ← 清理可能的脏状态
    if (error_callback_) { ... }
    return false;
}
```

### 修复根本原因 3：为 AsrWebsocket 引入 epoch 机制

```cpp
// 在 AsrWebsocket 中维护一个递增的 epoch
// EventHandler 通过比对 epoch 丢弃过期事件
std::atomic<uint32_t> current_epoch_{0};
```

---

## 六、相关源文件索引

- `ai_sdk_builder_demo/main/test_voice_assistant_persistent.cpp` — Demo 入口，悬空回调所在位置
- `ai_sdk_builder/components/ai_sdk/src/speech_recognition_persistent.cc` — `start()`/`stop()`/`cancel()` 实现
- `ai_sdk_builder/components/ai_sdk/src/asr_websocket.cc` — WebSocket 连接层，`connect()`/`disconnect()`/`EventHandler()`
- `ai_sdk_builder/components/ai_sdk/src/include/asr_websocket.h` — `AsrWebsocket` 类声明
