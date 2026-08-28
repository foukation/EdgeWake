package com.fxzs.lingxiagent.lingxi.gui_agent.execute

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.fxzs.lingxiagent.lingxi.accessibility_api.utils.ScreenshotUtils
import com.fxzs.lingxiagent.lingxi.core.AutoApi
import com.fxzs.lingxiagent.lingxi.core.api.click
import com.fxzs.lingxiagent.lingxi.core.api.containsText
import com.fxzs.lingxiagent.lingxi.core.api.editored
import com.fxzs.lingxiagent.lingxi.core.api.longClick
import com.fxzs.lingxiagent.lingxi.core.api.swipe
import com.fxzs.lingxiagent.lingxi.core.api.swipeDirection
import com.fxzs.lingxiagent.lingxi.gui_agent.entity.ActionType
import com.fxzs.lingxiagent.lingxi.gui_agent.entity.ExecuteStatus
import com.fxzs.lingxiagent.lingxi.gui_agent.entity.OperatorType
import com.fxzs.lingxiagent.lingxi.gui_agent.entity.ScrollDirection
import com.fxzs.lingxiagent.lingxi.gui_agent.util.ClipboardUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import timber.log.Timber


object TaskExecutor {

    private var tag : String = "TaskExecutor"
    private var breakFlag = false
    private var query = ""
    private var appInstalled: Boolean = false
    private var message = ""


    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun execute(context: Activity, action: com.lingxi.nexuspilot.data.PredictionAction, callback: (Int, String, String, Boolean, Boolean) -> Unit) {
        val app = context.applicationContext as? Application
            ?: throw IllegalStateException("Expected application context")

        val screenSize = ScreenshotUtils.getScreenSizeWithNavigationBar(context)
        Timber.Forest.tag(tag).i("execute action %s", action.toString())
        when (action.action_type) {
            ActionType.PRESS.alias -> {
                when (action.target) {
                    OperatorType.HOME.keyEvent -> AutoApi.Companion.home()
                    OperatorType.BACK.keyEvent -> AutoApi.Companion.back()
                    OperatorType.MENU.keyEvent -> {

                    }
                    OperatorType.ENTER.keyEvent -> {

                    }
                    OperatorType.APPSELECT.keyEvent -> {

                    }
                    OperatorType.CLEAR.keyEvent -> {

                    }
                    OperatorType.POWER.keyEvent -> {

                    }
                    OperatorType.VOLUME_UP.keyEvent -> {

                    }
                    OperatorType.VOLUME_DOWN.keyEvent -> {

                    }
                    OperatorType.VOLUME_MUTE.keyEvent -> {

                    }
                    else -> Timber.Forest.tag(tag).i("未知的操作类型PRESS：${action.target}")
                }
            }
            ActionType.CLICK.alias -> {
                // 使用截图的实际尺寸来计算坐标
                val screenshotWidth = ScreenshotUtils.screenshotSize.x
                val screenshotHeight = ScreenshotUtils.screenshotSize.y

                val x = (action.from[0] * screenshotWidth).toInt()
                val y = (action.from[1] * screenshotHeight).toInt()

                Timber.Forest.tag(tag).i("点击坐标：$x, $y, 截图尺寸：${screenshotWidth}x${screenshotHeight}")
                val isclick = click(x, y)
                Timber.Forest.tag(tag).i("点击坐标 返回结果：$isclick")
            }
            ActionType.INPUT.alias -> {
                runBlocking {
                    action.text?.let {
                        Timber.Forest.tag(tag).i("输入文本：$it")
                        try {
                            editored().apply {
                                text = it
                            }
                            // 尝试查找并点击搜索按钮
                            findAndClickSearchButton("搜索")
                        } catch (e: Exception) {
                            Timber.Forest.tag(tag).i("输入文本异常：$e")
                            //设置文本到粘贴板，这里无法获取到输入控件，走大模型模拟点击方案
                            ClipboardUtils.copyText(context, it)
                            val positionString = it.firstOrNull() ?: ""
                            query = "帮我点击一下词语$it 的“ $positionString ”"
                            callback(ExecuteStatus.INPUT.alias, query, "", false, false)
                            query = ""
                        }

                    }
                }
            }
            ActionType.LONG_PRESS.alias -> {
                // 使用截图的实际尺寸来计算坐标
                val screenshotWidth = ScreenshotUtils.screenshotSize.x
                val screenshotHeight = ScreenshotUtils.screenshotSize.y

                val x = (action.from[0] * screenshotWidth).toInt()
                val y = (action.from[1] * screenshotHeight).toInt()

                Timber.Forest.tag(tag).i("长按坐标：$x, $y, 截图尺寸：${screenshotWidth}x${screenshotHeight}")
                longClick(x, y)
            }
            ActionType.OPEN_APP.alias -> {
                if(findApp(action,app)){
//                    todo这里需要增加一个deeplink方式打开app


//                    val url = curTaskItem.deeplinkUrl
//                    val args = curTaskItem.deeplinkUrlArgs
//
//                    val defaultValue = curTaskItem.slotDefaultValue
//                    val slotsList = curTaskItem.slotKeyList.split(',')
//                    val value = slotsList.find { slot -> slots[slot] != null }
//                    val resultText = value?.let { slots[it] } ?: defaultValue
//
//                    val fullUrl = "${url}?${args}${resultText}"
//
//                    runBlocking {
//                        try {
//                            val intent = Intent(Intent.ACTION_VIEW)
//                            intent.setData(Uri.parse(fullUrl))
//                            context.startActivity(intent)
//                        } catch (e: Exception){
//                            println(e)
//                        }
//                    }
                    
                    // 安全解析 package_name，支持两种格式：
                    // 1. "com.example.app/com.example.app.MainActivity" (包名/类名)
                    // 2. "com.example.app" (仅包名)
                    val packageParts = action.package_name.split("/")
                    val packageName = packageParts[0]
                    val activityClassName = if (packageParts.size > 1) packageParts[1] else ""
                    
                    Timber.Forest.tag(tag).i("打开应用 - 包名: $packageName, Activity: $activityClassName")
                    appInstalled = true
                    openThirdPartyApp(context, packageName, activityClassName)
                    //这里是为了截图更准确，延迟500ms执行截图操作，避免截图时操作还未完成
                    //原始值500，这里改成7000，做个临时调整
//                    if("com.tencent.qqlive".equals(packageName, ignoreCase = true)) {
//                        Timber.Forest.tag(tag).i("打开 腾讯视频 appName ：$packageName")
//                        delay(7000)
//                    } else {
//                        delay(500)
//                    }
                    delay(500)

                }else{
                    message = if (action.app_name == "App之外"){
                        "当前app暂不支持操作"
                    }else{
                        "要执行的程序（${action.app_name}）还未安装，请到应用商店下载安装！"
                    }
                    //打断执行
                    breakFlag = true
                    //app安装状态
                    appInstalled = false
                    callback(ExecuteStatus.OPEN_APP_FAIL.alias,query,message,appInstalled,breakFlag)
                    message = ""
                }
            }
            ActionType.SCROLL.alias -> {
                // 使用截图的实际尺寸来计算坐标
                val screenshotWidth = ScreenshotUtils.screenshotSize.x
                val screenshotHeight = ScreenshotUtils.screenshotSize.y

                val x = (action.from[0] * screenshotWidth).toInt()
                val y = (action.from[1] * screenshotHeight).toInt()

                Timber.Forest.tag(tag).i("滚动坐标：$x, $y, 截图尺寸：${screenshotWidth}x${screenshotHeight}")
                when (action.direction) {
                    ScrollDirection.UP.alias -> {
                        Timber.Forest.tag(tag).i("SCROLL：${action.direction}")
                        swipeDirection(x, y, -1, 800, 400)
                    }
                    ScrollDirection.DOWN.alias -> {
                        Timber.Forest.tag(tag).i("SCROLL：${action.direction}")
                        swipeDirection(x, y, 1, 800, 400)
                    }
                    ScrollDirection.LEFT.alias -> {
                        Timber.Forest.tag(tag).i("SCROLL：${action.direction}")
                        swipeDirection(x, y, -2, 800, 400)
                    }
                    ScrollDirection.RIGHT.alias -> {
                        Timber.Forest.tag(tag).i("SCROLL：${action.direction}")
                        swipeDirection(x, y, 2, 800, 400)
                    }
                    else -> {
                        Timber.Forest.tag(tag).i("未知的操作类型SCROLL：${action.target}")
                    }

                }

            }
            ActionType.DRAG.alias -> {
                // 使用截图的实际尺寸来计算坐标
                val screenshotWidth = ScreenshotUtils.screenshotSize.x
                val screenshotHeight = ScreenshotUtils.screenshotSize.y

                val x1 = (action.from[0] * screenshotWidth).toInt()
                val y1 = (action.from[1] * screenshotHeight).toInt()
                val x2 = (action.to[0] * screenshotWidth).toInt()
                val y2 = (action.to[1] * screenshotHeight).toInt()

                Timber.Forest.tag(tag).i("拖拽坐标：起点=($x1,$y1), 终点=($x2,$y2), 截图尺寸：${screenshotWidth}x${screenshotHeight}")
                swipe(x1, y1, x2, y2, 400)
            }
            // ... 其他 action_type 分支（省略以实现简洁）
            else -> Timber.Forest.tag(tag).i("未知的操作类型action_type：${action.action_type}")
        }
    }

