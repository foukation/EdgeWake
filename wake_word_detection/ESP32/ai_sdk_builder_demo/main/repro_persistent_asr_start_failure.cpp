/**
 * @file repro_persistent_asr_start_failure.cpp
 * @brief 复现 "SpeechRecognitionPersistent 首次 asr.start() 失败后无法恢复" 的实验代码
 *
 * 背景
 * ----
 * docs/ai_sdk_builder_demo/doc/asr-start-failure-analysis.md 中分析了一个症状：
 *
 *   "asr.start() 第一次调用如果失败，后面就调不起来。"
 *
 * 该文档定位的根因有 3 个：
 *   RC1：demo 回调以引用方式捕获栈对象，session 切换后悬空 → demo 层问题
 *   RC2：esp_websocket_client_destroy() 不清残留事件，is_connected_ 被污染 → SDK 内部
 *   RC3：connection_semaphore_ 残留 give，下一次 start() take 立即返回 → SDK 内部
 *
 * 本实验目的
 * ----------
 * 在干净的环境里，把"首次 asr.start() 失败"这一条件可控地造出来，然后观察后续多次
 * 正常 asr.start() 是否还能成功。一旦后续大量失败，bug 即被复现。
 *
 * 实验设计
 * --------
 * 不像 test_voice_assistant_persistent.cpp 那样减小超时（会引入混淆变量），
 * 也不修改服务器 URL（CONNECTED 永远不会触发，与真实路径不同），
 * 本实验通过 "SDK 初始化状态" 这一开关来制造首次失败：
 *
 *   Phase 1 (诱导首次失败)：
 *     - 不调用 AIAssistantManager::initialize()
 *     - getInstance() 返回 dummy 实例（empty AIAssistConfig）
 *     - 调用 asr.start()，URL 用空 productKey/productSecret/deviceNo 签名
 *     - 服务端拒绝 → on_error → start() 返回 false
 *     ✱ 此时 SDK 内部已经历 destroy → 残留事件 / 信号量场景就位
 *
 *   Phase 2 (恢复正确初始化状态)：
 *     - AIAssistantManager::initialize(builder.build())
 *     - 同步等待 obtainDeviceInformation 成功（拿到 deviceId/deviceSecret）
 *     ✱ 现在 config 是合法的；如果 SDK 是干净的，asr.start() 应该能成功
 *
 *   Phase 3 (验证后续能否恢复)：
 *     - 循环 kStage3Sessions 次 asr.start() / asr.stop()
 *     - 统计成功 / 失败次数；如果 success_count == 0，bug 复现
 *
 * 自身约束
 * --------
 * - 回调统一以 std::shared_ptr 捕获共享状态，杜绝 RC1（悬空引用），
 *   保证我们看到的失败一定来自 SDK 内部，而不是 demo 层 bug。
 * - 不修改任何其它源文件；新文件本身自包含。
 *
 * 接入方法（user 后续自行决定）
 * ----------------------------
 *   1. 在 ai_sdk_builder_demo/main/CMakeLists.txt 的 SRCS 中加入：
 *      "repro_persistent_asr_start_failure.cpp"
 *      （本次提交已附带此修改）
 *   2. 在调用端（如 test_ai_sdk.cpp 或 main.cc）于 Wi-Fi 连接成功后调用：
 *      extern "C" void start_persistent_asr_start_failure_repro_test(void);
 *      start_persistent_asr_start_failure_repro_test();
 *      注意：此实验会自行 initialize() AI SDK，
 *           因此调用前不要再额外调用 AIAssistantManager::initialize()。
 *
 * 入口函数：start_persistent_asr_start_failure_repro_test()
 */

#include "ai_sdk/ai_assistant_manager.h"
#include "ai_sdk/ai_sdk_log.h"
#include "ai_sdk/speech_recognition_persistent.h"
#include "demo_config.h"

#include <esp_log.h>
#include <esp_timer.h>
#include <freertos/FreeRTOS.h>
#include <freertos/semphr.h>
#include <freertos/task.h>

#include <atomic>
#include <memory>
#include <string>

static const char* TAG = "REPRO_ASR_START";

// ============================================================================
// 实验参数
// ============================================================================

/// Phase 3 循环测试 asr.start() 的总次数
static constexpr int kStage3Sessions = 8;

/// 每次 attempt 结束后到下一次 attempt 之间的间隔（ms）
static constexpr int kRestartDelayMs = 2000;

