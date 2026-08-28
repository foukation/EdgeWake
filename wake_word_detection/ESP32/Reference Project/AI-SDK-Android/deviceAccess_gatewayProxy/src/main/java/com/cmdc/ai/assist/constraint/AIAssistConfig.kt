package com.cmdc.ai.assist.constraint

import android.content.Context

/**
 * AI助手配置类
 * 用于管理AI助手初始化所需的各项配置
 *
 * @property xAipId 应用的id (供应商向AI业务平台申请)
 * @property xAipUid 设备唯一标识(sn,imei,mac)
 * @property xAipProxy 原始请求，包含主机名和端口
 * @property productId 产品 id
 * @property productKey 产品 key
 * @property deviceNo 设备编号
 * @property deviceNoType 设备类型
 * @property token Chatbot 文本模型授权令牌
 * @property imageToken Chatbot 图片模型授权令牌
 * @property vad VAD 静音判停时间，单位 ms；未配置时使用服务端默认值
 * @property sn 用于排查日志，建议使用随机字符串
 */
data class AIAssistConfig(

    // 数据采集、网关使用
    /**
     * 应用的id (供应商向AI业务平台申请)
     * */
    internal val xAipId: String,          // 应用的id (供应商向AI业务平台申请)
    /**
     * 设备唯一标识(sn,imei,mac)
     * */
    internal val xAipUid: String,         // 设备唯一标识(sn,imei,mac)
    /**
     * 原始请求，包含主机名和端口
     * */
    internal val xAipProxy: String,       // 原始请求，包含主机名和端口

    // 纳管使用
    /**
     * 产品 id
     * */
    val productId: String,       // 产品 id
    /**
     * 产品 key
     * */
    val productKey: String,      // 产品 key
    /**
     * 设备编号
     * */
    val deviceNo: String,        // 设备编号
    /**
     * 设备编号类型
     * */
    val deviceNoType: String,    // 设备类型

    /**
     * Chatbot 文本模型授权令牌。
     * */
    val token: String,
    /**
     * Chatbot 图片模型授权令牌，仅用于 jiutian-image。
     * */
    val imageToken: String,
    /**
     * 用于排查日志，建议使用随机字符串
     * */
    var sn: String,         // 用于排查日志，建议使用随机字符串

    /**
     * 设备ID，平台上唯⼀设备标识
     * */
    var deviceId: String,
    var deviceSecret: String,
    var currentPcmFilePath: String,
    /**
     * 包名
     * */
    var packageName: String,

    /**
     * 语音助手 TTS 配置
     * 用于语音助手时的语音播报参数配置
     * */
    var dialogueTtsConfig: TtsConfig = TtsConfig.DEFAULT,
    /**
     * 用于客户端类型识别
     * */
    val clientID: String,
    /**
     * 中控版本控制
     * */
    val centralConfigVersion: String,
    /**
     * 是否启动语音自动增益控制（AGC）
     */
    val enableVoiceGain: Boolean = false,
    /**
     * 是否写入本地音频文件（PCM/WAV），默认关闭
     */
    val enableAudioFileSave: Boolean = false,
    /**
     * 设备唯一标识符（业务自定义）
     * */
    val deviceIdentifier: String,
    var context: Context?,
    /**
     * VAD 静音判停时间，单位 ms。
     *
     * null 表示不向服务端下发该字段，使用服务端默认值。
     */
    val vad: Int? = null
) {
    /**
     * 配置构建器类
     * 使用 Builder 模式，方便配置参数的设置
     */
    class Builder {
        private var xAipId: String = ""
        private var xAipUid: String = ""
        private var xAipProxy: String = ""
        private var productId: String = ""
        private var productKey: String = ""
        private var deviceNo: String = ""
        private var deviceNoType: String = ""
        private var token: String = ""
        private var imageToken: String = ""
        private var sn: String = ""
        private var deviceId: String = ""
        private var deviceSecret: String = ""
        private var currentPcmFilePath: String = ""
        private var packageName: String = ""
        private var dialogueTtsConfig: TtsConfig = TtsConfig.DEFAULT
        private var clientID: String = ""
        private var centralConfigVersion: String = ""
        private var enableVoiceGain: Boolean = false
        private var enableAudioFileSave: Boolean = false
        private var deviceIdentifier: String = ""
        private var context: Context? = null
        private var vad: Int? = null

        /**
         * 设置应用ID
         * @param xAipId 应用的id (供应商向AI业务平台申请)
         */
        internal fun setXAipId(xAipId: String) = apply { this.xAipId = xAipId }

        /**
         * 设置设备唯一标识
         * @param xAipUid 设备唯一标识(sn,imei,mac)
         */
        internal fun setXAipUid(xAipUid: String) = apply { this.xAipUid = xAipUid }

        /**
         * 设置代理地址
         * @param xAipProxy 原始请求，包含主机名和端口
         */
        internal fun setXAipProxy(xAipProxy: String) = apply { this.xAipProxy = xAipProxy }

        /**
         * 设置产品ID
         * @param productId 产品 id
         */
        fun setProductId(productId: String) = apply { this.productId = productId }

        /**
         * 设置产品密钥
         * @param productKey 产品 key
         */
        fun setProductKey(productKey: String) = apply { this.productKey = productKey }

        /**
         * 设置设备编号
         * @param deviceNo 设备编号
         */
        fun setDeviceNo(deviceNo: String) = apply { this.deviceNo = deviceNo }

        /**
         * 设置设备编号类型
         * @param deviceNoType 设备编号类型
         */
        fun setDeviceNoType(deviceNoType: String) = apply { this.deviceNoType = deviceNoType }

        /**
         * 设置 Chatbot 文本模型授权令牌。
         *
         * @param token 文本模型授权令牌
         */
        fun setToken(token: String) = apply { this.token = token }

        /**
         * 设置 Chatbot 图片模型授权令牌。
         *
         * 图片模型请求不会回退使用文本模型令牌。
         *
         * @param imageToken 图片模型授权令牌
         */
        fun setImageToken(imageToken: String) = apply { this.imageToken = imageToken }

        /**
         * 设置客户端类型标识
         * @param clientID 客户端类型标识
         */
        fun setClientID(clientID: String) = apply { this.clientID = clientID }

        /**
         * 设置中控配置版本
         * @param centralConfigVersion 中控配置版本
         */
        fun setCentralConfigVersion(centralConfigVersion: String) =
            apply { this.centralConfigVersion = centralConfigVersion }

        /**
         * 设置 VAD 静音判停时间。
         *
         * @param vad 静音持续时间，单位 ms，必须大于 0
         * @return Builder 返回当前 Builder 实例以支持链式调用
         * @throws IllegalArgumentException 当 vad 小于或等于 0
         */
        internal fun setVad(vad: Int) = apply {
            require(vad > 0) { "vad must be greater than 0 ms" }
            this.vad = vad
        }

        /**
         * 设置序列号
         * @param sn 用于排查日志，建议使用随机字符串
         */
        internal fun setSn(sn: String) = apply { this.sn = sn }

        internal fun setDeviceId(deviceId: String) = apply { this.deviceId = deviceId }

        internal fun setDeviceSecret(deviceSecret: String) =
            apply { this.deviceSecret = deviceSecret }

        /**
         * 设置智能对话 TTS 配置
         * @param config TTS 配置对象
         * @return Builder 支持链式调用
         */
        fun setDialogueTtsConfig(config: TtsConfig) = apply {
            this.dialogueTtsConfig = config
        }

        /**
         * 设置是否启用语音自动增益控制（AGC）
         * @param enableVoiceGain 是否启用语音增益功能
         * @return Builder 返回当前Builder实例以支持链式调用
         */
        fun setVoiceGainEnabled(enableVoiceGain: Boolean) = apply {
            this.enableVoiceGain = enableVoiceGain
        }

        /**
         * 设置是否写入本地音频文件（PCM/WAV）
         * @param enable 是否开启文件写入
         * @return Builder 支持链式调用
         */
        fun setAudioFileSaveEnabled(enable: Boolean) = apply {
            this.enableAudioFileSave = enable
        }

        /**
         * 设置设备唯一标识符
         * @param deviceIdentifier 设备唯一标识符（业务自定义）
         * @return Builder 返回当前 Builder 实例以支持链式调用
         */
        fun setDeviceIdentifier(deviceIdentifier: String) = apply {
            this.deviceIdentifier = deviceIdentifier
        }

        /**
         * 构建配置对象
         * @return AIAssistConfig 配置对象实例
         */
        fun build() = AIAssistConfig(
            xAipId,
            xAipUid,
            xAipProxy,
            productId,
            productKey,
            deviceNo,
            deviceNoType,
            token,
            imageToken,
            sn,
            deviceId,
            deviceSecret,
            currentPcmFilePath,
            packageName,
            dialogueTtsConfig,
            clientID,
            centralConfigVersion,
            enableVoiceGain,
            enableAudioFileSave,
            deviceIdentifier,
            context,
            vad = vad
        )

        /**
         * 验证配置是否完整
         * @throws IllegalStateException 如果必要的配置项为空
         */
        private fun validate() {
            requireNotEmpty(productId, "productId")
            requireNotEmpty(productKey, "productKey")
            requireNotEmpty(deviceNo, "deviceNo")
            requireNotEmpty(deviceNoType, "deviceNoType")
        }

        private fun requireNotEmpty(value: String, fieldName: String) {
            if (value.isEmpty()) {
                throw IllegalStateException("$fieldName must not be empty")
            }
        }
    }

    companion object {
        /**
         * 创建默认配置
         * @return AIAssistConfig 默认配置实例
         */
        fun createDefault() = Builder()
            .setXAipId("")
            .setXAipUid("")
            .setXAipProxy("")
            .setProductId("")
            .setProductKey("")
            .setDeviceNo("")
            .setDeviceNoType("")
            .setToken("")
            .setImageToken("")
            .setSn("")
            .build()
    }

    /**
     * 检查配置是否有效
     * @return Boolean 配置是否有效
     */
    fun isValid(): Boolean {
        return productId.isNotEmpty() &&
                productKey.isNotEmpty() &&
                deviceNo.isNotEmpty() &&
                deviceNoType.isNotEmpty()
    }
}
