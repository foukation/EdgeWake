package com.fxzs.lingxiagent.lingxi.translate.websocket;

import com.fxzs.lingxiagent.model.common.Constants;

/**
 * WebSocket配置类
 * 使用灵犀标准协议
 */
public class WebSocketConfig {
    
    /**
     * WebSocket服务地址（灵犀标准协议）
     */
    public static final String WEBSOCKET_URL = "wss://"+ Constants.BASE_URL_CONTROL + "v1/translate";



    /**
     * 获取WebSocket URL
     */
    public static String getWebSocketUrl() {
        return WEBSOCKET_URL;
    }
    
    /**
     * 音频配置
     */
    public static class AudioConfig {
        /** 采样率 */
        public static final int SAMPLING_RATE = 16000;
        
        /** 音频格式 */
        public static final String FORMAT = "pcm";
        
        /** 声道数 */
        public static final int CHANNELS = 1; // 单声道
        
        /** 位深度 */
        public static final int BIT_DEPTH = 16; // 16-bit
        
        /** 字节序 */
        public static final String BYTE_ORDER = "little-endian";
    }
    
    /**
     * TTS配置
     */
    public static class TtsConfig {
        /** TTS说话人 */
        public static final String SPEAKER = "man";
        
        /** 是否返回TTS音频 */
        public static final boolean RETURN_TARGET_TTS = false;
    }
}
