package com.cmdc.ai.assist.demo.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cmdc.ai.assist.AIAssistantManager
import com.cmdc.ai.assist.constraint.AsrResult
import com.cmdc.ai.assist.constraint.DialogueResult
import com.cmdc.ai.assist.demo.R
import com.cmdc.ai.assist.voiceAssistant.VoiceAssistant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 语音唤醒 → 自动对话 联调页。
 *
 * 演示如何用 SDK 的语音助手引擎 [VoiceAssistant] 一站式完成「喊『灵犀灵犀』→ 自动进入语音对话
 * → 拿到大模型答案 → 回到待唤醒」的闭环，宿主只需 [VoiceAssistant.start] 一次并处理回调。
 *
 * 关键点：
 * 1. 需要 [Manifest.permission.RECORD_AUDIO] 运行时权限，授权后才开始编排。
 * 2. 鉴权已在 [com.cmdc.ai.assist.demo.DemoApplication] 中通过 `AIAssistantManagerTest.initialize()` 完成。
 * 3. SDK 不负责把答案念出来（TTS 交宿主）；本 Demo 用「念完了」按钮模拟宿主播报完成，
 *    调用 [VoiceAssistant.notifySpeakFinished] 通知语音助手引擎重新打开唤醒麦（半双工）。
 */
class MainActivity : AppCompatActivity() {

    private companion object {
        /** 录音权限请求码。 */
        const val REQ_RECORD_AUDIO = 1001
    }

    /** 语音助手引擎（唤醒 ↔ ASR 对话状态机 + 麦克风交接）。 */
    private var voiceAssistant: VoiceAssistant? = null

    /** 顶部状态栏。 */
    private lateinit var tvStatus: TextView
    /** 日志区（可滚动）。 */
    private lateinit var tvLog: TextView
    /** 「开始」按钮：进入待唤醒。 */
    private lateinit var btnStart: Button
    /** 「停止」按钮：关闭唤醒与对话。 */
    private lateinit var btnStop: Button
    /** 「免唤醒」按钮：直接进入一轮对话。 */
    private lateinit var btnTrigger: Button
    /** 「念完了」按钮：模拟宿主 TTS 播报结束。 */
    private lateinit var btnSpeakFinished: Button

