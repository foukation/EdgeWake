package com.fxzs.lingxiagent.lingxi.gui_agent.actions

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import com.fxzs.lingxiagent.R
import com.fxzs.lingxiagent.lingxi.accessibility_api.utils.ScreenshotUtils
import com.fxzs.lingxiagent.lingxi.common.dialog.CustomPopupWindow
import com.fxzs.lingxiagent.lingxi.gui_agent.entity.ActionType
import com.fxzs.lingxiagent.lingxi.gui_agent.entity.ClarificationType
import com.fxzs.lingxiagent.lingxi.gui_agent.entity.ExecuteStatus
import com.fxzs.lingxiagent.lingxi.gui_agent.execute.TaskExecutor
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.AIServiceManager.Companion.getInstance
import com.fxzs.lingxiagent.lingxi.main.utils.BroadcastConstants
import com.fxzs.lingxiagent.lingxi.main.utils.BroadcastUtils
import com.fxzs.lingxiagent.util.SharedPreferencesUtil
import com.fxzs.lingxiagent.util.SignatureUtil
import com.lingxi.nexuspilot.NexusPilotManager
import com.lingxi.nexuspilot.data.Data
import com.lingxi.nexuspilot.data.PredictionAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@SuppressLint("StaticFieldLeak")
object HandlerLineTaskLlm {

    /**
     * 判断当前设备是否为平板
     */
    fun isTablet(context: android.content.Context): Boolean {
        return context.resources.getBoolean(R.bool.isTablet)
//        return false
    }

    private var mSessionID: String = ""
    private var Tag : String = "HandlerLineTaskLlm"
    var breakFlag = false
    // 假设的最大递归尝试次数
    private const val MAX_RETRY_COUNT = 10
    private var query: String = ""
    private var user_input: String = ""
    private var targetQuery: String = ""
    private var AppInstalled: Boolean = true
    private var appMessage: String = ""
    private var isClarify: Boolean = false
    private var mCallback: (String, String) -> Unit = { _, _ -> }
    private var mutableType: String? = null
    private var mutableText: String? = null
    //鉴权参数
    private var authorization: String = ""
    private var sign: String = ""
    private var timestamp: String = ""
    private var client_id: String = ""
    private var device_id: String = ""
    private var device_no: String = ""
    private var product_id: String = ""

