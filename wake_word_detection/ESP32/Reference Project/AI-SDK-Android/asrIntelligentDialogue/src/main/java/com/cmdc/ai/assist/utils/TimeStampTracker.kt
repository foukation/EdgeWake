package com.cmdc.ai.assist.utils

import timber.log.Timber

object TimeStampTracker {

    private val TAG = TimeStampTracker::class.java.simpleName
    private var startTime: Long = 0
    private var isStarted: Boolean = false

    /**
     * 开始计时
     */
    fun start() {
        startTime = System.currentTimeMillis()
        isStarted = true
        Timber.tag(TAG).d("开始时间戳: $startTime")
    }

    /**
     * 结束计时并输出耗时
     */
    fun end() {
        if (!isStarted) {
            Timber.tag(TAG).w("计时器未启动，请先调用 start() 方法")
            return
        }

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        Timber.tag(TAG).d("结束时间戳: $endTime")
        Timber.tag(TAG).d("耗时: ${duration}ms")

        // 重置状态
        isStarted = false
        startTime = 0
    }
}