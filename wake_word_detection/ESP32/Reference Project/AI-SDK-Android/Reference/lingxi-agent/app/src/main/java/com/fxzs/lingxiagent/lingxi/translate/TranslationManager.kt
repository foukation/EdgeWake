package com.fxzs.lingxiagent.lingxi.translate

import android.annotation.SuppressLint
import android.content.Context
import com.cmdc.ai.assist.AIAssistantManager
import com.cmdc.ai.assist.api.AIFoundationKit
import com.cmdc.ai.assist.api.ASRTranslation
import com.cmdc.ai.assist.constraint.Language
import com.cmdc.ai.assist.constraint.LanguageConvert
import com.cmdc.ai.assist.constraint.LanguageConvertModel
import com.cmdc.ai.assist.constraint.TranslateResponse
import com.cmdc.ai.assist.constraint.TranslationData
import com.cmdc.ai.assist.constraint.TranslationRequest
import com.cmdc.ai.assist.constraint.TranslationTypeCode
import com.fxzs.lingxiagent.model.honor.repository.BenefitCode
import org.json.JSONObject
import timber.log.Timber
import java.nio.ByteBuffer


class TranslationManager() {

    private val TAG: String = TranslationManager::class.java.simpleName

    private lateinit var context: Context
    private lateinit var asrTranslation: ASRTranslation
    private lateinit var callBack: TranslationCallback

    private var errorCallback: TranslationErrorCallback? = null

    interface TranslationCallback {
        fun onAsrMidResult(midResult: String)
        fun onAsrFinalResult(finalResult: String)
        fun onTranslationResult(translationResult: String)
        fun onComplete()
    }

    private val aiFoundationKit by lazy {
        AIFoundationKit()
    }

    constructor(context: Context, callBack: TranslationCallback, errorCallback: TranslationErrorCallback) : this() {
        this.context = context
        this.callBack = callBack
        this.errorCallback = errorCallback
        initASRTranslation()
    }

    constructor(context: Context, errorCallback: TranslationErrorCallback) : this() {
        this.errorCallback = errorCallback
        this.context = context
    }

    interface TranslationErrorCallback {
        fun onBenefitError(msg: String)
    }

    private fun initASRTranslation() {
        asrTranslation = AIAssistantManager.getInstance().asrTranslationHelp() as ASRTranslation

        asrTranslation.setListener(object : ASRTranslation.ASRTranslationListener {
            override fun onMessageReceived(message: TranslationData?) {
                Timber.tag(TAG).d("type： ${message?.type}")
                Timber.tag(TAG).d("asrResult： ${message?.asrResult}")
                Timber.tag(TAG).d("translationResult： ${message?.translationResult}")

                if (message?.type.equals("MID")) {
                    message?.asrResult?.let {
                        sendAsrMidResultMessage(it)
                    }
                    return
                }
                if (message?.type.equals("FIN")) {
                    message?.asrResult?.let {
                        sendAsrMidResultMessage(it)
                    }
                    message?.asrResult?.let {
                        sendAsrFinalResultMessage(it)
                    }
                    message?.translationResult?.let {
                        sendTranslationResultMessage(it)
                    }
                    return
                }

            }

            override fun onMessageReceived(bytes: ByteBuffer?) {
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Timber.tag(TAG).d("onClose： $reason")
                sendComplete()
            }

            override fun onError(ex: Exception?) {
                Timber.tag(TAG).d("onError： $ex")
            }
        })
    }

    /**
     * 执行中文到英文的翻译
     * 该函数会检查 asrTranslation 是否已初始化，如果未初始化则直接返回，
     * 否则启动中文到英文的识别翻译流程。
     */
    private fun translationZH_TO_EN() {
        if (!::asrTranslation.isInitialized) return
        asrTranslation.startRecognition(TranslationTypeCode.ZH_TO_EN)
    }

    /**
     * 执行英文到中文的翻译
     * 该函数会检查 asrTranslation 是否已初始化，如果未初始化则直接返回，
     * 否则启动英文到中文的识别翻译流程。
     */
    private fun translationEN_TO_ZH() {
        if (!::asrTranslation.isInitialized) return
        asrTranslation.startRecognition(TranslationTypeCode.EN_TO_ZH)
    }

    /**
     * 根据指定的语言进行翻译
     * @param fromLang 源语言，例如："中文"、"英语"等
     * @param toLang 目标语言，例如："中文"、"英语"等
     * 该函数会检查 asrTranslation 是否已初始化，如果未初始化则直接返回，
     * 否则启动从源语言到目标语言的语音识别翻译流程。
     */
    fun translation(fromLang: String, toLang: String) {
        if (!::asrTranslation.isInitialized) return
        asrTranslation.startRecognition(convertLanguageCode(fromLang), convertLanguageCode(toLang))
    }

    /**
     * 释放翻译资源
     * 该函数会检查 asrTranslation 是否已初始化，如果未初始化则直接返回，
     * 否则释放 asrTranslation 占用的资源。
     */
    fun release() {
        if (!::asrTranslation.isInitialized) return
        asrTranslation.release()
    }

