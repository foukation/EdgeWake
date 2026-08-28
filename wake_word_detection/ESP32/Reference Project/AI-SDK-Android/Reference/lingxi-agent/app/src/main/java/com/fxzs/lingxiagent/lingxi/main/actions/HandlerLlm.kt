package com.fxzs.lingxiagent.lingxi.main.actions

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.fxzs.lingxiagent.helper.FloatComponent
import com.fxzs.lingxiagent.helper.FloatType
import com.fxzs.lingxiagent.lingxi.accessibility_api.AccessibilityApi.Companion.isBaseServiceEnable
import com.fxzs.lingxiagent.lingxi.accessibility_api.utils.ScreenshotUtils
import com.fxzs.lingxiagent.lingxi.config.ChatFlowCallback
import com.fxzs.lingxiagent.lingxi.gui_agent.UiAgentManager
import com.fxzs.lingxiagent.lingxi.gui_agent.actions.HandlerLineTaskLlm
import com.fxzs.lingxiagent.lingxi.gui_agent.entity.AgentStatus
import com.fxzs.lingxiagent.lingxi.gui_agent.entity.TaskStatus
import com.fxzs.lingxiagent.lingxi.gui_agent.view.WaitingReplyDialogActivity
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.HonorDataType
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.HonorQueueManager
import com.fxzs.lingxiagent.lingxi.main.helper.FloatHelper
import com.fxzs.lingxiagent.lingxi.main.utils.BroadcastConstants
import com.fxzs.lingxiagent.lingxi.main.utils.BroadcastUtils
import com.fxzs.lingxiagent.lingxi.marquee.MarqueeManager
import com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker.TrackerUtils
import timber.log.Timber


object HandlerLlm {



    private var Tag: String = "HandlerLlm"
    private var title: String = ""
    private var popupWindowMessage: String = ""
    private var popupWindowType: Int = 1
    private var callback: ChatFlowCallback ?= null;
    private var isStop = false

