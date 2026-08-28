package com.cmdc.ai.assist.voiceAssistant

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import com.cmdc.ai.assist.AIAssistantManager
import com.cmdc.ai.assist.api.ASRIntelligentDialogue
import com.cmdc.ai.assist.constraint.AsrResult
import com.cmdc.ai.assist.constraint.DialogueResult
import com.cmdc.ai.assist.wakeup.VoiceWakeup
import com.cmdc.ai.assist.wakeup.WakeupCallback
import com.cmdc.ai.assist.wakeup.WakeupError
import com.cmdc.ai.assist.wakeup.WakeupEventInfo
import com.cmdc.ai.assist.wakeup.config.WakeupInitConfig
import com.cmdc.ai.assist.wakeup.config.WakeupIntent

/**
 * 语音助手引擎（唤醒 → 自动对话）。
 *
 * 把「唤醒封装 [VoiceWakeup]」与「对话 [ASRIntelligentDialogue]」用一个状态机串起来：
 * 喊“灵犀灵犀” → 自动开始一轮语音对话 → 答完自动回到待唤醒。宿主只需 [start] 一次。
 *
 * 唤醒与 ASR 的**麦克风串行交接**（两者各自持有一路独立 AudioRecord，抢同一物理麦）：
 * 进入对话前先关唤醒（放麦），对话结束后再开唤醒（回收麦）。
 *
 * 设计为**半双工**：念答案（TTS，由宿主负责）期间两个麦都关闭；宿主念完须调用
 * [notifySpeakFinished] 通知本类，才会重新打开唤醒麦，避免喇叭声被麦收进去或误唤醒。
 *
 * 前提：宿主须先调用 `AIAssistantManager.initialize(context, config)` 完成鉴权初始化，
 *
 * 典型用法：
 * ```
 * VoiceAssistant va = (VoiceAssistant) AIAssistantManager.getInstance().voiceAssistantHelp();
 * va.init(context);
 * va.setListener(listener);
 * va.start();
 * // 宿主在 onDialogueResult 里念完答案后：va.notifySpeakFinished();
 * ```
 */
class VoiceAssistant {

    /**
     * 引擎状态。表示当前处于「唤醒 → 对话」流程的哪一步。
     */
    enum class State {
        /** 待唤醒：唤醒麦开着监听“灵犀灵犀”。 */
        IDLE_LISTENING,

        /** 刚唤醒：正在放提示音并交接麦克风（唤醒关 → ASR 开）。 */
        WAKED,

        /** 正在听用户说话：ASR 麦开。 */
        ASR_LISTENING,

        /** 用户说完，正在等服务器返回答案。 */
        THINKING,

        /** 宿主正在念答案：此时唤醒麦与 ASR 麦都关闭（半双工）。 */
        SPEAKING
    }

    /**
     * 提示音模式。
     */
    enum class PromptMode {
        /** 默认：唤醒瞬间播放 SDK 内置提示音。 */
        BUILTIN,

        /** 播放宿主提供的音频资源（见 [Config.customPromptResId]）。 */
        CUSTOM,

        /** 不播放；仅通过 [Listener.onWakeup] 通知宿主，是否播放由宿主决定。 */
        NONE
    }

    /**
     * 引擎配置。
     */
    class Config {
        /** 透传给唤醒引擎的初始化配置（阈值 / AEC 等）。 */
        @JvmField
        var wakeupInitConfig: WakeupInitConfig = WakeupInitConfig()

        /** 提示音模式，默认播放内置提示音。 */
        @JvmField
        var promptMode: PromptMode = PromptMode.BUILTIN

        /** [PromptMode.CUSTOM] 时使用的音频资源 id（res/raw 下）。 */
        @JvmField
        var customPromptResId: Int = 0
    }

    /**
     * 事件回调。各方法均带默认空实现，按需重写。
     */
    interface Listener {
        /** 状态变化（可用于更新 UI）。 */
        fun onStateChanged(state: State) {}

        /** 唤醒命中。@param word 命中的唤醒词。 */
        fun onWakeup(word: String) {}

        /** 语音识别结果。@param asrResult 识别结果（含文本、是否最终、情绪标签等）。 */
        fun onAsrResult(asrResult: AsrResult) {}

        /** AI 对话答案（宿主念完答案后须调用 [notifySpeakFinished]）。 */
        fun onDialogueResult(result: DialogueResult) {}

        /** 出错。@param code 错误码；@param msg 错误消息。 */
        fun onError(code: Int, msg: String) {}
    }

    // ---------------------------------------------------------------------
    // 内部字段
    // ---------------------------------------------------------------------

    /** 主线程 Handler，用于把回调切回主线程。 */
    private val mainHandler = Handler(Looper.getMainLooper())

