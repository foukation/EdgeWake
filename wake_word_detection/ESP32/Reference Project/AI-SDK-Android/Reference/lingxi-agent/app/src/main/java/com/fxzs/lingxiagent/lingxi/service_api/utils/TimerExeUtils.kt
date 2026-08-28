package com.example.service_api.utils

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * 定时任务工具类，提供可配置间隔时间的定时任务功能
 */
object TimerExeUtils {
    private var executor: ScheduledExecutorService? = null
    private var currentTask: Runnable? = null
//    自定义执行间隔（例如每30分钟
//    TimerUtils.startPeriodicTask(
//    task = { /* 您的任务代码 */ },
//    period = 30
//    )
//    自定义时间单位（例如秒）
//    TimerUtils.startPeriodicTask(
//    task = { /* 您的任务代码 */ },
//    period = 600, // 600秒=10分钟
//    unit = TimeUnit.SECONDS
//    )

    /**
     * 启动定时任务
     * @param task 要执行的任务
     * @param initialDelay 初始延迟时间(分钟)，默认为0（立即执行）
     * @param period 执行间隔时间(分钟)，默认为10（10分钟）
     * @param unit 时间单位，默认为TimeUnit.MINUTES
     * @throws IllegalStateException 如果已有任务在运行
     */
    fun startPeriodicTask(
        task: Runnable,
        initialDelay: Long = 0,
        period: Long = 10,
        unit: TimeUnit = TimeUnit.MINUTES
    ) {
        if (executor != null) {
            // throw IllegalStateException("定时任务已在运行，请先停止当前任务")
//            Timber.tag("TimerExeUtils").e("定时任务已在运行，请先停止当前任务")
//            return
            if(isRunning()){
                stop()
            }
        }

        executor = Executors.newSingleThreadScheduledExecutor().apply {
            scheduleAtFixedRate(
                {
                    try {
                        task.run()
                    } catch (e: Exception) {
                        // 记录错误但继续执行
                        e.printStackTrace()
                    }
                },
                initialDelay,
                period,
                unit
            )
        }
        currentTask = task
    }

    /**
     * 停止定时任务
     */
    fun stop() {
        executor?.shutdownNow()
        executor = null
        currentTask = null
    }

    /**
     * 检查定时任务是否正在运行
     */
    fun isRunning(): Boolean {
        return executor != null && !executor?.isShutdown!!
    }
}