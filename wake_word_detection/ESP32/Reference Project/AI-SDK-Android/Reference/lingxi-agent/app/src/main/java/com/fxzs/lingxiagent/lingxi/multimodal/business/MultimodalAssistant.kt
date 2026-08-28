package com.ai.multimodal.business

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ai.multimodal.business.reminder.MedicationReminderService
import com.ai.multimodal.business.reminder.ReminderType
import com.ai.multimodal.domain.DrugInformationService
import com.ai.multimodal.model.response.ImageAssistantResponse
import com.ai.multimodal.model.response.MedicineResponse
import com.ai.multimodal.model.response.PromptResponse
import com.ai.multimodal.utils.formatDrugInformation
import com.fxzs.lingxiagent.util.audio.TTSManager
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException


class MultimodalAssistant {

    private val TAG = MultimodalAssistant::class.simpleName.toString()

    companion object {
        @JvmStatic
        internal var imageUri: Uri? = null
        internal var drugInformation: String? = null
        internal var intent: String? = null
    }

    fun conversationIntent(
        context: Context,
        input: String,
        intent: (String)
    ) {
        MultimodalAssistant.intent = intent

        if (drugInformation == null) {

            val broadcastIntent = Intent("com.ai.multimodal.textResponse")
            val result = "当前没有可用的药品信息。请上传药品信息资料。"
            broadcastIntent.putExtra(
                "textResponse",
                result
            )
            LocalBroadcastManager.getInstance(context.applicationContext)
                .sendBroadcast(broadcastIntent)
            textToAudio(result)
            return
        }

        if (MultimodalAssistant.intent == "set_alarm" ||
            MultimodalAssistant.intent == "remind" ||
            MultimodalAssistant.intent == "set_reminder" ||
            MultimodalAssistant.intent == "medication_reminder"
        ) {

            medicationReminderService(context, drugInformation!!)

            return
        }

        /*if (MultimodalAssistant.intent != "inquire" && MultimodalAssistant.intent != "inquiry") {

            val broadcastIntent = Intent("com.ai.multimodal.textResponse")
            val result = "非常抱歉，当前指令不支持。"
            broadcastIntent.putExtra(
                "textResponse",
                result
            )
            LocalBroadcastManager.getInstance(context.applicationContext)
                .sendBroadcast(broadcastIntent)
            textToAudio(result)
            return
        }*/

        drugInformation?.let {
            conversationQuestionAnswerService(context, input)
            return
        }

    }

    fun conversationDrugInformationService(
        context: Context,
        imageUri: (Uri)
    ) {
        try {
            val drugInformationService = DrugInformationService()
            imageUri.let {
                drugInformationService.extractDrugInformation(
                    context.applicationContext,
                    it,  // 只提供图片路径，其他参数使用默认值
                    onSuccess = { response: ImageAssistantResponse ->
                        // 使用 Java 8 Lambda 处理成功响应
                        Timber.tag(TAG).d("成功: %s", response.content)
                        val broadcastIntent = Intent("com.ai.multimodal.textResponse")
                        var result = formatDrugInformation(response.content)
                        val json: JSONObject = JSONObject(response.content)

                        Timber.tag(TAG).d(result)

                        result += "\n\n是否需要设置闹钟提醒吃药？"
                        val speak = if (json.optString("药品名称").isNotEmpty()) {
                            "检测到药品【${json.optString("药品名称")}】，需要了解更多关于此药品的信息吗？"
                        } else if (json.optString("药品名称")
                                .isNotEmpty() || json.optString("适应症状")
                                .isNotEmpty() || json.optString("用法用量").isNotEmpty()
                        ) {
                            "检测到药品信息，需要给你更详细的信息么？"
                        } else {
                            "这个内容不太像药品，请拍一张药品相关的图片。"
                        }

                        broadcastIntent.putExtra(
                            "textResponse",
                            speak
                        )
                        drugInformation = result
                        LocalBroadcastManager.getInstance(context.applicationContext)
                            .sendBroadcast(broadcastIntent)
                        textToAudio(speak)
                    },
                    onError = { error: String ->
                        // 使用 Java 8 Lambda 处理错误
                        Timber.tag(TAG).e("错误: $error")
                        sendErrorMessage(context, error)
                    }
                )
            }
        } catch (e: IOException) {
            e.printStackTrace()
            e.message?.let { sendErrorMessage(context, it) }
        }
    }