    /** 状态锁，保证状态流转与麦克风交接的原子性。 */
    private val lock = Any()

    /** 唤醒封装（单例）。 */
    private val wakeup: VoiceWakeup = VoiceWakeup.getInstance()

    /** 当前对话实例；每轮对话新建，release 后置空。 */
    private var dialogue: ASRIntelligentDialogue? = null

    /** 应用上下文。 */
    private var appContext: Context? = null

    /** 当前配置。 */
    private var config: Config = Config()

    /** 外部监听器。 */
    private var listener: Listener? = null

    /** 当前状态。 */
    @Volatile
    private var state: State = State.IDLE_LISTENING

    /** 是否已初始化。 */
    @Volatile
    private var initialized = false

    /** 当前是否已收到答案、正等待宿主播报完成（用于决定 onComplete 是否直接回到待唤醒）。 */
    @Volatile
    private var awaitingSpeakFinished = false

    // ---------------------------------------------------------------------
    // 对外方法
    // ---------------------------------------------------------------------

    /**
     * 初始化。
     *
     * @param context 上下文
     * @param config  语音助手引擎配置；不传则使用默认（内置提示音）
     */
    fun init(context: Context, config: Config = Config()) {
        this.appContext = context.applicationContext
        this.config = config
        // 先设回调，再 init（遵循唤醒引擎调用顺序：setWakeupCallback → init → start）
        wakeup.setWakeupCallback(wakeupCallback)
        wakeup.init(context, config.wakeupInitConfig)
        initialized = true
    }

    /**
     * 注册语音助手引擎事件回调。
     */
    fun setListener(listener: Listener) {
        this.listener = listener
    }

    /**
     * 启动语音助手引擎：进入待唤醒，打开唤醒麦。
     */
    fun start() {
        synchronized(lock) {
            if (!initialized) {
                notifyError(ERROR_NOT_INITIALIZED, "未初始化，请先调用 init()")
                return
            }
            startWakeupLocked()
        }
    }

    /**
     * 停止语音助手引擎：关闭唤醒与对话，回到 IDLE。
     */
    fun stop() {
        synchronized(lock) {
            releaseDialogueLocked()
            try {
                wakeup.stop()
            } catch (e: Exception) {
                // 忽略停止异常
            }
            changeStateLocked(State.IDLE_LISTENING)
        }
    }

    /**
     * 释放资源（退出时调用）。
     */
    fun release() {
        synchronized(lock) {
            releaseDialogueLocked()
            try {
                wakeup.release()
            } catch (e: Exception) {
                // 忽略释放异常
            }
            initialized = false
            changeStateLocked(State.IDLE_LISTENING)
        }
    }

    /**
     * 免唤醒：不喊唤醒词，直接进入一轮对话（等价于“按住说话”）。
     */
    fun triggerManually() {
        synchronized(lock) {
            if (!initialized) {
                notifyError(ERROR_NOT_INITIALIZED, "未初始化，请先调用 init()")
                return
            }
            if (state != State.IDLE_LISTENING) {
                // 非待唤醒态忽略（防重入）
                return
            }
            enterDialogueLocked(word = "", playPrompt = false)
        }
    }

    /**
     * 宿主波方案 tts 后调用，通知引擎可以重新打开唤醒麦（保证半双工）。
     */
    fun notifySpeakFinished() {
        synchronized(lock) {
            if (state == State.SPEAKING) {
                startWakeupLocked()
            }
        }
    }

    /**
     * 获取当前状态。
     */
    fun getState(): State = state

    // ---------------------------------------------------------------------
    // 唤醒回调 → 内部处理
    // ---------------------------------------------------------------------

    /** 唤醒引擎事件回调。 */
    private val wakeupCallback = object : WakeupCallback {
        // 唤醒引擎初始化完成。
        override fun onInit() {}

        // 唤醒监听启动成功。
        override fun onStart() {}

        override fun onWakeup(info: WakeupEventInfo?) {
            val word = info?.word ?: ""
            synchronized(lock) {
                // 防重入：仅在“待唤醒”态响应唤醒；对话进行中忽略。
                if (state != State.IDLE_LISTENING) {
                    return
                }
                enterDialogueLocked(word = word, playPrompt = true)
            }
        }

        // 模型推理分数回调（调试用）。
        override fun onWakeupFrameThreshold(score: Float) {}

        // 连续音频数据回调（16k/单声道/16bit）。
        override fun onAudioData(audioData: ShortArray?) {}

        // 唤醒监听停止成功。
        override fun onStop() {}

        override fun onError(error: WakeupError?) {
            notifyError(error?.code ?: ERROR_WAKEUP, error?.message ?: "唤醒错误")
        }

        // 唤醒资源释放完成。
        override fun onRelease() {}
    }

