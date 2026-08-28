# ESP32 RTOS AI-SDK 测试方案

## 文档版本

- **创建日期**: 2026-02-02
- **适用版本**: AI-SDK v0.9.3+
- **测试目标**: 验证 RTOS AI-SDK 的 AI 能力是否能正常返回结果

---

## 一、测试目标

### 核心目标

- ✅ **功能可用性测试**: 验证 AI-SDK 各项 AI 能力是否能正常返回结果
- ✅ **API 调用成功**: 验证 HTTP 请求返回 200 状态码
- ✅ **返回数据非空**: 验证返回的结果包含有效数据
- ❌ **不测试结果质量**: 不验证返回内容的正确性
- ❌ **不测试性能**: 不关注响应时间、并发能力等

---

## 二、测试范围

### 现有测试集资源

| 测试文件 | 测试能力 | 数据量 | 位置 |
|----------|----------|--------|------|
| **大模型问答测试集.xlsx** | 大模型闲聊 | 177 条问题 | 测试集/语音助手/ |
| **12月11号测试用例.xlsx** | 文本翻译 | 100 条（中英互译） | 测试集/文本翻译/ |
| **智能摘要API（流式/非流式）.xlsx** | 内容摘要 | 仅性能指标 | 测试集/文本摘要/ |

### AI-SDK 测试能力

| AI 能力 | API 接口 | 测试优先级 |
|---------|----------|-----------|
| **大模型闲聊** | `aiFoundationKit().largeModelChatbot()` | ★★★★★ |
| **文本翻译 v1** | `aiFoundationKit().textTranslate()` | ★★★★★ |
| **文本翻译 v2** | `aiFoundationKit().textTranslateWithModel()` | ★★★★☆ |
| **内容摘要** | `aiFoundationKit().contentSummary()` | ★★★★☆ |
| **ASR 智能对话** | `asrIntelligentDialogueHelp()` | ★★★☆☆ |

---

## 三、核心问题

### 问题：如何在 ESP32 固件中使用 Excel 测试集数据？

ESP32 固件无法直接读取 Excel 文件，需要将测试数据转换为固件可用的格式。

---

## 四、方案对比

### 方案 A：硬编码测试数据

**实现方式**: 将测试数据直接写入 C++ 代码

```cpp
static const char* TEST_QUESTIONS[] = {
    "中日钓鱼岛争端的相关信息",
    "中国的汉奸有哪些",
    // ... 更多问题
};
```

**优势**:
- ✅ 实现简单
- ✅ 无需外部依赖
- ✅ 启动即可测试

**劣势**:
- ❌ 不灵活，修改数据需重新编译固件
- ❌ 占用代码空间
- ❌ 无法动态更新测试集

---

### 方案 B：SPIFFS 存储 CSV

**实现方式**: Excel 转 CSV，存储到 Flash 文件系统

**优势**:
- ✅ 可通过 OTA 更新测试数据
- ✅ 支持 CSV 格式读取

**劣势**:
- ❌ 占用 Flash 空间
- ❌ 需要预先转换 Excel
- ❌ SPIFFS 可能与其他功能冲突

---

### 方案 C：串口输入测试数据 ⭐ 推荐

**实现方式**: 固件内置测试框架，通过串口接收测试命令

**原理**:
```
┌─────────────┐      串口       ┌──────────────┐
│  上位机     | ==================> │  ESP32 设备  │
│  (Python)   │  发送 JSON 命令      │              │
│             │ <================= │  执行 AI-SDK │
│  查看结果   │      接收结果       │              │
└─────────────┘                   └──────────────┘
```

**优势**:
- ✅ **不修改测试集**: Excel 保持原样，运行时转换
- ✅ **固件通用**: 测试逻辑在固件中，与数据分离
- ✅ **灵活性高**: 可随时更换测试数据
- ✅ **结果可见**: 串口输出测试结果
- ✅ **易于调试**: 实时查看测试进度

**劣势**:
- ⚠️ 需要上位机配合

---

### 方案 D：NVS 分区存储

**实现方式**: Excel 转 JSON，存储到 NVS (Non-Volatile Storage)

**优势**:
- ✅ 支持数据更新
- ✅ NVS 读写稳定

**劣势**:
- ❌ 配置复杂
- ❌ 需要预先转换数据
- ❌ NVS 空间有限

---

## 五、推荐方案：串口输入 + 固件测试框架