    fun findAndClickSearchButton(buttonText: String) {
        try {
            // 尝试查找搜索按钮并点击
            containsText(buttonText).let {
                Timber.Forest.tag(tag).i("找到搜索按钮并点击")
                it.click()
            }
        }catch (e: Exception) {
            Timber.Forest.tag(tag).i("找不到搜索按钮 ：$e")
        }


    }

    private fun findApp(action: com.lingxi.nexuspilot.data.PredictionAction, app: Application): Boolean {
        /**
         * 验证关联app是否安装
         */
        var curPackageName: String? = null // 应用包名，初始化为null
        if(action.package_name.isNullOrEmpty().not()){
//            curAppName = action.app_name
            // 安全解析包名，支持 "com.example.app" 或 "com.example.app/com.example.MainActivity" 两种格式
            curPackageName = action.package_name.split("/")[0].toString()
        }
        val packageManager: PackageManager = app.packageManager
        if(curPackageName.isNullOrEmpty()){
            return false
        }
        return try {
            packageManager.getPackageInfo(curPackageName, 0)
            true
        } catch (e : PackageManager.NameNotFoundException) {
            false
        }
    }

    fun openThirdPartyApp(context: Context, packageName: String, activityClassName: String) {
        try{

            val packageManager: PackageManager = context.packageManager
            val intent: Intent? = packageManager.getLaunchIntentForPackage(packageName)

            if (intent != null) {
                // 添加 FLAG_ACTIVITY_NEW_TASK 确保在新任务中启动
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(intent)
                Timber.tag(tag).i("成功启动应用: $packageName")
            } else {
                Timber.tag(tag).i("未找到包名为 $packageName 的启动Activity，或者应用未安装。")
            }

        }catch (e:Exception){
            Timber.Forest.tag("HandlerLineTaskLlm").i("启动三方app异常：$e")
        }

    }



}