    /** 日志时间戳格式化器。 */
    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    /** 初始化 UI 并申请录音权限，授权后启动语音助手引擎。 */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)
        bindViews()

        // 先申请录音权限；已授权则直接初始化语音助手引擎。
        if (hasRecordPermission()) {
            setupVoiceAssistant()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQ_RECORD_AUDIO
            )
        }
    }

    /** 绑定控件与按钮事件。 */
    private fun bindViews() {
        tvStatus = findViewById(R.id.tv_status)
        tvLog = findViewById(R.id.tv_log)
        btnStart = findViewById(R.id.btn_start)
        btnStop = findViewById(R.id.btn_stop)
        btnTrigger = findViewById(R.id.btn_trigger)
        btnSpeakFinished = findViewById(R.id.btn_speak_finished)

        // 让日志区可滚动。
        tvLog.movementMethod = ScrollingMovementMethod()

        // 开始：进入待唤醒（打开唤醒麦，可喊「灵犀灵犀」）。
        btnStart.setOnClickListener {
            voiceAssistant?.start()
            appendLog("start() → 进入待唤醒，可喊「灵犀灵犀」")
        }
        // 停止：关闭唤醒与对话。
        btnStop.setOnClickListener {
            voiceAssistant?.stop()
            appendLog("stop() → 已停止")
        }
        // 免唤醒：不喊唤醒词，直接进入一轮对话。
        btnTrigger.setOnClickListener {
            voiceAssistant?.triggerManually()
            appendLog("triggerManually() → 免唤醒直接说话")
        }
        // 念完了：模拟宿主 TTS 播报结束，通知语音助手引擎重开唤醒麦（半双工）。
        btnSpeakFinished.setOnClickListener {
            voiceAssistant?.notifySpeakFinished()
            appendLog("notifySpeakFinished() → 重新打开唤醒麦")
        }
    }

    /** 初始化并启动语音助手引擎。 */
    private fun setupVoiceAssistant() {
        val va = try {
            AIAssistantManager.getInstance().voiceAssistantHelp() as? VoiceAssistant
        } catch (e: Exception) {
            // 通常是 AIAssistantManager 尚未初始化（鉴权未完成）。
            appendLog("获取 VoiceAssistant 失败：${e.message}")
            null
        }
        if (va == null) {
            Toast.makeText(this, "语音助手不可用，请检查 SDK 初始化", Toast.LENGTH_LONG).show()
            return
        }
        voiceAssistant = va

        // 顺序：init → setListener → start（内部会先 setWakeupCallback 再 init 唤醒引擎）。
        va.init(this)
        va.setListener(assistantListener)
        va.start()
        appendLog("初始化完成，已进入待唤醒。请喊「灵犀灵犀」")
    }

    /** 语音助手引擎事件回调（SDK 已切回主线程，可直接更新 UI）。 */
    private val assistantListener = object : VoiceAssistant.Listener {
        // 状态机状态变化 → 刷新状态栏。
        override fun onStateChanged(state: VoiceAssistant.State) {
            tvStatus.text = "状态：${describeState(state)}"
        }

        // 唤醒词命中。
        override fun onWakeup(word: String) {
            appendLog("唤醒命中：$word")
        }

        // ASR 识别结果（中间/最终）。
        override fun onAsrResult(asrResult: AsrResult) {
            appendLog(if (asrResult.isFinal) "识别(最终)：${asrResult.text}" else "识别(中间)：${asrResult.text}")
        }

        // 大模型对话结果（SDK 不做 TTS，需宿主播报）。
        override fun onDialogueResult(result: DialogueResult) {
            val answer = result.assistant_answer_content ?: ""
            appendLog("回复：$result")
            // 提示：SDK 不做 TTS，宿主念完答案后需点「念完了」按钮。
            /*appendLog("（宿主念完答案后，请点『念完了』重开唤醒）")*/
            appendLog("--------我是分隔符--------")
        }

        // 错误回调。
        override fun onError(code: Int, msg: String) {
            appendLog("错误[$code]：$msg")
        }
    }

    /** 状态枚举 → 中文描述。 */
    private fun describeState(state: VoiceAssistant.State): String = when (state) {
        VoiceAssistant.State.IDLE_LISTENING -> "待唤醒"
        VoiceAssistant.State.WAKED -> "已唤醒(交接麦克风)"
        VoiceAssistant.State.ASR_LISTENING -> "聆听中"
        VoiceAssistant.State.THINKING -> "思考中"
        VoiceAssistant.State.SPEAKING -> "播报中(等宿主念完)"
    }

    /** 追加一行带时间戳的日志并自动滚动到底部。 */
    private fun appendLog(line: String) {
        val stamped = "${timeFmt.format(Date())}  $line\n"
        tvLog.append(stamped)
        // 滚动到底部。
        val layout = tvLog.layout ?: return
        val scrollY = layout.getLineTop(tvLog.lineCount) - tvLog.height
        if (scrollY > 0) {
            tvLog.scrollTo(0, scrollY)
        }
    }

    /** 是否已授予录音权限。 */
    private fun hasRecordPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED

    /** 录音权限授权结果回调：授权则启动引擎，否则提示并记录。 */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupVoiceAssistant()
            } else {
                Toast.makeText(this, "未授予录音权限，无法进行语音唤醒", Toast.LENGTH_LONG).show()
                appendLog("录音权限被拒绝")
            }
        }
    }

    /**
     *
     * 在 Activity 销毁时释放语音助手引擎 [voiceAssistant] 持有的唤醒引擎、ASR 引擎及麦克风等
     * 底层资源，防止麦克风句柄泄漏导致后续录音失败；随后置空引用并将销毁流程交由父类处理。
     *
     * @return 无返回值（Unit）。
     */
    override fun onDestroy() {
        // 释放唤醒与对话资源，避免麦克风泄漏。
        voiceAssistant?.release()
        voiceAssistant = null
        super.onDestroy()
    }
}
