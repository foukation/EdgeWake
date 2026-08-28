package com.cmdc.ai.assist.native

/**
 * Native URL 提供者
 * 从 C++ 层获取加密存储的 URL
 */
internal object NativeUrlProvider {
    
    init {
        try {
            System.loadLibrary("native_url_config")
        } catch (e: UnsatisfiedLinkError) {
            // 如果加载失败，抛出异常
            throw RuntimeException("Failed to load native library: native_url_config", e)
        }
    }
    
    // ============================================
    // Native 方法声明
    // ============================================
    
    /**
     * 获取终端服务 URL（生产环境）
     * @return 终端服务基础 URL
     */
    @JvmStatic
    external fun getTerminalServiceUrl(): String
    
    /**
     * 获取 AI PaaS URL（生产环境）
     * @return AI PaaS 基础 URL
     */
    @JvmStatic
    external fun getAiPaasUrl(): String
    
    /**
     * 获取 WebSocket URL（生产环境）
     * @return WebSocket 基础 URL
     */
    @JvmStatic
    external fun getWebSocketUrl(): String
    
    /**
     * 获取终端服务 URL（测试环境）
     * @return 终端服务基础 URL（测试）
     */
    @JvmStatic
    external fun getTerminalServiceUrlTest(): String
    
    /**
     * 获取 WebSocket URL（测试环境）
     * @return WebSocket 基础 URL（测试）
     */
    @JvmStatic
    external fun getWebSocketUrlTest(): String
    
    // ============================================
    // 缓存机制（减少 JNI 调用次数）
    // ============================================
    
    private var cachedTerminalUrl: String? = null
    private var cachedAiPaasUrl: String? = null
    private var cachedWssUrl: String? = null
    private var cachedTerminalUrlTest: String? = null
    private var cachedWssUrlTest: String? = null
    
    /**
     * 获取终端服务 URL（带缓存）
     */
    @JvmStatic
    fun getTerminalServiceUrlCached(): String {
        return cachedTerminalUrl ?: getTerminalServiceUrl().also {
            cachedTerminalUrl = it
        }
    }
    
    /**
     * 获取 AI PaaS URL（带缓存）
     */
    @JvmStatic
    fun getAiPaasUrlCached(): String {
        return cachedAiPaasUrl ?: getAiPaasUrl().also {
            cachedAiPaasUrl = it
        }
    }
    
    /**
     * 获取 WebSocket URL（带缓存）
     */
    @JvmStatic
    fun getWebSocketUrlCached(): String {
        return cachedWssUrl ?: getWebSocketUrl().also {
            cachedWssUrl = it
        }
    }
    
    /**
     * 获取终端服务 URL（测试环境，带缓存）
     */
    @JvmStatic
    fun getTerminalServiceUrlTestCached(): String {
        return cachedTerminalUrlTest ?: getTerminalServiceUrlTest().also {
            cachedTerminalUrlTest = it
        }
    }
    
    /**
     * 获取 WebSocket URL（测试环境，带缓存）
     */
    @JvmStatic
    fun getWebSocketUrlTestCached(): String {
        return cachedWssUrlTest ?: getWebSocketUrlTest().also {
            cachedWssUrlTest = it
        }
    }
    
    /**
     * 清除缓存（用于测试或环境切换）
     */
    @JvmStatic
    fun clearCache() {
        cachedTerminalUrl = null
        cachedAiPaasUrl = null
        cachedWssUrl = null
        cachedTerminalUrlTest = null
        cachedWssUrlTest = null
    }
}

