package com.cmdc.ai.assist.wakeup

/**
 * 唤醒回调接口。
 *
 * 用于接收唤醒引擎在初始化、启动、唤醒触发、异常、停止、释放等各阶段的事件通知。
 * 本接口为对底层唤醒引擎 `WakeupCallback` 的 1:1 复刻，方法名与参数均与引擎保持一致，
 * 仅将命名空间替换为 `com.cmdc.ai.assist.wakeup`。
 *
 * 建议在回调中避免执行耗时操作；如需更新 UI，请自行切回主线程。
 */
interface WakeupCallback {

    /** 初始化完成。 */
    fun onInit()

    /** 启动监听成功。 */
    fun onStart()

    /**
     * 触发唤醒。
     *
     * @param info 唤醒事件信息，其中 [WakeupEventInfo.confidence] 为唤醒分数
     */
    fun onWakeup(info: WakeupEventInfo?)

    /**
     * 模型推理分数回调，用作调试。
     *
     * @param score 模型推理分数
     */
    fun onWakeupFrameThreshold(score: Float)

    /**
     * 连续音频数据回调。
     *
     * @param audioData 音频数据，格式为 16k 采样、单声道、16bit、小端序
     */
    fun onAudioData(audioData: ShortArray?)

    /** 停止监听成功。 */
    fun onStop()

    /**
     * 异常回调。
     *
     * @param error 错误信息
     */
    fun onError(error: WakeupError?)

    /** 资源释放完成。 */
    fun onRelease()
}