    fun createActionListener(context: Activity): WaitingReplyDialogActivity.OnActionListener {
        return object : WaitingReplyDialogActivity.OnActionListener {
            @RequiresApi(Build.VERSION_CODES.R)
            override fun onCompleteClick() {
                Timber.tag(Tag).d("点击操作完成按钮")
                // 操作完成
                manualTakeOver(context)
                FloatComponent.show(FloatType.MENU_ACTION_FLOAT)
                FloatHelper.switchToDefaultIcon()
            }

            override fun onBackClick() {
                Timber.tag(Tag).d("点击返回应用按钮")
                MarqueeManager.stopMarquee(context)
                try {
                    val pm = context.packageManager
                    val launchIntent = pm.getLaunchIntentForPackage(context.packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                        )
                        context.startActivity(launchIntent)
                    }
                } catch (e: Exception) {
                    Timber.tag(Tag).e("bring to front failed: $e.message")
                }
            }

            override fun onStop(clickAble: Boolean) {
                if(!clickAble){
                    Timber.tag(Tag).d("不是用户点击，这里弹出按钮 clickAble = $clickAble")
                    FloatComponent.show(FloatType.MENU_ACTION_FLOAT)
                    FloatHelper.switchToLingxiIcon()
                }
            }

            override fun onStopClick() {
                Timber.tag(Tag).d("点击停止按钮")
                close()
                MarqueeManager.stopMarquee(context)
            }

            override fun onInputClick(inputText: String) {
                Timber.tag(Tag).i("用户输入: $inputText")
                UiAgentManager.setInput(inputText)
//                queueAddMsg(inputText)
                callback?.addGuiUserMsg(inputText)
                FloatHelper.switchToDefaultIcon()
                FloatComponent.show(FloatType.MENU_ACTION_FLOAT)

            }

            override fun onMicClick() {
                TODO("Not yet implemented")
            }

            override fun onCloseClick() {
                if (popupWindowType != WaitingReplyDialogActivity.TYPE_WITH_TASK_COMPLETE) {
                    FloatComponent.show(FloatType.MENU_ACTION_FLOAT)
                    FloatHelper.switchToLingxiIcon()
                } else {
                    MarqueeManager.stopMarquee(context)
                }

            }
        }
    }


    @SuppressLint("NewApi")
    fun start(context: Activity, query: String, callback: ChatFlowCallback, honorQueueManager: HonorQueueManager) {
        Timber.tag(Tag).i("HandlerLlm start")
        this.callback = callback;
        fun queueAddMsg(msg: String) {
            honorQueueManager.enqueueTypingTask(HonorDataType.RICH_TEXT, msg, 0)
        }


        //        FloatButtonPopupWindow.show(context, object : FloatButtonPopupWindow.OnButtonClickListener {
        //            override fun onButtonClick() {
        //                // 处理点击事件
        //                Timber.tag(Tag).d("按钮被点击了")
        //            }
        //        })
        //        title = "等待操作"
        //        popupWindowMessage = "内容内容内容内容内容内容内容内容内容内容内容内容内容内容"
        //        popupWindowType = WaitingReplyPopupWindow.TYPE_WITH_COMPLETE
        //
        //        WaitingReplyPopupWindow.with(context)
        //            .title(title)
        //            .type(popupWindowType)
        //            .systemWindow(true)
        //            .content(popupWindowMessage)
        //            .actionListener(createActionListener(context))
        //            .showOverlayAtBottom(); // 底部悬浮显示
        //        WaitingReplyDialogActivity.show(context,
        //            title,
        //            popupWindowMessage,
        //            popupWindowType
        //        );
        //
        //        WaitingReplyDialogActivity.with(context)
        //            .title("处理中")
        //            .content("正在执行操作，请稍等")
        //            .type(WaitingReplyDialogActivity.TYPE_WITH_COMPLETE)
        //            .actionListener(createActionListener(context))
        //            .show()
        //
        //        return
        if (!Settings.canDrawOverlays(context) || !isBaseServiceEnable) {
            callback.receiveGUIPermissionCard()
            return
        }
        ScreenshotUtils.clearLastScreenshot()
        // 清除所有保存的截图
        val deletedCount = ScreenshotUtils.clearDebugScreenshots()
        Timber.tag("Clean").i("已删除 $deletedCount 个文件")
        isStop = false
        Timber.tag(Tag).i("任务开始执行 : isStop : $isStop")
        //打开按钮
        openMenu(context)
        MarqueeManager.setFinish(0)
        MarqueeManager.startMarquee(context)
        AgentStatus.setStatus(AgentStatus.STATUS_RUNNING)
        queueAddMsg("任务开始执行")
        HandlerLineTaskLlm.breakFlag = false
        FloatHelper.switchToDefaultIcon()
        UiAgentManager.startExecutor(context, query) { code, message ->
            Timber.tag(Tag).i("code : $code  message : $message")
            when (code) {
                TaskStatus.SUCCESS.alias -> {
                    queueAddMsg("任务执行成功")

                    closeMenu()


                    // 设置为空闲状态
                    AgentStatus.setStatus(AgentStatus.STATUS_IDLE)
                    if(message.contains("系统错误过多")){
                        MarqueeManager.setFinish(2)
                        title = "任务执行异常"
                        popupWindowMessage = message
                        popupWindowType = WaitingReplyDialogActivity.TYPE_WITH_ERROR
                    } else {
                        MarqueeManager.setFinish(1)
                        title = "任务已完成"
                        popupWindowMessage = message
                        popupWindowType = WaitingReplyDialogActivity.TYPE_WITH_TASK_COMPLETE
                    }
                    this.callback?.addGuiAiMsg(message)
                    FloatComponent.hide(FloatType.MENU_ACTION_FLOAT)
                    WaitingReplyDialogActivity.with(context).title(title).type(popupWindowType)
                        .content(popupWindowMessage).actionListener(createActionListener(context))
                        .show()
                    // 任务完成事件上报
                    TrackerUtils.trackTaskCompleteEvent(UiAgentManager.sessionID)
                    //清理上一次的截图
                    ScreenshotUtils.clearLastScreenshot()
                }

                // 截图失败
                TaskStatus.SCREEN_SHOT_ERROR.alias -> {
                    // 设置为空闲状态
                    queueAddMsg("截图失败，已自动停止任务")
                    MarqueeManager.stopMarquee(context)
                    closeMenu()
                }

                TaskStatus.OPEN_APP.alias -> {
                }

                // 网络错误
                TaskStatus.NETWORK_ERROR.alias -> {
                    queueAddMsg("网络错误，请检查网络")
                    MarqueeManager.stopMarquee(context)
                    closeMenu()
                }

                // 多轮会话
                TaskStatus.MULTIPLE_CONVERSATION.alias -> {
                    if (isStop) {
                        return@startExecutor
                    }

                    title = "等待答复"
                    popupWindowMessage = message
                    this.callback?.addGuiAiMsg(message)
                    popupWindowType = WaitingReplyDialogActivity.TYPE_DEFAULT
                    FloatComponent.hide(FloatType.MENU_ACTION_FLOAT)
                    WaitingReplyDialogActivity.with(context).title(title).type(popupWindowType)
                        .content(popupWindowMessage).actionListener(createActionListener(context))
                        .show()
                }

                TaskStatus.MULTIPLE_OPERATIONS.alias -> {
                    queueAddMsg(message)
                }

                TaskStatus.OPEN_MENU.alias -> {
                    MarqueeManager.stopMarquee(context)
                    openMenu(context)
                }

                TaskStatus.CLOSE_MENU.alias -> {

                    MarqueeManager.stopMarquee(context)
                    closeMenu()
                }

                TaskStatus.EXCEPTION.alias -> {
                    queueAddMsg(message)
                    MarqueeManager.stopMarquee(context)
                    closeMenu()
                }

                TaskStatus.TASK_EXECUTE_FAIL.alias -> {
                    queueAddMsg(message)
                    MarqueeManager.stopMarquee(context)
                    closeMenu()
                }

                TaskStatus.TASK_INTERRUPTION.alias -> {
                    queueAddMsg(message)
                    MarqueeManager.stopMarquee(context)
                    closeMenu()
                }

                TaskStatus.MANUAL_TAKEOVER.alias -> {
                    Timber.tag(Tag).i("手动接管1 : isStop : $isStop 手动接管开始执行任务")
                    if (isStop) {
                        return@startExecutor
                    }
                    Timber.tag(Tag).i("手动接管2 : isStop : $isStop 手动接管开始执行任务")
                    title = "等待操作"
                    popupWindowMessage = message
                    this.callback?.addGuiAiMsg(message)
                    popupWindowType = WaitingReplyDialogActivity.TYPE_WITH_COMPLETE
                    FloatComponent.hide(FloatType.MENU_ACTION_FLOAT)
                    WaitingReplyDialogActivity.with(context).title(title).type(popupWindowType)
                        .content(popupWindowMessage).actionListener(createActionListener(context))
                        .show()

                }

                else -> {
                    Timber.Forest.tag(Tag).i("未知的CODE：${code}")
                }
            }
        }
    }

    private fun openMenu(context: Activity) {
        Timber.tag(Tag).i("openMenu 打开悬浮窗菜单")
        FloatHelper.openFloatMenu(
            close = { close() },
            lingxiClick = {
                FloatComponent.hide(FloatType.MENU_ACTION_FLOAT)
                Timber.tag(Tag).i("lingxiClick")
                WaitingReplyDialogActivity.with(context)
                    .title(title)
                    .type(popupWindowType)
                    .content(popupWindowMessage)
                    .actionListener(createActionListener(context))
                    .show() // 底部悬浮显示
            })

    }

    private fun closeMenu() {
        Timber.tag(Tag).i("closeMenu 关闭悬浮窗菜单")
        FloatHelper.closeFloatMenuNew()
    }

    fun clearCallback() {
        callback = null
    }

    fun close() {
        isStop = true
        Timber.tag(Tag).i("手动停止 : isStop : $isStop")
        Timber.tag(Tag).d("已发送任务结束广播 %s",BroadcastConstants.ACTION_TASK_COMPLETED)
        AgentStatus.setStatus(AgentStatus.STATUS_IDLE)
        Timber.tag(Tag).i("close()")
        UiAgentManager.breakTask()
        closeMenu()
        clearCallback()
        // 用户停止事件上报
        TrackerUtils.trackUserStopEvent(UiAgentManager.sessionID)
        //清理上一次的截图
        ScreenshotUtils.clearLastScreenshot()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun manualTakeOver(context: Activity){
        val type = "query_clarification"
        val text = "＊%用户操作完毕"
        Timber.tag(Tag).i("manualTakeOver 手动接管完成")

        // 延迟200ms执行
        android.os.Handler().postDelayed({
            UiAgentManager.sendMessage(context, text, type)
        }, 500)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun exit(context: Activity){
        val type = "query_clarification"
        val text = "退出"
        Timber.tag(Tag).i("exit 退出助手")
        UiAgentManager.sendMessage(context, text, type)
    }

}