### 5.1 系统架构

```
┌─────────────────────────────────────────────────┐
│                   上位机 (Python)                │
│  - 读取 Excel 测试集                                │
│  - 转换为 JSON 命令                                  │
│  - 通过串口发送                                    │
│  - 接收并解析测试结果                               │
└─────────────────────────────────────────────────┘
                          ↕ USB-UART
┌─────────────────────────────────────────────────┐
│              ESP32 设备 (RTOS AI-SDK)              │
│  - 接收串口 JSON 命令                              │
│  - 调用 AI-SDK API                                  │
│  - 通过串口返回测试结果                             │
└─────────────────────────────────────────────────┘
```

### 5.2 固件端实现 (ESP32)

```cpp
// main/ai_sdk_test_framework.cpp

#include "ai_sdk/ai_assistant_manager.h"
#include "esp_log.h"
#include "esp_wifi.h"
#include "driver/uart.h"

static const char* TAG = "AI_SDK_TEST";

// ============================================================================
// 测试命令结构
// ============================================================================

struct TestCommand {
    std::string type;      // "chatbot", "translate", "summary"
    std::string input;     // 输入数据
    std::string target_lang; // 目标语言（翻译用）
};

// ============================================================================
// 测试执行函数
// ============================================================================

/**
 * @brief 执行大模型闲聊测试
 * @param question 测试问题
 */
void run_chatbot_test(const std::string& question) {
    using namespace ai_sdk;

    ESP_LOGI(TAG, "");
    ESP_LOGI(TAG, "========== 测试大模型闲聊 ==========");
    ESP_LOGI(TAG, "问题: %s", question.c_str());

    auto& aiKit = AIAssistantManager::getInstance().aiFoundationKit();

    ChatbotCompletionRequest req;
    req.messages = {{"user", question}};
    req.stream = false;
    req.model = "jiutian_75b";

    bool completed = false;

    aiKit.largeModelChatbot(req,
        [&](const ChatbotCompletionResponse& resp) {
            if (!resp.choices.empty()) {
                auto answer = resp.choices[0].message.content;
                if (!answer.empty()) {
                    ESP_LOGI(TAG, "[PASS] 返回成功");
                    ESP_LOGI(TAG, "回答长度: %zu 字符", answer.length());
                    ESP_LOGI(TAG, "回答预览: %s", answer.substr(0, 100).c_str());
                } else {
                    ESP_LOGE(TAG, "[FAIL] 返回为空");
                }
            }
            completed = true;
        },
        [&](const std::string& error) {
            ESP_LOGE(TAG, "[FAIL] 错误: %s", error.c_str());
            completed = true;
        }
    );

    // 等待完成（最多 30 秒）
    int timeout = 300;
    while (!completed && timeout-- > 0) {
        vTaskDelay(pdMS_TO_TICKS(100));
    }

    if (!completed) {
        ESP_LOGE(TAG, "[FAIL] 测试超时");
    }
}

/**
 * @brief 执行翻译测试
 * @param text 待翻译文本
 * @param target_lang 目标语言代码
 */
void run_translate_test(const std::string& text, const std::string& target_lang) {
    using namespace ai_sdk;

    ESP_LOGI(TAG, "");
    ESP_LOGI(TAG, "========== 测试文本翻译 ==========");
    ESP_LOGI(TAG, "源文本: %s", text.substr(0, 50).c_str());
    ESP_LOGI(TAG, "目标语言: %s", target_lang.c_str());

    auto& aiKit = AIAssistantManager::getInstance().aiFoundationKit();

    TranslationRequest req;
    req.originText = text;
    req.targetLanguage = target_lang;
    req.sourceLanguage = "auto";

    bool completed = false;

    aiKit.textTranslate(req,
        [&](const TranslateResponse& resp) {
            if (resp.code == 0) {
                auto translated = resp.data.translateText;
                if (!translated.empty()) {
                    ESP_LOGI(TAG, "[PASS] 翻译成功");
                    ESP_LOGI(TAG, "翻译结果: %s", translated.c_str());
                } else {
                    ESP_LOGE(TAG, "[FAIL] 翻译结果为空");
                }
            } else {
                ESP_LOGE(TAG, "[FAIL] API 错误: %s", resp.msg.c_str());
            }
            completed = true;
        },
        [&](const std::string& error) {
            ESP_LOGE(TAG, "[FAIL] 错误: %s", error.c_str());
            completed = true;
        }
    );

    int timeout = 100;
    while (!completed && timeout-- > 0) {
        vTaskDelay(pdMS_TO_TICKS(100));
    }

    if (!completed) {
        ESP_LOGE(TAG, "[FAIL] 测试超时");
    }
}

/**
 * @brief 执行内容摘要测试
 * @param content 待摘要文本
 * @param language 摘要语言
 */
void run_summary_test(const std::string& content, const std::string& language) {
    using namespace ai_sdk;

    ESP_LOGI(TAG, "");
    ESP_LOGI(TAG, "========== 测试内容摘要 ==========");
    ESP_LOGI(TAG, "输入长度: %zu 字符", content.length());
    ESP_LOGI(TAG, "摘要语言: %s", language.c_str());

    auto& aiKit = AIAssistantManager::getInstance().aiFoundationKit();

    ContentSummaryRequest req;
    req.content = content;
    req.stream = false;
    req.language = language;

    bool completed = false;

    aiKit.contentSummary(req,
        [&](const ContentSummaryResponse& resp) {
            if (resp.status == 0) {
                auto summary = resp.data.content;
                if (!summary.empty()) {
                    ESP_LOGI(TAG, "[PASS] 摘要成功");
                    ESP_LOGI(TAG, "摘要长度: %zu 字符", summary.length());
                    ESP_LOGI(TAG, "摘要内容: %s", summary.c_str());
                } else {
                    ESP_LOGE(TAG, "[FAIL] 摘要为空");
                }
            } else {
                ESP_LOGE(TAG, "[FAIL] API 错误");
            }
            completed = true;
        },
        [&](const std::string& error) {
            ESP_LOGE(TAG, "[FAIL] 错误: %s", error.c_str());
            completed = true;
        }
    );

    int timeout = 100;
    while (!completed && timeout-- > 0) {
        vTaskDelay(pdMS_TO_TICKS(100));
    }
}

// ============================================================================
// 串口接收任务
// ============================================================================

void uart_receive_task(void* param) {
    uart_config_t uart_config = {
        .baud_rate = 115200,
        .data_bits = UART_DATA_8_BITS,
        .parity = UART_PARITY_DISABLE,
        .stop_bits = UART_STOP_BITS_1,
        .flow_ctrl = UART_HW_FLOWCTRL_DISABLE,
        .source_clk = UART_SCLK_DEFAULT,
    };
    uart_param_config(UART_NUM_1, &uart_config);

    uint8_t data[128];
    std::string buffer;

    ESP_LOGI(TAG, "串口测试接收任务启动");
    ESP_LOGI(TAG, "波特率: 115200");
    ESP_LOGI(TAG, "等待 JSON 命令...");

    while (true) {
        // 读取串口数据
        int len = uart_read_bytes(UART_NUM_1, data, sizeof(data), 100 / portTICK_PERIOD_MS);

        if (len > 0) {
            buffer.append((char*)data, len);

            // 检查是否接收到完整命令（以 \n 结尾）
            if (buffer.find('\n') != std::string::npos) {
                // 移除换行符
                buffer.erase(buffer.find('\n'));

                ESP_LOGI(TAG, "收到命令: %s", buffer.c_str());

                // 解析 JSON 命令
                // 简单解析（生产环境建议用 cJSON）

                if (buffer.find("\"type\":\"chatbot\"") != std::string::npos) {
                    // 提取 input 字段
                    size_t start = buffer.find("\"input\":") + 9;
                    size_t end = buffer.find("\", start);
                    if (start != std::string::npos && end != std::string::npos) {
                        std::string question = buffer.substr(start, end - start - 1);
                        run_chatbot_test(question);
                    }
                }
                else if (buffer.find("\"type\":\"translate\"") != std::string::npos) {
                    // 提取 input 和 target_lang
                    size_t input_start = buffer.find("\"input\":") + 9;
                    size_t input_end = buffer.find("\", input_start);
                    size_t lang_start = buffer.find("\"target_lang\":") + 14;
                    size_t lang_end = buffer.find("\", lang_start);

                    if (input_start != std::string::npos && input_end != std::string::npos &&
                        lang_start != std::string::npos && lang_end != std::string::npos) {
                        std::string text = buffer.substr(input_start, input_end - input_start - 1);
                        std::string lang = buffer.substr(lang_start, lang_end - lang_start - 1);
                        run_translate_test(text, lang);
                    }
                }
                else if (buffer.find("\"type\":\"summary\"") != std::string::npos) {
                    // 提取 content 和 language
                    size_t content_start = buffer.find("\"content\":") + 11;
                    size_t content_end = buffer.find("\", content_start);
                    size_t lang_start = buffer.find("\"language\":") + 12;
                    size_t lang_end = buffer.find("\", lang_start);

                    if (content_start != std::string::npos && content_end != std::string::npos &&
                        lang_start != std::string::npos && lang_end != std::string::npos) {
                        std::string content = buffer.substr(content_start, content_end - content_start - 1);
                        std::string lang = buffer.substr(lang_start, lang_end - lang_start - 1);
                        run_summary_test(content, lang);
                    }
                }
                else {
                    ESP_LOGE(TAG, "未知命令类型");
                }

                buffer.clear();
            }
        }

        vTaskDelay(1);
    }
}

// ============================================================================
// 主程序
// ============================================================================

extern "C" void app_main() {
    ESP_LOGI(TAG, "");
    ESP_LOGI(TAG, "========================================");
    ESP_LOGI(TAG, "  AI-SDK 测试框架");
    ESP_LOGI(TAG, "========================================");
    ESP_LOGI(TAG, "");
    ESP_LOGI(TAG, "初始化...");

    // 1. 初始化 WiFi
    ESP_LOGI(TAG, "1. 初始化 WiFi...");
    // ... WiFi 初始化代码 ...

    // 2. 同步时间
    ESP_LOGI(TAG, "2. 同步时间...");
    // ... SNTP 初始化代码 ...

    // 3. 初始化 AI-SDK
    ESP_LOGI(TAG, "3. 初始化 AI-SDK...");

    using namespace ai_sdk;

    Log::setLevel(LogLevel::INFO);

    auto builder = std::make_unique<AIAssistConfig::Builder>();
    auto config = builder->deviceNo("YOUR_DEVICE_NO")
                         ->deviceNoType("SN")
                         ->productId("YOUR_PRODUCT_ID")
                         ->productKey("YOUR_PRODUCT_KEY")
                         ->token("YOUR_TOKEN")
                         ->centralConfigVersion("*")
                         ->build();

    AIAssistantManager::initialize(std::move(config));

    ESP_LOGI(TAG, "AI-SDK 初始化完成");
    ESP_LOGI(TAG, "");

    // 4. 创建串口接收任务
    ESP_LOGI(TAG, "4. 启动串口接收任务...");
    xTaskCreate(uart_receive_task, "uart_rx", 8192, NULL, 5, NULL);

    ESP_LOGI(TAG, "");
    ESP_LOGI(TAG, "========================================");
    ESP_LOGI(TAG, "  测试框架已就绪");
    ESP_LOGI(TAG, "========================================");
    ESP_LOGI(TAG, "");
    ESP_LOGI(TAG, "通过串口发送 JSON 命令进行测试:");
    ESP_LOGI(TAG, "");
    ESP_LOGI(TAG, "1. 大模型闲聊:");
    ESP_LOGI(TAG, "   {\"type\":\"chatbot\",\"input\":\"你好\"}");
    ESP_LOGI(TAG, "");
    ESP_LOGI(TAG, "2. 文本翻译:");
    ESP_LOGI(TAG, "   {\"type\":\"translate\",\"input\":\"Hello\",\"target_lang\":\"zh\"}");
    ESP_LOGI(TAG, "");
    ESP_LOGI(TAG, "3. 内容摘要:");
    ESP_LOGI(TAG, "   {\"type\":\"summary\",\"content\":\"长文本...\",\"language\":\"Chinese\"}");
    ESP_LOGI(TAG, "");
    ESP_LOGI(TAG, "========================================");
}
```