    // ---------------------------------------------------------------------
    // 状态流转（均在持有 lock 时调用）
    // ---------------------------------------------------------------------

    /** 打开唤醒监听，回到待唤醒态。 */
    private fun startWakeupLocked() {
        releaseDialogueLocked()
        awaitingSpeakFinished = false
        try {
            val wakeupIntent = WakeupIntent()
            wakeupIntent.isEnableNS = true
            wakeup.start(wakeupIntent)
        } catch (e: Exception) {
            notifyError(ERROR_WAKEUP, "启动唤醒失败: ${e.message}")
        }
        changeStateLocked(State.IDLE_LISTENING)
    }

    /**
     * 从待唤醒进入一轮对话：放提示音 → 关唤醒（放麦）→ 开 ASR（拿麦）。
     *
     * @param word       命中的唤醒词（手动触发时为空）
     * @param playPrompt 是否播放提示音（手动触发不播放）
     */
    private fun enterDialogueLocked(word: String, playPrompt: Boolean) {
        changeStateLocked(State.WAKED)
        if (word.isNotEmpty()) {
            notifyWakeup(word)
        }

        // 放提示音，遮盖“唤醒关 → ASR 开”的交接空窗，并提示用户可以说话。
        if (playPrompt) {
            playPromptSound()
        }

        // 关唤醒，释放麦克风给 ASR。
        try {
            wakeup.stop()
        } catch (e: Exception) {
            // 忽略停止异常
        }

        // 新建对话实例（ASRIntelligentDialogue release 后作废，每轮必须新建）。
        val asr = obtainDialogue()
        if (asr == null) {
            notifyError(ERROR_ASR_UNAVAILABLE, "无法获取对话实例，请确认已 AIAssistantManager.initialize()")
            // 拿不到对话，退回待唤醒。
            startWakeupLocked()
            return
        }
        dialogue = asr
        asr.setListener(asrListener)

        val ctx = appContext
        if (ctx == null) {
            notifyError(ERROR_NOT_INITIALIZED, "上下文为空，请先 init()")
            startWakeupLocked()
            return
        }
        try {
            asr.startRecognition(ctx)
            changeStateLocked(State.ASR_LISTENING)
        } catch (e: Exception) {
            notifyError(ERROR_ASR, "启动对话失败: ${e.message}")
            startWakeupLocked()
        }
    }

    /** 释放当前对话实例。 */
    private fun releaseDialogueLocked() {
        dialogue?.let {
            try {
                it.release()
            } catch (e: Exception) {
                // 忽略释放异常
            }
        }
        dialogue = null
    }

    /**
     * 获取一个新的对话实例。
     *
     * 通过 `AIAssistantManager.asrIntelligentDialogueHelp()` 反射创建（每次返回新实例），
     * 从而复用 SDK 既有的能力获取方式，且无需感知其内部麦克风来源。
     */
    private fun obtainDialogue(): ASRIntelligentDialogue? {
        return try {
            AIAssistantManager.getInstance().asrIntelligentDialogueHelp() as? ASRIntelligentDialogue
        } catch (e: Exception) {
            null
        }
    }

    // ---------------------------------------------------------------------
    // 对话回调 → 内部处理
    // ---------------------------------------------------------------------

