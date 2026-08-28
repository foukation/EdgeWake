package com.fxzs.lingxiagent.lingxi.config;

import java.io.Serializable;

/**
 * 应用所需的权限类型枚举
 */
public enum PermissionType implements Serializable {
    /**
     * 无障碍权限（用于辅助功能，如自动点击、屏幕朗读等）
     */
    ACCESSIBILITY,
    /**
     * 音频权限（用于录音、语音识别或播放音频）
     */
    AUDIO,
    /**
     * 悬浮窗权限（用于在其他应用上层显示悬浮窗口）
     */
    FLOAT,
    /**
     * 读取手机状态权限（用于获取设备信息、网络状态、通话状态等）
     */
    READ_PHONE_STATE
}