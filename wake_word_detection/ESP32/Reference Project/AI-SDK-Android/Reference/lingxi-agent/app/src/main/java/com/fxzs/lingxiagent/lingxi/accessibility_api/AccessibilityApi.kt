@file:Suppress("unused")

package com.fxzs.lingxiagent.lingxi.accessibility_api

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.provider.Settings
import android.util.SparseArray
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.annotation.RequiresApi
import com.fxzs.lingxiagent.lingxi.core.AppScope
import com.fxzs.lingxiagent.lingxi.core.AutoApi
import com.fxzs.lingxiagent.lingxi.core.OnPageUpdate
import com.fxzs.lingxiagent.lingxi.core.PageUpdateMonitor
import com.fxzs.lingxiagent.lingxi.core.utils.AutoGestureDescription
import com.fxzs.lingxiagent.lingxi.core.utils.convert
import com.fxzs.lingxiagent.lingxi.core.utils.jumpAccessibilityServiceSettings
import com.fxzs.lingxiagent.lingxi.core.utils.whileWaitTime
import com.fxzs.lingxiagent.service.BaseAccessibilityService
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.min
import com.fxzs.lingxiagent.lingxi.core.utils.GestureResultCallback as GestureCallback

/**
 *
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class AccessibilityApi : AccessibilityService(), AutoApi {

    abstract val enableListenPageUpdate: Boolean

    override fun performAction(action: Int) = this.performGlobalAction(action)
    override fun rootInActiveWindow() = rootInActiveWindow

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun windows(): List<AccessibilityWindowInfo>? = windows

    private val pageListener: OnPageUpdate = ::onPageUpdate

    override fun onServiceConnected() {
        if (!isEnableBaseService()){
            BASE_SERVICE_CLS = BaseAccessibilityService::class.java
        }
        if (!isEnableGestureService()){
            GESTURE_SERVICE_CLS = BaseAccessibilityService::class.java
        }

        if (isEnableBaseService() &&this::class.java == BASE_SERVICE_CLS) {
            baseService = this
        }
        if (isEnableGestureService() && this::class.java == GESTURE_SERVICE_CLS) {
            gestureService = this
        }
        registerImpl()
        PageUpdateMonitor.enableListenPageUpdate = enableListenPageUpdate
        PageUpdateMonitor.addOnPageUpdateListener(pageListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        AutoApi.clearImpl()
        PageUpdateMonitor.removeOnPageUpdateListener(pageListener)
        if (this::class.java == BASE_SERVICE_CLS) {
            baseService = null
        }
        if (isEnableGestureService() && this::class.java == GESTURE_SERVICE_CLS) {
            gestureService = null
        }
    }

    override fun getRootInActiveWindow(): AccessibilityNodeInfo? {
        return try {
            super.getRootInActiveWindow()
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Activity or Dialog update
     * @param currentScope AppScope
     */
    open fun onPageUpdate(currentScope: AppScope) {}

    /**
     * @param event AccessibilityEvent?
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!enableListenPageUpdate || event == null) return
        PageUpdateMonitor.onAccessibilityEvent(event)
    }

    override fun takeScreenshot(): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return runBlocking {
                suspendCoroutine<Bitmap> { cont ->
                    super.takeScreenshot(0, appCtx.mainExecutor, object : TakeScreenshotCallback {
                        override fun onSuccess(screenshot: ScreenshotResult) {
                            val bitmap = Bitmap.wrapHardwareBuffer(screenshot.hardwareBuffer, screenshot.colorSpace)
                            if (bitmap != null) {
                                cont.resume(bitmap)
                            }
                        }

                        override fun onFailure(errorCode: Int) {
                            cont.resumeWithException(RuntimeException("takeScreenshot failed, code: $errorCode"))
                        }
                    })
                }
            }
        }
        return null
    }

    override fun onInterrupt() {
    }

    companion object {
        lateinit var BASE_SERVICE_CLS: Class<*>
        lateinit var GESTURE_SERVICE_CLS: Class<*>

        @SuppressLint("StaticFieldLeak")
        private var appCtx_: Context? = null
        val appCtx
            get() = appCtx_ ?: throw NullPointerException(
                "please call AccessibilityApi.init(...) in Application.onCreate()")

        fun init(
            ctx: Context,
            baseServiceCls: Class<*>,
            gestureServiceCls: Class<*> = baseServiceCls
        ) {
            appCtx_ = ctx.applicationContext
            BASE_SERVICE_CLS = baseServiceCls
            GESTURE_SERVICE_CLS = gestureServiceCls
        }

        private fun isEnableGestureService() = Companion::GESTURE_SERVICE_CLS.isInitialized
        private fun isEnableBaseService() = Companion::BASE_SERVICE_CLS.isInitialized

        // 无障碍基础服务
        @SuppressLint("StaticFieldLeak")
        var baseService: AccessibilityApi? = null

        val requireBase: AccessibilityApi
            get() = run {
                requireBaseAccessibility(false)
                baseService!!
            }

        // 无障碍高级服务 执行手势等操作
        /**
         * GestureService base on AccessibilityApi
         */
        
        @SuppressLint("StaticFieldLeak")
        var gestureService: AccessibilityService? = null

        val requireGesture: AccessibilityService
            get() = run {
                requireGestureAccessibility(false)
                gestureService!!
            }

        // currentAppScope
        val currentScope get() = AutoApi.currentScope

        // Service is enable
        val isBaseServiceEnable: Boolean
            get() = (baseService != null)

        val isServiceEnable: Boolean
            get() = isBaseServiceEnable

        val isGestureServiceEnable: Boolean get() = gestureService != null

        var isWaitingPermission: Boolean = false
        /**
         *  单个校验：悬浮窗权限是否开启（封装系统API，全局复用）
         */
        fun isFloatPermissionEnable(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context.applicationContext)
            } else {
                true // Android6.0以下默认拥有悬浮窗权限，无需校验
            }
        }

        /**
         *  核心组合校验：悬浮窗 + 基础无障碍 双权限是否全部开启（业务层最常用）
         * 等价于：Settings.canDrawOverlays(context) && isBaseServiceEnable()
         */
        fun isAllRequiredPermissionEnable(context: Context): Boolean {
            return isFloatPermissionEnable(context) && isBaseServiceEnable
        }
        /**
         * 等待无障碍开启，最长等待30s
         * @param waitMillis Long
         * @return Boolean true 开启成功 ; false 超时
         * @throws NeedAccessibilityException
         */
        @JvmOverloads
        @JvmStatic
        @Throws(NeedAccessibilityException::class)
        suspend fun waitAccessibility(waitMillis: Long = 30000, cls: Class<*>): Boolean {

            val se = if (cls == BASE_SERVICE_CLS) isBaseServiceEnable
            else isGestureServiceEnable

            if (se) return true
            else jumpAccessibilityServiceSettings(cls)

            return whileWaitTime(min(30000, waitMillis), 500) {
                if (isBaseServiceEnable) true
                else null
            } ?: throw NeedAccessibilityException(cls.name)
        }

        // 声明 需要基础无障碍权限
        fun requireBaseAccessibility(autoJump: Boolean = false) {
            if (!isBaseServiceEnable) {
                if (autoJump) jumpAccessibilityServiceSettings(BASE_SERVICE_CLS)
                // throw NeedAccessibilityException(BASE_SERVICE_CLS.name)
            }
        }

        // 声明 需要手势无障碍权限
        fun requireGestureAccessibility(autoJump: Boolean = false) {
            if (!isGestureServiceEnable) {
                if (autoJump) jumpAccessibilityServiceSettings(GESTURE_SERVICE_CLS)
                throw NeedAccessibilityException(GESTURE_SERVICE_CLS.name)
            }
        }

    }

    override suspend fun doGesturesAsync(gesture: AutoGestureDescription, callback: GestureCallback?, handler: Handler?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            throw IllegalStateException("dispatchGesture require android N+")
        }
        requireGesture.dispatchGesture(gesture.convert(), callback?.let { cb ->
            @RequiresApi(Build.VERSION_CODES.N)
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    cb.onCompleted(gesture)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    cb.onCancelled(gesture)
                }
            }
        }, handler)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun windowsOnAllDisplays(): SparseArray<List<AccessibilityWindowInfo>> {
        return requireBase.windowsOnAllDisplays
    }
}