    /** ASR 对话事件回调。 */
    private val asrListener = object : ASRIntelligentDialogue.RealtimeAsrListener {
        // ASR 建联成功。
        override fun onConnected() {}

        override fun onAsrMidResult(asrResult: AsrResult) {
            notifyAsrResult(asrResult)
        }

        override fun onAsrFinalResult(asrResult: AsrResult) {
            notifyAsrResult(asrResult)
            synchronized(lock) {
                if (state == State.ASR_LISTENING) {
                    changeStateLocked(State.THINKING)
                }
            }
        }

        override fun onDialogueResult(result: DialogueResult) {
            notifyDialogueResult(result)
            synchronized(lock) {
                // 收到答案：进入 SPEAKING，等待宿主念完后调 notifySpeakFinished()。
                awaitingSpeakFinished = true
                changeStateLocked(State.SPEAKING)
            }
        }

        override fun onError(code: Int, message: String) {
            notifyError(code, message)
            synchronized(lock) {
                // 出错直接结束本轮，回到待唤醒。
                startWakeupLocked()
            }
        }

        override fun onComplete() {
            synchronized(lock) {
                // 若没有收到答案（如静音超时、服务端兜底关闭），直接回到待唤醒；
                // 若已进入 SPEAKING（等待播报），则由 notifySpeakFinished() 触发回收。
                if (!awaitingSpeakFinished) {
                    startWakeupLocked()
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // 提示音
    // ---------------------------------------------------------------------

    /**
     * 播放提示音（临时 MediaPlayer，播放完成后自动释放）。
     *
     * 按 [Config.promptMode] 分流：
     * - [PromptMode.BUILTIN]：播放 SDK 内置提示音。内置音频存于本模块 `assets/`（而非 `res/raw`），
     *   因为 fat-aar 在 AGP 7.3+ 上不再合并被嵌模块的 `res`，`R.raw` 会在最终包里缺失；
     *   `assets` 则可正常合并，故内置音走 assets 读取以规避该问题。
     * - [PromptMode.CUSTOM]：播放宿主提供的音频资源（[Config.customPromptResId]）。该资源属于宿主 App，
     *   不经过 SDK 的 fat-aar 打包，`R.raw` 解析正常。
     * - [PromptMode.NONE]：不播放，交由宿主在 [Listener.onWakeup] 中自行处理。
     */
    private fun playPromptSound() {
        val ctx = appContext ?: return
        when (config.promptMode) {
            PromptMode.BUILTIN -> playBuiltinPromptSound(ctx)
            PromptMode.CUSTOM -> playPromptSoundFromRes(ctx, config.customPromptResId)
            PromptMode.NONE -> return
        }
    }

    /**
     * 播放 SDK 内置提示音（来自本模块 `assets/voice_wakeup_prompt.mp3`）。
     *
     * @param ctx 应用上下文，用于访问 assets
     */
    private fun playBuiltinPromptSound(ctx: Context) {
        try {
            val player = MediaPlayer()
            // AssetFileDescriptor 用完即关（use 保证关闭）；mp3 在 assets 中默认不压缩，可用 openFd。
            ctx.assets.openFd(BUILTIN_PROMPT_ASSET).use { afd ->
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            player.setOnCompletionListener {
                try {
                    it.release()
                } catch (e: Exception) {
                    // 忽略释放异常
                }
            }
            // assets 播放需手动 prepare；异步准备完成后再开始，避免阻塞调用线程。
            player.setOnPreparedListener { it.start() }
            player.prepareAsync()
        } catch (e: Exception) {
            // 提示音失败不影响主流程
        }
    }

    /**
     * 播放宿主提供的音频资源。
     *
     * @param ctx   应用上下文
     * @param resId 宿主 App 的音频资源 id（res/raw 下）；为 0 时不播放
     */
    private fun playPromptSoundFromRes(ctx: Context, resId: Int) {
        if (resId == 0) {
            return
        }
        try {
            val player = MediaPlayer.create(ctx, resId) ?: return
            player.setOnCompletionListener {
                try {
                    it.release()
                } catch (e: Exception) {
                    // 忽略释放异常
                }
            }
            player.start()
        } catch (e: Exception) {
            // 提示音失败不影响主流程
        }
    }

    // ---------------------------------------------------------------------
    // 回调派发（切回主线程）
    // ---------------------------------------------------------------------

    /** 更新状态并在主线程回调 onStateChanged。 */
    private fun changeStateLocked(newState: State) {
        if (state == newState) {
            return
        }
        state = newState
        val l = listener ?: return
        mainHandler.post { l.onStateChanged(newState) }
    }

    /** 主线程回调唤醒命中。 */
    private fun notifyWakeup(word: String) {
        val l = listener ?: return
        mainHandler.post { l.onWakeup(word) }
    }

    /** 主线程回调识别结果。 */
    private fun notifyAsrResult(asrResult: AsrResult) {
        val l = listener ?: return
        mainHandler.post { l.onAsrResult(asrResult) }
    }

    /** 主线程回调对话答案。 */
    private fun notifyDialogueResult(result: DialogueResult) {
        val l = listener ?: return
        mainHandler.post { l.onDialogueResult(result) }
    }

    /** 主线程回调错误。 */
    private fun notifyError(code: Int, msg: String) {
        val l = listener ?: return
        mainHandler.post { l.onError(code, msg) }
    }

    companion object {

        /** 未初始化。 */
        const val ERROR_NOT_INITIALIZED = 1001

        /** 无法获取对话实例（通常是未调用 AIAssistantManager.initialize）。 */
        const val ERROR_ASR_UNAVAILABLE = 1002

        /** 对话启动/运行错误。 */
        const val ERROR_ASR = 1003

        /** 唤醒错误。 */
        const val ERROR_WAKEUP = 1004

        /** 内置提示音在 assets 中的文件名（存于本模块 `assets/`，避开 fat-aar 不合并 res 的问题）。 */
        private const val BUILTIN_PROMPT_ASSET = "voice_wakeup_prompt.mp3"

        /** 单例实例。 */
        @Volatile
        private var instance: VoiceAssistant? = null

        /**
         * 获取语音助手引擎单例。
         */
        @JvmStatic
        fun getInstance(): VoiceAssistant {
            return instance ?: synchronized(this) {
                instance ?: VoiceAssistant().also { instance = it }
            }
        }
    }
}
