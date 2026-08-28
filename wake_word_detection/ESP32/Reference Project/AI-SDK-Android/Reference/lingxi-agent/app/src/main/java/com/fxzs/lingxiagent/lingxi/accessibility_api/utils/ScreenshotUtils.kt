package com.fxzs.lingxiagent.lingxi.accessibility_api.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.os.Build
import android.os.Environment
import android.util.Base64
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.core.graphics.scale
import com.fxzs.lingxiagent.service.BaseAccessibilityService
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream
import kotlin.math.min


object ScreenshotUtils {

    private var Tag : String = "ScreenshotUtils"
    @Volatile
    var isBitmapLandscape: Boolean = false
        private set

    @Volatile
    var screenshotSize: Point = Point(0, 0)
        private set

    /**
     * 保存上一次的截图 Bitmap，用于比较差异
     */
    @Volatile
    private var lastScreenshotBitmap: Bitmap? = null
    
    /**
     * 比较计数器，用于命名保存的图片
     */
    private var compareCounter: Int = 0
    @RequiresApi(Build.VERSION_CODES.R)
    fun getScreenshotBase64(context: Context, callback: (String?) -> Unit) {
        val screenSize =  getScreenSizeWithNavigationBar(context)
        TaskPool.CACHE.execute {
            var imageBase64: String? = null
            try {
                // 调用截图方法
                BaseAccessibilityService().takeScreenshotSec { result ->
                    if (result != null) {
                
                        // 根据截图的实际方向来确定是否为横屏
                        isBitmapLandscape = result.width > result.height
                        screenshotSize = Point(result.width, result.height)
                        Timber.tag(Tag).i("屏幕尺寸：${screenSize.x}x${screenSize.y}, 截图尺寸：${result.width}x${result.height}, 截图方向：${if (isBitmapLandscape) "横屏" else "竖屏"}")
                
                        // 更新上一次的截图（复制一份保存）
                        lastScreenshotBitmap?.recycle()
                        lastScreenshotBitmap = result.copy(result.config, false)
                
                        // 使用截图的实际尺寸来计算比例
                        val longSide = if (isBitmapLandscape) result.width else result.height
                        val shortSide = if (isBitmapLandscape) result.height else result.width

                        // 计算比例，确保长边对应1120像素
                        val proportion = 1120.0 / longSide
                        val scaledLongSide = 1120
                        val scaledShortSide = (shortSide * proportion).toInt()

                        val targetWidth = if (isBitmapLandscape) scaledLongSide else scaledShortSide
                        val targetHeight = if (isBitmapLandscape) scaledShortSide else scaledLongSide

                        Timber.tag(Tag).i("图片压缩尺寸: 原尺寸=${result.width}x${result.height}, 目标尺寸=${targetWidth}x${targetHeight}, 比例=$proportion")
                        imageBase64 = getBase64FromBitmapSize(result, targetWidth, targetHeight)
                        // 回收 Bitmap（注意：这里不能回收 result，因为我们已经复制了一份保存到 lastScreenshotBitmap）
                        // result.recycle() // 注释掉这行，因为 result 已经被复制到 lastScreenshotBitmap

                        // 在主线程上回调结果
                        TaskPool.MAIN.post {
                            callback(imageBase64)
                        }
                    } else {
                        // 在主线程上回调结果
                        TaskPool.MAIN.post {
                            callback("截图失败")
                        }

                    }
                }
            } catch (e: Exception) {
                // 处理可能的异常（例如，截图失败、文件写入失败等）
                Timber.tag(Tag).i("截图失败: ${e.printStackTrace()}")
                e.printStackTrace()
            }
        }
    }

    fun getScreenSizeWithNavigationBar(context: Context): Point {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getRealSize(size)
        return size
    }

    fun getBase64FromBitmapSize(bitmap: Bitmap?, maxWidth: Int, maxHeight: Int): String? {

        // 检查位图是否为空
        bitmap ?: return null

        // 计算缩放比例
        val scaleWidth = maxWidth.toFloat() / bitmap.width.toFloat()
        val scaleHeight = maxHeight.toFloat() / bitmap.height.toFloat()
        val scale = min(scaleWidth, scaleHeight)

        // 创建缩放后的位图
        val scaledBitmap =
            bitmap.scale((bitmap.width * scale).toInt(), (bitmap.height * scale).toInt())

        // 使用 ByteArrayOutputStream 捕获压缩后的位图数据
        val byteArrayOutputStream = ByteArrayOutputStream()
        // 将缩放后的位图压缩为 PNG 格式，并写入 ByteArrayOutputStream（这里可以根据需要调整质量）
        scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)

        // 注意：如果需要更小的文件大小，可以考虑使用 JPEG 格式并降低质量
        // scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)