fun requireBaseAccessibility(autoJump: Boolean = false) {
    try {
        AccessibilityApi.requireBaseAccessibility(autoJump)
    } catch (e: NeedAccessibilityException) {
        // 在这里处理异常
    }
}

suspend fun waitBaseAccessibility(waitMillis: Long = 30000) {
    AccessibilityApi.waitAccessibility(waitMillis, AccessibilityApi.BASE_SERVICE_CLS)
}

fun requireGestureAccessibility(autoJump: Boolean = false) {
    AccessibilityApi.requireGestureAccessibility(autoJump)
}

suspend fun waitGestureAccessibility(waitMillis: Long = 30000) {
    AccessibilityApi.waitAccessibility(waitMillis, AccessibilityApi.GESTURE_SERVICE_CLS)
}

suspend fun waitAccessibility(waitMillis: Long = 30000, cls: Class<*>): Boolean {
    return AccessibilityApi.waitAccessibility(waitMillis, cls)
}


/**
 * 无障碍服务未运行异常
 * @constructor
 */
open class NeedAccessibilityException(name: String?) : RuntimeException("无障碍服务未运行: $name")

class NeedBaseAccessibilityException :
    NeedAccessibilityException(AccessibilityApi.BASE_SERVICE_CLS.name)

class NeedGestureAccessibilityException :
    NeedAccessibilityException(AccessibilityApi.GESTURE_SERVICE_CLS.name)

