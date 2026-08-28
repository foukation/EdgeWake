package com.fxzs.lingxiagent.util.audio;

import android.content.Context;
import android.media.MediaPlayer;
import android.text.TextUtils;

import java.util.concurrent.LinkedBlockingDeque;

import timber.log.Timber;

/**
 * TTS 媒体播放器类，用于管理和播放 TTS（文本转语音）音频
 * 该类使用单例模式，通过 getInstance() 方法获取实例
 * 支持音频播放队列管理、播放控制和监听器回调等功能
 *
 * @author yhs
 */
public class TtsMediaPlayer {
    private static final String TAG = "TtsMediaPlayer";
    /**
     * TTS 音频 URL 队列，用于存储待播放的 TTS 音频地址
     */
    private final LinkedBlockingDeque<String> ttsUrls = new LinkedBlockingDeque<>();
    /**
     * 当前播放的音频 URL
     */
    private String playUrl;
    /**
     * 当前播放任务的唯一标识符
     */
    private String currentQid = null;
    /**
     * Android 媒体播放器实例
     */
    private MediaPlayer player;

    /**
     * 播放器停止监听器
     */
    private OnPlayerListener mOnPlayerListener;

    public void setPlayerListener(OnPlayerListener playerListener) {
        this.mOnPlayerListener = playerListener;
    }

    /**
     * 停止当前播放并清空播放队列
     * 会删除所有已缓存的音频文件并重置相关状态
     */
    public void stop() {
        currentQid = null;
        AsyncAudioFileDeleter.INSTANCE.deleteAsync(playUrl);
        AsyncAudioFileDeleter.INSTANCE.deleteAsync(ttsUrls);
        playUrl = null;
        ttsUrls.clear();
        if (player != null && player.isPlaying()) {
            player.stop();
            if (mOnPlayerListener != null) {
                mOnPlayerListener.playerStop();
            }
        }
    }

    /**
     * 释放媒体播放器资源
     * 将播放器的 OnCompletionListener 设置为 null 并释放播放器资源
     */
    public void release() {
        if (player == null)
            return;
        player.setOnCompletionListener(null);
        player.release();
        player = null;
    }

    /**
     * 添加 TTS 音频到播放队列并开始播放
     *
     * @param qid    任务 ID，用于标识当前播放任务
     * @param ttsUrl 需要播放的 TTS 音频 URL
     */
    public void speak(String qid, String ttsUrl) {
        if (!TextUtils.equals(qid, currentQid)) {
            stop();
            currentQid = qid;
        }
        ttsUrls.add(ttsUrl);
        checkToPlay();
    }

    /**
     * 检查并播放队列中的下一个音频
     * 如果当前有音频正在播放则直接返回，否则从队列中取出下一个音频进行播放
     */
    private void checkToPlay() {
        if (player.isPlaying()) {
            return;
        }
        playUrl = ttsUrls.poll();
        if (TextUtils.isEmpty(playUrl)) {
            if (mOnPlayerListener != null) {
                mOnPlayerListener.playerStop();
            }
            return;
        }
        try {
            player.reset();
            player.setDataSource(playUrl);
            player.setOnPreparedListener(mediaPlayer -> {
                player.start();
                if (mOnPlayerListener != null) {
                    mOnPlayerListener.playerStart();
                }
            });
            player.prepareAsync();
        } catch (Exception e) {
            // ..
        }
    }

    /**
     * 获取 TtsMediaPlayer 单例实例
     *
     * @return TtsMediaPlayer 单例对象
     */
    public static TtsMediaPlayer getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * 私有构造函数，初始化 MediaPlayer 并设置播放完成监听器
     */
    private TtsMediaPlayer() {
        player = new MediaPlayer();
        /*
          媒体播放完成监听器，当一个音频播放完成后会调用此监听器
          主要用于清理已播放的音频文件并检查播放队列中是否有下一个音频需要播放
         */
        final MediaPlayer.OnCompletionListener completionListener = mp -> {
            AsyncAudioFileDeleter.INSTANCE.deleteAsync(playUrl);
            checkToPlay();
        };
        player.setOnCompletionListener(completionListener);
    }

    /**
     * 静态内部类，用于实现单例模式的延迟加载
     */
    private static class Holder {
        private static final TtsMediaPlayer INSTANCE = new TtsMediaPlayer();
    }


    // ===== 新增方法：播放 res/raw/ 下的音频 =====
    /**
     * 播放 res/raw/ 目录下的音频资源（如 R.raw.wakeup）
     * 该方法会立即播放，不加入 TTS 队列，适用于短提示音
     *
     * @param rawResId res/raw/ 下的资源 ID，如 R.raw.wakeup
     */
    public void playRawSound(int rawResId, Context context) {
        try {
            // 创建临时 MediaPlayer 播放 raw 资源（不影响主 player 队列）
            MediaPlayer tempPlayer = MediaPlayer.create(context, rawResId);
            if (tempPlayer != null) {
                tempPlayer.setOnCompletionListener(mp -> {
                    mp.release(); // 播放完自动释放
                    if (mOnPlayerListener != null) {
                        mOnPlayerListener.playerStop();
                    }
                });
                tempPlayer.setOnPreparedListener(mp -> {
                    if (mOnPlayerListener != null) {
                        mOnPlayerListener.playerStart();
                    }
                });
                tempPlayer.start();
            }
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "Failed to play raw sound: %d", rawResId);
        }
    }
}
