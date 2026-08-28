package com.cmdc.ai.assist.wakeup

import java.io.Serializable

/**
 * 唤醒事件信息。
 *
 * 唤醒触发时通过回调返回，包含命中的唤醒词、置信度、发生时间等信息。
 * 本类为对底层唤醒引擎 `WakeupEventInfo` 的 1:1 复刻，字段名保持一致，
 * 仅将命名空间替换为 `com.cmdc.ai.assist.wakeup`。
 *
 * @property word       命中的唤醒词
 * @property confidence 置信度（唤醒分数），取值范围 [0.0, 1.0]
 * @property sn         唤醒事件唯一标识
 * @property time       唤醒发生时间戳（毫秒）
 * @property audioData  唤醒时刻的前 2 秒音频数据，可用于保存排查问题
 */
class WakeupEventInfo(
    @JvmField var word: String?,
    @JvmField var confidence: Float,
    @JvmField var sn: String?,
    @JvmField var time: Long,
    @JvmField var audioData: ShortArray?
) : Serializable {

    override fun toString(): String {
        val audio = if (audioData == null) "null" else "short[${audioData!!.size}]"
        return "WakeupEventInfo{word='$word', confidence=$confidence, sn='$sn', time=$time, audioData=$audio}"
    }
}