/// 单次 attempt 等待回调（on_error / on_close）的最大时间（ms）
/// SDK 内部 CONNECTION_TIMEOUT_MS = 30000，本超时仅用于错误/关闭场景的兜底
static constexpr int kCallbackWaitMs = 35000;

/// asr.start() 返回 true 后保持连接的时间（ms），用于让残留事件有时间触发
static constexpr int kHoldOpenMs = 3000;

// ============================================================================
// 单次 attempt 共享状态
// 用 shared_ptr 由回调持有，避免栈对象悬空（自我规避 RC1）
// ============================================================================

struct ReproSessionState {
    SemaphoreHandle_t done_sem{nullptr};   // 任一回调触发后 give
    std::atomic_bool  got_error{false};    // on_error 是否触发
    std::atomic_bool  got_close{false};    // on_close 是否触发
    std::atomic_int   error_code{0};       // 最近一次 on_error 的 code

    ReproSessionState() {
        done_sem = xSemaphoreCreateBinary();
    }
    ~ReproSessionState() {
        if (done_sem != nullptr) {
            vSemaphoreDelete(done_sem);
            done_sem = nullptr;
        }
    }

    // 禁拷贝/移动：与 FreeRTOS 句柄绑定的资源不应被复制
    ReproSessionState(const ReproSessionState&)            = delete;
    ReproSessionState& operator=(const ReproSessionState&) = delete;
};

// ============================================================================
// 单次 attempt：调用 asr.start()，记录耗时和回调情况
// ============================================================================

/**
 * @brief 执行一次 asr.start() 尝试，并等待结果
 *
 * @param phase   阶段标签（仅用于日志），如 "PHASE1"、"PHASE3"
 * @param session 本阶段内的 session 编号（仅用于日志）
 * @return asr.start() 是否返回 true
 */
static bool repro_attempt_start(const char* phase, int session) {
    auto& asr = ai_sdk::AIAssistantManager::getInstance().speechRecognitionPersistentHelp();

    // 共享状态用 shared_ptr，确保即使 attempt 函数返回了，回调访问也安全
    auto state = std::make_shared<ReproSessionState>();
    if (state->done_sem == nullptr) {
        ESP_LOGE(TAG, "[%s][S%d] 创建信号量失败", phase, session);
        return false;
    }

    // 注意：以值（shared_ptr 副本）方式捕获 state，杜绝悬空引用
    asr.setCallbacks(
        /* on_result */
        [phase, session](const ai_sdk::SpeechRecognitionPersistentResult& result) {
            // 本实验只关心 start() 能否打通，不关心识别文本
            ESP_LOGI(TAG, "[%s][S%d][ASR 结果] type=%s err_no=%d result=%s",
                     phase, session,
                     result.type.c_str(),
                     result.err_no,
                     result.result.c_str());
        },
        /* on_error */
        [state, phase, session](int code, const std::string& msg) {
            state->got_error.store(true);
            state->error_code.store(code);
            ESP_LOGE(TAG, "[%s][S%d][ASR 错误] code=%d msg=%s",
                     phase, session, code, msg.c_str());
            xSemaphoreGive(state->done_sem);
        },
        /* on_close */
        [state, phase, session]() {
            state->got_close.store(true);
            ESP_LOGW(TAG, "[%s][S%d][ASR 关闭]", phase, session);
            xSemaphoreGive(state->done_sem);
        });

    // ---- 调用 asr.start() 并计时 ----
    ESP_LOGI(TAG, "[%s][S%d] >>> 调用 asr.start()", phase, session);
    int64_t t0 = esp_timer_get_time();
    bool started = asr.start();
    int64_t dt_ms = (esp_timer_get_time() - t0) / 1000;
    ESP_LOGI(TAG, "[%s][S%d] <<< asr.start() 返回 %s, 耗时 %lld ms",
             phase, session, started ? "true" : "false",
             static_cast<long long>(dt_ms));

    if (started) {
        // start 成功：保持连接片刻，再正常 stop
        ESP_LOGI(TAG, "[%s][S%d] 保持连接 %d ms 后 stop", phase, session, kHoldOpenMs);
        vTaskDelay(pdMS_TO_TICKS(kHoldOpenMs));
        asr.stop();

        // 等待 on_close 触发（兜底超时）
        if (xSemaphoreTake(state->done_sem, pdMS_TO_TICKS(kCallbackWaitMs)) != pdTRUE) {
            ESP_LOGW(TAG, "[%s][S%d] 等待 on_close 超时", phase, session);
        }
    } else {
        // start 失败：等可能延后到达的 on_error / on_close（如残留事件）
        if (xSemaphoreTake(state->done_sem, pdMS_TO_TICKS(kCallbackWaitMs)) != pdTRUE) {
            ESP_LOGW(TAG, "[%s][S%d] start 失败后无后续回调（已超时）",
                     phase, session);
        }
    }

    // 摘要日志
    ESP_LOGW(TAG, "[%s][S%d] 总结: start=%s err=%s(code=%d) close=%s",
             phase, session,
             started ? "true" : "false",
             state->got_error.load() ? "yes" : "no",
             state->error_code.load(),
             state->got_close.load() ? "yes" : "no");

    return started;
}

