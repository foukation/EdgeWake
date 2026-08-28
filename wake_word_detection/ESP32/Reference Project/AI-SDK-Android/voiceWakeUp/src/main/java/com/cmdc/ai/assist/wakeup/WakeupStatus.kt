package com.cmdc.ai.assist.wakeup

/**
 * 唤醒引擎状态。
 *
 * 表示唤醒引擎当前所处的生命周期阶段。本枚举为对底层唤醒引擎
 * `WakeupStatus` 的 1:1 复刻，仅将命名空间替换为 `com.cmdc.ai.assist.wakeup`。
 */
enum class WakeupStatus {

    /** 未初始化：尚未调用初始化接口。 */
    UN_INIT,

    /** 已初始化：已完成初始化，但尚未开始监听。 */
    INIT,

    /** 运行中：已开始监听，正在检测唤醒词。 */
    START,

    /** 已停止：已停止监听。 */
    STOP
}
