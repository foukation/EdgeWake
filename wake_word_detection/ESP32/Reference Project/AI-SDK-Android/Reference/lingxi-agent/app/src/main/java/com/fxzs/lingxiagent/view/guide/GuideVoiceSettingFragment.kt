package com.fxzs.lingxiagent.view.guide

import android.R.attr.animationDuration
import android.R.attr.button
import android.R.attr.repeatCount
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.animation.AccelerateDecelerateInterpolator
import androidx.core.animation.Animator
import androidx.core.animation.AnimatorListenerAdapter
import androidx.core.animation.AnimatorSet
import androidx.core.animation.ObjectAnimator
import androidx.core.net.toUri
import com.fxzs.lingxiagent.R
import com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker.EventConstants
import com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker.TrackerUtils.trackWakeAbortEvent
import com.fxzs.lingxiagent.receiver.LingxiAskWidgetProvider
import com.fxzs.lingxiagent.util.AppPermissionRequestManager
import com.fxzs.lingxiagent.util.WakeUpPermissionHelper
import com.fxzs.lingxiagent.view.common.BaseFragment
import com.fxzs.lingxiagent.view.common.CommonDialog.OnDialogClickListener
import com.fxzs.lingxiagent.viewmodel.main.VMEmpty
import com.google.android.material.animation.AnimatorSetCompat
import com.loc.fi
import org.apache.commons.compress.harmony.pack200.PackingUtils.config

/**
 *创建者：ZyOng
 *描述：引导设置
 *创建时间：2026/4/9 17:39
 */
class GuideVoiceSettingFragment : BaseFragment<VMEmpty>() {
    private var btnNext: TextView? = null
    private var switchWakeup: SwitchCompat? = null
    private var switchPowerWakeup: SwitchCompat? = null
    private var switchKeyboardWakeup: SwitchCompat? = null
    private var currentProcessingSwitch: SwitchCompat? = null
    private var ivFinger: ImageView?=null
    private var ivFinger2: ImageView?=null
    private var ivFinger3: ImageView?=null
    private var fingerAnimator: AnimatorSet? = null
    private var isExcelAnimator: Boolean = false

    override fun getLayoutResource(): Int {
        return R.layout.fragment_guide_voice
    }

    override fun getViewModelClass(): Class<VMEmpty> {
        return VMEmpty::class.java
    }

