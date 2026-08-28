package com.fxzs.lingxiagent.conversation

import com.fxzs.lingxiagent.lingxi.lingxi_conversation.TabEntity


/**
 * AI对话状态管理器
 * 用于管理AI对话的状态转换和并发控制
 */
class AIConversationManager {
    /**
     * 对话状态枚举
     * IDLE: 空闲状态，可以发起新对话
     * REQUESTING: 已发送请求，等待响应
     * STREAMING: 正在接收流式响应
     * ERROR: 发生错误
     */
    enum class ConversationState {
        IDLE,           // 空闲状态
        REQUESTING,     // 已发送请求，等待响应
        STREAMING,      // 正在接收流式响应
        ERROR          // 发生错误
    }

//    // 当前对话状态
//    private var currentState: ConversationState = ConversationState.IDLE

    // 当前对话的请求ID，用于追踪请求
    private var currentRequestId: String? = null

    // 是否允许中断当前对话
    private var allowInterrupt: Boolean = true

    // 错误回调，用于向外部通知错误信息
    private var errorCallback: ((String) -> Unit)? = null

    /**
     * 发起新的对话请求
     * @param requestId 请求ID，用于追踪请求
     * @return 是否成功发起请求
     */
    fun startConversation(requestId: String): Boolean {
        return when (currentState) {
            ConversationState.IDLE -> {
                // 空闲状态，可以发起新对话
                currentState = ConversationState.REQUESTING
                currentRequestId = requestId
                true
            }

            ConversationState.REQUESTING -> {
                // 已有请求在处理中
                if (currentRequestId == requestId) {
                    // 同一个请求重复调用，这通常是业务逻辑错误
                    errorCallback?.invoke("请求已在处理中，请勿重复发起")
                    false
                } else {
                    // 不同请求，提示用户等待
                    errorCallback?.invoke("已有请求正在处理中，请稍候...")
                    false
                }
            }

            ConversationState.STREAMING -> {
                // 正在接收流式响应
                if (currentRequestId == requestId) {
                    // 同一个请求重复调用，这通常是业务逻辑错误
                    errorCallback?.invoke("请求正在接收响应，请勿重复发起")
                    false
                } else if (allowInterrupt) {
                    // 允许中断，停止当前对话，开始新对话
                    stopCurrentConversation()
                    currentState = ConversationState.REQUESTING
                    currentRequestId = requestId
                    true
                } else {
                    // 不允许中断，提示用户等待
                    errorCallback?.invoke("当前对话正在进行中，请等待完成...")
                    false
                }
            }

            ConversationState.ERROR -> {
                // 错误状态，重置后可以发起新对话
                resetState()
                currentState = ConversationState.REQUESTING
                currentRequestId = requestId
                true
            }
        }
    }

    /**
     * 开始接收流式响应
     * @param requestId 请求ID，用于验证是否是当前请求
     */
    fun startStreaming(requestId: String) {
        // 只有在 REQUESTING 状态且是当前请求时，才允许开始流式响应
        if (currentState == ConversationState.REQUESTING && currentRequestId == requestId) {
            currentState = ConversationState.STREAMING
        }
    }

    /**
     * 结束当前对话
     * @param requestId 请求 ID，用于验证是否是当前请求
     */
    fun endConversation(requestId: String, tabType: TabEntity.TabType) {

        if (tabType.value == TabEntity.TabType.MEDICINE.value) return
        if (tabType.value == TabEntity.TabType.TRAVEL.value) return
        if (tabType.value == TabEntity.TabType.TRIP_PLANNER_HONOR.value) return
        if (tabType.value == TabEntity.TabType.TRANSLATE.value) return

        endConversation(requestId)
    }

    /**
     * 结束当前对话
     * @param requestId 请求 ID，用于验证是否是当前请求
     */
    fun endConversation(requestId: String) {

        // 只有在 STREAMING 或 REQUESTING 状态且是当前请求时，才允许结束对话
        if ((currentState == ConversationState.STREAMING ||
                    currentState == ConversationState.REQUESTING) /*&&
            currentRequestId == requestId*/
        ) {
            currentState = ConversationState.IDLE
            currentRequestId = null
        }
    }

    /**
     * 停止当前对话
     * 用于中断当前对话，重置状态
     */
    private fun stopCurrentConversation() {
        currentState = ConversationState.IDLE
        currentRequestId = null
    }

    /**
     * 重置状态
     * 用于错误恢复
     */
    private fun resetState() {
        currentState = ConversationState.IDLE
        currentRequestId = null
    }

    /**
     * 设置是否允许中断当前对话
     * @param allow 是否允许中断
     */
    fun setAllowInterrupt(allow: Boolean) {
        allowInterrupt = allow
    }

    /**
     * 设置错误回调
     * @param callback 错误回调函数
     */
    fun setErrorCallback(callback: (String) -> Unit) {
        errorCallback = callback
    }

    /**
     * 获取当前状态
     * @return 当前对话状态
     */
    fun getCurrentState(): ConversationState = currentState

    /**
     * 获取当前请求ID
     * @return 当前请求ID
     */
    fun getCurrentRequestId(): String? = currentRequestId

    companion object {
        // 当前对话状态
        public var currentState: ConversationState = ConversationState.IDLE
    }
}