    private fun conversationQuestionAnswerService(
        context: Context,
        input: String,
    ) {
        try {
            val drugInformationService = DrugInformationService()
            drugInformation.let {
                drugInformationService.questionAnswer(
                    input,
                    "你是一个药物助手，只用以下药物信息回答问题，不要自己增加这上面没有的内容。： $it",
                    onSuccess = { response: PromptResponse ->
                        // 使用 Java 8 Lambda 处理成功响应
                        Timber.tag(TAG).d("成功: %s", response.content)
                        val broadcastIntent = Intent("com.ai.multimodal.textResponse")

                        /*var result = ""
                        if (intent == "inquire") result = response.content
                        if (intent == "inquiry") result =
                            response.content + "\n\n是否需要设置闹钟提醒吃药？"*/

                        val result = response.content

                        broadcastIntent.putExtra(
                            "textResponse",
                            result
                        )
                        LocalBroadcastManager.getInstance(context.applicationContext)
                            .sendBroadcast(broadcastIntent)
                        textToAudio(result)
                    },
                    onError = { error: String ->
                        // 使用 Java 8 Lambda 处理错误
                        Timber.tag(TAG).d("错误: $error")
                        sendErrorMessage(context, error)
                    }
                )
            }
        } catch (e: IOException) {
            e.printStackTrace()
            e.message?.let { sendErrorMessage(context, it) }
        }
    }

    /**
     * 文本转语音
     * */
    fun textToAudio(input: String) {
        TTSManager.getInstance().textForceToAudio(input)
    }

    private fun medicationReminderService(
        context: Context,
        input: String,
    ) {
        try {
            val drugInformationService = DrugInformationService()
            drugInformation.let {
                drugInformationService.medicationReminder(
                    input,
                    onSuccess = { response: MedicineResponse ->
                        // 使用 Java 8 Lambda 处理成功响应
                        Timber.tag(TAG).d("成功: %s", response.content)

                        setMedicationReminders(context.applicationContext, response)

                        val broadcastIntent = Intent("com.ai.multimodal.textResponse")
                        val result = "已为您设置闹钟提醒，请到系统闹钟查看。"
                        broadcastIntent.putExtra(
                            "textResponse",
                            result
                        )
                        LocalBroadcastManager.getInstance(context.applicationContext)
                            .sendBroadcast(broadcastIntent)

                        textToAudio(result)

                    },
                    onError = { error: String ->
                        // 使用 Java 8 Lambda 处理错误
                        Timber.tag(TAG).d("错误: $error")
                        sendErrorMessage(context, error)
                    }
                )
            }
        } catch (e: IOException) {
            e.printStackTrace()
            e.message?.let { sendErrorMessage(context, it) }
        }
    }

    private fun setMedicationReminders(context: Context, response: MedicineResponse) {

        // 设置闹钟提醒
        val reminderService = MedicationReminderService(context)
        // 药物名称和用法用量
        /*val medicationName = "感冒药"
        val dosageInstructions = "口服：一次1片，一日3次，三天为一疗程"*/

        // 设置提醒（默认使用通知栏提醒）
        /*reminderService.setMedicationReminder(medicationName, dosageInstructions)*/
        // 设置提醒，使用系统闹钟提醒
        /*reminderService.setMedicationReminder(
            medicationName = "胃药",
            dosageInstructions = "口服：一次1片，一日2次，长期服用",
            reminderType = ReminderType.SYSTEM_ALARM
        )*/
        reminderService.setMedicationReminderFromApi(
            response,
            reminderType = ReminderType.SYSTEM_ALARM
        )

    }

    private fun sendErrorMessage(context: Context, error: String) {
        val broadcastIntent = Intent("com.ai.multimodal.textResponse")
        broadcastIntent.putExtra(
            "textResponse",
            error
        )
        LocalBroadcastManager.getInstance(context.applicationContext)
            .sendBroadcast(broadcastIntent)
    }

}
