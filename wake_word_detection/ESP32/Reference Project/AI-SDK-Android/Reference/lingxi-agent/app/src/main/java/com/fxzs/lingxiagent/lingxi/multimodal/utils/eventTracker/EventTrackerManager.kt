package com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker

import android.content.Context
import com.cmdc.ai.assist.eventTracking.AIEventTracker
import com.cmdc.ai.assist.eventTracking.BuildConfig
import com.cmdc.ai.assist.eventTracking.EventTrackerConfig
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 *
 * 参数名	必选	类型	说明
 * im	是	string	用户标识(imei,sn)
 * av	是	string	应用版本号
 * un	否	string	上传网络(5G,4G,WIFI)
 * ut	是	string	上传时间(yyyy-MM-dd hh:mm:dd)
 * sv	是	string	sdk版本
 * mac	否	string	mac地址
 * os	是	string	操作系统（android,ios）
 * et	是	string	行为时间
 * ei	是	string	事件ID
 * el	是	string	事件名称（event lable）
 * co	是	string	厂商名称
 * mo	是	string	设备型号
 * ch	否	string	渠道
 * em	是	string	其他事件参数（json格式）
 *
 *
 * 事件跟踪管理器
 *
 * 负责管理应用的事件跟踪功能，包括初始化跟踪器、启动和关闭应用事件的记录和上传等。
 * 采用单例模式设计，确保在整个应用生命周期中只有一个实例。
 *
 * 主要功能包括：
 * 1. 初始化事件跟踪服务
 * 2. 记录和上传应用启动事件
 * 3. 记录和上传应用关闭事件
 */
class EventTrackerManager private constructor() {

    companion object {
        @Volatile
        var INSTANCE: EventTrackerManager? = null
        private val TAG = EventTrackerManager::class.simpleName.toString()

        /**
         * 获取 EventTrackerManager 的单例实例
         *
         * 使用双重检查锁定模式实现线程安全的单例模式。
         * 首先检查实例是否已经创建，如果未创建则进入同步块再次检查并创建实例。
         *
         * @return EventTrackerManager的单例实例
         */
        private fun getInstance(): EventTrackerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EventTrackerManager().also { INSTANCE = it }
            }
        }

        /**
         * 初始化 EventTrackerManager
         *
         * 通过获取单例实例并调用其内部初始化方法来完成EventTrackerManager的初始化。
         *
         * @param context 应用上下文，用于初始化相关服务
         */
        fun initialize(context: Context) {
            getInstance().init(context)
        }
    }

    // 应用上下文
    private lateinit var applicationContext: Context

    // 初始化状态
    private var isInitialized = false

    // 各模块事件跟踪实例
    private var mTrackingSystemBehavior: TrackingSystemBehavior? = null
    private var mTrackingAIOffice: TrackingAIOffice? = null
    private var mTrackingAIAgent: TrackingAIAgent? = null
    private var mTrackingCoreDialog: TrackingCoreDialog? = null
    private var mTrackingAIWriting: TrackingAIWriting? = null
    private var mTrackingAITranslation: TrackingAITranslation? = null
    private var mTrackingUserLogin: TrackingUserLogin? = null
    private var mTrackingUserProfile: TrackingUserProfile? = null
    private var mWakeUp: TrackingWakeUp? = null
    private var mTrackingSession: TrackingSession? = null
    /** 事件ID到对应跟踪模块的映射表，用于高效路由事件到正确的处理模块 */
    private val eventToModuleMap = ConcurrentHashMap<String, TrackingBase>(256)

    /**
     * 内部初始化方法
     */
    private fun init(context: Context) {
        if (isInitialized) {
            return
        }

        this.applicationContext = context.applicationContext
        this.isInitialized = true

        initializeServices()
    }

    private fun initializeServices() {

        val config = EventTrackerConfig(
            maxEventsPerBatch = 20,
            processIntervalMillis = 10000,
            autoUpload = true,
            debugMode = BuildConfig.DEBUG
        )
        val tracker = AIEventTracker.getInstance(applicationContext)
        tracker.init(config)

        // 初始化所有模块
        val systemBehavior = TrackingSystemBehavior(applicationContext, tracker)
        val aiOffice = TrackingAIOffice(applicationContext, tracker)
        val aiAgent = TrackingAIAgent(applicationContext, tracker)
        val coreDialog = TrackingCoreDialog(applicationContext, tracker)
        val aiWriting = TrackingAIWriting(applicationContext, tracker)
        val aiTranslation = TrackingAITranslation(applicationContext, tracker)
        val userLogin = TrackingUserLogin(applicationContext, tracker)
        val userProfile = TrackingUserProfile(applicationContext, tracker)
        val wakeUp = TrackingWakeUp(applicationContext, tracker)
        val session = TrackingSession(applicationContext, tracker)

        // 赋值给成员变量
        mTrackingSystemBehavior = systemBehavior
        mTrackingAIOffice = aiOffice
        mTrackingAIAgent = aiAgent
        mTrackingCoreDialog = coreDialog
        mTrackingAIWriting = aiWriting
        mTrackingAITranslation = aiTranslation
        mTrackingUserLogin = userLogin
        mTrackingUserProfile = userProfile
        mWakeUp = wakeUp
        mTrackingSession = session

        // 一次性构建所有映射 - 预计算优化
        buildEventMappings(
            systemBehavior, aiOffice, aiAgent, coreDialog,
            aiWriting, aiTranslation, userLogin, userProfile,wakeUp,session
        )

    }

    /**
     * 高性能映射表构建 - 一次性预计算所有映射关系
     */
    private fun buildEventMappings(vararg modules: TrackingBase) {
        // 预估容量避免 HashMap 扩容开销
        val totalCapacity = modules.sumOf { it.getEventCount() }
        val optimizedMap = HashMap<String, TrackingBase>(totalCapacity * 4 / 3 + 1)

        // 批量构建映射
        modules.forEach { module ->
            module.getAllEventIds().forEach { eventId ->
                optimizedMap[eventId] = module
            }
        }

        // 一次性放入并发容器
        eventToModuleMap.putAll(optimizedMap)

    }

    /**
     * 启动应用程序事件跟踪
     * 获取应用版本号和包名，构建启动事件数据并上传
     * @author 于海生
     * @since 2025-09-26
     */
    fun startApp() {
        mTrackingSystemBehavior?.startApp()
    }

    /**
     * 跟踪指定事件
     * @param eventId 事件唯一标识符
     * @param extraJson 事件的额外参数，JSON格式
     */
    fun trackEvent(eventId: String, extraJson: JSONObject) {
        eventToModuleMap[eventId]?.trackEvent(eventId, extraJson)
    }

}