---

## 六、上位机测试脚本 (Python)

### 6.1 主测试脚本

```python
# run_ai_sdk_tests.py

import pandas as pd
import serial
import json
import time
from pathlib import Path

# ====================== 配置区 ======================
SERIAL_PORT = "COM3"  # Windows: COMx, Linux: /dev/ttyUSBx
BAUD_RATE = 115200
TEST_XLSX_PATH = r"E:\github\ESP32-RTOS-AI-SDK\RTOS-AI_SDK-测试\测试集"
# =======================================================

class AISDKTester:
    def __init__(self, serial_port, baud_rate):
        self.ser = serial.Serial(serial_port, baud_rate, timeout=5)
        time.sleep(2)  # 等待串口稳定

        # 清空接收缓冲区
        self.ser.reset_input_buffer()

        print(f"✓ 串口已连接: {serial_port}")

    def send_command(self, cmd):
        """发送 JSON 命令到设备"""
        json_str = json.dumps(cmd, ensure_ascii=False) + "\n"
        self.ser.write(json_str.encode('utf-8'))

        # 短暂等待设备响应
        time.sleep(0.5)

    def test_chatbot(self, questions, count=10):
        """测试大模型闲聊"""
        print(f"\n{'='*60}")
        print("测试大模型闲聊")
        print('='*60)

        passed = 0
        failed = 0

        for i in range(min(count, len(questions))):
            question = questions[i]

            print(f"\n[{i+1}/{min(count, len(questions))}] {question[:40]}...")

            cmd = {
                "type": "chatbot",
                "input": question
            }

            try:
                self.send_command(cmd)

                # 等待设备完成测试
                time.sleep(6)

                # 读取串口输出（可选，用于记录结果）
                # while self.ser.in_waiting():
                #     print(self.ser.read(self.ser.in_waiting()).decode('utf-8', errors='ignore'), end='')

                passed += 1

            except Exception as e:
                print(f"  ✗ 异常: {str(e)[:50]}")
                failed += 1

        print(f"\n结果: {passed} 通过, {failed} 失败")
        return passed, failed

    def test_translate(self, df, count=10):
        """测试文本翻译"""
        print(f"\n{'='*60}")
        print("测试文本翻译")
        print('='*60)

        passed = 0
        failed = 0

        for i in range(min(count, len(df))):
            row = df.iloc[i]
            text = row['原始文本']
            target_lang = row['目标语言']

            print(f"\n[{i+1}/{min(count, len(df))}] {text[:40]}... -> {target_lang}")

            cmd = {
                "type": "translate",
                "input": text,
                "target_lang": target_lang
            }

            try:
                self.send_command(cmd)
                time.sleep(4)
                passed += 1

            except Exception as e:
                print(f"  ✗ 异常: {str(e)[:50]}")
                failed += 1

        print(f"\n结果: {passed} 通过, {failed} 失败")
        return passed, failed

    def close(self):
        """关闭串口"""
        self.ser.close()
        print("\n串口已关闭")


def main():
    print("="*60)
    print("AI-SDK 测试工具")
    print("="*60)

    # 初始化测试器
    try:
        tester = AISDKTester(SERIAL_PORT, BAUD_RATE)

        total_passed = 0
        total_failed = 0

        # 1. 测试大模型闲聊
        print("\n正在加载测试集...")
        chatbot_df = pd.read_excel(f"{TEST_XLSX_PATH}/语音助手/大模型问答测试集.xlsx")
        questions = chatbot_df.iloc[:, 0].dropna().tolist()

        p, f = tester.test_chatbot(questions, count=5)
        total_passed += p
        total_failed += f

        # 2. 测试文本翻译
        translate_df = pd.read_excel(f"{TEST_XLSX_PATH}/文本翻译/12月11号测试用例.xlsx")
        p, f = tester.test_translate(translate_df, count=5)
        total_passed += p
        total_failed += f

        # 3. 输出总结
        print("\n" + "="*60)
        print("测试总结")
        print("="*60)
        print(f"总测试数: {total_passed + total_failed}")
        print(f"通过: {total_passed}")
        print(f"失败: {total_failed}")
        print(f"可用性: {total_passed / (total_passed + total_failed) * 100:.1f}%")
        print("="*60)

        tester.close()

    except Exception as e:
        print(f"\n错误: {e}")
        print("请检查:")
        print("1. 串口是否正确连接")
        print("2. ESP32 设备是否正常启动")
        print("3. Excel 测试集路径是否正确")


if __name__ == "__main__":
    main()
```

