package com.fxzs.lingxiagent.view.user;

import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.common.CommonDialog;
import com.fxzs.lingxiagent.view.common.DataBindingUtils;
import com.fxzs.lingxiagent.view.common.NoMultiClickListener;
import com.fxzs.lingxiagent.view.common.WebViewActivity;
import com.fxzs.lingxiagent.viewmodel.user.VMChangeMobile;

public class ChangeMobileActivity extends BaseActivity<VMChangeMobile> {
    
    private ImageView ivBack;
    private EditText etMobile;
    private EditText etCode;
    private TextView btnSendCode;
    private Button btnConfirm;
    private TextView tvCurrentMobile;
    private CheckBox cbAgreement;
    @Override
    protected int getLayoutResource() {
        return R.layout.activity_change_mobile;
    }
    
    @Override
    protected Class<VMChangeMobile> getViewModelClass() {
        return VMChangeMobile.class;
    }
    
    @Override
    protected void initializeViews() {
        ivBack = findViewById(R.id.iv_back);
        etMobile = findViewById(R.id.et_mobile);
        etCode = findViewById(R.id.et_code);
        btnSendCode = findViewById(R.id.btn_send_code);
        btnConfirm = findViewById(R.id.btn_confirm);
        tvCurrentMobile = findViewById(R.id.tv_current_mobile);
        cbAgreement = findViewById(R.id.cb_agreement);

        // 设置点击事件
        ivBack.setOnClickListener(v -> finish());
        btnSendCode.setOnClickListener(v -> {
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
        btnConfirm.setOnClickListener(new NoMultiClickListener() {
            @Override
            public void onNoMultiClick(View v) {
                // 检查是否同意协议
                if (!cbAgreement.isChecked()) {
                    // 使用协议确认弹窗（带有可点击的链接）
                    CommonDialog.showAgreementDialog(ChangeMobileActivity.this,getString(R.string.login_user_agreement),getString(R.string.login_user_agreement_title),
                            new CommonDialog.OnDialogClickListener() {
                                @Override
                                public void onConfirm() {
                                    cbAgreement.setChecked(true);
                                    viewModel.changeMobile();
                                }

                                @Override
                                public void onCancel() {
                                    // 用户点击不同意，不做任何操作
                                }
                            });
                    return;
                }
                viewModel.changeMobile();
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
        // 双向绑定输入框
        DataBindingUtils.bindEditText(etMobile, viewModel.getMobile(), this);
        DataBindingUtils.bindEditText(etCode, viewModel.getVerificationCode(), this);
        
        // 绑定按钮状态
//        DataBindingUtils.bindEnabled(btnSendCode, viewModel.getSendCodeEnabled(), this);
        DataBindingUtils.bindEnabled(btnConfirm, viewModel.getConfirmEnabled(), this);
        
        // 绑定按钮文本
        DataBindingUtils.bindTextView(btnSendCode, viewModel.getSendCodeText(), this);
        
        // 绑定当前手机号显示
        DataBindingUtils.bindTextView(tvCurrentMobile, viewModel.getCurrentMobileText(), this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 设置状态栏颜色为白色，与背景一致，并保证内容不被遮挡
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(Color.parseColor("#FFFFFF"));
            getWindow().getDecorView().postDelayed(() -> {
                getWindow().setStatusBarColor(Color.parseColor("#FFFFFF"));
            }, 100);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }

    @Override
    protected void setupObservers() {
        // 观察修改成功事件
        viewModel.getChangeSuccess().observe(this, success -> {
            if (success != null && success) {
                showToast("手机号修改成功");
                setResult(RESULT_OK);
                finish();
            }
        });
    }
}