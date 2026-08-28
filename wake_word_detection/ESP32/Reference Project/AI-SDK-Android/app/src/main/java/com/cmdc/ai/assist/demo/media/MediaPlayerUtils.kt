package com.cmdc.ai.assist.demo.media

import android.media.MediaPlayer
import android.text.TextUtils

/**
 *
 * @author jv.lee
 * @date 2023/11/1
 */
class MediaPlayerUtils {

    companion object {
        val instance by lazy { MediaPlayerUtils() }
    }

    private var mPlayer: MediaPlayer? = null
    private var path = ""

    fun create() {
        mPlayer = MediaPlayer()
    }

    fun play(uri: String) {
        if (TextUtils.isEmpty(uri)) return
        path = uri
        mPlayer?.reset()
        mPlayer?.setDataSource(uri)
        mPlayer?.prepare()
        mPlayer?.start()
    }

    fun start() {
        if (mPlayer?.isPlaying == true) {
            mPlayer?.start()
        }
    }

    fun pause() {
        if (mPlayer?.isPlaying == true) {
            mPlayer?.pause()
        }
    }

    fun stop() {
        if (mPlayer?.isPlaying == true) {
            mPlayer?.stop()
            mPlayer?.reset()
        }
    }

    fun isPlaying(): Boolean {
        return mPlayer?.isPlaying ?: false
    }

    fun release() {
        mPlayer?.release()
        mPlayer = null
    }

    fun getUri() = path

    fun getPlayer() = mPlayer


}