---

## 七、使用步骤

### 7.1 准备固件

1. **编译测试固件**
   ```bash
   cd E:\github\ESP32-RTOS-AI-SDK
   idf.py build flash monitor
   ```

2. **验证串口连接**
   - 设备端: USB-UART 连接到 PC
   - 确认串口号（如 COM3）

### 7.2 准备测试环境

1. **安装依赖**
   ```bash
   pip install pandas pyserial
   ```

2. **配置测试脚本**
   - 修改 `SERIAL_PORT` 为实际串口号
   - 确认 `TEST_XLSX_PATH` 路径正确

### 7.3 运行测试

```bash
python run_ai_sdk_tests.py
```

---

## 八、JSON 命令格式

### 大模型闲聊

```json
{
  "type": "chatbot",
  "input": "你好，请介绍一下你自己"
}
```

### 文本翻译

```json
{
  "type": "translate",
  "input": "Hello, world!",
  "target_lang": "zh"
}
```

### 内容摘要

```json
{
  "type": "summary",
  "content": "这是一段需要摘要的长文本内容...",
  "language": "Chinese"
}
```

---

## 九、串口输出示例

```
========================================
  AI-SDK 测试框架
========================================

初始化...
1. 初始化 WiFi...
2. 同步时间...
3. 初始化 AI-SDK...
AI-SDK 初始化完成

4. 启动串口接收任务...

收到命令: {"type":"chatbot","input":"你好"}

========== 测试大模型闲聊 ==========
问题: 你好
[PASS] 返回成功
回答长度: 45 字符
回答预览: 你好！我是一个人工智能助手...
```

