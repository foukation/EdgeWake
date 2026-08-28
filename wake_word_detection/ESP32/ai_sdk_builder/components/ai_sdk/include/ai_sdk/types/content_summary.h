/**
 * @file content_summary.h
 * @brief 内容摘要数据结构定义
 *
 * 此文件定义了内容摘要功能的数据结构：
 * - ContentSummaryRequest: 请求参数
 * - ContentSummaryResponse: 响应结果
 *
 * 使用示例:
 * @code
 * ContentSummaryRequest request;
 * request.content = "这是一段很长的文本内容...";
 * request.stream = true;
 * request.language = "Chinese";
 *
 * kit.contentSummary(request,
 *     [](const ContentSummaryResponse& resp) {
 *         printf("摘要: %s\n", resp.data.content.c_str());
 *     },
 *     [](const std::string& error) {
 *         printf("错误: %s\n", error.c_str());
 *     });
 * @endcode
 */
#pragma once

#include <string>

namespace ai_sdk {

/**
 * @struct ContentSummaryRequest
 * @brief 内容摘要请求参数
 */
struct ContentSummaryRequest {
    /**
     * @brief 需要进行摘要处理的原始内容文本
     *
     * 必选参数。
     * 可以包含会议纪要、文档正文等需要处理的文本内容。
     * 服务器将基于此内容生成摘要。
     */
    std::string content;

    /**
     * @brief 是否启用流式响应模式
     *
     * 可选参数，默认为 true。
     * - true: 启用流式传输，服务器将逐步返回处理结果
     * - false: 使用标准响应模式，等待完整处理结果后一次性返回
     */
    bool stream = true;

    /**
     * @brief 指定返回摘要的语言
     *
     * 可选参数，默认为 "auto"。
     * - "auto": 自动检测语言
     * - "Chinese": 中文
     * - "English": 英文
     * - "French": 法文
     * - 其他支持的语言...
     */
    std::string language = "auto";
};

/**
 * @struct ContentData
 * @brief 内容数据结构
 */
struct ContentData {
    /**
     * @brief 实际的内容文本
     *
     * 包含生成的摘要内容。
     * 流式模式下为增量内容，非流式模式下为完整内容。
     */
    std::string content;
};

/**
 * @struct ContentSummaryResponse
 * @brief 内容摘要响应结果
 */
struct ContentSummaryResponse {
    /**
     * @brief 消息信息
     *
     * 通常用于表示请求状态的描述。
     */
    std::string msg;

    /**
     * @brief 内容数据
     *
     * 包含实际的摘要内容。
     */
    ContentData data;

    /**
     * @brief 日志标识符
     *
     * 用于跟踪和调试。
     */
    std::string logId;

    /**
     * @brief 状态码
     *
     * 表示请求的处理状态。
     * 0 表示成功。
     */
    int status = 0;
};

}  // namespace ai_sdk
