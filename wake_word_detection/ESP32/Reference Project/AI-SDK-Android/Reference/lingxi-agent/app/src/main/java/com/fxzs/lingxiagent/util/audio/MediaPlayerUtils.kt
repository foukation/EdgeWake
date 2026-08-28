package com.fxzs.lingxiagent.util.audio

import android.media.AudioManager
import android.media.MediaPlayer
import android.text.TextUtils

/**
 * 媒体播放器工具类
 *
 * 该类封装了Android MediaPlayer的常用操作，提供简单易用的媒体播放功能。
 * 采用单例模式设计，确保全局只有一个播放器实例，避免资源冲突。
 * 支持网络音频和本地音频文件的播放控制。
 *
 * 功能特点：
 * - 单例模式：确保全局播放器实例的唯一性
 * - 简化接口：封装MediaPlayer的复杂操作
 * - 状态管理：跟踪播放状态和当前播放路径
 * - 资源管理：自动处理播放器的创建和释放
 *
 * 支持的媒体格式：
 * - MP3、AAC、WAV等常见音频格式
 * - 网络流媒体和本地文件
 *
 * 使用示例：
 * ```kotlin
 * MediaPlayerUtils.instance.create()
 * MediaPlayerUtils.instance.play("http://example.com/audio.mp3")
 * MediaPlayerUtils.instance.pause()
 * MediaPlayerUtils.instance.resume()
 * MediaPlayerUtils.instance.stop()
 * ```
 *
 * @author jv.lee
 * @date 2023/11/1
 * @since 1.0.0
 */
class MediaPlayerUtils {

    /**
     * 伴生对象，提供单例实例访问
     */
    companion object {
        /**
         * 懒加载单例实例
         * 使用lazy确保线程安全的延迟初始化
         */
        val instance by lazy { MediaPlayerUtils() }
    }

    /**
     * Android MediaPlayer实例
     * 可空类型，确保资源正确释放
     */
    private var mPlayer: MediaPlayer? = null

    /**
     * 当前播放的媒体路径
     * 用于跟踪当前播放的媒体文件URL或路径
     */
    private var path = ""

    /**
     * 创建MediaPlayer实例
     *
     * 初始化MediaPlayer对象。在使用播放功能之前必须调用此方法。
     * 如果之前已存在实例，会被新实例覆盖。
     */
    fun create() {
        mPlayer = MediaPlayer()
    }

    /**
     * 播放指定的媒体文件
     *
     * 重置播放器状态，设置数据源，准备并开始播放。
     * 支持网络URL和本地文件路径。
     *
     * @param uri 媒体文件的URI或本地路径
     */
    fun play(uri: String) {
        play(uri, null)
    }

    fun play(uri: String, listener: OnStartListener?) {
        if (TextUtils.isEmpty(uri)) return
        path = uri
        mPlayer?.reset()
        // 设置音频流类型和最大音量
        mPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
        mPlayer?.setVolume(1.0f, 1.0f)  // 左右声道都设为最大
        mPlayer?.setDataSource(uri)
        mPlayer?.prepareAsync()
        mPlayer?.setOnPreparedListener { mp ->
            listener?.onStart()
            mp.start()
        }
        mPlayer?.setOnCompletionListener { mp ->
            listener?.onComplete()
        }
    }

    interface OnStartListener {
        fun onStart()
        fun onComplete()
    }

    /**
     * 恢复播放
     *
     * 恢复被暂停的播放。如果播放器当前没有播放状态，
     * 此方法不会产生效果。
     *
     * 注意：此方法名存在歧义，实际上是恢复播放而非开始播放
     */
    fun start() {
        if (mPlayer?.isPlaying == true) {
            mPlayer?.start()
        }
    }

    /**
     * 暂停播放
     *
     * 暂停当前的媒体播放。暂停后可以调用start()方法恢复播放。
     * 如果当前没有在播放，此方法不会产生效果。
     */
    fun pause() {
        if (mPlayer?.isPlaying == true) {
            mPlayer?.pause()
        }
    }

    /**
     * 停止播放
     *
     * 完全停止当前的媒体播放并重置播放器状态。
     * 停止后需要重新调用play()方法才能开始新的播放。
     */
    fun stop() {
        if (mPlayer?.isPlaying == true) {
            mPlayer?.stop()
            mPlayer?.reset()
        }
    }

    /**
     * 检查是否正在播放
     *
     * @return Boolean true表示正在播放，false表示未播放
     */
    fun isPlaying(): Boolean {
        return mPlayer?.isPlaying ?: false
    }

    /**
     * 释放MediaPlayer资源
     *
     * 释放MediaPlayer占用的系统资源。
     * 在不需要播放器时应调用此方法以避免内存泄漏。
     * 释放后需要重新调用create()方法才能再次使用。
     */
    fun release() {
        mPlayer?.release()
        mPlayer = null
    }

    /**
     * 获取当前播放的媒体URI
     *
     * @return String 当前播放的媒体文件路径或URL
     */
    fun getUri() = path

    /**
     * 获取MediaPlayer实例
     *
     * 提供对底层MediaPlayer实例的直接访问，
     * 用于高级操作和监听器设置。
     *
     * @return MediaPlayer? 当前的MediaPlayer实例，可能为null
     */
    fun getPlayer() = mPlayer


}