// ============================================================================
// 同步包装：阻塞等待 obtainDeviceInformation 完成
// ============================================================================

/**
 * @brief 同步调用 obtainDeviceInformation，最多等待 timeout_ms
 *
 * @return 设备信息是否获取成功
 */
static bool repro_obtain_device_info_sync(int timeout_ms) {
    auto& mgr = ai_sdk::AIAssistantManager::getInstance();

    auto done    = std::make_shared<SemaphoreHandle_t>(xSemaphoreCreateBinary());
    auto success = std::make_shared<std::atomic_bool>(false);

    if (*done == nullptr) {
        ESP_LOGE(TAG, "obtainDeviceInformation: 创建信号量失败");
        return false;
    }

    mgr.gateWayHelp().obtainDeviceInformation(
        [done, success](const ai_sdk::DeviceInfoResponse& resp) {
            ESP_LOGI(TAG, "obtainDeviceInformation 成功: deviceId=%s",
                     resp.data.deviceId.c_str());
            success->store(true);
            xSemaphoreGive(*done);
        },
        [done](const std::string& err) {
            ESP_LOGE(TAG, "obtainDeviceInformation 失败: %s", err.c_str());
            xSemaphoreGive(*done);
        });

    bool ok = false;
    if (xSemaphoreTake(*done, pdMS_TO_TICKS(timeout_ms)) == pdTRUE) {
        ok = success->load();
    } else {
        ESP_LOGE(TAG, "obtainDeviceInformation 超时（%d ms）", timeout_ms);
    }
    vSemaphoreDelete(*done);
    *done = nullptr;
    return ok;
}

// ============================================================================
// 主任务：三阶段实验
// ============================================================================