---

## 十、测试用例数据来源

### 从 Excel 提取

| 文件 | 工作表 | 列名 | 说明 |
|------|--------|------|------|
| 大模型问答测试集.xlsx | Sheet1 | 第一列 | 177 个测试问题 |
| 12月11号测试用例.xlsx | Sheet1 | 原始文本、目标语言 | 100 条翻译测试 |

### 数据转换

**Excel → JSON 命令**（Python 自动完成）

```python
# Excel
| 问题 |
|------|
| 中日钓鱼岛争端的相关信息 |
| 中国的汉奸有哪些 |

# JSON
{"type": "chatbot", "input": "中日钓鱼岛争端的相关信息"}
```

---

## 十一、测试结果验证

### 通过标准

| AI 能力 | 通过条件 |
|---------|----------|
| **大模型闲聊** | 回答非空 |
| **文本翻译** | 翻译结果非空 |
| **内容摘要** | 摘要内容非空 |

### 失败处理

- ✅ API 调用失败 → 记录错误信息
- ✅ 返回数据为空 → 记录 FAIL
- ✅ 超时未响应 → 记录超时

---

## 十二、故障排查

### 常见问题

| 问题 | 可能原因 | 解决方案 |
|------|----------|----------|
| 串口无法连接 | 串口号错误 | 检查设备管理器 |
| 设备无响应 | 固件未启动 | 检查 monitor 输出 |
| API 调用失败 | 网络未连接 | 检查 WiFi 配置 |
| 认证错误 | Token 错误 | 检查配置文件 |

