package com.fxzs.lingxiagent.view.auth;

import android.content.Intent;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.lifecycle.Observer;

import com.fxzs.lingxiagent.JumpParameterManager;
import com.fxzs.lingxiagent.MainActivity;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.auth.dto.LoginMode;
import com.fxzs.lingxiagent.model.auth.dto.LoginResponse;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.model.user.UserUtil;
import com.fxzs.lingxiagent.util.ScreenSecurityUtils;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.common.CommonDialog;
import com.fxzs.lingxiagent.view.common.ConfirmDialog;
import com.fxzs.lingxiagent.view.common.DataBindingUtils;
import com.fxzs.lingxiagent.view.common.GlobalToast;
import com.fxzs.lingxiagent.view.common.NoMultiClickListener;
import com.fxzs.lingxiagent.view.common.WebViewActivity;
import com.fxzs.lingxiagent.viewmodel.auth.VMRegister;

import timber.log.Timber;

public class RegisterActivity extends BaseActivity<VMRegister> {

    // 顶部栏
    private ImageView ivBack;
    private TextView tvRegisterLink;

    // 标签切换
    private TextView tvVerificationTab;
    private TextView tvPasswordTab;
    private View indicatorLine;
    private View layoutVerification;
    private View layoutPassword;

    // 输入框
    private EditText etPhone;
    private EditText etPhoneVerification;
    private EditText etVerification;
    private EditText etPassword;
    private TextView tvGetCode;
    private ImageView ivTogglePassword;
    private TextView tvForgetPassword;
    private TextView tvRemainFailCount;

    // 底部
    private Button btnLogin;
    private CheckBox cbAgreement;
    private ProgressBar progressBar;

