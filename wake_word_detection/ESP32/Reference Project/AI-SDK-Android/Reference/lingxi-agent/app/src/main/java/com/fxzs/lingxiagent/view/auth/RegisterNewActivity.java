package com.fxzs.lingxiagent.view.auth;

import android.content.Intent;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.fxzs.lingxiagent.JumpParameterManager;
import com.fxzs.lingxiagent.MainActivity;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.auth.dto.LoginMode;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.model.user.UserUtil;
import com.fxzs.lingxiagent.util.ScreenSecurityUtils;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.common.CommonDialog;
import com.fxzs.lingxiagent.view.common.DataBindingUtils;
import com.fxzs.lingxiagent.view.common.GlobalToast;
import com.fxzs.lingxiagent.view.common.NoMultiClickListener;
import com.fxzs.lingxiagent.view.common.WebViewActivity;
import com.fxzs.lingxiagent.viewmodel.auth.VMRegister;

import timber.log.Timber;

public class RegisterNewActivity extends BaseActivity<VMRegister> {
    
    // UI组件
    private ImageView ivBack;
    private EditText etPhone;
    private EditText etVerification;
    private EditText etPassword;
    private TextView tvGetCode;
    private ImageView ivTogglePassword;
    private Button btnRegister;
    private CheckBox cbAgreement;
    private ProgressBar progressBar;
    private final String TAG = RegisterNewActivity.class.getName();
    @Override
    protected int getLayoutResource() {
        return R.layout.activity_register_new;
    }
    
    @Override
    protected Class<VMRegister> getViewModelClass() {
        return VMRegister.class;
    }
    
    @Override
    protected void initializeViews() {
        ScreenSecurityUtils.disableScreenshot(this);
        // 初始化视图
        ivBack = findViewById(R.id.iv_back);
        etPhone = findViewById(R.id.et_phone);
        etVerification = findViewById(R.id.et_verification);
        etPassword = findViewById(R.id.et_password);
        tvGetCode = findViewById(R.id.tv_get_code);
        ivTogglePassword = findViewById(R.id.iv_toggle_password);
        btnRegister = findViewById(R.id.btn_register);
        cbAgreement = findViewById(R.id.cb_agreement);
        progressBar = findViewById(R.id.progressBar);
        
        // 设置点击事件
        ivBack.setOnClickListener(v -> finish());
        
        tvGetCode.setOnClickListener(v -> {
            if (!cbAgreement.isChecked()) {
                // 使用协议确认弹窗（带有可点击的链接）
                CommonDialog.showAgreementDialog(this,getString(R.string.login_user_agreement),getString(R.string.login_user_agreement_title),
                        new CommonDialog.OnDialogClickListener() {
                            @Override
                            public void onConfirm() {
                                cbAgreement.setChecked(true);
                                viewModel.sendVerificationCode(Constants.SCENE_REGISTER);
                            }

                            @Override
                            public void onCancel() {
                                // 用户点击不同意，不做任何操作
                            }
                        });
                return;
            }
            viewModel.sendVerificationCode(Constants.SCENE_REGISTER);
        });
        
        ivTogglePassword.setOnClickListener(v -> {
            viewModel.togglePasswordVisibility();
        });
        
        btnRegister.setOnClickListener(new NoMultiClickListener() {
            @Override
            public void onNoMultiClick(View v) {
                String password = etPassword.getText().toString().trim();

                // 密码校验
                String passwordError = UserUtil.verifyPassword(password);
                if (passwordError != null) {
                    GlobalToast.show(RegisterNewActivity.this, passwordError, GlobalToast.Type.ERROR);
                    return;
                }

                if (!cbAgreement.isChecked()) {
                    // 使用协议确认弹窗（带有可点击的链接）
                    CommonDialog.showAgreementDialog(RegisterNewActivity.this, getString(R.string.login_user_agreement), getString(R.string.login_user_agreement_title),
                            new CommonDialog.OnDialogClickListener() {
                                @Override
                                public void onConfirm() {
                                    // 用户点击同意，自动勾选协议并继续注册
                                    cbAgreement.setChecked(true);
                                    deviceAuth();
                                }

                                @Override
                                public void onCancel() {
                                    // 用户点击不同意，不做任何操作
                                }
                            });
                    return;
                }

                deviceAuth();
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

        viewModel.setLoginMode(LoginMode.Register);
    }
    
    @Override
    protected void setupDataBinding() {
        // 设置双向数据绑定
        DataBindingUtils.bindEditText(etPhone, viewModel.getPhone(), this);
        DataBindingUtils.bindEditText(etVerification, viewModel.getVerificationCode(), this);
        DataBindingUtils.bindEditText(etPassword, viewModel.getPassword(), this);
        DataBindingUtils.bindCheckBox(cbAgreement, viewModel.getAgreementChecked(), this);
        DataBindingUtils.bindButtonEnabled(btnRegister, viewModel.getRegisterEnabled(), this);
    }
    
    @Override
    protected void setupObservers() {
        // 观察加载状态
        viewModel.getLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // 观察验证码倒计时文本
        viewModel.getCountdownText().observe(this, text -> {
            tvGetCode.setText(text);
        });
        
        // 观察是否可以获取验证码
        viewModel.getCanGetCode().observe(this, canGet -> {
            tvGetCode.setEnabled(canGet);
        });
        
        // 观察密码可见性
        viewModel.getPasswordVisible().observe(this, visible -> {
            ivTogglePassword.setSelected(visible);
            if (visible) {
                etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            etPassword.setSelection(etPassword.getText().length());
        });
        
        // 观察注册结果
        viewModel.getRegisterResult().observe(this, success -> {
            if (success != null && success) {
                // 跳转到主界面
//                Intent intent = new Intent(this, MainActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                startActivity(intent);
//                finish();
                JumpParameterManager.INSTANCE.jumpTargetPage(RegisterNewActivity.this);
                finish();
            }
        });
    }

    private void deviceAuth(){
        commonDeviceAuth(new DeviceAuthCallback() {//鉴权设备
            @Override
            public void onSuccess(String msg) {
                Timber.tag(TAG).d("获取deviceId = 成功%s", msg);
                viewModel.performRegister();
            }

            @Override
            public void onFail(String msg) {
                Timber.tag(TAG).d("获取deviceId = 失败");
                GlobalToast.show(RegisterNewActivity.this,"设备鉴权失败",GlobalToast.Type.ERROR);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ScreenSecurityUtils.enableScreenshot(this);
    }
}