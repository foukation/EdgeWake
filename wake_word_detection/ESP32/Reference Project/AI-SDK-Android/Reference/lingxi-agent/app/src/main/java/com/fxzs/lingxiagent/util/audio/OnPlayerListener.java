package com.fxzs.lingxiagent.util.audio;

/**
 * 播放器停止监听器接口
 * 当播放器停止时会回调 playerStop 方法
 */
public interface OnPlayerListener {

    void playerStart();

    void playerStop();
}