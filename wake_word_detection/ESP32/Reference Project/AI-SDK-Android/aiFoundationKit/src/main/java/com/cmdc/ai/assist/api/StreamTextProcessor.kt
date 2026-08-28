package com.cmdc.ai.assist.api

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.regex.Pattern
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

/**
 * 大模型流式响应文本处理器
 * 用于接收流式文本，按句子分割并加入队列供TTS使用
 * 支持基于字符数的早期分割，提高 TTS 响应速度
 */
class StreamTextProcessor(queueCapacity: Int? = null) {

    // 句子分隔符的正则表达式（逗号、句号、换行符等）
    companion object {
        private val SENTENCE_DELIMITERS = Pattern.compile("[，。！？；,!\\?;\\n]+")

        // 软分隔符（用于早期分割的标点符号）
        private val SOFT_DELIMITERS = Pattern.compile("[，,、]")
    }

    // 存储完整句子的队列
    private val sentenceQueue: BlockingQueue<String>

    // 临时缓存未完成的文本片段
    private val textBuffer = StringBuilder()

    // 处理状态标识
    private val isProcessing = AtomicBoolean(false)

    // 句子处理回调（可选，用于监听句子生成）
    private var sentenceCallback: Consumer<String>? = null

    // 早期分割的字符数阈值（默认10个字符）
    var earlySplitThreshold = 8
        private set

    // 是否启用早期分割
    var enableEarlySplit = true
        private set

    // 记录是否是第一句话
    private val isFirstSentence = AtomicBoolean(true)

    init {
        sentenceQueue = if (queueCapacity != null) {
            LinkedBlockingQueue(queueCapacity)
        } else {
            LinkedBlockingQueue()
        }
    }

    /**
     * 设置句子处理回调
     * @param callback 当有新句子生成时的回调函数
     */
    fun setSentenceCallback(callback: Consumer<String>?) {
        this.sentenceCallback = callback
    }

    /**
     * 设置早期分割的字符数阈值
     * @param threshold 字符数阈值
     */
    fun setEarlySplitThreshold(threshold: Int) {
        this.earlySplitThreshold = threshold
    }

    /**
     * 启用或禁用早期分割
     * @param enable 是否启用
     */
    fun setEnableEarlySplit(enable: Boolean) {
        this.enableEarlySplit = enable
    }

    /**
     * 处理流式文本片段
     * @param textChunk 接收到的文本片段
     */
    fun processTextChunk(textChunk: String?) {
        if (textChunk == null || textChunk.isEmpty()) {
            return
        }

        synchronized(textBuffer) {
            textBuffer.append(textChunk)

            // 先尝试提取完整句子
            extractSentences()

            // 如果启用早期分割且是第一句话，检查是否需要早期分割
            if (enableEarlySplit && isFirstSentence.get()) {
                checkEarlySplit()
            }
        }
    }

    /**
     * 检查是否需要进行早期分割
     */
    private fun checkEarlySplit() {
        val currentText = textBuffer.toString().trim { it <= ' ' }

        // 如果当前文本长度超过阈值，尝试在软分隔符处分割
        if (currentText.length >= earlySplitThreshold) {
            // 寻找最后一个软分隔符的位置
            val lastSoftDelimiterIndex = findLastSoftDelimiter(currentText)

            if (lastSoftDelimiterIndex > 0) {
                // 在软分隔符处分割
                val earlySegment =
                    currentText.substring(0, lastSoftDelimiterIndex).trim { it <= ' ' }
                val remaining = currentText.substring(lastSoftDelimiterIndex + 1).trim { it <= ' ' }

                if (earlySegment.isNotEmpty()) {
                    addSentenceToQueue(earlySegment)
                    isFirstSentence.set(false)

                    // 更新缓冲区
                    textBuffer.setLength(0)
                    if (remaining.isNotEmpty()) {
                        textBuffer.append(remaining)
                    }
                }
            } else if (currentText.length >= (earlySplitThreshold * 1.5).toInt()) {
                // 如果没有软分隔符但文本很长，强制分割
                val earlySegment = currentText.substring(0, earlySplitThreshold).trim { it <= ' ' }
                val remaining = currentText.substring(earlySplitThreshold).trim { it <= ' ' }

                if (earlySegment.isNotEmpty()) {
                    addSentenceToQueue(earlySegment)
                    isFirstSentence.set(false)

                    // 更新缓冲区
                    textBuffer.setLength(0)
                    if (remaining.isNotEmpty()) {
                        textBuffer.append(remaining)
                    }
                }
            }
        }
    }

    /**
     * 查找最后一个软分隔符的位置
     * @param text 要搜索的文本
     * @return 最后一个软分隔符的位置，如果没找到返回-1
     */
    private fun findLastSoftDelimiter(text: String): Int {
        var lastIndex = -1
        val matcher = SOFT_DELIMITERS.matcher(text)
        while (matcher.find()) {
            lastIndex = matcher.start()
        }
        return lastIndex
    }

