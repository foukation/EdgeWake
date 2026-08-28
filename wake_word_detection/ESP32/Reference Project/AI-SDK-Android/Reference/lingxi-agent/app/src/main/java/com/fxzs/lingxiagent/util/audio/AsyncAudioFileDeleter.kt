package com.fxzs.lingxiagent.util.audio

import com.fxzs.lingxiagent.util.ZUtil.FileUtils
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingDeque

object AsyncAudioFileDeleter {

    private val TAG = "AsyncAudioFileDeleter"
    private val executor = Executors.newFixedThreadPool(2)

    // 异步删除单个文件
    fun deleteAsync(filePath: String?) {
        if (filePath.isNullOrBlank()) return
        executor.execute {
            try {
                FileUtils.deleteCacheFile(filePath)
            } catch (e: Exception) {
                Timber.tag(TAG).i(e)
            }
        }
    }

    // 异步删除多个文件
    fun deleteAsync(filePaths: LinkedBlockingDeque<String>) {
        val filePathsC = filePaths.toList()
        filePathsC.forEach { deleteAsync(it) }
    }
}