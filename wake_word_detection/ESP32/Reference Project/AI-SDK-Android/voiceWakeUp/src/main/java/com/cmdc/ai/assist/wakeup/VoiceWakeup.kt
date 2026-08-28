package com.cmdc.ai.assist.wakeup

import android.content.Context
import com.cmdc.ai.assist.wakeup.config.WakeupInitConfig
import com.cmdc.ai.assist.wakeup.config.WakeupIntent

/**
 * 语音唤醒门面。
 *
 * 本类是对底层唤醒引擎 `ai.dueros.wakeup.DuWakeup` 的封装（1:1 复刻其对外接口，
 * 去除第三方品牌前缀 `Du`，并统一到 `com.cmdc.ai.assist.wakeup` 命名空间）。
 * 对外方法、参数、返回值均与底层引擎保持一致，内部负责在“本命名空间类型”与
 * “底层引擎类型”之间做双向转换后转发给引擎。
 *
 * 典型调用顺序：[setWakeupCallback] → [init] → [start] → [stop] → [release]。
 *
 * 获取实例：可通过 [getInstance] 获取单例（与底层引擎的单例语义一致）。
 */
class VoiceWakeup {

    /** 底层唤醒引擎实例。 */
    private val engine: ai.dueros.wakeup.DuWakeup = ai.dueros.wakeup.DuWakeup.getInstance()

    /**
     * 设置唤醒回调。
     *
     * @param callback 唤醒回调；内部会将底层引擎的回调事件转换为本命名空间类型后转发
     */
    fun setWakeupCallback(callback: WakeupCallback?) {
        if (callback == null) {
            engine.setWakeupCallback(null)
            return
        }
        engine.setWakeupCallback(object : ai.dueros.wakeup.WakeupCallback {
            override fun onInit() = callback.onInit()

            override fun onStart() = callback.onStart()

            override fun onWakeup(info: ai.dueros.wakeup.WakeupEventInfo?) =
                callback.onWakeup(fromEngine(info))

            override fun onWakeupFrameThreshold(score: Float) =
                callback.onWakeupFrameThreshold(score)

            override fun onAudioData(audioData: ShortArray?) = callback.onAudioData(audioData)

            override fun onStop() = callback.onStop()

            override fun onError(error: ai.dueros.wakeup.DuWakeupError?) =
                callback.onError(fromEngine(error))

            override fun onRelease() = callback.onRelease()
        })
    }

    /**
     * 初始化唤醒引擎。
     *
     * @param context 上下文
     * @param config  初始化配置；为 `null` 时底层引擎使用默认参数
     */
    fun init(context: Context, config: WakeupInitConfig?) {
        engine.init(context, toEngine(config))
    }

    /**
     * 开启唤醒监听。
     *
     * @param intent 唤醒意图配置；为 `null` 时底层引擎使用默认参数
     */
    fun start(intent: WakeupIntent?) {
        engine.start(toEngine(intent))
    }

    /**
     * 手动喂入音频数据。仅在唤醒意图的 `isUseFeedData` 为 `true` 时需要调用。
     *
     * @param data PCM 音频数据，格式为 16k 采样、单声道、16bit
     * @param size 本次有效采样点数
     */
    fun feedAudioData(data: ShortArray, size: Int) {
        engine.feedAudioData(data, size)
    }

    /** 停止唤醒监听。 */
    fun stop() {
        engine.stop()
    }

    /** 释放唤醒引擎资源。 */
    fun release() {
        engine.release()
    }

    /**
     * 获取引擎版本号。
     *
     * @return 版本号字符串
     */
    fun getVersion(): String? = engine.version

    /**
     * 获取当前引擎状态。
     *
     * @return 唤醒引擎状态
     */
    fun getStatus(): WakeupStatus = fromEngine(engine.status)

    /**
     * 设置日志等级，取值同 [android.util.Log]。
     *
     * @param level 日志等级
     */
    fun setLogLevel(level: Int) {
        engine.setLogLevel(level)
    }