    @RequiresApi(Build.VERSION_CODES.R)
    fun getAndUploadScreenshot(
        context: Activity,
        sessionID: String,
        queryString: String,//query，二次交互时，会修改
        targetQueryString: String,//目标query，保持不变
        type: String?,
        text: String?,
        retryCount: Int = 0,
        isNewTask: Boolean = true,
        callback: (String, String) -> Unit
    ) {
        mSessionID = sessionID
        mCallback = callback
        query = targetQueryString
        Timber.tag(Tag).i("queryString $queryString ")
        targetQuery = targetQueryString
        if (breakFlag) {
            Timber.tag(Tag).i("任务中断 AppInstalled $AppInstalled")
            breakFlag = false
            if (AppInstalled){
                callback("任务已中断","")

            } else {
                Timber.tag(Tag).i(appMessage)
                callback(appMessage,"")
            }
            return
        }
        //这里修改为每次上传都需要截图。Gui Agent业务要求
        var isNewTask = false
        if (isNewTask) {
            handleLlmAction(
                context, sessionID, null, queryString, null,null,retryCount, callback
            )
        } else {
            val screenshotStartTime = System.currentTimeMillis() // 记录截图开始时间
            Timber.tag(Tag).i("截图开始时间: $screenshotStartTime")
            ScreenshotUtils.getScreenshotBase64(context) { base64String ->
                val screenshotEndTime = System.currentTimeMillis() // 记录截图结束时间
                Timber.tag(Tag).i("截图时间: ${screenshotEndTime - screenshotStartTime} ms")
                if (base64String != null) {
                    handleLlmAction(
                        context, sessionID, base64String, queryString, type, text, retryCount, callback
                    )
                } else {
                    if (retryCount < MAX_RETRY_COUNT) {

                        // 若未达到最大尝试次数，则递归调用自身重新截图
                        Timber.tag(Tag)
                            .i("截图失败，重新尝试（尝试次数：${retryCount + 1}/${MAX_RETRY_COUNT}）")
                        getAndUploadScreenshot(
                            context,
                            mSessionID,
                            query,
                            targetQuery,
                            type,
                            text,
                            retryCount + 1,
                            false,
                            callback
                        )
                    } else {
                        // 若已达到最大尝试次数，则通知调用者截图失败
                        Timber.tag(Tag).i("截图多次尝试均失败")
                        callback("截图多次尝试均失败","")

                    }
                }
            }
        }

    }
    @RequiresApi(Build.VERSION_CODES.R)
    fun handleLlmAction(
        context: Activity,
        sessionID: String,
        base64String: String?,
        resultString: String,
        type: String?,
        text: String?,
        retryCount: Int,
        callback: (String, String) -> Unit
    ) {
//        mutableType = type
//        mutableText = text

        try {
//            val params = LLmQueryParams(sessionID, base64String, resultString)
            Timber.tag(Tag).i(" 请求 params sessionID: $sessionID query: $resultString type: $type text: $text base64Size: ${base64String?.length ?: 0} 字节")
            
            // 发送广播：通知系统有任务正在执行
            BroadcastUtils.taskExecutingBroadcast()
            Timber.tag(Tag).d("已发送任务执行广播 %s",BroadcastConstants.ACTION_TASK_EXECUTING)
            
            val uploadStartTime = System.currentTimeMillis() // 记录上传开始时间
            val screenMode = "normal"

            var aiAssistConfig = getInstance().getAiAssistConfig()
            timestamp = (System.currentTimeMillis()).toString()
            sign = SignatureUtil.setMd5Signature(aiAssistConfig.deviceSecret + timestamp)
            authorization = SharedPreferencesUtil.getToken()
            device_no = aiAssistConfig.deviceNo
            product_id = aiAssistConfig.productId
            device_id = aiAssistConfig.deviceId
            if (isTablet(context)) {
                client_id = "lingxi_pad"
            } else {
                client_id ="lingxi_android"
            }

            // 在后台线程执行网络请求，避免阻塞主线程导致ANR
            CoroutineScope(Dispatchers.IO).launch {
                val predictResult = NexusPilotManager.predict(sessionID, base64String, resultString, type, text, screenMode,
                    authorization, sign, timestamp, client_id, device_id, device_no, product_id)
                
                Timber.tag(Tag).i(" result: $predictResult")
                
                // 切换回主线程处理结果
                withContext(Dispatchers.Main) {
                    processPredictResult(
                        context, sessionID, base64String, resultString, type, text,
                        retryCount, callback, predictResult, uploadStartTime
                    )
                }
            }
        } catch (e: Exception) {
            Timber.tag(Tag).i("handleLlmAction执行异常：${e.message}")
            callback("网络请求异常", e.message?.toString() ?: "未知异常")
        }
    }