        // 获取压缩后的字节数组
        val imageBytes = byteArrayOutputStream.toByteArray()
        Timber.tag(Tag).i("图片压缩: ${imageBytes.size} 字节")

        // 将字节数组转换为 Base64 编码的字符串，不使用换行符
        return Base64.encodeToString(imageBytes, Base64.NO_WRAP)

    }

    /**
     * 快速比较两个 Bitmap 是否有差异
     * @param bitmap1 第一个 Bitmap
     * @param bitmap2 第二个 Bitmap
     * @param threshold 颜色差异阈值 (0-255)，默认 30
     * @param minChangeRatio 最小变化比例 (0.0-1.0)，默认 0.2 表示 20%
     * @return true 表示有差异，false 表示无差异
     */
    fun hasBitmapDifference(bitmap1: Bitmap?, bitmap2: Bitmap?, threshold: Int = 30, minChangeRatio: Float = 0.2f): Boolean {
        // 检查位图是否为空
        if (bitmap1 == null || bitmap2 == null) {
            Timber.tag(Tag).w("Bitmap 为空，无法比较")
            return false
        }

        // 检查尺寸是否一致
        if (bitmap1.width != bitmap2.width || bitmap1.height != bitmap2.height) {
            Timber.tag(Tag).w("Bitmap 尺寸不一致，视为有差异：${bitmap1.width}x${bitmap1.height} vs ${bitmap2.width}x${bitmap2.height}")
            return true
        }

        return hasBitmapDifferenceInternal(bitmap1, bitmap2, threshold, minChangeRatio)
    }

    /**
     * 内部方法：执行实际的 Bitmap 比较（快速版本）
     */
    private fun hasBitmapDifferenceInternal(bitmap1: Bitmap, bitmap2: Bitmap, threshold: Int, minChangeRatio: Float = 0.2f): Boolean {
        val width = bitmap1.width
        val height = bitmap1.height
        val totalPixels = width * height
        val minDiffPixels = (totalPixels * minChangeRatio).toInt()
        
        // 优化：使用像素数组批量获取，避免重复调用 getPixel
        val pixels1 = IntArray(width * height)
        val pixels2 = IntArray(width * height)
        bitmap1.getPixels(pixels1, 0, width, 0, 0, width, height)
        bitmap2.getPixels(pixels2, 0, width, 0, 0, width, height)
        
        var diffPixelCount = 0
        var index = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel1 = pixels1[index]
                val pixel2 = pixels2[index]
                index++
                
                // 快速比较：如果完全相同，跳过
                if (pixel1 == pixel2) {
                    continue
                }
                
                // 计算颜色差异
                val diff = calculateColorDifference(pixel1, pixel2)
                
                // 超过阈值的像素才计数
                if (diff > threshold) {
                    diffPixelCount++
                    
                    // 提前退出：如果已达到最小变化比例，直接返回 true
                    if (diffPixelCount >= minDiffPixels) {
                        return true
                    }
                }
            }
        }
        
        // 所有像素都检查完毕，差异像素未达到最小比例
        return false
    }



    /**
     * 计算两个像素颜色的差异 (返回 RGB 各通道差值的平均值)
     */
    private fun calculateColorDifference(color1: Int, color2: Int): Int {
        val r1 = Color.red(color1)
        val g1 = Color.green(color1)
        val b1 = Color.blue(color1)
        
        val r2 = Color.red(color2)
        val g2 = Color.green(color2)
        val b2 = Color.blue(color2)
        
        val diffR = kotlin.math.abs(r1 - r2)
        val diffG = kotlin.math.abs(g1 - g2)
        val diffB = kotlin.math.abs(b1 - b2)
        
        return (diffR + diffG + diffB) / 3
    }



    /**
     * 差异区域信息
     */
    data class DifferenceInfo(
        val region: DifferenceRegion,
        val differencePercentage: Double,
        val diffBitmap: Bitmap?
    ) {
        override fun toString(): String {
            return "差异区域：[${region.minX},${region.minY}] 到 [${region.maxX},${region.maxY}], " +
                   "宽度=${region.width}, 高度=${region.height}, " +
                   "差异比例=%.2f%%".format(differencePercentage)
        }
    }

    /**
     * 差异区域坐标
     */
    data class DifferenceRegion(
        val minX: Int,
        val minY: Int,
        val maxX: Int,
        val maxY: Int
    ) {
        val width: Int get() = maxX - minX + 1
        val height: Int get() = maxY - minY + 1
    }

    /**
     * 清除保存的上一次截图
     */
    fun clearLastScreenshot() {
        lastScreenshotBitmap?.recycle()
        lastScreenshotBitmap = null
        Timber.tag(Tag).i("已清除上一次截图")
    }
    
    /**
     * 删除所有保存的调试图片
     * @param deleteAll true 表示删除所有图片，false 表示只保留最近 N 次比较的图片
     * @param keepRecentCount 当 deleteAll=false 时，保留最近 N 次比较的图片（默认保留 3 次）
     * @return 删除的文件数量
     */
    fun clearDebugScreenshots(deleteAll: Boolean = true, keepRecentCount: Int = 3): Int {
        return try {
            // 获取外部存储目录
            val externalDir = Environment.getExternalStorageDirectory()
            val saveDir = File(externalDir, "Pictures/ScreenshotUtils")
            
            if (!saveDir.exists()) {
                Timber.tag(Tag).i("调试图片目录不存在，无需清理")
                return 0
            }
            
            var deletedCount = 0
            
            if (deleteAll) {
                // 删除所有图片
                saveDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.name.endsWith(".png")) {
                        if (file.delete()) {
                            deletedCount++
                        } else {
                            Timber.tag(Tag).w("删除文件失败：${file.absolutePath}")
                        }
                    }
                }
                Timber.tag(Tag).i("已删除所有调试图片，共 $deletedCount 个文件")
            } else {
                // 保留最近 N 次比较的图片
                val allFiles = saveDir.listFiles()?.filter { it.isFile && it.name.endsWith(".png") } ?: emptyList()
                
                // 按文件名排序（因为命名格式是：截图 1_1.png, 截图 1_2.png, 截图 2_1.png...）
                val sortedFiles = allFiles.sortedByDescending { it.name }
                
                // 计算需要保留的文件数（每次比较有 2 张图）
                val keepFileCount = keepRecentCount * 2
                
                // 删除旧文件
                if (sortedFiles.size > keepFileCount) {
                    val filesToDelete = sortedFiles.drop(keepFileCount)
                    filesToDelete.forEach { file ->
                        if (file.delete()) {
                            deletedCount++
                        } else {
                            Timber.tag(Tag).w("删除文件失败：${file.absolutePath}")
                        }
                    }
                    Timber.tag(Tag).i("已清理调试图片，保留最近 $keepRecentCount 次比较，删除 $deletedCount 个文件")
                } else {
                    Timber.tag(Tag).i("当前图片数量 ${allFiles.size} <= 保留数量 $keepFileCount，无需清理")
                }
            }
            
            // 如果目录为空，可以选择删除目录本身
            if (saveDir.listFiles().isEmpty()) {
                saveDir.delete()
                Timber.tag(Tag).i("已删除空目录：${saveDir.absolutePath}")
            }
            
            deletedCount
            
        } catch (e: Exception) {
            Timber.tag(Tag).e("清理调试图片失败：${e.message}")
            e.printStackTrace()
            0
        }
    }

    /**
     * 获取当前屏幕截图并与上一次截图比较差异
     * @param context 上下文
     * @param threshold 颜色差异阈值 (0-255)，默认 30
     * @param saveToSDCard 是否保存到 SD 卡，默认 false（不保存）
     * @param minChangeRatio 最小变化比例 (0.0-1.0)，默认 0.2 表示 20%
     * @param callback 回调函数，返回是否有差异和是否首次截图
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun captureAndCompare(
        context: Context,
        threshold: Int = 30,
        saveToSDCard: Boolean = false,  // 新增参数：是否保存到 SD 卡
        minChangeRatio: Float = 0.2f,   // 新增参数：最小变化比例
        callback: (Boolean, Boolean) -> Unit  // 第一个 Boolean：是否有差异，第二个 Boolean：是否首次截图
    ) {
        TaskPool.CACHE.execute {
            try {
                // 调用截图方法
                BaseAccessibilityService().takeScreenshotSec { result ->
                    if (result != null) {

                        // 判断是否是第一次截图
                        val isFirstCapture = lastScreenshotBitmap == null

                        // 如果是第一次截图，直接返回无差异，无需后续处理
                        if (isFirstCapture) {
                            // 保存当前截图供下次比较
                            lastScreenshotBitmap?.recycle()
                            if (result.config == android.graphics.Bitmap.Config.HARDWARE) {
                                lastScreenshotBitmap = result.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                            } else {
                                lastScreenshotBitmap = result
                            }
                            
                            TaskPool.MAIN.post {
                                callback(false, true)  // 无差异，首次截图
                            }
                            return@takeScreenshotSec
                        }

                        // 将 HARDWARE 配置的 Bitmap 转换为软件配置的 Bitmap，以便可以访问像素
                        val softwareBitmap = if (result.config == android.graphics.Bitmap.Config.HARDWARE) {
                            result.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                        } else {
                            result
                        }

                        // 如果有上一次的截图，确保它也是软件配置
                        var validLastBitmap = lastScreenshotBitmap
                        if (validLastBitmap != null && validLastBitmap.config == android.graphics.Bitmap.Config.HARDWARE) {
                            val convertedBitmap = validLastBitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                            validLastBitmap?.recycle()
                            lastScreenshotBitmap = convertedBitmap
                            validLastBitmap = convertedBitmap
                        }

                        // 有上一次的截图，进行快速比较
                        val compareStartTime = System.currentTimeMillis()
                        
                        // 保存图片以便调试（在比较之前，需要复制 Bitmap，因为后续会回收）
                        if (saveToSDCard) {
                            val bitmap1Copy = validLastBitmap!!.copy(validLastBitmap!!.config, false)
                            val bitmap2Copy = softwareBitmap.copy(softwareBitmap.config, false)
                            // 注意：不在这里回收，由 saveBitmapForDebug 方法在 finally 块中回收
                            saveBitmapForDebug(bitmap1Copy, bitmap2Copy)
                        }
                        
                        val hasDifference = hasBitmapDifference(validLastBitmap!!, softwareBitmap, threshold, minChangeRatio)
                        val compareDuration = System.currentTimeMillis() - compareStartTime
                        
                        Timber.tag(Tag).i("屏幕${if (hasDifference) "有变化" else "无变化"}，比较耗时：${compareDuration}ms")

                        // 更新上一次的截图（复制一份保存）
                        lastScreenshotBitmap?.recycle()
                        lastScreenshotBitmap = softwareBitmap.copy(softwareBitmap.config, false)
                        
                        // 如果是转换后的新 Bitmap，需要回收原图
                        if (softwareBitmap != result) {
                            softwareBitmap.recycle()
                        }

                        // 在主线程上回调结果
                        TaskPool.MAIN.post {
                            callback(hasDifference, false)  // 是否有差异，非首次截图
                        }
                    } else {
                        // 在主线程上回调结果
                        TaskPool.MAIN.post {
                            callback(false, false)
                        }
                    }
                }
            } catch (e: Exception) {
                // 处理可能的异常
                Timber.tag(Tag).e("截图失败：${e.message}")
                e.printStackTrace()
                TaskPool.MAIN.post {
                    callback(false, false)
                }
            }
        }
    }



    /**
     * 检测屏幕是否有变化
     * @param context 上下文
     * @return true 表示检测到差异，false 表示无差异或首次截图
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun hasScreenChanged(context: Context): Boolean {
        var hasChanged = false
        val latch = java.util.concurrent.CountDownLatch(1)
        
        captureAndCompare(context) { hasDifference, isFirstCapture ->
            // 如果不是第一次截图且有差异，则认为屏幕有变化
            hasChanged = !isFirstCapture && hasDifference
            Timber.tag(Tag).i("屏幕变化检测：hasChanged=$hasChanged, 有差异=$hasDifference")
            latch.countDown()
        }
        
        try {
            // 等待截图和比较完成
            latch.await()
        } catch (e: InterruptedException) {
            Timber.tag(Tag).e("等待截图完成时被中断：${e.message}")
            Thread.currentThread().interrupt()
        }
        
        return hasChanged
    }
    
    /**
     * 保存用于比较的两张 Bitmap 到存储卡
     * @param bitmap1 第一张 Bitmap（上一次的截图）
     * @param bitmap2 第二张 Bitmap（当前的截图）
     */
    private fun saveBitmapForDebug(bitmap1: Bitmap, bitmap2: Bitmap) {
        TaskPool.CACHE.execute {
            try {
                compareCounter++
                val comparisonNum = compareCounter
                
                // 获取外部存储目录
                val externalDir = Environment.getExternalStorageDirectory()
                val saveDir = File(externalDir, "Pictures/ScreenshotUtils")
                
                if (!saveDir.exists()) {
                    saveDir.mkdirs()
                }
                
                // 保存图片 1
                val file1 = File(saveDir, "截图${comparisonNum}_1.png")
                FileOutputStream(file1).use { out ->
                    bitmap1.compress(Bitmap.CompressFormat.PNG, 100, out)
                    out.flush()
                }
                Timber.tag(Tag).i("已保存图片 1: ${file1.absolutePath}")
                
                // 保存图片 2
                val file2 = File(saveDir, "截图${comparisonNum}_2.png")
                FileOutputStream(file2).use { out ->
                    bitmap2.compress(Bitmap.CompressFormat.PNG, 100, out)
                    out.flush()
                }
                Timber.tag(Tag).i("已保存图片 2: ${file2.absolutePath}")
                
            } catch (e: Exception) {
                Timber.tag(Tag).e("保存图片失败：${e.message}")
                e.printStackTrace()
            } finally {
                // 确保在保存完成后回收 Bitmap
                if (!bitmap1.isRecycled) {
                    bitmap1.recycle()
                }
                if (!bitmap2.isRecycled) {
                    bitmap2.recycle()
                }
            }
        }
    }
}