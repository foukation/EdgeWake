/**
 * @file speech_recognition_persistent_result.h
 * @brief 持续语音识别结果数据类型定义
 *
 * 服务端 JSON 响应示例（endpoint: /app-ws/v1/long-asr）：
 * {
 *   "err_no": 0,
 *   "err_msg": "OK",
 *   "log_id": 1234567890,
 *   "sn": "uuid_ws_001",
 *   "type": "FIN_TEXT",
 *   "result": "今天天气真不错",
 *   "start_time": 0,
 *   "end_time": 1500,
 *   "product_id": 15372,
 *   "product_line": "open"
 * }
 *
 * 注意：
 * - 服务端使用 VAD 自动分割语音段，每段结束后返回一条结果
 * - 连接不会自动断开，一次连接可收到多条结果（持续识别的核心）
 * - 所有服务端消息全量反序列化后透传给回调，调用方应检查 err_no 和 type
 */
#pragma once
#include <string>

namespace ai_sdk {

/**
 * @brief 持续语音识别结果
 *
 * 服务端每完成一段语音识别（由服务端 VAD 检测端点）即返回一条此结构。
 * 通过 SpeechRecognitionPersistent::ResultCallback 回调给上层。
 *
 * 字段名称与服务端 JSON key 严格对应。
 */
struct SpeechRecognitionPersistentResult {
    int         err_no;        ///< 错误码，0=成功，非0=本段识别失败（JSON: "err_no"）
    std::string err_msg;       ///< 错误描述，成功时为 "OK"（JSON: "err_msg"）
    long        log_id;        ///< 服务端日志追踪 ID（JSON: "log_id"）
    std::string sn;            ///< 本段识别的序列号，格式 "uuid_ws_序号"（JSON: "sn"）
    std::string type;          ///< 消息类型，当前已知值为 "FIN_TEXT"（JSON: "type"）
    std::string result;        ///< 识别出的文本内容（JSON: "result"）
    long        start_time;    ///< 本段语音在音频流中的起始时间，单位 ms（JSON: "start_time"）
    long        end_time;      ///< 本段语音在音频流中的结束时间，单位 ms（JSON: "end_time"）
    int         product_id;    ///< 识别所用语言模型 ID，与 START 帧 dev_pid 对应（JSON: "product_id"）
    std::string product_line;  ///< 业务线标识，如 "open"（JSON: "product_line"）
};

} // namespace ai_sdk
