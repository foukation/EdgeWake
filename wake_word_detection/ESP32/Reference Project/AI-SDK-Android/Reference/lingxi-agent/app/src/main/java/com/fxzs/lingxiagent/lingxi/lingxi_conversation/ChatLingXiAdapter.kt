package com.fxzs.lingxiagent.lingxi.lingxi_conversation;

import android.annotation.SuppressLint
import android.util.Log
import com.cmdc.ai.assist.AIAssistantManager
import com.cmdc.ai.assist.api.AIFoundationKit
import com.cmdc.ai.assist.api.AISessionManager
import com.cmdc.ai.assist.api.AISessionManager.buildMessagesInsideRcChat
import com.cmdc.ai.assist.constraint.DialogueResult
import com.cmdc.ai.assist.constraint.InsideRcChatRequest
import com.fxzs.lingxiagent.conversation.AIConversationManager
import com.fxzs.lingxiagent.model.honor.repository.BenefitCode
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ChatLingXiAdapter(
    private val aiConversationManager: AIConversationManager,
    private val requestId: String
) {
    private val aiFoundationKit = AIFoundationKit()
    private var messageIndex = 0
    private val domainList = mutableListOf<String>()
    private val intentList = mutableListOf<String>()
    var isBreakFlow: Boolean = false


    init {
        // 出行 域
        domainList.add(IntentDomain.TRAVEL.alias)

        domainList.add(IntentDomain.SYSTEM_CONTROL.alias)
        domainList.add(IntentDomain.PHONE.alias)
        domainList.add(IntentDomain.CAR_CONTROL.alias)
        domainList.add(IntentDomain.ALARM.alias)
        domainList.add(IntentDomain.TELECOMSERVICE.alias)
        domainList.add(IntentDomain.HEALTHCARE.alias)
        domainList.add(IntentDomain.CUSTOMERSERVICE.alias)
        domainList.add(IntentDomain.MEMBERSHIP.alias)

    }

    init {
        // 设备控制相关意图
        intentList.add(SystemControlIntent.APP.alias)
        // 导航相关意图
        intentList.add(NavIntent.NAV_AIGuide.alias)
        intentList.add(NavIntent.NAV_POI.alias)
        intentList.add(NavIntent.NAV_NAV.alias)
        // gui 操作相关意图
        intentList.add("")
        // 出行 相关意图
        intentList.add("")
        // 同城聚会 相关意图
        intentList.add("")
        // 音乐播放 相关意图
        intentList.add(MediaIntent.MEDIA_MUSIC.alias)
        // 媒体 相关意图
        intentList.add(MediaIntent.MEDIA_VIDEOPLY.alias)
//        intentList.add(MediaIntent.MEDIA_UNICAST.alias)
    }

    @SuppressLint("BinaryOperationInTimber")
    fun insideRcChat(content: String, callback: (Any?) -> Any?): Disposable {
        try {
            val messages = AISessionManager.getChatDataList().buildMessagesInsideRcChat()
            messages.add(
                InsideRcChatRequest.Message(
                    role = "user",
                    content
                )
            )

            aiFoundationKit.insideRcChat(
                InsideRcChatRequest(
                    qid = UUID.randomUUID().toString(),
                    third_user_id = UUID.randomUUID().toString(),
                    cuid = AIAssistantManager.getInstance().aiAssistConfig.deviceId,
                    messages = messages,
                    stream = true,
                    dialog_request_id = UUID.randomUUID().toString()
                ),
                responseCallback@{ response ->
                    if (isBreakFlow) return@responseCallback
                    processDialogueResponse(callback, response)
                    if (messageIndex == 0 && response.is_end == 0) {
                        aiConversationManager.startStreaming(requestId)
                        messageIndex++
                    }
                    if (response.is_end == 1) {
                        aiConversationManager.endConversation(requestId)
                        messageIndex++
                    }
                }, { error ->
                    Log.d("Billfit", error.toString())
                    val errorMsg = error.message ?: ""

                    val index = errorMsg.indexOf("rawJson=")
                    if (index != -1) {
                        try {
                            val jsonStr = errorMsg.substring(index + 8)
                            val jsonObject = JSONObject(jsonStr)

                            val code = jsonObject.optInt("code")
                            val msg = jsonObject.optString("msg")

                            val isBenefitError = BenefitCode.isBenefitError(code.toString())

                            if (isBenefitError) {
                                callback(msg)
                            }

                        } catch (_: Exception) {
                            Log.d("Billfit", "Benefit parse error")
                        }
                    }
                    println(error)
                    aiConversationManager.endConversation(requestId)
                }
            )
            return Disposables.fromAction {
                isBreakFlow = true
                aiConversationManager.endConversation(requestId)
            }
        } catch (e: Exception) {
            callback(null)
            println(e.message)
            aiConversationManager.endConversation(requestId)
            return Disposables.disposed()
        }
    }

    /**
     * 处理对话响应结果，提取 NLU 信息并根据条件决定是否回调
     *
     * @param callback 处理响应结果的回调函数，当满足条件时会调用该函数
     * @param response 对话响应结果对象
     */
    private fun processDialogueResponse(
        callback: (DialogueResult?) -> Any?,
        response: DialogueResult
    ) {
        if (isBreakFlow) return
        callback(response)

        if (response.header == null) return
        if (response.payload == null) return
        if (!response.header!!.has("name")) return
        if (response.header!!.optString("name") != NameType.NLU.alias) return
        val nlu: JSONArray? = response.payload!!.optJSONArray("nlu")
        val nluFir = nlu!![0] as JSONObject
        val domain = nluFir.optString("domain")
        val intent = nluFir.optString("intent")

        if (domainList.contains(domain) || intentList.contains(intent)) {
            isBreakFlow = true
        }
    }
}