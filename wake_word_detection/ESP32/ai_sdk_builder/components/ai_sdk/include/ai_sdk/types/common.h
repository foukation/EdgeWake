/**
 * @file common.h
 * @brief 通用常量和状态码定义
 *
 * 此文件定义了 AI SDK 中使用的通用常量和状态码。
 * 这些常量用于解析 API 响应结果，判断请求成功或失败原因。
 *
 * 使用方式：
 * @code
 * #include "ai_sdk/types/common.h"
 * // 或通过聚合头文件
 * #include "ai_sdk/types.h"
 *
 * if (response.code == ai_sdk::ResCode::SUC) {
 *     // 请求成功
 * }
 * @endcode
 */
#pragma once

namespace ai_sdk {

/**
 * @namespace ResCode
 * @brief API 响应状态码常量定义
 *
 * 用于解析云端 API 返回的状态码。
 * 所有 API 响应中的 code/status 字段都可以用这些常量进行判断。
 */
namespace ResCode {
    /** 请求成功 */
    constexpr int SUC = 0;

    /** 认证错误：token 无效或已过期 */
    constexpr int AUTH_ERR = 401;

    /** 参数错误：请求参数缺失或格式不正确 */
    constexpr int PARAM_ERR = 400;

    /** 服务器内部错误 */
    constexpr int SERVE_ERR = 500;

    /** 图片格式错误或图片未传递 */
    constexpr int IMAGE_ERR = 216101;
}

}  // namespace ai_sdk

