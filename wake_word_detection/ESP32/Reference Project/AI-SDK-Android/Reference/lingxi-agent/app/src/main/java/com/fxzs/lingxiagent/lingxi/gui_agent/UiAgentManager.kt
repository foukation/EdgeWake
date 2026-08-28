package com.fxzs.lingxiagent.lingxi.gui_agent

import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import com.fxzs.lingxiagent.lingxi.gui_agent.actions.HandlerLineTaskLlm
import com.fxzs.lingxiagent.lingxi.gui_agent.actions.HandlerLineTaskLlm.getAndUploadScreenshot
import com.fxzs.lingxiagent.lingxi.gui_agent.actions.HandlerLineTaskLlm.isTablet
import com.fxzs.lingxiagent.lingxi.gui_agent.entity.TaskStatus
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.AIServiceManager.Companion.getInstance
import com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker.TrackerUtils
import com.fxzs.lingxiagent.model.honor.repository.BenefitCode
import com.fxzs.lingxiagent.util.BillDialogHelper
import com.fxzs.lingxiagent.util.SharedPreferencesUtil
import com.fxzs.lingxiagent.util.SignatureUtil
import com.lingxi.nexuspilot.NexusPilotManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

object UiAgentManager {

    private var multipleOperation: Boolean = false
    var sessionID: String = ""
    private var Tag : String = "UiAgentManager"
    private var query: String = ""
    private var targetQuery: String = ""
    private var AppInstalled: Boolean = false

    //鉴权参数
    private var authorization: String = ""
    private var sign: String = ""
    private var timestamp: String = ""
    private var client_id: String = ""
    private var device_id: String = ""
    private var device_no: String = ""
    private var product_id: String = ""

    fun startExecutor(context: Activity, resultString: String, callback: (Int,String) -> Unit){
        query = resultString
        targetQuery = resultString
        if (multipleOperation) {
            // 已经在执行任务
            callback(TaskStatus.MULTIPLE_OPERATIONS.alias,"请勿频繁操作")
            return
        }
//        val devicesID = SessionUtils.getDeviceId(context);
//        NexusPilotManager.init(context,devicesID)

        //每次任务执行将app安装状态改成false
        AppInstalled = false
        multipleOperation = true

        var aiAssistConfig = getInstance().getAiAssistConfig()
        timestamp = (System.currentTimeMillis()).toString()
        sign = SignatureUtil.setMd5Signature(aiAssistConfig.deviceSecret + timestamp)
        authorization = SharedPreferencesUtil.getToken()
        device_no = aiAssistConfig.deviceNo
        product_id = aiAssistConfig.productId
        device_id = aiAssistConfig.deviceId
        if (isTablet(context)) {
            client_id ="lingxi_android"
        } else {
            client_id = "lingxi_pad"
        }

        var sessionResult = NexusPilotManager.createSession(authorization, sign, timestamp, client_id, device_id, device_no, product_id)

        if (BenefitCode.isBenefitError(sessionResult?.request_id.toString())) {
            CoroutineScope(Dispatchers.Main).launch {
                val message: String = sessionResult?.message ?: ""
                BillDialogHelper.showBillDialog(context, message) {
                    context.finish()
                }
                callback(TaskStatus.EXCEPTION.alias, message)
                multipleOperation = false
            }
            return
        }

        Timber.tag(Tag).i(" result: $sessionResult")
        sessionID = sessionResult?.data?.session_id.toString()
        // 创建session，上报事件
        TrackerUtils.trackSessionCreateEvent(sessionID)
        try{
            CoroutineScope(Dispatchers.Main).launch {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    getAndUploadScreenshot(
                        context,
                        sessionID,
                        query,
                        targetQuery,
                        null,
                        null,
                    ) { message,clarify->

                        Timber.tag(Tag).i(message)
                        // 任务执行完成，回调结果
                        taskCompleted()
                        if (message?.equals("任务完成") == true) {
                            callback(TaskStatus.SUCCESS.alias,clarify)
                        } else if (message?.equals("任务已中断") == true) {
                            callback(TaskStatus.TASK_INTERRUPTION.alias,"任务已中断")
                        } else if (message?.equals("执行异常多次尝试均失败") == true) {
                            callback(TaskStatus.EXCEPTION.alias,"执行任务出错")
                            TrackerUtils.trackSessionAbortEvent(sessionID, TaskStatus.EXCEPTION.alias.toString(), clarify)
                        } else if (message?.equals("返回错误数据，任务执行失败") == true) {
                            callback(TaskStatus.EXCEPTION.alias,"返回错误数据，任务执行失败")
                        } else if (message?.equals("请求错误") == true) {
                            callback(TaskStatus.TASK_EXECUTE_FAIL.alias,"请求错误")
                        } else if (message?.equals("截图上传多次尝试均失败") == true) {
                            callback(TaskStatus.TASK_EXECUTE_FAIL.alias,"截图上传多次尝试均失败")
                            TrackerUtils.trackSessionAbortEvent(sessionID, TaskStatus.TASK_EXECUTE_FAIL.alias.toString(), clarify)
                        } else if (message?.equals("网络请求异常") == true) {
                            callback(TaskStatus.TASK_EXECUTE_FAIL.alias,"网络请求异常")
                            TrackerUtils.trackSessionAbortEvent(sessionID, TaskStatus.TASK_EXECUTE_FAIL.alias.toString(), clarify)
                        } else if (message?.equals("问题澄清") == true) {
                            callback(TaskStatus.MULTIPLE_CONVERSATION.alias,clarify)
                        }else if (message?.contains("当前app暂不支持操作") == true
                            || message?.contains("还未安装") == true) {
                            callback(TaskStatus.TASK_EXECUTE_FAIL.alias,message)
                        }else if (message?.equals("手动接管") == true) {
                            callback(TaskStatus.MANUAL_TAKEOVER.alias,clarify)
                        }else {
                            callback(TaskStatus.TASK_EXECUTE_FAIL.alias,"任务执行失败")
                        }
                    }
                }
            }
        }catch (e: Exception) {
            callback(TaskStatus.EXCEPTION.alias,"任务执行异常")
            multipleOperation = false
            Timber.tag(Tag).i("getAndUploadScreenshot：${e.message}")
        }
    }

    fun setInput(inputString: String){
        HandlerLineTaskLlm.setInputText(inputString)
    }

    fun breakTask() {
        multipleOperation = false
        Timber.tag(Tag).i("breakTask: 任务中断")
        HandlerLineTaskLlm.breakTask()
    }

    fun taskCompleted(){
        multipleOperation = false
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun sendMessage(context: Activity,content: String,type:String) {
        HandlerLineTaskLlm.sendMessage(context,content,type)
    }

    fun closeMenu(){
        HandlerLineTaskLlm.closeMenu()
    }


}