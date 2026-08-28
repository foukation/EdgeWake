/**
 * @file voice_assistant.h
 * @brief 语音助手相关数据结构定义
 *
 * 此文件定义了语音识别和智能对话相关的数据结构。
 * 这些结构用于 AsrIntelligentDialogue 类的回调函数参数。
 *
 * 使用示例：
 *
 *   #include "ai_sdk/types/voice_assistant.h"
 *   // 或通过 asr_intelligent_dialogue.h 自动包含
 *   // 或通过 types.h 聚合头文件包含
 *
 *   auto& asr = manager.asrIntelligentDialogueHelp();
 *   asr.setCallbacks(
 *       []() { },                                           // 连接成功
 *       [](const ai_sdk::AsrResult& result) {
 *           if (result.is_final) {
 *               // 最终识别结果
 *               printf("识别结果: %s\n", result.text.c_str());
 *           }
 *       },
 *       [](const ai_sdk::DialogueResult& result) {
 *           if (result.is_end) {
 *               // 对话结束，获取完整回答
 *               printf("助手回答: %s\n", result.assistant_answer_content.c_str());
 *           }
 *       },
 *       [](int code, const std::string& msg) { },           // 错误处理
 *       []() { }                                            // 完成
 *   );
 */
#pragma once

#include <string>

namespace ai_sdk {

/**
 * @brief ASR 识别结果结构体
 *
 * 包含语音识别的中间结果或最终结果。
 * 在语音识别过程中，服务器会多次返回识别结果：
 * - 中间结果（is_final = false）：实时显示识别进度
 * - 最终结果（is_final = true）：确认最终识别内容
 */
struct AsrResult {
    /**
     * 识别文本内容
     * 包含当前识别出的语音文本
     *
     * 示例：
     * - 中间结果："你好"、"你好灵犀"
     * - 最终结果："你好灵犀，今天天气怎么样"
     */
    std::string text;

    /**
     * 是否为最终结果
     * - false：中间识别结果，会持续更新
     * - true：最终确认结果，识别结束
     *
     * 建议：
     * - 中间结果用于实时显示（提升用户体验）
     * - 最终结果用于执行指令
     */
    bool is_final;

    /**
     * 情绪标签（仅最终结果包含）
     * 由服务器根据语音内容和语义分析返回的情绪标签。
     * 仅在 fin_result（is_final = true）中存在，mid_result 不返回此字段。
     * 为空字符串表示服务器未返回情绪标签。
     *
     * 可能的值：
     * "happy", "angry", "dejected", "wronged", "thingking",
     * "terrified", "smirk", "confused", "bored", "dizzy", "chaos", "wink"
     */
    std::string emotion;
};

/**
 * @brief 智能对话结果结构体
 *
 * 包含 AI 助手返回的完整对话信息。
 * 支持多种响应类型：文本、TTS、图片、音乐等。
 *
 * 响应类型判断：
 * - 通过 directive 字段快速判断响应类型
 * - 通过 header 和 payload 字段获取详细信息
 */
struct DialogueResult {
    /**
     * 问题唯一标识
     * 用于追踪对话上下文和关联多轮对话
     *
     * 同一个问题的多次流式响应会有相同的 qid
     */
    std::string qid;

    /**
     * 对话结束标志
     * - 0：对话进行中（流式响应）
     * - 1：对话结束（完整响应）
     *
     * 当 is_end = 1 时，assistant_answer_content 包含完整回答
     */
    int is_end = 0;

    /**
     * 助手回答内容
     * AI 生成的完整文本回答
     * 通常在 is_end = 1 时填充
     */
    std::string assistant_answer_content;

    /**
     * 通用渲染指令头部（JSON 字符串格式）
     * 包含 namespace 和 name 字段，标识指令类型
     *
     * 示例：
     * {"namespace": "ai.fxzsos.device_interface.voice_output", "name": "Speak"}
     */
    std::string header;

    /**
     * 指令载荷（JSON 字符串格式）
     * 包含具体指令的数据
     *
     * 示例（Speak - TTS 播放）：
     * {"url": "https://tts.example.com/audio.mp3", "format": "mp3"}
     *
     * 示例（RenderStreamCard - 流式文本）：
     * {"answer": "今天天气..."}
     *
     * 示例（Play - 音乐播放）：
     * {"url": "https://music.example.com/song.mp3", "title": "歌曲名"}
     */
    std::string payload;

    /**
     * 指令名称（便捷字段）
     * 从 header 中提取的 name 值，方便快速判断指令类型
     *
     * 常见值：
     * - "Speak"：TTS 语音播放
     * - "RenderStreamCard"：流式文本渲染
     * - "Play"：音乐/音频播放
     * - "RenderMultiImageCard"：多图片展示
     * - "StopListen"：停止监听
     */
    std::string directive;
};

}  // namespace ai_sdk