    /**
     * 处理预测结果
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun processPredictResult(
        context: Activity,
        sessionID: String,
        base64String: String?,
        resultString: String,
        type: String?,
        text: String?,
        retryCount: Int,
        callback: (String, String) -> Unit,
        predictResult: com.lingxi.nexuspilot.data.PredictResult,
        uploadStartTime: Long
    ) {
        Timber.tag(Tag)
            .i("请求错误11${predictResult.toString()}")
        if (predictResult.success) {
            val uploadEndTime = System.currentTimeMillis()
            Timber.tag(Tag).i("截图上传时间: ${uploadEndTime - uploadStartTime} ms")
            
            CoroutineScope(Dispatchers.Main).launch {
                // 检查操作是否完成
                predictResult?.data?.prediction?.action_expand?.let { actions ->
                        // 判断图片执行时截图与上传时截图是否一致，不一致则重新截图并上传
//                        ScreenshotUtils.captureAndCompare(context, 30, false, 0.2f) { hasDifference, isFirstCapture ->
//                            if (isFirstCapture) {
//                                Timber.tag(Tag).i("第一次截图")
//                            } else if (hasDifference) {
//                                Timber.tag(Tag).i("屏幕已变化")
//                            } else {
//                                Timber.tag(Tag).i("屏幕未变化")
//                            }
//                        }

                        if(actions[0].action_type == ActionType.OPEN_APP.alias){
                            // 遍历 actions 列表，找到第一个已安装的应用
                            var selectedAction: PredictionAction? = null
                            var selectedIndex = -1
                            
                            for ((index, action) in actions.withIndex()) {
                                if (action.action_type == ActionType.OPEN_APP.alias && isAppInstalled(context, action.package_name)) {
                                    selectedAction = action
                                    selectedIndex = index
                                    Timber.tag(Tag).i("找到已安装应用: ${action.app_name}, package: ${action.package_name}, index: $index")
                                    break
                                }
                            }
                            
                            if (selectedAction != null) {
                                mutableType = "app_choice"
                                mutableText = selectedAction.app_name
                                if (exeTask(
                                        selectedAction,
                                        context,
                                        predictResult.data,
                                        retryCount,
                                        mSessionID,
                                        resultString,
                                        mutableType,
                                        mutableText,
                                        callback,
                                        predictResult?.data,
                                        selectedIndex,
                                        actions
                                    )
                                ) return@launch
                            } else {
                                // 所有应用都未安装
                                Timber.tag(Tag).i("所有推荐应用均未安装")
                                mutableType = "app_choice"
                                mutableText = actions[0].app_name
                                if (exeTask(
                                        actions[0],
                                        context,
                                        predictResult.data,
                                        retryCount,
                                        mSessionID,
                                        resultString,
                                        mutableType,
                                        mutableText,
                                        callback,
                                        predictResult?.data,
                                        0,
                                        actions
                                    )
                                ) return@launch
                            }
                        }else if(actions[0].action_type == ActionType.CLARIFICATION.alias) {
                            if (actions[0].clarification_type == ClarificationType.PAYMENT.alias
                                || actions[0].clarification_type == ClarificationType.LOGIN.alias
                                || actions[0].clarification_type == ClarificationType.SECURITY_VERIFICATION.alias
                                || actions[0].clarification_type == ClarificationType.PERMISSION_CONSENT.alias){


                                callback("手动接管", actions[0].clarification_question)
                                return@launch

                            } else {
                                callback("问题澄清", actions[0].clarification_question)
                                while (true) {
                                    delay(1000L)
                                    if (isClarify) {
                                        isClarify = false
                                        Timber.tag(Tag)
                                            .i("用户输入内容 query: $user_input")
                                        break
                                    }
                                }
                                // 平板设备需要额外延迟等待，否则可能会截到弹窗内容，此处延迟等待弹窗移除后再截图
                                val delayTime = if (isTablet(context)) 500L else 0L
                                if (delayTime > 0) {
                                    Timber.tag(Tag).i("平板设备延迟${delayTime}ms等待弹窗移除再截图")
                                    delay(delayTime)
                                }
                                //这里要加上客户端选择或者输入的内容
                                getAndUploadScreenshot(
                                    context,
                                    mSessionID,
                                    query,
                                    targetQuery,
                                    "query_clarification",
                                    user_input,
                                    retryCount + 1,
                                    false,
                                    callback
                                )
                                user_input = ""
                                mutableType = ""
                                mutableText = ""
                                return@launch
                            }


                        }else{
                            // 遍历 action_expand 列表
                            actions.forEachIndexed { index, action ->
                                if (exeTask(
                                        action,
                                        context,
                                        predictResult.data,
                                        retryCount,
                                        mSessionID,
                                        resultString,
                                        mutableType,
                                        mutableText,
                                        callback,
                                        predictResult?.data,
                                        index,
                                        actions
                                    )
                                ) return@launch
                            }
                        }
                        // 延迟后继续递归请求
                        delay(2000L)
                        // 操作未完成，递归调用处理函数
                        Timber.tag(Tag).i("操作完成，准备递归调用...")
                        getAndUploadScreenshot(
                            context,
                            mSessionID,
                            query,
                            targetQuery,
                            mutableType,
                            mutableText,
                            0,
                            false,
                            callback
                        )
                        user_input = ""
                        mutableType = ""
                        mutableText = ""
                } ?: run {
                    //返回数据无效，重新截图上传
                    Timber.tag(Tag).i("返回数据无效，重新截图上传...")
                    getAndUploadScreenshot(
                        context,
                        mSessionID,
                        resultString,
                        targetQuery,
                        mutableType,
                        mutableText,
                        0,
                        false,
                        callback
                    )
                }
            }
        } else {
            // 根据需求，可能还需要处理重试逻辑或直接回调错误
            Timber.tag(Tag)
                .i("请求错误，重新尝试（尝试次数：${retryCount + 1}/${MAX_RETRY_COUNT}）errMsg: ${predictResult.message}")
            if (retryCount < MAX_RETRY_COUNT) {
                // 重试逻辑（注意：此处可能需要调整重试时的参数和逻辑）
                getAndUploadScreenshot(
                    context,
                    mSessionID,
                    resultString,
                    targetQuery,
                    mutableType,
                    mutableText,
                    retryCount + 1,
                    false,
                    callback
                )
            } else {
                // 达到最大重试次数，回调错误
                callback("截图上传多次尝试均失败",predictResult.message)
            }
        }
    }

    @SuppressLint("NewApi")
    private suspend fun exeTask(
        action: PredictionAction,
        context: Activity,
        data: Data?,
        retryCount: Int,
        sessionID: String,
        resultString: String,
        type: String?,
        text: String?,
        callback: (String, String) -> Unit,
        data0: Data?,
        index: Int,
        actions: List<PredictionAction>
    ): Boolean {
        if (action.action_type != ActionType.FINISHED.alias) {


            appMessage = "";
            AppInstalled = true;
            // 执行当前任务
            try {
                TaskExecutor.execute(
                    context,
                    action
                ) { code, query1, message, appinstalled, breakflag ->
                    when (code) {
                        ExecuteStatus.INPUT.alias -> {
//                            query = query1
                            mutableType = "grounding"
                            mutableText = query1
                            Timber.tag(Tag)
                                .i("操作完成 callback... $query")
                        }

                        ExecuteStatus.OPEN_APP_FAIL.alias -> {
                            Timber.tag(Tag)
                                .i("操作完成 callback... $message")
                            AppInstalled = appinstalled
                            breakFlag = breakflag
                            appMessage = message
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                //                                        callback("执行任务出错")
                Timber.tag(Tag)
                    .e("执行异常... ${e.message} ${e.printStackTrace()}")
                Timber.tag(Tag)
                    .i("执行异常，重新尝试（尝试次数：${retryCount + 1}/${MAX_RETRY_COUNT}")
                if (retryCount < MAX_RETRY_COUNT) {
                    // 重试逻辑（注意：此处可能需要调整重试时的参数和逻辑）
                    getAndUploadScreenshot(
                        context,
                        mSessionID,
                        resultString,
                        targetQuery,
                        type,
                        text,
                        retryCount + 1,
                        false,
                        callback
                    )
                } else {
                    // 达到最大重试次数，回调错误
                    callback("执行异常多次尝试均失败", e.message!!.toString())
                }
                return true
            }

        } else {
            var text = data0?.prediction?.action?.text
            // 操作已完成，通知调用者
            callback("任务完成", text.toString())
            return true
        }
        if (index < actions.lastIndex) { // 非最后一个元素时延迟
            delay(500L) // 500ms间隔
        }
        return false
    }

    fun setInputText(input: String) {
        user_input = input
        isClarify = true
        Timber.tag(Tag).i("setInputText:$user_input")
    }

    fun breakTask() {
        breakFlag = true
    }
    fun closeMenu() {

    }

    /**
     * 判断应用是否已安装
     * @param context 上下文
     * @param packageName 应用包名
     * @return true表示已安装，false表示未安装
     */
    private fun isAppInstalled(context: Activity, packageName: String?): Boolean {
        if (packageName.isNullOrEmpty()) {
            return false
        }
        return try {
            // 安全解析包名，支持 "com.example.app" 或 "com.example.app/com.example.MainActivity" 两种格式
            val actualPackageName = packageName.split("/")[0]
            context.packageManager.getPackageInfo(actualPackageName, 0)
            true
        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun sendMessage(context: Activity,content: String,type: String) {
        CoroutineScope(Dispatchers.Main).launch {
            // 平板设备需要额外延迟等待弹窗动画完成
            val delayTime = if (isTablet(context)) 500L else 0L
            if (delayTime > 0) {
                Timber.tag(Tag).i("平板设备延迟${delayTime}ms等待弹窗移除再截图")
                delay(delayTime)
            }
            getAndUploadScreenshot(
                context,
                mSessionID,
                query,
                targetQuery,
                type,
                content,
                0,
                false,
                mCallback
            )
        }
    }

}