package com.cmdc.ai.assist.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.os.Process
import java.io.RandomAccessFile

object PerformanceMonitor {

    // 内存监控结构体
    data class MemoryInfo(
        val totalMem: Long,     // 设备总内存（MB）
        val availMem: Long,     // 可用内存（MB）
        val threshold: Long,    // 低内存阈值（MB）
        val lowMemory: Boolean, // 是否低内存状态
        val nativeHeap: Long    // 应用Native堆内存（MB）
    ) {
        fun toMap() = mapOf(
            "total_memory" to totalMem,
            "available_memory" to availMem,
            "low_memory_threshold" to threshold,
            "is_low_memory" to lowMemory,
            "native_heap" to nativeHeap
        )
    }

    // 获取应用内存使用情况
    fun getAppMemoryUsage(context: Context): MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo().apply {
            activityManager.getMemoryInfo(this)
        }

        return MemoryInfo(
            totalMem = memoryInfo.totalMem / 1024 / 1024,
            availMem = memoryInfo.availMem / 1024 / 1024,
            threshold = memoryInfo.threshold / 1024 / 1024,
            lowMemory = memoryInfo.lowMemory,
            nativeHeap = Debug.getNativeHeapAllocatedSize() / 1024 / 1024
        )
    }

    // 获取进程CPU使用率（百分比）
    fun getProcessCpuUsage(): Float {
        val pid = Process.myPid()
        var cpuUsage = 0f

        try {
            val statFile = RandomAccessFile("/proc/$pid/stat", "r")
            val statData = statFile.readLine().split(" ")

            val utime = statData[13].toLong()
            val stime = statData[14].toLong()
            val totalTime = utime + stime

            val uptimeFile = RandomAccessFile("/proc/uptime", "r")
            val uptime = uptimeFile.readLine().split(" ")[0].toFloat()

            cpuUsage = ((totalTime / 100f) / uptime) * 100
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return cpuUsage.coerceIn(0f, 100f)
    }

    // 获取系统CPU总使用率（百分比）
    fun getSystemCpuUsage(): Float {
        var totalCpu = 0f
        try {
            val statFile = RandomAccessFile("/proc/stat", "r")
            val firstLine = statFile.readLine().split(" ")

            val user = firstLine[2].toLong()
            val nice = firstLine[3].toLong()
            val system = firstLine[4].toLong()
            val idle = firstLine[5].toLong()

            val total = user + nice + system + idle
            totalCpu = ((total - idle) * 100f / total).coerceIn(0f, 100f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return totalCpu
    }
}