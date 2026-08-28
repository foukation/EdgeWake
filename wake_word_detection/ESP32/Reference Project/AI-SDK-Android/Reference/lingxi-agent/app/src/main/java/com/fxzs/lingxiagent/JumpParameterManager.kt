package com.fxzs.lingxiagent

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.fxzs.lingxiagent.lingxi.main.utils.CustomToast
import com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker.EventConstants
import com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker.TrackerUtils
import com.fxzs.lingxiagent.lingxi.translate.SimultaneousTranslateActivity
import com.fxzs.lingxiagent.model.common.Constants
import com.fxzs.lingxiagent.network.ZNet.bean.getCatDetailListBean
import com.fxzs.lingxiagent.util.AppManager
import com.fxzs.lingxiagent.util.AppPermissionRequestManager
import com.fxzs.lingxiagent.util.WakeUpPermissionHelper
import com.fxzs.lingxiagent.util.ZUtil.Constant
import com.fxzs.lingxiagent.view.agent.AgentContainActivity
import com.fxzs.lingxiagent.view.auth.OneClickLoginActivity
import com.fxzs.lingxiagent.view.meeting.MeetingContainActivity
import com.fxzs.lingxiagent.view.ppt.PptTopicInputActivity
import com.fxzs.lingxiagent.view.user.UserAppSettingsActivity
import timber.log.Timber
import java.io.Serializable

object JumpParameterManager {

    // === 常量提取，避免魔法字符串 ===
    public const val PAGE_WAKEUP_SERVICE = "wakeupservice"
    private const val PAGE_HOME = "home"
    private const val PAGE_SETTING = "setting"
    private const val PAGE_WAKEUP_WINDOW = "wakeupwindow"
    private const val PAGE_GUI_AGENT = "guiagent"
    private const val PAGE_MEETING = "meeting"
    private const val PAGE_PPT = "ppt"
    private const val PAGE_TRANSLATE = "translate"
    private const val PAGE_DEEP = "deepresearch"

    // 保存目标页面标识
    private var targetPage: String? = null
    // 保存额外参数
    private var extraParams: MutableMap<String, String>? = null

    /**
     * 保存跳转参数
     */
    fun saveJumpParams(page: String?, params: Map<String, String>? = null) {
        targetPage = page
        extraParams = params?.toMutableMap() ?: mutableMapOf()
    }

    fun getTargetPage(): String? = targetPage

    fun getExtraParam(key: String): String? = extraParams?.get(key)

    /**
     * 清空参数
     * 建议：在跳转动作真正执行成功后再调用，或者由调用者决定何时调用。
     * 当前逻辑是在跳转代码内部调用，需确保跳转代码一定被执行。
     */
    fun clearParams() {
        targetPage = null
        extraParams?.clear()
    }

    /**
     * 统一处理页面关闭和动画
     */
    private fun finishCurrentActivity(context: Context) {
        if (context is Activity) {
            context.overridePendingTransition(0, 0)
            context.finish()
        }
    }

    /**
     * 处理唤醒服务跳转 (原 jumpWakeupService 逻辑整合)
     * 为了保持原有外部调用接口，保留此函数，但内部逻辑简化
     */
    fun jumpWakeupService(context: Context): Boolean {
        if (PAGE_WAKEUP_SERVICE == targetPage) {
            if(WakeUpPermissionHelper.checkAllPermissionsGranted(context) && WakeUpPermissionHelper.isWakeUpEnabled(context)){
                WakeUpPermissionHelper.toggleWakeUpService(context, true)
            }else{
                Timber.tag("jumpWakeupService").d("语音和浮窗和通知三者权限不足")
            }
            cleanupAndFinish(context)
            return true
        }
        return false
    }

