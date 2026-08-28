package com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.cmdc.ai.assist.eventTracking.AIEventTracker
import com.cmdc.ai.assist.eventTracking.BuildConfig
import com.cmdc.ai.assist.eventTracking.EventData
import com.fxzs.lingxiagent.lingxi.multimodal.utils.DeviceInfoUtil
import org.json.JSONObject

/**
 * 事件跟踪基础抽象类
 *
 * 提供所有跟踪模块的通用功能和模板方法，包括：
 * 1. 事件映射管理
 * 2. 通用事件数据创建
 * 3. 应用信息获取
 * 4. 网络环境检测集成
 *
 * 子类需要实现：
 * - eventMap: 定义该模块支持的事件ID到事件名称的映射
 * - trackEvent: 具体的事件跟踪逻辑
 *
 * @param applicationContext 应用程序上下文，用于获取设备和应用信息
 * @param tracker AI事件跟踪器实例，用于数据上传
 */
abstract class TrackingBase(
    protected val applicationContext: Context,
    protected val tracker: AIEventTracker
) {

    /** 事件ID到事件名称的映射表，由子类实现 */
    protected abstract val eventMap: Map<String, String>

    /** 未知事件的默认标识 */
    protected val unknownEvent = "unknown_event"

    /** 当前模块支持的所有事件ID集合，延迟初始化以提高性能 */
    private val _eventIds: Set<String> by lazy { eventMap.keys }

    /**
     * 获取当前模块支持的所有事件ID
     * @return 事件ID集合
     */
    fun getAllEventIds(): Set<String> = _eventIds

    /**
     * 获取所有事件的数量
     * @return 事件总数
     */
    fun getEventCount(): Int = _eventIds.size

    /**
     * 根据事件ID获取事件名称
     * @param eventId 事件唯一标识符
     * @return 对应的事件名称，如果未找到返回unknownEvent
     */
    fun getEventName(eventId: String): String = eventMap[eventId] ?: unknownEvent

    /**
     * 检查指定事件ID是否被当前模块支持
     * @param eventId 待检查的事件ID
     * @return true表示支持该事件，false表示不支持
     */
    fun containsEventId(eventId: String): Boolean = eventMap.containsKey(eventId)

    /**
     * 获取应用版本号
     * @return 应用版本名称，获取失败时返回空字符串
     */
    protected fun getAppVersion(): String {
        return try {
            val packageInfo: PackageInfo = applicationContext.packageManager
                .getPackageInfo(applicationContext.packageName, 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * 创建公共的EventData参数
     * @param eventId 事件唯一标识符
     * @param eventLabel 事件标签/名称
     * @param extraJson 额外的事件参数，JSON格式
     * @return 构建完成的EventData对象
     */
    protected fun createCommonEventData(
        eventId: String,
        eventLabel: String,
        extraJson: JSONObject
    ): EventData {
        val versionName = getAppVersion()

        return EventData(
            im = DeviceInfoUtil.getDeviceNo(applicationContext),
            av = versionName,
            un = NetworkUtils.getNetworkTypeString(applicationContext),
            sv = BuildConfig.SDK_VERSION,
            mac = "",
            os = "android",
            ei = eventId,
            el = eventLabel,
            co = Build.MANUFACTURER,
            mo = Build.MODEL,
            ch = "",
            em = extraJson.toString()
        )
    }

    /**
     * 跟踪指定事件的抽象方法
     * 子类必须实现此方法来定义具体的事件跟踪逻辑
     * @param eventId 事件唯一标识符
     * @param extraJson 事件额外参数，JSON格式
     */
    abstract fun trackEvent(eventId: String, extraJson: JSONObject)

}