static void repro_task(void* arg) {
    (void)arg;

    // 让 SDK 日志开到 DEBUG，便于看到 SDK 内部信号量 / 事件序列
    ai_sdk::Log::setLevel(ai_sdk::LogLevel::DEBUG);

    // ---------------------------------------------------------------
    // Phase 1：未 initialize，直接 asr.start() → 预期失败
    // ---------------------------------------------------------------
    ESP_LOGW(TAG, "==========================================================");
    ESP_LOGW(TAG, " PHASE 1: 未 initialize，直接 asr.start()（预期失败）");
    ESP_LOGW(TAG, "==========================================================");

    if (ai_sdk::AIAssistantManager::isInitialized()) {
        ESP_LOGE(TAG, "SDK 已被外部初始化！本实验需要从未初始化状态开始。");
        ESP_LOGE(TAG, "请确认调用者未提前 initialize()，然后再次运行实验。");
        vTaskDelete(nullptr);
        return;
    }

    bool phase1_started = repro_attempt_start("PHASE1", 1);

    ESP_LOGW(TAG, "Phase 1 结果：asr.start() 返回 %s（预期 false）",
             phase1_started ? "true" : "false");

    // 给 SDK 内部一些时间，让残留事件（如有）可能向事件循环投递
    vTaskDelay(pdMS_TO_TICKS(kRestartDelayMs));

    // ---------------------------------------------------------------
    // Phase 2：正确 initialize() + obtainDeviceInformation
    // ---------------------------------------------------------------
    ESP_LOGW(TAG, "==========================================================");
    ESP_LOGW(TAG, " PHASE 2: initialize() + obtainDeviceInformation()");
    ESP_LOGW(TAG, "==========================================================");

    if (!ai_sdk::AIAssistantManager::isInitialized()) {
        auto builder = std::make_unique<ai_sdk::AIAssistConfig::Builder>();
        auto config  = builder
                          ->deviceNo(DEMO_DEVICE_NO)
                          .deviceNoType(DEMO_DEVICE_NO_TYPE)
                          .productId(DEMO_PRODUCT_ID)
                          .productKey(DEMO_PRODUCT_KEY)
                          .centralConfigVersion(DEMO_CENTRAL_CONFIG_VER)
                          .token(DEMO_TOKEN)
                          .deviceId("")
                          .deviceSecret("")
                          .build();
        ai_sdk::AIAssistantManager::initialize(std::move(config));
        ESP_LOGI(TAG, "AIAssistantManager::initialize() 已调用");
    }

    if (!repro_obtain_device_info_sync(30000)) {
        ESP_LOGE(TAG, "Phase 2 失败：无法获取设备信息，实验无法继续");
        ESP_LOGE(TAG, "请检查 Wi-Fi / 凭证 / 服务器可达性");
        vTaskDelete(nullptr);
        return;
    }

    // 让 SDK 内部 config 完全稳定
    vTaskDelay(pdMS_TO_TICKS(1000));

    // ---------------------------------------------------------------
    // Phase 3：循环 N 次 asr.start()，观察能否恢复
    // ---------------------------------------------------------------
    ESP_LOGW(TAG, "==========================================================");
    ESP_LOGW(TAG, " PHASE 3: 循环 %d 次 asr.start()", kStage3Sessions);
    ESP_LOGW(TAG, "==========================================================");

    int phase3_success = 0;
    for (int s = 1; s <= kStage3Sessions; ++s) {
        bool ok = repro_attempt_start("PHASE3", s);
        if (ok) {
            ++phase3_success;
        }
        ESP_LOGW(TAG, "Phase 3 Session %d/%d: %s",
                 s, kStage3Sessions, ok ? "成功" : "失败");
        vTaskDelay(pdMS_TO_TICKS(kRestartDelayMs));
    }

    // ---------------------------------------------------------------
    // 最终结论
    // ---------------------------------------------------------------
    ESP_LOGW(TAG, "==========================================================");
    ESP_LOGW(TAG, " 实验结果");
    ESP_LOGW(TAG, "==========================================================");
    ESP_LOGW(TAG, "  Phase 1（未 init）: asr.start() = %s（预期 false）",
             phase1_started ? "true" : "false");
    ESP_LOGW(TAG, "  Phase 3（已 init）: %d / %d 成功",
             phase3_success, kStage3Sessions);

    if (phase3_success == 0) {
        ESP_LOGE(TAG, "  >>> BUG 复现：首次失败后，所有后续 asr.start() 均失败");
    } else if (phase3_success < kStage3Sessions) {
        ESP_LOGW(TAG, "  >>> 部分恢复：%d 次成功，%d 次失败",
                 phase3_success, kStage3Sessions - phase3_success);
    } else {
        ESP_LOGI(TAG, "  >>> 未观察到 BUG：所有后续 asr.start() 均成功");
    }

    // 实验结束，任务自然 idle（保留任务句柄，便于后续观察日志）
    while (true) {
        vTaskDelay(pdMS_TO_TICKS(60000));
    }
}

// ============================================================================
// 对外入口
// ============================================================================

static TaskHandle_t g_repro_task_handle = nullptr;

/**
 * @brief 启动 "首次 asr.start() 失败后无法恢复" 复现实验
 *
 * 调用前置条件：
 *   - Wi-Fi 已连接（实验内部不负责连网）
 *   - AIAssistantManager 未被任何代码 initialize() 过
 *
 * 调用方式：在 Wi-Fi 连接成功之后、任何 AIAssistantManager::initialize()
 * 之前调用本函数即可。
 *
 * 任务参数：
 *   - 栈大小 12288 字节
 *   - 优先级 5（与现有 demo 任务一致）
 */
extern "C" void start_persistent_asr_start_failure_repro_test(void) {
    if (g_repro_task_handle != nullptr) {
        ESP_LOGW(TAG, "Repro 任务已在运行，忽略重复调用");
        return;
    }

    BaseType_t rc = xTaskCreate(
        repro_task,
        "repro_asr_start",
        12288,
        nullptr,
        5,
        &g_repro_task_handle);

    if (rc != pdPASS) {
        ESP_LOGE(TAG, "创建 repro 任务失败 (rc=%d)", static_cast<int>(rc));
        g_repro_task_handle = nullptr;
    } else {
        ESP_LOGI(TAG, "Repro 任务已创建");
    }
}