    /**
     * 跳转到一键登录页面
     */
    fun jumpLoginPage(context: Context) {
        val intent = Intent(context, OneClickLoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            // 无论什么上下文，启动新任务栈都需要 NEW_TASK
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // 只有当上下文是 Activity 时，才清除整个任务栈，确保登录页成为唯一的根
            // 如果是 Application Context，CLEAR_TASK 可能会因为找不到对应的任务栈而表现不一致，
            // 但通常配合 NEW_TASK 使用是安全的，意在清空旧栈建立新栈。
            if (context is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }

            // 移除 FLAG_ACTIVITY_NEW_DOCUMENT，除非明确需要多实例文档模式，否则登录页不需要
        }

        try {
            context.startActivity(intent)
            finishCurrentActivity(context)
        } catch (e: Exception) {
            Timber.e(e, "跳转登录页失败")
            Toast.makeText(context, "登录页启动失败，请重试", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 跳转到目标页面 (核心逻辑优化)
     */
    fun jumpTargetPage(context: Context) {
        val currentPage = getTargetPage()
        Timber.tag("JumpMgr").d("准备跳转页面：$currentPage")

        var intent: Intent? = null

        try {
            when (currentPage) {
                PAGE_HOME -> intent = Intent(context, MainActivity::class.java)

                PAGE_SETTING -> intent = Intent(context, UserAppSettingsActivity::class.java)

                PAGE_WAKEUP_WINDOW -> {
                    if("powerkey".equals(extraParams?.get("source"))){
                        TrackerUtils.trackCommonEvent(EventConstants.WakeUpManagement.POWER_WAKE_UP)
                    }else if("keyboard".equals(extraParams?.get("source"))){
                        TrackerUtils.trackCommonEvent(EventConstants.WakeUpManagement.KEYBOARD_WAKE_UP)
                    } else if("widget".equals(extraParams?.get("source"))){
                        TrackerUtils.trackCommonEvent(EventConstants.WakeUpManagement.WIDGET_QUICK_ENTRY)
                    }


                    if (!AppPermissionRequestManager.hasOverlayPermission(context) || (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)) {
                        CustomToast.showToast(context, "请开启悬浮窗和麦克风权限，可以直接语音下达指令", Toast.LENGTH_LONG).show()
                        intent = Intent(context, MainActivity::class.java)
                    }else{
                        WakeUpPermissionHelper.startWakeUpServiceAndShowFloat(context)
                        // 这是一个动作而非页面跳转，执行动作后清理并退出
//                    cleanupAndFinish(context)
                        AppManager.finishAll()
                        return
                    }
                }

                PAGE_GUI_AGENT -> {
                    intent = buildAgentIntent(context, AgentContainActivity.TYPE_AGENT).apply {
                        val bean = createGuiAgentBean()
                        putExtra(Constant.INTENT_DATA2, bean as Serializable)
                        putExtra(Constant.INTENT_DATA_GUI_QUERY, getExtraParam("itemContent") ?: "")
                    }
                }

                PAGE_MEETING -> intent = Intent(context, MeetingContainActivity::class.java)

                PAGE_PPT -> intent = Intent(context, PptTopicInputActivity::class.java)

                PAGE_TRANSLATE -> intent = Intent(context, SimultaneousTranslateActivity::class.java)

                PAGE_DEEP -> {
                    intent = buildAgentIntent(context, AgentContainActivity.TYPE_AGENT).apply {
                        val bean = createDeepAgentBean()
                        putExtra(Constant.INTENT_DATA2, bean as (Serializable))
                        putExtra(Constant.INTENT_DATA_GUI_QUERY, getExtraParam("itemContent") ?: "")
                    }
                }

                else -> {
                    Timber.w("未知的目标页面标识：$currentPage，默认跳转首页")
                    intent = Intent(context, MainActivity::class.java)
                }
            }

            intent.let {
                // 统一标志位：新任务栈并清空旧栈
                // 注意：如果希望保留部分历史栈，可能需要调整标志位，但根据原代码逻辑是清空
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
                context.startActivity(it)
            }

        } catch (e: Exception) {
            Timber.e(e, "跳转目标页面 [$currentPage] 失败")
            Toast.makeText(context, "页面跳转失败", Toast.LENGTH_SHORT).show()
            // 即使失败也建议清理参数，防止死循环尝试跳转
        } finally {
            cleanupAndFinish(context)
        }
    }

     fun openScheme(schemeUrl: String,context: Context) {
        try {
            val uri = Uri.parse(schemeUrl)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.fxzs.lingxiagent")
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(intent)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
    /**
     * 统一清理参数并结束当前 Activity
     */
    private fun cleanupAndFinish(context: Context) {
        clearParams()
        finishCurrentActivity(context)
    }

    // === 辅助函数：构建 Agent Intent 基础部分 ===
    private fun buildAgentIntent(context: Context, type: Int): Intent {
        return Intent(context, AgentContainActivity::class.java).apply {
            putExtra(Constant.INTENT_TYPE, type)
        }
    }

    // === 辅助函数：构建 GUI Agent Bean ===
    private fun createGuiAgentBean(): Any { // 返回类型取决于 getCatDetailListBean() 的具体类型，这里用 Any 占位，实际应为具体类
        val bean = getCatDetailListBean()
        bean.modelName = Constants.AGENT_GUI
        bean.name = Constants.AGENT_GUI
        bean.modelId = 160
        bean.botId = "NexusPilot"
        bean.preInput = "你好呀！我是自动执行助手，一句指令让我操作各类应用，点咖啡、订机票、订酒店，都能轻松帮你搞定～！"
        bean.description = "你好呀！我是自动执行助手，一句指令让我操作各类应用，点咖啡、订机票、订酒店，都能轻松帮你搞定～"
        bean.recommendQuestions = "[\"帮我在美团定一杯瑞幸的生椰拿铁\",\"帮我在携程定一张北京到广州的飞机票，后天出发，中午12点以后的第一班\",\"帮我订一个广州保利世贸展览馆附近的酒店，大床房带早餐，下个月1号入住\"]"
        return bean
    }

    // === 辅助函数：构建 Deep Agent Bean ===
    private fun createDeepAgentBean(): Any {
        val bean = getCatDetailListBean()
        bean.modelName = Constants.AGENT_DEEP_RESEARCH
        bean.name = Constants.AGENT_DEEP_RESEARCH
        bean.modelId = 156
        bean.botId = "depthResearch"
        bean.preInput = "你好呀！我是您的智能研究助手，专注高效分析与精准洞察，无需复杂操作，一句话即可获得全面、可靠的研究支持。现在，请告诉我您想了解什么？我将为您呈现深度分析！"
        bean.description = "灵犀深度研究智能体，一句话生成深度报告，专业分析即刻拥有！无论是行业趋势、市场分析还是购物选品、游戏攻略，只需输入简单需求，深度研究智能体即可自动完成全网搜索、数据整合与报告撰写，为您提供全面、精准的研究成果。高效智能，助您决策无忧！"
        return bean
    }

    /**
     * 判断 MainActivity 是否在当前任务栈中
     */
    fun isMainActivityInStack(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val tasks = am.getRunningTasks(1)
        if (tasks != null && !tasks.isEmpty()) {
            val topActivity = tasks[0].topActivity
            if (topActivity != null && topActivity.className == MainActivity::class.java.name) {
                return true
            }
        }

        // 遍历任务栈，判断 Main 是否存在
        for (task in tasks!!) {
            if (task.baseActivity != null && task.baseActivity!!.className == MainActivity::class.java.name) {
                return true
            }
        }
        return false
    }
}