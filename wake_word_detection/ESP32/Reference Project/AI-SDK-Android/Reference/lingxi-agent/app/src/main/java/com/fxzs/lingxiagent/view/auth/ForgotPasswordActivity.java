package com.fxzs.lingxiagent.view.auth;

import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.util.ScreenSecurityUtils;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.common.CommonDialog;
import com.fxzs.lingxiagent.view.common.DataBindingUtils;
import com.fxzs.lingxiagent.view.common.NoMultiClickListener;
import com.fxzs.lingxiagent.view.common.WebViewActivity;
import com.fxzs.lingxiagent.viewmodel.auth.VMForgotPassword;

public class ForgotPasswordActivity extends BaseActivity<VMForgotPassword> {
    private ImageView ivBack;
    private EditText etPhone;
    private EditText etVerification;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private TextView tvGetCode;
    private ImageView ivTogglePassword;
    private ImageView ivToggleConfirmPassword;
    private Button btnResetPassword;
    private ProgressBar progressBar;
    private CheckBox cbAgreement;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_forgot_password;
    }

    @Override
    protected Class<VMForgotPassword> getViewModelClass() {
        return VMForgotPassword.class;
    }

    @Override
    protected void initializeViews() {
        ScreenSecurityUtils.disableScreenshot(this);
        ivBack = findViewById(R.id.iv_back);
        etPhone = findViewById(R.id.et_phone);
        etVerification = findViewById(R.id.et_verification);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        tvGetCode = findViewById(R.id.tv_get_code);
        ivTogglePassword = findViewById(R.id.iv_toggle_password);
        ivToggleConfirmPassword = findViewById(R.id.iv_toggle_confirm_password);
        btnResetPassword = findViewById(R.id.btn_reset_password);
        progressBar = findViewById(R.id.progressBar);
        cbAgreement = findViewById(R.id.cb_agreement);

        ivBack.setOnClickListener(v -> finish());
        tvGetCode.setOnClickListener(v -> {
            // 检查是否同意协议
            if (!cbAgreement.isChecked()) {
                // 使用协议确认弹窗（带有可点击的链接）
                CommonDialog.showAgreementDialog(this,getString(R.string.login_user_agreement),getString(R.string.login_user_agreement_title),
                        new CommonDialog.OnDialogClickListener() {
                            @Override
                            public void onConfirm() {
                                cbAgreement.setChecked(true);
                                viewModel.sendVerificationCode();
                            }

                            @Override
                            public void onCancel() {
                            }
                        });
                return;
            }
            viewModel.sendVerificationCode();
        });
        ivTogglePassword.setOnClickListener(v -> viewModel.togglePasswordVisibility());
        ivToggleConfirmPassword.setOnClickListener(v -> viewModel.toggleConfirmPasswordVisibility());
        btnResetPassword.setOnClickListener(new NoMultiClickListener() {
            @Override
            public void onNoMultiClick(View v) {
                // 检查是否同意协议
                if (!cbAgreement.isChecked()) {
                    // 使用协议确认弹窗（带有可点击的链接）
                    CommonDialog.showAgreementDialog(ForgotPasswordActivity.this,getString(R.string.login_user_agreement),getString(R.string.login_user_agreement_title),
                            new CommonDialog.OnDialogClickListener() {
                                @Override
                                public void onConfirm() {
                                    cbAgreement.setChecked(true);
                                    viewModel.performPasswordReset();
                                }

                                @Override
                                public void onCancel() {
                                }
                            });
                    return;
                }
                viewModel.performPasswordReset();
            }
        });

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
    }

    @Override
    protected void setupDataBinding() {
        DataBindingUtils.bindEditText(etPhone, viewModel.getPhone(), this);
        DataBindingUtils.bindEditText(etVerification, viewModel.getVerificationCode(), this);
        DataBindingUtils.bindEditText(etPassword, viewModel.getNewPassword(), this);
        DataBindingUtils.bindEditText(etConfirmPassword, viewModel.getConfirmPassword(), this);
        DataBindingUtils.bindTextView(tvGetCode, viewModel.getCountdownText(), this);
    }

    @Override
    protected void setupObservers() {
        // 观察是否可以获取验证码
        viewModel.getCanGetCode().observe(this, canGet -> {
            tvGetCode.setEnabled(canGet);
        });
        // 密码可见性
        viewModel.getPasswordVisible().observe(this, visible -> {
            if (visible) {
                etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                ivTogglePassword.setSelected(true);
            } else {
                etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                ivTogglePassword.setSelected(false);
            }
            etPassword.setSelection(etPassword.length());
        });
        viewModel.getConfirmPasswordVisible().observe(this, visible -> {
            if (visible) {
                etConfirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                ivToggleConfirmPassword.setSelected(true);
            } else {
                etConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                ivToggleConfirmPassword.setSelected(false);
            }
            etConfirmPassword.setSelection(etConfirmPassword.length());
        });
        // 按钮高亮/变灰联动，enabled+alpha
        viewModel.getNextEnabled().observe(this, enabled -> {
            btnResetPassword.setEnabled(enabled != null && enabled);
        });
        // 重置成功关闭页面
        viewModel.getResetResult().observe(this, success -> {
            if (success != null && success) {
                finish();
            }
        });
    }

    @Override
    protected void handleLoadingState(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ScreenSecurityUtils.enableScreenshot(this);
    }
}