    /**
     * 从缓冲区提取完整句子
     */
    private fun extractSentences() {
        val currentText = textBuffer.toString()
        val parts = SENTENCE_DELIMITERS.split(currentText, -1)

        // 如果只有一个部分且没有分隔符，说明还没有完整句子
        if (parts.size <= 1) {
            return
        }

        // 处理完整的句子（除了最后一个部分）
        for (i in 0 until parts.size - 1) {
            val sentence = parts[i].trim { it <= ' ' }
            if (sentence.isNotEmpty()) {
                addSentenceToQueue(sentence)
                isFirstSentence.set(false)
            }
        }

        // 保留最后一个未完成的部分
        textBuffer.setLength(0)
        val lastPart = parts[parts.size - 1]
        if (lastPart.isNotEmpty()) {
            textBuffer.append(lastPart)
        }
    }

    /**
     * 将句子添加到队列
     * @param sentence 完整的句子
     */
    private fun addSentenceToQueue(sentence: String) {
        try {
            sentenceQueue.offer(sentence)

            // 触发回调
            sentenceCallback?.accept(sentence)

            println("新句子加入队列: $sentence")
        } catch (e: Exception) {
            System.err.println("添加句子到队列失败: ${e.message}")
        }
    }

    /**
     * 完成流式处理，处理剩余的文本
     */
    fun finishProcessing() {
        synchronized(textBuffer) {
            val remainingText = textBuffer.toString().trim { it <= ' ' }
            if (remainingText.isNotEmpty()) {
                addSentenceToQueue(remainingText)
                textBuffer.setLength(0)
            }
        }
        isProcessing.set(false)
    }

    /**
     * 开始处理流式数据
     */
    fun startProcessing() {
        isProcessing.set(true)
        isFirstSentence.set(true)
        clearQueue()
        synchronized(textBuffer) {
            textBuffer.setLength(0)
        }
    }

    /**
     * 获取下一个句子（阻塞方法）
     * @return 下一个完整的句子
     * @throws InterruptedException 如果线程被中断
     */
    @Throws(InterruptedException::class)
    fun getNextSentence(): String {
        return sentenceQueue.take()
    }

    /**
     * 尝试获取下一个句子（非阻塞方法）
     * @return 下一个句子，如果队列为空则返回null
     */
    fun pollNextSentence(): String? {
        return sentenceQueue.poll()
    }

    /**
     * 获取队列中剩余句子数量
     * @return 队列大小
     */
    fun getQueueSize(): Int {
        return sentenceQueue.size
    }

    /**
     * 检查是否还在处理中
     * @return 处理状态
     */
    fun isProcessing(): Boolean {
        return isProcessing.get()
    }

    /**
     * 检查是否有句子可用
     * @return 是否有句子
     */
    fun hasSentences(): Boolean {
        return sentenceQueue.isNotEmpty()
    }

    /**
     * 清空队列
     */
    fun clearQueue() {
        sentenceQueue.clear()
    }

    /**
     * 异步处理 TTS 转换
     * @param ttsProcessor TTS处理函数
     * @return CompletableFuture
     */
    fun startTTSProcessing(ttsProcessor: Consumer<String>): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            try {
                while (isProcessing() || hasSentences()) {
                    val sentence = getNextSentence()
                    sentence?.let { ttsProcessor.accept(it) }
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                System.err.println("TTS处理被中断: ${e.message}")
            }
        }
    }

    fun main(args: Array<String>) {
        val processor = StreamTextProcessor()

        // 设置早期分割阈值为10个字符
        processor.setEarlySplitThreshold(10)
        processor.setEnableEarlySplit(false)

        // 设置句子处理回调
        processor.setSentenceCallback(Consumer { sentence ->
            println("检测到新句子: $sentence (长度: ${sentence.length})")
        })

        // 开始处理
        processor.startProcessing()

        // 模拟流式数据接收 - 测试长句子的早期分割
        processor.processTextChunk("今天是一个非常美好的阳光明媚的")
        processor.processTextChunk("日子，")
        processor.processTextChunk("")
        processor.processTextChunk("微风徐徐。")
        processor.processTextChunk("我们可以去公园")
        processor.processTextChunk("散步，")
        processor.processTextChunk("享受这美好的时光。")

        // 完成处理
        processor.finishProcessing()

        // 获取处理结果
        println("队列中的句子数量: ${processor.getQueueSize()}")

        // 消费队列中的句子
        while (processor.hasSentences()) {
            val sentence = processor.pollNextSentence()
            sentence?.let { println("TTS处理: $it") }
        }
    }
}