    /**
     * 发送 ASR 中间结果消息
     *
     * @param midResult 中间识别结果字符串
     */
    private fun sendAsrMidResultMessage(midResult: String) {
        if (!::callBack.isInitialized) return
        callBack.onAsrMidResult(midResult)
    }

    /**
     * 发送 ASR 最终结果消息
     *
     * @param finalResult 最终识别结果字符串
     */
    private fun sendAsrFinalResultMessage(finalResult: String) {
        if (!::callBack.isInitialized) return
        callBack.onAsrFinalResult(finalResult)
    }

    /**
     * 发送翻译结果消息
     *
     * @param translationResult 翻译结果字符串
     */
    private fun sendTranslationResultMessage(translationResult: String) {
        if (!::callBack.isInitialized) return
        callBack.onTranslationResult(translationResult)
    }

    /**
     * 发送完成消息
     */
    private fun sendComplete() {
        if (!::callBack.isInitialized) return
        callBack.onComplete()
    }

    /**
     * 文本翻译
     * */
    @SuppressLint("TimberArgCount", "RestrictedApi")
    fun textTranslate(
        content: String,
        fromLang: String,
        toLang: String,
        onSuccess: (TranslateResponse) -> Unit
    ) {
        aiFoundationKit.textTranslateWithModel(
            TranslationRequest(
                targetLanguage = convertLanguageCodeModel(toLang),
                originText = content,
                sourceLanguage = convertLanguageCodeModel(fromLang)
            ),
            { response ->
                onSuccess.invoke(response)
                Timber.tag(TAG).d("%s%s", "response: ", response)
            }, { error ->

                val errorMsg = error.message ?: ""
                Timber.tag(TAG).d("BenefitError: $errorMsg")

                val index = errorMsg.indexOf("rawJson=")

                if (index != -1) {

                    try {
                        val jsonStr = errorMsg.substring(index + 8)
                        val jsonObject = JSONObject(jsonStr)

                        val code = jsonObject.optInt("code")
                        val msg = jsonObject.optString("msg")

                        val isBenefitError = BenefitCode.isBenefitError(code.toString())

                        if (isBenefitError) {
                            errorCallback?.onBenefitError(msg)
                        }

                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "Benefit parse error")
                    }
                }
            })
    }

    /**
     * 将语言名称字符串转换为对应的Language枚举值
     *
     * @param language 语言名称字符串，如"中文"、"英语"等
     * @return 对应的 Language 枚举值，如果未匹配到则返回 Language.CHINESE
     */
    private fun convertLanguageCode(language: String): Language {
        if (language == "中文") return Language.CHINESE
        if (language == "英语") return Language.ENGLISH
        if (language == "日语") return Language.JAPANESE
        if (language == "韩语") return Language.KOREAN
        if (language == "西班牙语") return Language.SPANISH
        if (language == "法语") return Language.FRENCH
        if (language == "德语") return Language.GERMAN
        if (language == "俄语") return Language.RUSSIAN
        if (language == "意大利语") return Language.ITALIAN
        return Language.CHINESE
    }

    /**
     * 将语言名称字符串转换为对应的语言代码字符串
     *
     * @param language 语言名称字符串，如"中文"、"英语"等
     * @return 对应的语言代码字符串，如果未匹配到则返回中文语言代码
     */
    private fun convertLanguageCode_(language: String): String {
        if (language == "中文") return LanguageConvert.LANG_ZH.code
        if (language == "英语") return LanguageConvert.LANG_EN.code
        if (language == "日语") return LanguageConvert.LANG_JP.code
        if (language == "韩语") return LanguageConvert.LANG_KOR.code
        if (language == "西班牙语") return LanguageConvert.LANG_SPA.code
        if (language == "法语") return LanguageConvert.LANG_FRA.code
        if (language == "德语") return LanguageConvert.LANG_DE.code
        if (language == "俄语") return LanguageConvert.LANG_RU.code
        if (language == "意大利语") return LanguageConvert.LANG_IT.code
        return LanguageConvert.LANG_AUTO.code
    }

    /**
     * 将语言名称字符串转换为对应的语言代码字符串
     *
     * @param language 语言名称字符串，如"中文"、"英语"等
     * @return 对应的语言代码字符串，如果未匹配到则返回中文语言代码
     */
    private fun convertLanguageCodeModel(language: String): String {
        if (language == "中文") return LanguageConvertModel.LANG_ZH.code
        if (language == "英语") return LanguageConvertModel.LANG_EN.code
        if (language == "日语") return LanguageConvertModel.LANG_JA.code
        if (language == "韩语") return LanguageConvertModel.LANG_KO.code
        if (language == "西班牙语") return LanguageConvertModel.LANG_ES.code
        if (language == "法语") return LanguageConvertModel.LANG_FR.code
        if (language == "德语") return LanguageConvertModel.LANG_DE.code
        if (language == "俄语") return LanguageConvertModel.LANG_RU.code
        if (language == "意大利语") return LanguageConvertModel.LANG_IT.code
        return "auto"
    }

}