    // 当前登录模式
    private boolean isPasswordMode = true;
    private final String TAG = RegisterActivity.class.getName();

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_register;
    }

    @Override
    protected Class<VMRegister> getViewModelClass() {
        return VMRegister.class;
    }

    @Override
    protected void initializeViews() {
        ScreenSecurityUtils.disableScreenshot(this);
        // 顶部栏
        ivBack = findViewById(R.id.iv_back);
        tvRegisterLink = findViewById(R.id.tv_register_link);

        // 标签切换
        tvVerificationTab = findViewById(R.id.tv_verification_tab);
        tvPasswordTab = findViewById(R.id.tv_password_tab);
        indicatorLine = findViewById(R.id.indicator_line);
        layoutVerification = findViewById(R.id.layout_verification);
        layoutPassword = findViewById(R.id.layout_password);

        // 输入框
        etPhone = findViewById(R.id.et_phone);
        etPhoneVerification = findViewById(R.id.et_phone_verification);
        etVerification = findViewById(R.id.et_verification);
        etPassword = findViewById(R.id.et_password);
        tvGetCode = findViewById(R.id.tv_get_code);
        ivTogglePassword = findViewById(R.id.iv_toggle_password);
        tvForgetPassword = findViewById(R.id.tv_forget_password);
        tvRemainFailCount = findViewById(R.id.tv_remain_fail_count);

        // 底部
        btnLogin = findViewById(R.id.btn_login);
        cbAgreement = findViewById(R.id.cb_agreement);
        progressBar = findViewById(R.id.progressBar);

        // 设置点击事件
        ivBack.setOnClickListener(v -> finish());
        tvRegisterLink.setOnClickListener(v -> {
            // 跳转到新的注册界面
            Intent intent = new Intent(this, RegisterNewActivity.class);
            startActivity(intent);
        });
        tvGetCode.setOnClickListener(v -> {
            // 检查是否同意协议
            if (!cbAgreement.isChecked()) {
                // 使用协议确认弹窗（带有可点击的链接）
                CommonDialog.showAgreementDialog(this, getString(R.string.login_user_agreement), getString(R.string.login_user_agreement_title),
                        new CommonDialog.OnDialogClickListener() {
                            @Override
                            public void onConfirm() {
                                cbAgreement.setChecked(true);
                                viewModel.sendVerificationCode(Constants.SCENE_LOGIN);
                            }

                            @Override
                            public void onCancel() {
                                // 用户点击不同意，不做任何操作
                            }
                        });
                return;
            }
            viewModel.sendVerificationCode(Constants.SCENE_LOGIN);
        });
        ivTogglePassword.setOnClickListener(v -> viewModel.togglePasswordVisibility());
        btnLogin.setOnClickListener(new LoginClickListener());

        // 标签切换点击事件
        tvVerificationTab.setOnClickListener(v -> switchToVerificationMode());
        tvPasswordTab.setOnClickListener(v -> switchToPasswordMode());

        // 忘记密码点击事件
        if (tvForgetPassword != null) {
            tvForgetPassword.setOnClickListener(v -> {
                Intent intent = new Intent(RegisterActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            });
        }

        // 协议点击事件
        findViewById(R.id.tv_cmcc_clause).setOnClickListener(v -> {
            WebViewActivity.start(this, Constants.CM_CONTACT_URL, getString(R.string.cm_certification_clause));
        });
        findViewById(R.id.tv_user_agreement).setOnClickListener(v -> {
            WebViewActivity.start(this, Constants.USER_AGREEMENT, getString(R.string.user_agreement));
        });

        findViewById(R.id.tv_privacy_policy).setOnClickListener(v -> {
            WebViewActivity.start(this, Constants.PRIVACY_POLICY_DETAILED, getString(R.string.privacy_policy));
        });

        // 默认显示密码登录模式
        switchToPasswordMode();
    }

    @Override
    protected void setupDataBinding() {
        // 双向绑定输入框 - 账号密码模式的手机号
        DataBindingUtils.bindEditText(etPhone, viewModel.getPhone(), this);
        // 验证码模式的手机号
        DataBindingUtils.bindEditText(etPhoneVerification, viewModel.getPhone(), this);
        DataBindingUtils.bindEditText(etVerification, viewModel.getVerificationCode(), this);
        DataBindingUtils.bindEditText(etPassword, viewModel.getPassword(), this);

        // 绑定按钮状态
        DataBindingUtils.bindEnabled(btnLogin, viewModel.getRegisterEnabled(), this);
        DataBindingUtils.bindEnabled(tvGetCode, viewModel.getCanGetCode(), this);

        // 绑定文本显示
        DataBindingUtils.bindTextView(tvGetCode, viewModel.getCountdownText(), this);

        // 绑定复选框
        cbAgreement.setOnCheckedChangeListener((buttonView, isChecked) ->
                viewModel.getAgreementChecked().set(isChecked));

        // 绑定密码可见性
        viewModel.getPasswordVisible().observe(this, visible -> {
            ivTogglePassword.setSelected(visible);
            if (visible) {
                etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            // 保持光标在末尾
            etPassword.setSelection(etPassword.length());
        });
    }

    @Override
    protected void setupObservers() {
        // 观察发送验证码的结果
        viewModel.getSendSmsResult().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean success) {
                if (null != success && success) {
                    tvRemainFailCount.setVisibility(View.INVISIBLE);
                }
            }
        });
        // 添加对registerEnabled的观察，用于调试
        viewModel.getRegisterEnabled().observeForever(enabled -> {
            Timber.tag("RegisterActivity").d("registerEnabled changed to: " + enabled);
        });

        // 观察验证码登录结果
        viewModel.getLoginBySmsResult().observe(this, new Observer<LoginResponse>() {
            @Override
            public void onChanged(LoginResponse loginResponse) {
                if (null != loginResponse) {
                    if (TextUtils.isEmpty(loginResponse.getToken())) {
                        // 登录失败，提示剩余验证次数
                        int remainFailCount = loginResponse.getRemainFailCount();
                        // 应产品要求，不显示剩余验证次数提示
//                        tvRemainFailCount.setVisibility(View.VISIBLE);
//                        tvRemainFailCount.setText("验证码不正确，剩余验证" + remainFailCount + "次！");
                    } else {
                        // 登录成功，直接跳转到主界面
//                        Intent intent = new Intent(getBaseContext(), MainActivity.class);
//                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                        startActivity(intent);
//                        finish();
                        JumpParameterManager.INSTANCE.jumpTargetPage(RegisterActivity.this);
                        finish();
                    }
                }
            }
        });

        // 观察注册结果
        viewModel.getRegisterResult().observe(this, success -> {
            if (success != null && success) {
                // 登录成功，直接跳转到主界面
//                Intent intent = new Intent(this, MainActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                startActivity(intent);
//                finish();
                JumpParameterManager.INSTANCE.jumpTargetPage(RegisterActivity.this);
                finish();
            }
        });

        viewModel.getDialogMessage().observe(this, message -> {
            if (!TextUtils.isEmpty(message)) {
                CommonDialog.showConfirmDialog(RegisterActivity.this, "提示", message, "确定", null);
            }
        });
    }

    @Override
    protected void handleLoadingState(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    /**
     * 切换到验证码登录模式
     */
    private void switchToVerificationMode() {
        isPasswordMode = false;
        viewModel.setLoginMode(LoginMode.VerificationMode);

        // 更新标签样式
        tvVerificationTab.setTextColor(getColor(R.color.account_text_primary));
        tvVerificationTab.setTypeface(null, android.graphics.Typeface.BOLD);
        tvPasswordTab.setTextColor(getColor(android.R.color.darker_gray));
        tvPasswordTab.setTypeface(null, android.graphics.Typeface.NORMAL);

        // 移动指示线到验证码下方
        // 使用post确保视图已经布局完成
        tvVerificationTab.post(() -> {
            int tabX = tvVerificationTab.getLeft();
            int tabWidth = tvVerificationTab.getWidth();
            int indicatorWidth = indicatorLine.getWidth();
            // 计算使指示线居中的x坐标
            int centerX = tabX + (tabWidth - indicatorWidth) / 2;

            indicatorLine.animate()
                    .x(centerX)
                    .setDuration(200)
                    .start();
        });

        // 切换布局
        layoutVerification.setVisibility(View.VISIBLE);
        layoutPassword.setVisibility(View.GONE);

        // 清空密码输入
        viewModel.getPassword().set("");
    }

    /**
     * 切换到账号密码登录模式
     */
    private void switchToPasswordMode() {
        isPasswordMode = true;
        viewModel.setLoginMode(LoginMode.Password);

        // 更新标签样式
        tvPasswordTab.setTextColor(getColor(R.color.account_text_primary));
        tvPasswordTab.setTypeface(null, android.graphics.Typeface.BOLD);
        tvVerificationTab.setTextColor(getColor(android.R.color.darker_gray));
        tvVerificationTab.setTypeface(null, android.graphics.Typeface.NORMAL);

        // 移动指示线到账号密码下方
        // 使用post确保视图已经布局完成
        tvPasswordTab.post(() -> {
            int tabX = tvPasswordTab.getLeft();
            int tabWidth = tvPasswordTab.getWidth();
            int indicatorWidth = indicatorLine.getWidth();
            // 计算使指示线居中的x坐标
            int centerX = tabX + (tabWidth - indicatorWidth) / 2;

            indicatorLine.animate()
                    .x(centerX)
                    .setDuration(200)
                    .start();
        });

        // 切换布局
        layoutPassword.setVisibility(View.VISIBLE);
        layoutVerification.setVisibility(View.GONE);

        // 清空验证码输入
        viewModel.getVerificationCode().set("");
    }

    /**
     * 执行登录操作
     */
    private void performLogin() {
        // 检查是否同意协议
        if (!cbAgreement.isChecked()) {
            // 使用协议确认弹窗（带有可点击的链接）
            CommonDialog.showAgreementDialog(this, getString(R.string.login_user_agreement), getString(R.string.login_user_agreement_title),
                    new CommonDialog.OnDialogClickListener() {
                        @Override
                        public void onConfirm() {
                            // 用户点击同意，自动勾选协议并继续登录
                            cbAgreement.setChecked(true);
                            performLogin();
                        }

                        @Override
                        public void onCancel() {
                            // 用户点击不同意，不做任何操作
                        }
                    });
            return;
        }

        // 如果是密码模式，进行密码校验
        if (isPasswordMode){
            String passwordError = UserUtil.verifyPassword(viewModel.getPassword().get());
            if (passwordError != null) {
                GlobalToast.show(this, passwordError, GlobalToast.Type.ERROR);
                return;
            }
        }
        commonDeviceAuth(new DeviceAuthCallback() {//鉴权设备
            @Override
            public void onSuccess(String msg) {
                Timber.tag(TAG).d("获取deviceId = 成功%s", msg);
                if (isPasswordMode) {
                    viewModel.loginByPassword();
                } else {
                    viewModel.loginBySms();
                }
            }

            @Override
            public void onFail(String msg) {
                Timber.tag(TAG).d("获取deviceId = 失败");
                GlobalToast.show(RegisterActivity.this,"设备鉴权失败",GlobalToast.Type.ERROR);
            }
        });
    }

    private class LoginClickListener extends NoMultiClickListener {

        @Override
        public void onNoMultiClick(View v) {
            performLogin();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ScreenSecurityUtils.enableScreenshot(this);
    }
}