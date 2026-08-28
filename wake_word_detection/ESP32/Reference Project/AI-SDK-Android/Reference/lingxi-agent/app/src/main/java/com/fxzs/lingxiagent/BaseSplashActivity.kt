package com.fxzs.lingxiagent

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.AIServiceManager
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.AIServiceManager.Companion.initialize
import com.fxzs.lingxiagent.model.auth.AuthHelper
import com.fxzs.lingxiagent.util.SharedPreferencesUtil
import com.fxzs.lingxiagent.util.WakeUpPermissionHelper
import com.fxzs.lingxiagent.util.ZUtils
import com.fxzs.lingxiagent.view.common.BaseActivity
import com.fxzs.lingxiagent.view.common.CommonDialog
import com.fxzs.lingxiagent.view.common.CommonDialog.OnDialogClickListener
import com.fxzs.lingxiagent.view.common.GlobalToast
import com.fxzs.lingxiagent.viewmodel.main.VMSplash
import timber.log.Timber

/**
 * 闪屏页公共基类
 * 负责：外部参数解析、设备鉴权、登录状态判断、目标页面跳转
 */
abstract class BaseSplashActivity : BaseActivity<VMSplash>() {
    private val TAG = AIServiceManager::class.simpleName.toString()
    protected var dialog: Dialog? = null

    // 外部跳转参数
    protected var externalTargetPage: String? = null
    protected val externalExtraParams = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ZUtils.setSystem(this)
        initCustomUi()
    }

    override fun getLayoutResource(): Int {
        return R.layout.activity_splash
    }

    override fun getViewModelClass(): Class<VMSplash> {
        return VMSplash::class.java
    }

    override fun setupDataBinding() {}

    /**
     * 子类可复写此方法以自定义 UI 样式（如设置透明背景）
     * 默认空实现
     */
    protected open fun initCustomUi() {}

    /**
     * 解析外部 App 跳转传入的参数
     */
    private fun parseExternalIntentParams() {
        val intent = intent
        val action = intent.action
        val data: Uri? = intent.data

        if (Intent.ACTION_VIEW == action && data != null) {
            Timber.tag(TAG).d("解析外部跳转参数：%s", data.toString())
            // 1. 解析核心参数
            externalTargetPage = data.getQueryParameter("target")
            val source = data.getQueryParameter("source") ?: "unknown"

            // 2. 权限校验逻辑（原有）
            if(source == "powerkey" && !WakeUpPermissionHelper.isWakeUpPowerEnabled(this)){
                finish()
                return
            }
            if(source == "keyboard" && !WakeUpPermissionHelper.isWakeUpKeyBordEnabled(this)){
                finish()
                return
            }

            // 3. 保存 itemContent 到外部参数集合
            externalExtraParams.clear() // 清空旧参数
            // 关键：解析 itemContent 参数
            val itemContent = data.getQueryParameter("itemContent") ?: ""
            externalExtraParams["itemContent"] = itemContent // 存储 itemContent
            // 可选：保存其他需要的参数（如 source）
            externalExtraParams["source"] = source

            // 4. 传递所有参数到 JumpParameterManager
            JumpParameterManager.saveJumpParams(externalTargetPage, externalExtraParams)
        }
    }

    override fun initializeViews() {
        parseExternalIntentParams()
        jumpIntent()
    }

    /**
     * 设备鉴权逻辑
     */
    protected fun deviceAuth() {
        initialize(this)
        val aiAssistConfig =  AIServiceManager.getInstance().getAiAssistConfig()
        Log.d(TAG,"获取设备信息 = "+aiAssistConfig )
        if (!aiAssistConfig.isValid()) {
            GlobalToast.show(this, "设备鉴权失败", GlobalToast.Type.ERROR)
            SharedPreferencesUtil.saveAuthStatus(false)
            navigateToLogin()
            return
        }

        commonDeviceAuth(object : DeviceAuthCallback {
            override fun onSuccess(msg: String?) {
                Timber.tag(TAG).d("获取 deviceId = 成功 %s", msg)
                refreshToken()
            }

            override fun onFail(msg: String?) {
                Timber.tag(TAG).d("获取 deviceId = 失败")
                SharedPreferencesUtil.clearLoginInfo()
                execIntent()
            }
        })
    }

    private fun refreshToken() {
        Timber.tag(TAG).d("refreshToken = ${AuthHelper.getInstance().isLogin}")
        if (!AuthHelper.getInstance().isLogin) {
            execIntent()
            return
        }
        viewModel.refreshToken().observe(this) { loginResponse ->
            runOnUiThread {
                Timber.tag(TAG).d("refreshToken = $loginResponse")
                execIntent()
            }
        }
    }

    /**
     * 协议弹窗与流程控制
     */
    private fun jumpIntent() {
        if (!SharedPreferencesUtil.isAgreePrivacy()) {
            if(JumpParameterManager.PAGE_WAKEUP_SERVICE == (JumpParameterManager.getTargetPage())){
                JumpParameterManager.clearParams()
                finish()
                return
            }
            showAgreementDialog()
        } else {
            deviceAuth()
        }
    }

    private fun showAgreementDialog() {
        dialog = CommonDialog.showAgreementDialog(
            this,
            getString(R.string.first_user_agreement_content),
            getString(R.string.first_user_agreement_title),
            object : OnDialogClickListener {
                override fun onConfirm() {
                    CommonDialog.Builder(this@BaseSplashActivity)
                        .setTitle("获取应用列表权限说明")
                        .setMessage("为保障应用在不同机型上正常运行，需要读取应用列表信息：\n" +
                                "1. 功能兼容场景：确保快捷操作等功能正常运行，识别设备可响应相关功能的应用；\n" +
                                "2. 技术优化场景：分析运行兼容性、定位技术故障，在页面启动等场景以匿名方式读取已安装应用列表。\n" +
                                "该信息仅用于功能兼容与问题定位，不会用于用户画像、广告投放、第三方共享。\n" +
                                "您可自主选择是否授权，拒绝授权不会影响应用基础功能使用，也可在设备系统设置中随时管理权限。",)
                        .setConfirmText("了解并继续")
                        .setCancelText(this@BaseSplashActivity.getString(android.R.string.cancel))
                        .setOnClickListener(object : OnDialogClickListener {
                            override fun onConfirm() {
                                SharedPreferencesUtil.setAgreePrivacy(true)
                                IYAApplication.getInstance().initAllSensitiveServicesAfterAgreePrivacy()
                                deviceAuth()
                            }

                            override fun onCancel() {
                                finish()
                            }
                        })
                        .show()
                }

                override fun onCancel() {
                    finish()
                }
            })

        dialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                true // 拦截返回键
            } else {
                false
            }
        }
    }

    /**
     * 核心跳转逻辑
     */
    private fun execIntent() {
        //单纯起服务 也需要鉴权信息，要不后面会 aiAssistConfig has not been initialized
        if(JumpParameterManager.jumpWakeupService(this)){
            return
        }
        if (!AuthHelper.getInstance().isLogin) {
            navigateToLogin()
        } else {
            JumpParameterManager.jumpTargetPage(this)
        }
    }

    private fun navigateToLogin() {
        JumpParameterManager.jumpLoginPage(this)
    }

    override fun setupObservers() {}

    override fun onDestroy() {
        super.onDestroy()
        dialog?.dismiss()
        dialog = null
    }
}