    override fun initializeViews(view: View?) { // 绑定开关控件
        btnNext = findViewById(R.id.btn_next)
        switchWakeup = findViewById(R.id.switch_wakeup)
        switchPowerWakeup = findViewById(R.id.switch_power_wakeup)
        switchKeyboardWakeup = findViewById(R.id.switch_keybord_wakeup)
        ivFinger = findViewById(R.id.iv_finger)
        ivFinger2 = findViewById(R.id.iv_finger2)
        ivFinger3 = findViewById(R.id.iv_finger3)
        btnNext!!.setOnClickListener {
            (activity as? GuideActivity)?.onGuideEvent()
        }


        // ========== 主唤醒开关（需要通知权限） ==========
        switchWakeup!!.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                currentProcessingSwitch = switchWakeup
                checkPermissionsAndEnable(switchWakeup!!, true) // 带通知权限检查
            } else {
                WakeUpPermissionHelper.setWakeUpEnabled(context, false)
                WakeUpPermissionHelper.toggleWakeUpService(context, false)
                trackWakeAbortEvent(false, EventConstants.WakeUpManagement.VOICE_WAKE_CREATE)

            }
        })


        // ========== 电源键唤醒开关（无需通知权限） ==========
        switchPowerWakeup!!.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                currentProcessingSwitch = switchPowerWakeup
                checkPermissionsAndEnable(switchPowerWakeup!!, false) // 不带通知权限检查
            } else {
                WakeUpPermissionHelper.setWakeUpPowerEnabled(context, false)
                trackWakeAbortEvent(false, EventConstants.WakeUpManagement.POWER_WAKE_CREATE)

            }
        })


        // ========== 键盘唤醒开关（无需通知权限） ==========
        switchKeyboardWakeup!!.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                currentProcessingSwitch = switchKeyboardWakeup
                checkPermissionsAndEnable(switchKeyboardWakeup!!, false) // 不带通知权限检查
            } else {
                WakeUpPermissionHelper.setKeyWakeupKeybordEnabled(context, false)
                trackWakeAbortEvent(false, EventConstants.WakeUpManagement.KEYBOARD_WAKE_CREATE)

            }
        })
    }

    /**
     * 权限引导+开关启用通用逻辑
     * @param targetSwitch 目标开关
     * @param needNotification 是否需要通知权限
     */
    private fun checkPermissionsAndEnable(
        targetSwitch: SwitchCompat,
        needNotification: Boolean
    ) { // 前置校验：如果开关已被取消勾选，直接返回

        if (!WakeUpPermissionHelper.checkRecordAudioPermission(context)) {
            AppPermissionRequestManager.requestAudioPermission(
                activity,
                WakeUpPermissionHelper.REQUEST_CODE_RECORD_AUDIO,
                AppPermissionRequestManager.PERMISSION_AUDIO_MESSAGE_WAKEUP,
                targetSwitch
            )
            return
        }

        if (needNotification && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!WakeUpPermissionHelper.checkPostNotifications(context)) {
                AppPermissionRequestManager.requestNotificationsPermission2(
                    activity,
                    WakeUpPermissionHelper.REQUEST_CODE_POST_NOTIFICATIONS,
                    "请授权通知权限，以保证唤醒服务正常使用",targetSwitch
                )
                return
            }
        }

        // ========== 步骤3：检查悬浮窗权限 ==========
        if (!WakeUpPermissionHelper.checkFloatingWindowPermission(context)) {
            AppPermissionRequestManager.requestOverlayPermissionDialog(
                activity,
                object : OnDialogClickListener {
                    override fun onConfirm() {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                        intent.setData(("package:" + context?.packageName).toUri())
                        startActivityForResult(intent, WakeUpPermissionHelper.REQUEST_CODE_FLOATING)
                    }

                    override fun onCancel() {
                        targetSwitch.setChecked(false) // 权限取消，开关回退
                    }
                })
            return
        }

        enableSwitchFunction(targetSwitch)
    }


    /**
     * 根据开关类型启用对应功能
     */
    private fun enableSwitchFunction(targetSwitch: SwitchCompat?) {
        if (targetSwitch === switchWakeup) { // 主唤醒开关：保存状态+启动服务
            WakeUpPermissionHelper.setWakeUpEnabled(context, true)
            WakeUpPermissionHelper.toggleWakeUpService(context, true)
            trackWakeAbortEvent(true, EventConstants.WakeUpManagement.VOICE_WAKE_CREATE)
//            stopFingerAnimation()
        } else if (targetSwitch === switchPowerWakeup) { // 电源键唤醒：仅保存状态
            WakeUpPermissionHelper.setWakeUpPowerEnabled(context, true)
            trackWakeAbortEvent(true, EventConstants.WakeUpManagement.POWER_WAKE_CREATE)
//            stopFingerAnimation()
        } else if (targetSwitch === switchKeyboardWakeup) { // 键盘唤醒：仅保存状态
            WakeUpPermissionHelper.setKeyWakeupKeybordEnabled(context, true)
            trackWakeAbortEvent(true, EventConstants.WakeUpManagement.KEYBOARD_WAKE_CREATE)
//            stopFingerAnimation()
        }
    }

    override fun onResume() {
        super.onResume()
        initSwitchStates()
    }

    private fun initSwitchStates() { // 主唤醒开关（检查全部权限：录音+悬浮窗+通知）
        val isMainWakeupEnabled = WakeUpPermissionHelper.isWakeUpEnabled(context)
        switchWakeup!!.setChecked(isMainWakeupEnabled && checkAllPermissions(true))

        // 电源键唤醒开关（检查基础权限：录音+悬浮窗）
        val isPowerWakeupEnabled = WakeUpPermissionHelper.isWakeUpPowerEnabled(context)
        switchPowerWakeup!!.setChecked(isPowerWakeupEnabled && checkAllPermissions(false))

        // 键盘唤醒开关（检查基础权限：录音+悬浮窗）
        val isKeyboardWakeupEnabled = WakeUpPermissionHelper.isWakeUpKeyBordEnabled(context)
        switchKeyboardWakeup!!.setChecked(isKeyboardWakeupEnabled && checkAllPermissions(false))

        if (!switchWakeup?.isChecked!! && !switchPowerWakeup?.isChecked!! && !switchKeyboardWakeup?.isChecked!!){
            ivFinger?.post {
                ivFinger?.startFingerAnimationAt3Positions()
            }
        }
    }

    private fun checkAllPermissions(needNotification: Boolean): Boolean { // 1. 必检：录音权限
        if (!WakeUpPermissionHelper.checkRecordAudioPermission(context)) {
            return false
        }

        // 2. 可选：通知权限（仅主唤醒开关需要）
        if (needNotification && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!WakeUpPermissionHelper.checkPostNotifications(context)) {
                return false
            }
        }

        // 3. 必检：悬浮窗权限
        if (!WakeUpPermissionHelper.checkFloatingWindowPermission(context)) {
            return false
        }

        return true
    }


    override fun setupDataBinding() {
    }

    override fun setupObservers() {
    }


    /**
     * 权限请求回调（处理录音/通知权限）
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // 检查权限是否全部授予
        var allGranted = true
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false
                break
            }
        }

        // 权限被拒绝：关闭当前处理的开关
        if (!allGranted && currentProcessingSwitch != null) {
            currentProcessingSwitch!!.setChecked(false)
            return
        }

        // 权限被授予：继续检查下一个权限
        if (currentProcessingSwitch === switchWakeup) {
            checkPermissionsAndEnable(switchWakeup!!, true)
        } else if (currentProcessingSwitch === switchPowerWakeup) {
            checkPermissionsAndEnable(switchPowerWakeup!!, false)
        } else if (currentProcessingSwitch === switchKeyboardWakeup) {
            checkPermissionsAndEnable(switchKeyboardWakeup!!, false)
        }
    }

    /**
     * 悬浮窗权限设置回调
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == WakeUpPermissionHelper.REQUEST_CODE_FLOATING) {
            LingxiAskWidgetProvider.refreshAllWidgets(context) // 回到页面后重新检查权限并尝试启用开关
            if (currentProcessingSwitch === switchWakeup) {
                checkPermissionsAndEnable(switchWakeup!!, true)
            } else if (currentProcessingSwitch === switchPowerWakeup) {
                checkPermissionsAndEnable(switchPowerWakeup!!, false)
            } else if (currentProcessingSwitch === switchKeyboardWakeup) {
                checkPermissionsAndEnable(switchKeyboardWakeup!!, false)
            }
        }
    }


    private fun createDelayAnimator(delay: Long): AnimatorSet {
        return AnimatorSet().apply {
            // 必须至少有一个子动画
            playTogether(ObjectAnimator.ofFloat(ivFinger!!, "alpha", 0f, 0f))
            this.duration = delay
        }
    }


    fun ImageView.startFingerAnimationAt3Positions() {
        if (isExcelAnimator) return
        stopFingerAnimation()

        val interval = 500L // 间隔1秒

        ivFinger?.visibility = View.VISIBLE

        val anim1 = ivFinger?.createPositionAnimator(1500)
        val delay1 = createDelayAnimator(interval)
        val anim2 = ivFinger2?.createPositionAnimator(1500)
        val delay2 = createDelayAnimator(interval)
        val anim3 = ivFinger3?.createPositionAnimator(1500)

        fingerAnimator = AnimatorSet().apply {
            playSequentially(anim1?.addEndListener {
                Log.d("GuideVoiceSettingFragment", "动画1结束")
                ivFinger?.visibility = View.GONE
                 },
                delay1.addEndListener {
                    Log.d("GuideVoiceSettingFragment", "等待1 结束")
                    ivFinger2?.visibility = View.VISIBLE
                },
                anim2?.addEndListener {
                    ivFinger2?.visibility = View.GONE
                    Log.d("GuideVoiceSettingFragment", "等待2 结束") },
                delay2.addEndListener {
                    ivFinger3?.visibility = View.VISIBLE
                    Log.d("GuideVoiceSettingFragment", "动画1结束")

                },
                anim3?.addEndListener {
                    Log.d("GuideVoiceSettingFragment", "动画3结束")
                    ivFinger3?.visibility = View.GONE
                    stopFingerAnimation()
                    isExcelAnimator = true
                })
            start()
        }
    }


    // 创建单个位置的动画
    private fun ImageView.createPositionAnimator(
        duration: Long
    ): AnimatorSet {

        // 移动到目标位置
        return AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(this@createPositionAnimator, "translationX", -30f, 0f, 0f, -30f), // translationY: 相对于当前布局位置的上下移动（可选）
                ObjectAnimator.ofFloat(this@createPositionAnimator, "translationY", -30f, 0f, 0f, -30f))
            this.duration = duration
        }

    }
    fun Animator.addEndListener(onEnd: () -> Unit): Animator {
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onEnd()
            }
        })
        return this
    }

    fun stopFingerAnimation() {
        isExcelAnimator= true
        fingerAnimator?.cancel()
        fingerAnimator = null
        ivFinger?.visibility = View.GONE
        ivFinger2?.visibility = View.GONE
        ivFinger3?.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        stopFingerAnimation()
    }





}