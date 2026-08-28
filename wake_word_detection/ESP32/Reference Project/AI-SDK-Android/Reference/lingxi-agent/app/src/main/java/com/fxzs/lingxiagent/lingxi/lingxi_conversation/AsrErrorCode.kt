package com.fxzs.lingxiagent.lingxi.lingxi_conversation

/**
 *创建者：ZyOng
 *描述：语音识别错误码
 *创建时间：2026/2/25 18:11
 */
 class AsrErrorCode {
   companion object{
       /** 无录音权限 */
       val ERROR_PERMISSION = 100
       /** 音频设备异常（被占用/初始化失败/读取异常） */
       val ERROR_AUDIO_DEVICE = 101
       /** WebSocket 网络连接失败 */
       val ERROR_NETWORK = 200
       /** WebSocket 连接超时 */
       val ERROR_TIMEOUT = 201
       /** 数据发送失败（start/audio/finish 信号） */
       val ERROR_SEND_FAILED = 202
       /** 服务端消息解析失败 */
       val ERROR_PROTOCOL = 301
       /** SDK 内部未预期异常 */
       val ERROR_INTERNAL = 500
       /** 配置文件加载失败 */
       val ERROR_CONFIG = 501
       /** 识别已在进行中，勿重复调用 */
       val ERROR_ALREADY_RUNNING = 600
       /** 实例已释放，请重新创建 */
       val ERROR_RELEASED = 601
   }
}