package com.cmdc.ai.assist.api

/**
 * 录音流接口
 * 定义了录音流的基本操作，包括开始录音、读取录音数据和关闭录音流
 */
interface IRecordStream {

    /**
     * 开始录音
     */
    fun startRecording()

    /**
     * 从录音流中读取数据
     * @param buffer 用于存储读取数据的字节数组
     * @param i 读取的起始位置
     * @param size 要读取的数据大小
     * @return 实际读取的数据大小
     */
    fun read(buffer: ByteArray, i: Int, size: Int): Int

    /**
     * 关闭录音流
     */
    fun close()

    /**
     * 从录音流中读取数据，从索引0开始读取整个缓冲区大小的数据
     * @param buffer 用于存储读取数据的字节数组
     * @return 实际读取的数据大小
     */
    fun read(buffer: ByteArray): Int

    /**
     * 非阻塞读取录音数据。
     *
     * 与 [read] 的区别：
     * - 底层缓冲有数据时立刻返回已读取字节数；
     * - 没有数据时立刻返回 0，不会阻塞等待麦克风采集；
     * - 不负责惰性启动录音设备。
     *
     * 典型使用场景：上层在阻塞读完一帧后，连续非阻塞读出建联期间堆积的音频，
     * 以"有积压时连发追平，无积压时回到阻塞读自然节拍"取代死板的 Thread.sleep。
     *
     * 默认实现返回 0，便于不支持非阻塞的实现类向后兼容（视作"无积压"）。
     *
     * @param buffer 用于存储读取数据的字节数组
     * @param i      写入起始偏移
     * @param size   期望读取的字节数
     * @return 实际读取的字节数；无数据或不支持时返回 0
     */
    fun readNonBlocking(buffer: ByteArray, i: Int, size: Int): Int = 0

}