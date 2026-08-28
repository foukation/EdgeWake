package com.fxzs.lingxiagent.util.audio

import android.content.Context
import android.text.TextUtils
import com.cmdc.ai.assist.api.AIFoundationKit
import com.cmdc.ai.assist.constraint.TextToAudioRequest
import com.cmdc.ai.assist.constraint.TextToAudioResponse
import com.fxzs.lingxiagent.model.chat.dto.VoiceSettingBean
import com.fxzs.lingxiagent.util.SharedPreferencesUtil
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.LinkedBlockingDeque

/**
 *
 * TTS 管理类
 *
 * @author yhs
 */
class TTSManager {
    val targetLanguageList: List<String> =
        mutableListOf("中文", "英语", "西班牙语", "法语", "德语", "意大利语") //支持翻译

    companion object {
        private const val TAG = "TTSManager"

        @Volatile
        private var instance: TTSManager? = null

        /**
         * 获取TTS管理器单例
         */
        @JvmStatic
        fun getInstance(): TTSManager {
            return instance ?: synchronized(this) {
                instance ?: TTSManager().also { instance = it }
            }
        }
    }

    /**
     * 存储待处理的文本队列
     */
    private val ttsText = LinkedBlockingDeque<String>()

    /**
     * 存储临时文本队列，用于批量处理
     */
    private val ttsText_ = LinkedBlockingDeque<String>()

    /**
     * 当前会话 ID
     */
    private var currentConversationId: String? = null

    /**
     * 当前语音设置选项
     */
    private var currentVoiceOption: VoiceSettingBean? = null

    /**
     * 定时任务
     */
    private var timerJob: Job? = null

    /**
     * AI 基础工具包
     */
    private val aiFoundationKit by lazy {
        AIFoundationKit()
    }

    private val processor by lazy {
        aiFoundationKit.streamTextProcessorHelp().apply {
            setEarlySplitThreshold(10)
            setEnableEarlySplit(true)
            setSentenceCallback { sentence ->
                Timber.tag(TAG).i("检测到新句子: $sentence (长度: ${sentence.length})")
            }
            startProcessing()
        }
    }

    /**
     * 初始化 TTS 管理器
     * @param context Android上下文
     */
    /**
     * 初始化 TTS 管理器
     * @param context Android上下文
     */
    private fun init(context: Context) {
    }


    /**
     * 播放 tts
     * */
    /**
     * 播放TTS音频
     * @param url 音频文件URL地址
     */
    fun playTTS(url: String) {
        val uuid = UUID.randomUUID().toString()
        currentConversationId = uuid
        TtsMediaPlayer.getInstance()
            .speak(uuid, url)
    }

    /**
     * 播放 tts
     * */
    /**
     * 播放TTS音频
     * @param url 音频文件URL地址
     * @param conversationId 会话ID
     */
    fun playTTS(url: String, conversationId: String) {
        currentConversationId = conversationId
        TtsMediaPlayer.getInstance()
            .speak(conversationId, url)
    }

    /*
     强制转换播放 用在手动播放 /或者直接播放tts
     */
    fun textForceToAudio(input: String) {
        isStop = false
        textToAudio(input)
    }

    /**
     * 文本转语音
     * */
    /**
     * 将文本转换为语音并播放
     * @param input 需要转换为语音的文本内容
     */
    fun textToAudio(input: String) {
        Timber.tag(TAG).i("textToAudio：isStop: ${isStop}" + "input: ${input}")
        if (isStop) {
            stop()
            return
        }
        processor.startProcessing()
        processor.processTextChunk(input)
        processor.finishProcessing()
        Timber.tag(TAG).i("队列中的句子数量: ${processor.getQueueSize()}")
        val uuid = UUID.randomUUID().toString()
        currentConversationId = uuid
        updateCurrentVoiceOption()
        playPendingSentences(uuid)
    }

