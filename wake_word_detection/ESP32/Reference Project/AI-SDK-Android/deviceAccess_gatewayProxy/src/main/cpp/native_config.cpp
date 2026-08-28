#include <jni.h>
#include <string>
#include <cstdint>

// ============================================
// 第一部分：加密的 URL 数据
// ============================================

namespace {
    // XOR 密钥
    constexpr uint8_t XOR_KEY = 0x5A;
    
    // 原始: "https://ivs.chinamobiledevice.com:11443"
    constexpr uint8_t ENCRYPTED_TERMINAL_URL[] = {
        0x32, 0x2e, 0x2e, 0x2a, 0x29, 0x60, 0x75, 0x75, 0x33, 0x2c, 0x29, 0x74, 0x39, 0x32, 0x33, 0x34, 0x3b, 0x37, 0x35, 0x38, 0x33, 0x36, 0x3f, 0x3e, 0x3f, 0x2c, 
        0x33, 0x39, 0x3f, 0x74, 0x39, 0x35, 0x37, 0x60, 0x6b, 0x6b, 0x6e, 0x6e, 0x69
    };
    constexpr size_t ENCRYPTED_TERMINAL_URL_LEN = sizeof(ENCRYPTED_TERMINAL_URL);
    
    // 原始: "https://aqua-digital.aipaas.com"
    constexpr uint8_t ENCRYPTED_AIPAAS_URL[] = {
        0x32, 0x2e, 0x2e, 0x2a, 0x29, 0x60, 0x75, 0x75, 0x3b, 0x2b, 0x2f, 0x3b, 0x77, 0x3e, 0x33, 0x3d, 0x33, 0x2e, 0x3b, 0x36, 0x74, 0x3b, 0x33, 0x2a, 0x3b, 0x3b, 
        0x29, 0x74, 0x39, 0x35, 0x37
    };
    constexpr size_t ENCRYPTED_AIPAAS_URL_LEN = sizeof(ENCRYPTED_AIPAAS_URL);
    
    // 原始: "wss://ivs.chinamobiledevice.com:11443"
    constexpr uint8_t ENCRYPTED_WSS_URL[] = {
        0x2d, 0x29, 0x29, 0x60, 0x75, 0x75, 0x33, 0x2c, 0x29, 0x74, 0x39, 0x32, 0x33, 0x34, 0x3b, 0x37, 0x35, 0x38, 0x33, 0x36, 0x3f, 0x3e, 0x3f, 0x2c, 0x33, 0x39, 
        0x3f, 0x74, 0x39, 0x35, 0x37, 0x60, 0x6b, 0x6b, 0x6e, 0x6e, 0x69
    };
    constexpr size_t ENCRYPTED_WSS_URL_LEN = sizeof(ENCRYPTED_WSS_URL);
    
    // 原始: "https://ivs.chinamobiledevice.com:11443/ai-admin-beta"
    constexpr uint8_t ENCRYPTED_TERMINAL_URL_TEST[] = {
        0x32, 0x2e, 0x2e, 0x2a, 0x29, 0x60, 0x75, 0x75, 0x33, 0x2c, 0x29, 0x74, 0x39, 0x32, 0x33, 0x34, 0x3b, 0x37, 0x35, 0x38, 0x33, 0x36, 0x3f, 0x3e, 0x3f, 0x2c, 
        0x33, 0x39, 0x3f, 0x74, 0x39, 0x35, 0x37, 0x60, 0x6b, 0x6b, 0x6e, 0x6e, 0x69, 0x75, 0x3b, 0x33, 0x77, 0x3b, 0x3e, 0x37, 0x33, 0x34, 0x77, 0x38, 0x3f, 0x2e, 0x3b
    };
    constexpr size_t ENCRYPTED_TERMINAL_URL_TEST_LEN = sizeof(ENCRYPTED_TERMINAL_URL_TEST);
    
    // 原始: "wss://ivs.chinamobiledevice.com:11443/ai-admin-beta/app-ws/v2/asr"
    constexpr uint8_t ENCRYPTED_WSS_URL_TEST[] = {
        0x2d, 0x29, 0x29, 0x60, 0x75, 0x75, 0x33, 0x2c, 0x29, 0x74, 0x39, 0x32, 0x33, 0x34, 0x3b, 0x37, 0x35, 0x38, 0x33, 0x36, 0x3f, 0x3e, 0x3f, 0x2c, 0x33, 0x39, 
        0x3f, 0x74, 0x39, 0x35, 0x37, 0x60, 0x6b, 0x6b, 0x6e, 0x6e, 0x69, 0x75, 0x3b, 0x33, 0x77, 0x3b, 0x3e, 0x37, 0x33, 0x34, 0x77, 0x38, 0x3f, 0x2e, 0x3b, 0x75, 0x3b, 0x2a, 0x2a, 0x77, 0x2d, 0x29, 0x75, 0x2c, 0x68, 0x75, 0x3b, 0x29, 0x28
    };
    constexpr size_t ENCRYPTED_WSS_URL_TEST_LEN = sizeof(ENCRYPTED_WSS_URL_TEST);
    
    // ============================================
    // 第二部分：解密函数
    // ============================================
    
    std::string decryptUrl(const uint8_t* encrypted, size_t len) {
        std::string result;
        result.reserve(len);
        for (size_t i = 0; i < len; ++i) {
            result += static_cast<char>(encrypted[i] ^ XOR_KEY);
        }
        return result;
    }
    
    // ============================================
    // 第三部分：可选的安全检查
    // ============================================
    
    // 简单的安全检查（可扩展）
    bool isSecure() {
        // 这里可以添加反调试、完整性检查等
        // 简化版本直接返回 true
        return true;
    }
    
    std::string getUrlSafely(const uint8_t* encrypted, size_t len) {
        if (!isSecure()) {
            // 如果检测到不安全环境，返回空字符串
            return "";
        }
        return decryptUrl(encrypted, len);
    }
}

// ============================================
// 第四部分：JNI 导出函数
// ============================================

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_cmdc_ai_assist_native_NativeUrlProvider_getTerminalServiceUrl(
        JNIEnv* env, jobject /* this */) {
    std::string url = getUrlSafely(ENCRYPTED_TERMINAL_URL, ENCRYPTED_TERMINAL_URL_LEN);
    return env->NewStringUTF(url.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_cmdc_ai_assist_native_NativeUrlProvider_getAiPaasUrl(
        JNIEnv* env, jobject /* this */) {
    std::string url = getUrlSafely(ENCRYPTED_AIPAAS_URL, ENCRYPTED_AIPAAS_URL_LEN);
    return env->NewStringUTF(url.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_cmdc_ai_assist_native_NativeUrlProvider_getWebSocketUrl(
        JNIEnv* env, jobject /* this */) {
    std::string url = getUrlSafely(ENCRYPTED_WSS_URL, ENCRYPTED_WSS_URL_LEN);
    return env->NewStringUTF(url.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_cmdc_ai_assist_native_NativeUrlProvider_getTerminalServiceUrlTest(
        JNIEnv* env, jobject /* this */) {
    std::string url = getUrlSafely(ENCRYPTED_TERMINAL_URL_TEST, ENCRYPTED_TERMINAL_URL_TEST_LEN);
    return env->NewStringUTF(url.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_cmdc_ai_assist_native_NativeUrlProvider_getWebSocketUrlTest(
        JNIEnv* env, jobject /* this */) {
    std::string url = getUrlSafely(ENCRYPTED_WSS_URL_TEST, ENCRYPTED_WSS_URL_TEST_LEN);
    return env->NewStringUTF(url.c_str());
}

} // extern "C"