    /**
     * 获取当前生效的唤醒词列表。
     *
     * @return 唤醒词数组，可能为 `null`
     */
    fun getCurrentWakeupWords(): Array<String>? = engine.currentWakeupWords

    companion object {

        @Volatile
        private var instance: VoiceWakeup? = null

        /**
         * 获取语音唤醒单例。
         *
         * @return 语音唤醒门面实例
         */
        @JvmStatic
        fun getInstance(): VoiceWakeup {
            return instance ?: synchronized(this) {
                instance ?: VoiceWakeup().also { instance = it }
            }
        }

        // -----------------------------------------------------------------
        // 内部类型转换：本命名空间类型 <-> 底层引擎类型
        // -----------------------------------------------------------------

        /** 本命名空间初始化配置 -> 底层引擎初始化配置。 */
        private fun toEngine(config: WakeupInitConfig?): ai.dueros.wakeup.config.WakeupInitConfig? {
            if (config == null) {
                return null
            }
            val engineConfig = ai.dueros.wakeup.config.WakeupInitConfig()
            engineConfig.isCustomWords = config.isCustomWords

            val model = config.wakeWordModel
            if (engineConfig.wakeWordModel == null) {
                engineConfig.wakeWordModel = ai.dueros.wakeup.config.WakeWordModel()
            }
            engineConfig.wakeWordModel.modelPath = model.modelPath

            val wake = config.wakeConfig
            if (engineConfig.wakeConfig == null) {
                engineConfig.wakeConfig = ai.dueros.wakeup.config.WakeConfig()
            }
            engineConfig.wakeConfig.wakeInterval = wake.wakeInterval
            engineConfig.wakeConfig.wakeThreshold = wake.wakeThreshold
            engineConfig.wakeConfig.saveWakeupAudio = wake.saveWakeupAudio
            engineConfig.wakeConfig.wakeupAudioPath = wake.wakeupAudioPath
            engineConfig.wakeConfig.delayTriggerDuration = wake.delayTriggerDuration
            engineConfig.wakeConfig.detectionWindowsFrames = wake.detectionWindowsFrames
            engineConfig.wakeConfig.thresholdFramesCount = wake.thresholdFramesCount
            engineConfig.wakeConfig.customWakeThreshold = wake.customWakeThreshold
            return engineConfig
        }

        /** 本命名空间唤醒意图 -> 底层引擎唤醒意图。 */
        private fun toEngine(intent: WakeupIntent?): ai.dueros.wakeup.config.DuWakeupIntent? {
            if (intent == null) {
                return null
            }
            val engineIntent = ai.dueros.wakeup.config.DuWakeupIntent()
            engineIntent.audioSource = intent.audioSource
            engineIntent.isEnableAEC = intent.isEnableAEC
            engineIntent.isEnableNS = intent.isEnableNS
            engineIntent.isUseFeedData = intent.isUseFeedData
            engineIntent.wakeupWords = intent.wakeupWords
            return engineIntent
        }

        /** 底层引擎唤醒事件 -> 本命名空间唤醒事件。 */
        private fun fromEngine(info: ai.dueros.wakeup.WakeupEventInfo?): WakeupEventInfo? {
            if (info == null) {
                return null
            }
            return WakeupEventInfo(info.word, info.confidence, info.sn, info.time, info.audioData)
        }

        /** 底层引擎错误 -> 本命名空间错误。 */
        private fun fromEngine(error: ai.dueros.wakeup.DuWakeupError?): WakeupError? {
            if (error == null) {
                return null
            }
            return WakeupError(error.code, error.message)
        }

        /** 底层引擎状态 -> 本命名空间状态。 */
        private fun fromEngine(status: ai.dueros.wakeup.WakeupStatus?): WakeupStatus {
            return when (status) {
                ai.dueros.wakeup.WakeupStatus.INIT -> WakeupStatus.INIT
                ai.dueros.wakeup.WakeupStatus.START -> WakeupStatus.START
                ai.dueros.wakeup.WakeupStatus.STOP -> WakeupStatus.STOP
                else -> WakeupStatus.UN_INIT
            }
        }
    }
}
