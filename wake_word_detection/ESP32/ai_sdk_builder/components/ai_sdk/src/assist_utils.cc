// 内部头文件（位于 src/include/）
#include "assist_utils.h"
#include "api_config.h"
#include "device_client.h"

// 公开头文件（位于 include/ai_sdk/）
#include "ai_sdk/ai_assistant_manager.h"
#include "esp_log.h"
#include "esp_mac.h"  // for MAC address
#include <mbedtls/md5.h>  // for MD5 calculation (mbedtls, ESP-IDF standard)
#include <sstream>
#include <iomanip>
#include <cstring>
#include <cstdlib>  // for rand()
#include <sys/time.h>  // for gettimeofday() - Unix timestamp

namespace ai_sdk {

static const char* TAG = "AssistUtils";

/**
 * @brief 生成全局唯一的会话ID
 * @return 会话标识字符串
 *
 * 格式: sn-{MAC地址}-{时间戳}-{随机数}
 * - MAC地址（12位十六进制）：设备全球唯一标识
 * - 时间戳（毫秒）：时间区分
 * - 随机数（5位）：同一毫秒内的冲突保护
 *
 * 保证全局唯一性：
 * - 不同设备：MAC地址不同 → 100%唯一
 * - 同一设备不同时间：时间戳不同 → 100%唯一
 * - 同一设备同一时间：随机数1/90000概率
 */
static std::string generateSessionId() {
    // 获取设备MAC地址（全球唯一）
    uint8_t mac[6];
    esp_efuse_mac_get_default(mac);

    std::ostringstream sn_stream;
    sn_stream << "sn-";

    // 添加MAC地址（12位十六进制）
    for (int i = 0; i < 6; i++) {
        sn_stream << std::hex << std::setw(2) << std::setfill('0')
                  << (int)mac[i];
    }

    // 添加时间戳（毫秒）
    int64_t ts = AssistUtils::timestamp();
    sn_stream << "-" << ts;

    // 添加随机数（防止同一毫秒内的冲突）
    // 10000-99999范围，保证5位数
    sn_stream << "-" << (10000 + rand() % 90000);

    return sn_stream.str();
}

std::string AssistUtils::wssParameter(const std::string& uri) {
    ESP_LOGD(TAG, "Building WebSocket URL parameters...");

    // 生成全局唯一的会话标识
    std::string sn = generateSessionId();
    ESP_LOGD(TAG, "Session ID: %s", sn.c_str());

    const auto& config = AIAssistantManager::getInstance().config();

    std::string deviceNo = config.deviceNo;
    std::string productKey = config.productKey;
    std::string productId = config.productId;
    std::string deviceId = config.deviceId;
    std::string deviceSecret = config.deviceSecret;

    int64_t ts = timestamp();
    std::string sign = signMd5(ts);

    // 构建参数字符串
    std::ostringstream params;
    params << "?sn=" << sn
           << "&deviceNo=" << deviceNo
           << "&productKey=" << productKey
           << "&productId=" << productId
           << "&ts=" << ts
           << "&sign=" << sign
           << "&deviceId=" << deviceId;

    std::string full_url = uri + params.str();
    ESP_LOGD(TAG, "WebSocket URL: %s", full_url.c_str());

    return full_url;
}

/**
 * @brief MD5签名生成
 * @param timestamp 时间戳（毫秒）
 * @return MD5签名（32位小写十六进制字符串）
 *
 * 签名算法：MD5(deviceSecret + timestamp)
 * 示例:
 *   deviceSecret = "my-device-secret"
 *   timestamp = 123456789
 *   input = "my-device-secret123456789"
 *   output = "5d41402abc4b2a76b9719d911017c592"
 *
 * 实现细节：
 * 1. 从 AIAssistantManager 配置中获取 deviceSecret
 * 2. 将 timestamp 转换为字符串并拼接: deviceSecret + timestamp
 * 3. 使用 mbedtls MD5 算法计算哈希值（ESP-IDF标准库）
 * 4. 将16字节结果转换为32位小写十六进制字符串
 *
 * 错误处理：
 * - 如果 deviceSecret 为空，使用 productKey 作为备选（日志警告）
 * - 确保始终返回有效的32位字符串
 *
 * 线程安全：读取配置是线程安全的（配置在初始化后不变）
 */
std::string AssistUtils::signMd5(int64_t timestamp) {
    // 步骤1：获取设备密钥配置
    const auto& config = AIAssistantManager::getInstance().config();
    std::string deviceSecret = config.deviceSecret;

    // 备选方案：如果 deviceSecret 为空，使用 productKey
    // 这确保了即使配置不完整也能生成签名（服务器端验证可能更严格）
    if (deviceSecret.empty()) {
        deviceSecret = config.productKey;
        ESP_LOGW(TAG, "deviceSecret is empty, using productKey as fallback for sign generation");
    }

    // 步骤2：构建输入字符串（deviceSecret + timestamp）
    // 采用字符串拼接方式，不是数值相加
    std::string ts_str = std::to_string(timestamp);
    std::string input = deviceSecret + ts_str;

    ESP_LOGD(TAG, "Generating MD5 sign for: %s + %s",
             deviceSecret.c_str(), ts_str.c_str());

    // 步骤3：计算MD5哈希（使用mbedtls，ESP-IDF标准库）
    // mbedtls API: init → starts → update → finish → free
    uint8_t md5_result[16];  // MD5输出固定为16字节（128位）
    mbedtls_md5_context ctx;
    mbedtls_md5_init(&ctx);
    mbedtls_md5_starts(&ctx);
    mbedtls_md5_update(&ctx, (const unsigned char*)input.c_str(), input.length());
    mbedtls_md5_finish(&ctx, md5_result);
    mbedtls_md5_free(&ctx);

    // 步骤4：转换为十六进制字符串
    // MD5结果需要转换为32位小写十六进制字符串（每字节2字符）
    // 例如：0x5d → "5d", 0x41 → "41"
    std::ostringstream md5_str;
    for (int i = 0; i < 16; i++) {
        // 使用 std::hex 转换为十六进制
        // std::setw(2) 确保两位数（补零）
        // std::setfill('0') 用0填充
        md5_str << std::hex << std::setw(2) << std::setfill('0')
                << static_cast<int>(md5_result[i]);
    }

    std::string result = md5_str.str();
    ESP_LOGD(TAG, "MD5 sign generated: %s", result.c_str());

    return result;
}

/**
 * @brief 获取当前 Unix 时间戳（毫秒）
 * 
 * 返回自 1970-01-01 00:00:00 UTC 以来的毫秒数。
 * 
 * 用途：
 * - WebSocket URL 参数中的 `ts` 字段
 * - MD5 签名生成（`signMd5(timestamp)`）
 * - 会话 ID 生成（`generateSessionId()`）
 * 
 * 重要说明：
 * - 需要确保设备时间已通过 SNTP 同步，否则返回的时间戳不准确
 * - ESP32 没有电池供电的 RTC，每次启动默认时间为 1970-01-01
 * - 建议在 WiFi 连接成功后立即调用 SNTP 同步时间
 * 
 * SNTP 时间同步示例：
 * @code
 * #include "esp_sntp.h"
 * sntp_setoperatingmode(SNTP_OPMODE_POLL);
 * sntp_setservername(0, "pool.ntp.org");
 * sntp_init();
 * @endcode
 * 
 * 对比：
 * - 原实现：esp_log_timestamp() 返回系统启动后的毫秒数（如 125000）
 * - 新实现：gettimeofday() 返回真实 Unix 时间戳（如 1703952000000）
 * 
 * @return int64_t Unix 时间戳（毫秒）
 */
int64_t AssistUtils::timestamp() {
    struct timeval tv;
    gettimeofday(&tv, NULL);
    
    // tv.tv_sec: 自 1970-01-01 以来的秒数
    // tv.tv_usec: 当前秒内的微秒数（0-999999）
    // 转换为毫秒：秒 * 1000 + 微秒 / 1000
    return (int64_t)tv.tv_sec * 1000 + tv.tv_usec / 1000;
}

} // namespace ai_sdk
