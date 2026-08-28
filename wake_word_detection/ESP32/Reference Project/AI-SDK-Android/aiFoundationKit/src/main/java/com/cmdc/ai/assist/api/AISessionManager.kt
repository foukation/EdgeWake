package com.cmdc.ai.assist.api

import android.text.TextUtils
import com.cmdc.ai.assist.constraint.ChatData
import com.cmdc.ai.assist.constraint.InsideRcChatRequest
import com.cmdc.ai.assist.constraint.InsiderRcMessage
import org.json.JSONObject
import kotlin.math.max

/**
 * AI 会话管理器
 *
 * 这是一个单例对象，负责管理 AI 对话的会话状态、历史消息记录和消息格式转换。
 * 主要功能包括：
 * - 存储和管理聊天历史记录
 * - 提供会话初始化和重置功能
 * - 将聊天数据转换为不同API所需的消息格式
 * - 支持多轮对话的上下文管理
 *
 * 使用场景：
 * - AI助手的多轮对话管理
 * - 聊天历史记录的持久化存储
 * - 不同API接口间的消息格式转换
 */
object AISessionManager {

    /**
     * 聊天数据列表，用于存储对话历史记录
     *
     * 这个列表维护了完整的对话历史，包括：
     * - 用户发送的消息（isSend = true）
     * - AI助手的回复消息（isSend = false）
     * - 相关的媒体信息、生成状态等辅助数据
     *
     * 注意：这是一个私有变量，外部只能通过 getChatDataList() 方法访问
     */
    private val chatDataList: MutableList<ChatData> = ArrayList()

    /**
     * 初始化新会话
     *
     * 清空当前所有的聊天历史记录，开始一个全新的对话会话。
     * 通常在以下场景调用：
     * - 用户主动开始新的对话
     * - 系统检测到需要清空上下文时
     * - 应用重启或会话超时时
     */
    fun initSession() {
        chatDataList.clear()
    }

    /**
     * 获取聊天数据列表
     *
     * 返回当前会话中的所有聊天数据，包括用户消息和AI回复。
     * 外部代码可以通过此方法访问和操作聊天历史记录。
     *
     * @return 返回一个可变的聊天数据列表，列表元素类型为 [ChatData]
     *
     * 使用示例：
     * ```kotlin
     * val chatList = AISessionManager.getChatDataList()
     * chatList.add(ChatData(true, "用户消息"))
     * ```
     */
    fun getChatDataList(): MutableList<ChatData> {
        return chatDataList
    }

    /**
     * 将聊天数据列表转换为 InsiderRcMessage 格式的消息列表
     *
     * 这是一个扩展函数，用于将内部的 ChatData 格式转换为 InsiderRcMessage 格式，
     * 主要用于与特定的AI服务API进行交互。
     *
     * 转换逻辑：
     * - 只有当列表包含至少2条消息时才进行转换（确保有对话上下文）
     * - 最多取最近的20条消息（避免上下文过长影响性能）
     * - 用户消息：role="user"，content为消息文本
     * - AI回复：role="assistant"，从rawAssistAnswer中解析content和source
     *
     * @return 返回转换后的 InsiderRcMessage 列表，用于API调用
     *
     * 注意事项：
     * - 如果rawAssistAnswer解析失败，该条消息会被跳过
     * - 返回的列表可能少于原始列表的条目数
     */
    fun MutableList<ChatData>.buildMessages(): MutableList<InsiderRcMessage> {
        val messages: MutableList<InsiderRcMessage> = ArrayList()
        if (size >= 2) {
            val chatDataList: List<ChatData> = this
            val subList: List<ChatData> = chatDataList
                .subList(max(0, (chatDataList.size - 20)), chatDataList.size)
            for (chatData in subList) {
                if (chatData.isSend) {
                    messages.add(InsiderRcMessage("user", chatData.text, "", "", Any()))
                } else {
                    if (!TextUtils.isEmpty(chatData.rawAssistAnswer)) {
                        try {
                            val assistAnswer = JSONObject(chatData.rawAssistAnswer!!)
                            messages.add(
                                InsiderRcMessage(
                                    "assistant",
                                    assistAnswer.optString("content"),
                                    "",
                                    assistAnswer.optString("source"),
                                    Any()
                                )
                            )
                        } catch (e: java.lang.Exception) {
                            // ..
                        }
                    }
                }
            }
        }

        return messages
    }

    /**
     * 将聊天数据列表转换为 InsideRcChatRequest.Message 格式的消息列表
     *
     * 这是另一个扩展函数，用于将内部的 ChatData 格式转换为 InsideRcChatRequest.Message 格式，
     * 主要用于设备端NLU及聊天API的调用。
     *
     * 与 buildMessages() 的区别：
     * - 使用不同的消息类型（InsideRcChatRequest.Message）
     * - 消息结构更简单，只包含role和content字段
     * - 适用于不同的API接口
     *
     * 转换逻辑：
     * - 同样只处理包含至少2条消息的列表
     * - 最多取最近的20条消息
     * - 用户消息：role="user"，content为消息文本
     * - AI回复：role="assistant"，从rawAssistAnswer中解析content
     *
     * @return 返回转换后的 InsideRcChatRequest.Message 列表，用于API调用
     *
     * 使用场景：
     * - 调用设备端NLU及聊天API时
     * - 需要传递对话历史上下文时
     * - 多轮对话的上下文管理
     */
    fun MutableList<ChatData>.buildMessagesInsideRcChat(): MutableList<InsideRcChatRequest.Message> {
        val messages: MutableList<InsideRcChatRequest.Message> = ArrayList()
        if (size >= 2) {
            val chatDataList: List<ChatData> = this
            val subList: List<ChatData> = chatDataList
                .subList(max(0, (chatDataList.size - 20)), chatDataList.size)
            for (chatData in subList) {
                if (chatData.isSend) {
                    messages.add(
                        InsideRcChatRequest.Message(
                            role = "user",
                            content = chatData.text
                        )
                    )
                } else {
                    if (!TextUtils.isEmpty(chatData.rawAssistAnswer)) {
                        try {
                            val assistAnswer = JSONObject(chatData.rawAssistAnswer!!)
                            messages.add(
                                InsideRcChatRequest.Message(
                                    role = "assistant",
                                    content = assistAnswer.optString("content"),
                                )
                            )
                        } catch (e: java.lang.Exception) {
                            // ..
                        }
                    }
                }
            }
        }

        return messages
    }

}