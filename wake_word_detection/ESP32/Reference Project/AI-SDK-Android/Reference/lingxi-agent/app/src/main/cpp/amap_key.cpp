/**
 * amap_key.cpp
 *
 * 高德地图 API Key 安全存储实现
 * 将 Key 拆分为多段字符数组，在运行时动态拼接，防止静态字符串扫描
 *
 * 包名：com.fxzs.lingxiagent
 */

#include <jni.h>
#include <string>
#include <algorithm>

// ──────────────────────────────────────────────────────────────────
//  将 Key 拆成 4 段，每段再做简单的字节偏移混淆（XOR 0x5A）
//  Key 原文: 3ce8e0e1265c65c636789804c3571b2c
//
//  生成步骤（Python 示例）：
//    key = "3ce8e0e1265c65c636789804c3571b2c"
//    parts = [key[i:i+8] for i in range(0, len(key), 8)]
//    for p in parts:
//        print([hex(ord(c) ^ 0x5A) for c in p])
// ──────────────────────────────────────────────────────────────────

// 每段异或 0x5A 后的字节值
static const unsigned char kP0[] = {0x69, 0x39, 0x3f, 0x62, 0x3f, 0x6a, 0x3f, 0x6b}; // "3ce8e0e1"
static const unsigned char kP1[] = {0x68, 0x6c, 0x6f, 0x39, 0x6c, 0x6f, 0x39, 0x6c}; // "265c65c6"
static const unsigned char kP2[] = {0x69, 0x6c, 0x6d, 0x62, 0x63, 0x62, 0x6a, 0x6e}; // "36789804"
static const unsigned char kP3[] = {0x39, 0x69, 0x6f, 0x6d, 0x6b, 0x38, 0x68, 0x39}; // "c3571b2c"

static const size_t kPartLen = 8;
static const size_t kKeyLen  = 32;

/**
 * 运行时解码并拼接完整 Key
 */
static std::string decodeKey() {
    std::string key;
    key.reserve(kKeyLen);

    const unsigned char* parts[] = {kP0, kP1, kP2, kP3};
    for (auto part : parts) {
        for (size_t i = 0; i < kPartLen; i++) {
            key += static_cast<char>(part[i] ^ 0x5A);
        }
    }
    return key;
}


// ──────────────────────────────────────────────────────────────────
//  JNI 导出函数
//  Java/Kotlin 签名：com.fxzs.lingxiagent.AMapKeyProvider.getApiKey()
//
//  规则：Java_ + 包名（点换下划线）+ _类名 + _方法名
// ──────────────────────────────────────────────────────────────────

extern "C"
JNIEXPORT jstring JNICALL
Java_com_fxzs_lingxiagent_util_AMapKeyProvider_getApiKey(
        JNIEnv* env,
        jobject /* this */) {
    std::string key = decodeKey();
    return env->NewStringUTF(key.c_str());
}
