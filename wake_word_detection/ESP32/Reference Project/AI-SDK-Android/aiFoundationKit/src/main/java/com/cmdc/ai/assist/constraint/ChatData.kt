package com.cmdc.ai.assist.constraint

import java.io.Serializable

/**
 * 表示聊天数据的类，用于存储聊天相关的各种信息
 * 实现了 Serializable 接口，以便于对象的序列化和反序列化
 *
 * @param isSend 发送状态，true 表示已发送，false 表示已接收
 * @param text 聊天文本内容
 */
class ChatData(var isSend: Boolean, var text: String) : Serializable {
    /**
     * 媒体资源的URL，如图片或视频链接
     * */
    var mediaUrl: String? = null

    /**
     * 相册名称，用于标识媒体资源所属的相册或专辑
     * */
    var albumName: String? = null

    /**
     * 图片资源的URL或路径
     * */
    var pic: String? = null

    /**
     * 是否正在生成内容，用于标识某些需要时间生成的内容状态
     * */
    var isGenerating: Boolean = false

    /**
     * 生成内容的进度百分比
     * */
    var percent: Int = 0

    /**
     * 是否正在播放音乐，用于标识音乐播放状态
     * */
    var isMusicPlay: Boolean = false

    /**
     * 辅助生成答案的原始内容，用于存储生成过程中间结果
     * */
    var rawAssistAnswer: String? = null
}
