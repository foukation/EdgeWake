package com.fxzs.lingxiagent.lingxi.common.log

import android.content.Context
import android.os.Environment
import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipUtils {
    fun zipFile(inputFile: File, outputFile: File) {
        try {
            FileOutputStream(outputFile).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    val fis = FileInputStream(inputFile)
                    val zipEntry = ZipEntry(inputFile.name)
                    zos.putNextEntry(zipEntry)

                    val buffer = ByteArray(1024)
                    var length: Int
                    while (fis.read(buffer).also { length = it } > 0) {
                        zos.write(buffer, 0, length)
                    }

                    fis.close()
                    zos.closeEntry()
                }
            }
        } catch (e: IOException) {
            Log.e("ZipUtils", "压缩日志文件失败", e)
        }
    }
}

class FileLoggingTree(context: Context) : Timber.Tree() {

    companion object {
        private const val LOG_FOLDER = "logs"
        private const val LOG_FILE_PREFIX = "app_log_"
        private const val LOG_FILE_EXTENSION = ".txt"
        private const val MAX_LOG_FILE_SIZE = 10 * 1024 * 1024 // 10MB
        private const val MAX_LOG_FILES = 40 // 最多保留40个日志文件
    }

    private val logDir: File = getLogDirectory(context)
    private var currentLogFile: File = File("")
    private var currentSize: Long = 0

    init {
        synchronized(this) {
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            currentLogFile = getLatestLogFileSafe()
            currentSize = calculateFileSize(currentLogFile)
        }
    }

    private fun getLogDirectory(context: Context): File {
        // 优先尝试外部存储
        return if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            val externalDir = context.getExternalFilesDir(null)
            if (externalDir != null && externalDir.canWrite()) {
                File(externalDir, LOG_FOLDER)
            } else {
                // 外部存储不可写，回退到内部存储
                File(context.filesDir, LOG_FOLDER)
            }
        } else {
            // 外部存储不可用，使用内部存储
            File(context.filesDir, LOG_FOLDER)
        }
    }

    private fun getLatestLogFileSafe(): File {
        return try {
            getLatestLogFile()
        } catch (e: Exception) {
            Log.e("FileLoggingTree", "获取最新日志文件失败，创建新文件", e)
            createNewLogFileSafe()
        }
    }

    private fun createNewLogFileSafe(): File {
        val dateString = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(logDir, "$LOG_FILE_PREFIX$dateString$LOG_FILE_EXTENSION").also {
            it.createNewFile()
            currentSize = 0
        }
    }

    private fun getLatestLogFile(): File {
        // 获取所有日志文件并按日期排序
        val logFiles = getSortedLogFiles()
        if(!logFiles.isEmpty()){
            currentLogFile = logFiles.last()
            cleanOldLogFiles()
        }
        // 如果没有日志文件或最新文件超过大小限制，创建新文件
        return if (logFiles.isEmpty() || logFiles.last().length() > MAX_LOG_FILE_SIZE) {
            createNewLogFile()
        } else {
            logFiles.last()
        }
    }

    private fun getSortedLogFiles(): List<File> {
        return logDir.listFiles { file ->
            file.name.startsWith(LOG_FILE_PREFIX) && file.name.endsWith(LOG_FILE_EXTENSION) || file.name.endsWith(".zip")
        }?.sortedBy { it.name } ?: emptyList()
    }

    private fun createNewLogFile(): File {
        try {
            if(currentLogFile.exists() ){
                // 压缩上一个日志文件
                if ( currentLogFile.length() > 0) {
                    try {
                        val zipFile = File(logDir, "${currentLogFile.nameWithoutExtension}.zip")
                        ZipUtils.zipFile(currentLogFile, zipFile)
                        currentLogFile.delete()
                    } catch (e: Exception) {
                        Log.e("FileLoggingTree", "压缩日志文件失败", e)
                    }
                }
            }
        }catch (e: Exception) {
            Log.e("FileLoggingTree", "压缩日志文件失败", e)
        }


        // 清理旧日志文件
        cleanOldLogFiles()

        // 创建新日志文件
        val dateString = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(logDir, "$LOG_FILE_PREFIX$dateString$LOG_FILE_EXTENSION").also {
            it.createNewFile()
            currentSize = 0
        }
    }

    private fun cleanOldLogFiles() {
        try {
            val logFiles = getSortedLogFiles()
            if (logFiles.size > MAX_LOG_FILES) {
                val filesToDelete = logFiles.subList(0, logFiles.size - MAX_LOG_FILES)
                filesToDelete.forEach { file ->
                    if (!file.delete()) {
                        Log.i("FileLoggingTree", "删除旧日志文件失败: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FileLoggingTree", "清理旧日志文件时出错", e)
        }
    }
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // 检查文件大小，超过限制则创建新文件
        if (calculateFileSize(currentLogFile) > MAX_LOG_FILE_SIZE) {
            currentLogFile = createNewLogFile()
        }

        try {
            FileWriter(currentLogFile, true).use { writer ->
                val priorityString = when (priority) {
                    Log.VERBOSE -> "V"
                    Log.DEBUG -> "D"
                    Log.INFO -> "I"
                    Log.WARN -> "W"
                    Log.ERROR -> "E"
                    Log.ASSERT -> "A"
                    else -> priority.toString()
                }

                val logTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                    .format(Date())
                val processId = android.os.Process.myPid()
                val threadId = Thread.currentThread().id

                val logMessage = "$logTime [$processId-$threadId] [$priorityString/${tag ?: "null"}] $message"
                writer.append(logMessage)

                t?.let {
                    val stackTrace = it.stackTrace.joinToString("\n") { element ->
                        "    at ${element.className}.${element.methodName}(${element.fileName}:${element.lineNumber})"
                    }
                    writer.append("\n${it.javaClass.name}: ${it.message}\n$stackTrace")
                }
                writer.append("\n")
                currentSize = calculateFileSize(currentLogFile)
//                Log.i("FileLoggingTree currentSize", currentSize.toString())
            }
        } catch (e: IOException) {
            Log.e("FileLoggingTree", "写入日志文件失败", e)
        }
    }

    private fun calculateFileSize(file: File): Long {
        return try {
            file.length()
        } catch (e: SecurityException) {
            Log.e("FileLoggingTree", "获取文件大小失败", e)
            0L
        }
    }
}