---

## 十三、扩展测试场景

### 添加新的测试类型

1. **修改固件**：添加新的测试命令解析逻辑
2. **修改脚本**：添加新的命令发送函数
3. **重新编译**：固件更新后重新编译

### 批量测试

```python
# 批量测试所有问题（177 条）
tester.test_chatbot(questions, count=177)
```

---

## 十四、方案总结

### 优势

- ✅ **不修改测试集**: Excel 保持原样
- ✅ **固件通用**: 测试逻辑与数据分离
- ✅ **灵活性高**: 可随时更换测试数据
- ✅ **易于调试**: 实时查看测试进度
- ✅ **易于扩展**: 添加新测试类型简单

### 适用场景

- **日常验证**: 快速验证 AI-SDK 功能可用性
- **集成测试**: ESP32 设备端到端测试
- **回归测试**: 固件更新后验证功能
- **压力测试**: 批量测试稳定性

---

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.0 | 2026-02-02 | 初始版本 |

---

## 附录

### A. 串口配置说明

- **UART_NUM**: UART_NUM_1
- **波特率**: 115200
- **数据位**: 8
- **停止位**: 1
- **校验位**: 无

### B. 测试集文件说明

| 文件 | 编码 | 备注 |
|------|------|------|
| 大模型问答测试集.xlsx | UTF-8 | 第一列为测试问题 |
| 12月11号测试用例.xlsx | UTF-8 | 包含中英文测试 |

### C. API 端点配置

- **Chatbot API**: 需要 Token 认证
- **翻译 API**: 支持自动语言检测
- **摘要 API**: 支持多语言输出
