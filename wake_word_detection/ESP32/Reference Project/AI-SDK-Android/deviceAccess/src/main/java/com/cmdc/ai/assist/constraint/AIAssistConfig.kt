package com.cmdc.ai.assist.constraint

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
 * @property token 用户登录验证
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

    // ASR 使用
    /**
     * 用户登录验证
     * */
    val token: String,           // 用户登录验证
    /**
     * 用于排查日志，建议使用随机字符串
     * */
    var sn: String,         // 用于排查日志，建议使用随机字符串

    /**
     * 设备ID，平台上唯⼀设备标识
     * */
    var deviceId: String,
    var deviceSecret: String,
    var currentPcmFilePath: String
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
        private var sn: String = ""
        private var deviceId: String = ""
        private var deviceSecret: String = ""
        private var currentPcmFilePath: String = ""

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
         * 设置用户令牌
         * @param token 用户登录验证
         */
        internal fun setToken(token: String) = apply { this.token = token }

        /**
         * 设置序列号
         * @param sn 用于排查日志，建议使用随机字符串
         */
        internal fun setSn(sn: String) = apply { this.sn = sn }

        internal fun setDeviceId(deviceId: String) = apply { this.deviceId = deviceId }

        internal fun setDeviceSecret(deviceSecret: String) =
            apply { this.deviceSecret = deviceSecret }

        /**
         * 构建配置对象
         * @return AIAssistConfig 配置对象实例
         */
        fun build() = AIAssistConfig(
            xAipId, xAipUid, xAipProxy,
            productId, productKey, deviceNo, deviceNoType,
            token, sn, deviceId, deviceSecret, currentPcmFilePath
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