    /**
     * 文本转语音
     * */
    /**
     * 将文本转换为语音并播放（带会话ID）
     * @param input 需要转换为语音的文本内容
     * @param conversationId 会话ID
     */
    fun textToAudio(input: String, conversationId: String) {
        Timber.tag(TAG)
            .i("textToAudio:isStop: ${isStop}" + "conversationId: ${conversationId}" + "input: ${input}")
        if (isStop) {
            stop()
            return
        }
        if (!TextUtils.equals(conversationId, currentConversationId)) {
            ttsText.clear()
            ttsText_.clear()
            currentConversationId = conversationId
            updateCurrentVoiceOption()
            cancelTimer()
            startTimer()
        }
        ttsText.add(input)
    }

    /**
     * 批量处理TTS文本队列
     */
    private fun processTtsTextBatch() {
        processor.startProcessing()
        while (ttsText_.isNotEmpty()) {
            val text = ttsText_.poll()
            text?.let {
                processor.processTextChunk(it)
            }
        }
        processor.finishProcessing()
        Timber.tag(TAG).i("队列中的句子数量: ${processor.getQueueSize()}")
    }

    /**
     * 处理下一个TTS文本
     * @param conversationId 会话ID
     */
    private fun processNextTtsText(conversationId: String) {
        ttsText.poll()?.let {
            convertTextToAudio(it) { response ->
                Timber.tag(TAG).d("%s%s", "response: ", response)
                TtsMediaPlayer.getInstance()
                    .speak(conversationId, response.data?.absolutePath ?: "")
            }
        }
    }

    /**
     * 播放待处理的句子
     * @param uuid 唯一标识符
     */
    private fun playPendingSentences(uuid: String) {
        if (processor.hasSentences()) {
            val sentence = processor.pollNextSentence()
            sentence?.let {
                Timber.tag(TAG).i("TTS处理: $it")
                convertTextToAudio(it) { response ->
                    Timber.tag(TAG).d("%s%s", "response: ", response)
                    if (isStop) return@convertTextToAudio
                    if (TextUtils.isEmpty(this.currentConversationId)) return@convertTextToAudio
                    TtsMediaPlayer.getInstance()
                        .speak(uuid, response.data?.absolutePath ?: "")
                    playPendingSentences(uuid)
                }
            }
        }
    }

    /**
     * 播放下一个待处理的句子
     * @param uuid 唯一标识符
     */
    private fun playPendingNextSentences(uuid: String) {
        if (!processor.hasSentences()) return
        val sentence = processor.pollNextSentence()
        sentence?.let {
            Timber.tag(TAG).i("TTS处理: $it")
            convertTextToAudio(it) { response ->
                Timber.tag(TAG).d("%s%s", "response: ", response)
                TtsMediaPlayer.getInstance()
                    .speak(uuid, response.data?.absolutePath ?: "")
                playPendingNextSentences(uuid)
            }
        }
    }

    /**
     * 将文本转换为音频文件
     *
     * 该函数使用 AI 基础工具包将输入的文本转换为音频文件，支持调节语速、音调、音量等参数，
     * 并可选择不同的发音人类型。转换成功后通过回调函数返回结果，失败时记录错误日志。
     *
     * 默认声音 使用 4100 活力女主播
     *
     * @param input 需要转换为音频的文本内容
     * @param onSuccess 文本转音频成功时的回调函数，参数为 TextToAudioResponse 类型，包含转换结果和音频文件信息
     */
    private fun convertTextToAudio(
        input: String,
        onSuccess: (TextToAudioResponse) -> Unit,
    ) {
        try {
            aiFoundationKit.textToAudio(
                TextToAudioRequest(
                    text = input,
                    spd = currentVoiceOption?.spd ?: 5,
                    pit = 5,
                    vol = currentVoiceOption?.vol ?: 5,
                    aue = 3,
                    /**
                     * 发音人
                     *
                     * 0 标准女主播
                     * 1亲切男声
                     * 3情感男声
                     * 4童声
                     * 5003 情感男声
                     * 5118 甜美女声
                     * 106 专业男主播
                     * 103 可爱童声
                     * 110 童声主播
                     * 111 软萌妹子
                     * 6 成熟女主播
                     * 4003 情感男声
                     * 106 专业男主播
                     * 4115 电台男主播
                     * 4119 甜美女声
                     * 4105 清激女声
                     * 4117 活泼女声
                     * 4100 活力女主播
                     * 4103 可爱女声
                     * 4144 娱乐女声
                     * 4278 知识女主播
                     * 4143 配音男声
                     * 4140 专业女主播
                     * 4129 知识男主播
                     * 4149 广告男声
                     * 4254 广告女声
                     * 4206 综艺男声
                     * 4226 电台女主播
                     * */
                    per = currentVoiceOption?.per ?: 4100,
                ),
                { response ->
                    onSuccess(response)
                }, { error ->
                    Timber.tag(TAG).e("%s%s", "error: ", error)
                })
        } catch (e: Exception) {
            Timber.tag(TAG).e("%s%s", "error: ", e.message)
        }
    }

