package com.fxzs.lingxiagent.view.user;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.fxzs.lingxiagent.MainActivity;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.util.ScreenSecurityUtils;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.common.CommonDialog;
import com.fxzs.lingxiagent.view.common.DataBindingUtils;
import com.fxzs.lingxiagent.view.common.NoMultiClickListener;
import com.fxzs.lingxiagent.view.common.WebViewActivity;
import com.fxzs.lingxiagent.viewmodel.user.VMChangePassword;

public class ChangePasswordActivity extends BaseActivity<VMChangePassword> {
//    private EditText etOldPassword;
    private EditText etNewPassword;
    private EditText etConfirmPassword;
    private Button btnConfirm;
    private ImageView ivToggleOldPwd, ivToggleNewPwd, ivToggleConfirmPwd;
    private EditText etPhone;
    private EditText etVerification;
    private TextView tvGetCode;
    private CheckBox cbAgreement;
    @Override
    protected int getLayoutResource() {
        return R.layout.activity_change_password;
    }
    
    @Override
    protected Class<VMChangePassword> getViewModelClass() {
        return VMChangePassword.class;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScreenSecurityUtils.disableScreenshot(this);
        setWhiteStatusBar();
    }
    
    @Override
    protected void initializeViews() {
        ImageView ivBack = findViewById(R.id.iv_back);
//        etOldPassword = findViewById(R.id.et_old_password);
//        ivToggleOldPwd = findViewById(R.id.iv_toggle_old_password);
        etPhone = findViewById(R.id.et_phone);
        etVerification = findViewById(R.id.et_verification);
        tvGetCode = findViewById(R.id.tv_get_code);
        etNewPassword = findViewById(R.id.et_new_password);
        ivToggleNewPwd = findViewById(R.id.iv_toggle_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        ivToggleConfirmPwd = findViewById(R.id.iv_toggle_confirm_password);
        btnConfirm = findViewById(R.id.btn_confirm);
        cbAgreement = findViewById(R.id.cb_agreement);

        // 设置点击事件
        ivBack.setOnClickListener(v -> finish());
//        ivToggleOldPwd.setOnClickListener(v -> viewModel.toggleOldPasswordVisibility());
        ivToggleNewPwd.setOnClickListener(v -> viewModel.toggleNewPasswordVisibility());
        ivToggleConfirmPwd.setOnClickListener(v -> viewModel.toggleConfirmPasswordVisibility());
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
                                // 用户点击不同意，不做任何操作
                            }
                        });
                return;
            }
            viewModel.sendVerificationCode();
        });
//        btnConfirm.setOnClickListener(v -> viewModel.changePassword());
        btnConfirm.setOnClickListener(new NoMultiClickListener() {
            @Override
            public void onNoMultiClick(View v) {
                // 检查是否同意协议
                if (!cbAgreement.isChecked()) {
                    // 使用协议确认弹窗（带有可点击的链接）
                    CommonDialog.showAgreementDialog(ChangePasswordActivity.this,getString(R.string.login_user_agreement),getString(R.string.login_user_agreement_title),
                            new CommonDialog.OnDialogClickListener() {
                                @Override
                                public void onConfirm() {
                                    cbAgreement.setChecked(true);
                                    viewModel.performPasswordReset();
                                }

                                @Override
                                public void onCancel() {
                                    // 用户点击不同意，不做任何操作
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
        // 双向绑定密码输入框
        DataBindingUtils.bindEditText(etNewPassword, viewModel.getNewPassword(), this);
        DataBindingUtils.bindEditText(etConfirmPassword, viewModel.getConfirmPassword(), this);
        
        // 绑定确认按钮状态
        DataBindingUtils.bindEnabled(btnConfirm, viewModel.getConfirmEnabled(), this);
        DataBindingUtils.bindTextView(tvGetCode, viewModel.getCountdownText(), this);
    }

    @Override
    protected void setupObservers() {
        // 密码可见性
//        viewModel.getOldPasswordVisible().observe(this, visible -> {
//            if (visible) {
//                etOldPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
//            } else {
//                etOldPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
//            }
//            ivToggleOldPwd.setSelected(visible);
//            etOldPassword.setSelection(etOldPassword.length());
//        });
        viewModel.getNewPasswordVisible().observe(this, visible -> {
            if (visible) {
                etNewPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                etNewPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            ivToggleNewPwd.setSelected(visible);
            etNewPassword.setSelection(etNewPassword.length());
        });
        viewModel.getConfirmPasswordVisible().observe(this, visible -> {
            if (visible) {
                etConfirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                etConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            ivToggleConfirmPwd.setSelected(visible);
            etConfirmPassword.setSelection(etConfirmPassword.length());
        });
        // 观察修改成功事件
        viewModel.getChangeSuccess().observe(this, success -> {
            if (success != null && success) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.putExtra("selected_tab", MainActivity.TAB_LINGXI); // 选中对话Tab
                startActivity(intent);

                finish();
            }
        });
        viewModel.getCanGetCode().observe(this, canGet -> {
            tvGetCode.setEnabled(canGet != null && canGet);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ScreenSecurityUtils.enableScreenshot(this);
    }
}