    /**
     * 更新当前语音选项配置
     */
    private fun updateCurrentVoiceOption() {
        this.currentVoiceOption = getVoiceOption()
    }

    /**
     * 获取当前语音选项配置
     */
    fun getCurrentVoiceOption(): VoiceSettingBean? {
        updateCurrentVoiceOption()
        return this.currentVoiceOption
    }

    /**
     * 获取语音选项配置
     * @return 语音设置选项
     */
    private fun getVoiceOption(): VoiceSettingBean {
        val voiceOption = SharedPreferencesUtil.getVoiceOption()
        if (TextUtils.isEmpty(voiceOption)) return getDefaultVoiceOption()
        return Gson().fromJson(voiceOption, VoiceSettingBean::class.java)
    }

    /**
     * 获取默认语音选项配置
     * @return 默认语音设置选项
     */
    private fun getDefaultVoiceOption(): VoiceSettingBean {
        return VoiceSettingBean("小雨-活力女主播", "新闻播报", 4100, false)
    }

    /**
     * 启动定时器任务
     */
    private fun startTimer() {
        timerJob = CoroutineScope(Dispatchers.Main).launchPeriodicAsync(2000) {
            // 如果临时队列不为空则返回
            if (ttsText_.size > 0) return@launchPeriodicAsync
            // 如果处理器中还有句子未处理完则返回
            if (processor.hasSentences()) return@launchPeriodicAsync
            // 将主队列中的文本转移到临时队列
            if (ttsText.size > 0) ttsText.drainTo(ttsText_)
            // 批量处理文本
            processTtsTextBatch()
            // 播放下一个待处理句子
            currentConversationId?.let { playPendingNextSentences(it) }
        }
    }

    /**
     * 取消定时器任务
     */
    private fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    /**
     * 启动周期性异步任务
     * @param intervalMillis 间隔时间（毫秒）
     * @param task 需要执行的任务
     * @return Job对象
     */
    private fun CoroutineScope.launchPeriodicAsync(
        intervalMillis: Long,
        task: () -> Unit
    ): Job {
        return this.launch {
            while (isActive) {
                task()
                delay(intervalMillis)
            }
        }
    }

    var isStop: Boolean = false
        set(value) {
            field = value
        }

    /**
     * 停止TTS播放
     */
    fun stop() {
        Timber.tag(TAG).i("textToAudio tts:stop")
        isStop = true
        TtsMediaPlayer.getInstance().stop()
        if (timerJob?.isActive == true) cancelTimer()
        currentConversationId = null
        ttsText.clear()
        ttsText_.clear()
        processor.clearQueue()
    }

    /**
     * 设置播放器监听器
     * @param onPlayerListener 播放器监听器
     */
    fun setOnPlayerListener(onPlayerListener: OnPlayerListener?) {
        if (onPlayerListener != null) {
            TtsMediaPlayer.getInstance().setPlayerListener(onPlayerListener)
        }
    }

}