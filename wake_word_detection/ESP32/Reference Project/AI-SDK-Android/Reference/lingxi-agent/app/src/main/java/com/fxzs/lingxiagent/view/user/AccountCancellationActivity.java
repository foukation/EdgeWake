package com.fxzs.lingxiagent.view.user;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.fxzs.lingxiagent.MainActivity;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.model.user.UserUtil;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.common.CommonDialog;
import com.fxzs.lingxiagent.view.common.DataBindingUtils;
import com.fxzs.lingxiagent.view.common.NoMultiClickListener;
import com.fxzs.lingxiagent.view.common.WebViewActivity;
import com.fxzs.lingxiagent.viewmodel.user.VMAccountCancellation;

import java.io.File;

import timber.log.Timber;

public class AccountCancellationActivity extends BaseActivity<VMAccountCancellation> {
    private ImageView ivBack;
    private LinearLayout btnCancelAccount;
    private ImageView cbAgreement, ivAvatar;
    private TextView tvAgreement, tvMobile, tvGetCode;
    private EditText etVerification;
    private boolean isAgreementChecked = false;
    
    @Override
    protected int getLayoutResource() {
        return R.layout.activity_account_cancellation;
    }
    
    @Override
    protected Class<VMAccountCancellation> getViewModelClass() {
        return VMAccountCancellation.class;
    }
    
    @Override
    protected void initializeViews() {
        ivBack = findViewById(R.id.iv_back);
        btnCancelAccount = findViewById(R.id.btn_cancel_account_container);
        cbAgreement = findViewById(R.id.cb_agreement);
        tvAgreement = findViewById(R.id.tv_agreement);
        ivAvatar = findViewById(R.id.iv_avatar);
        tvMobile = findViewById(R.id.tv_mobile);
        tvGetCode = findViewById(R.id.tv_get_code);
        etVerification = findViewById(R.id.et_verification);

        // 设置返回按钮
        ivBack.setOnClickListener(v -> finish());
        
        // 设置注销按钮初始状态
        if (btnCancelAccount != null) {
            btnCancelAccount.setEnabled(false);
            btnCancelAccount.setAlpha(0.6f);
        }
        
        // 设置协议文字点击
        setupAgreementText();
        // 设置获取验证码点击监听
        tvGetCode.setOnClickListener(view -> viewModel.sendVerificationCode());
        
        // 设置复选框监听
        cbAgreement.setOnClickListener(v -> {
            isAgreementChecked = !isAgreementChecked;
            updateCheckboxState();
            viewModel.getAgreementChecked().set(isAgreementChecked);
            // 调试日志
            Timber.tag("AccountCancellation").d( "Checkbox checked: " + isAgreementChecked);
        });
        
        // 设置注销按钮点击
        if (btnCancelAccount != null) {
            btnCancelAccount.setOnClickListener(new NoMultiClickListener() {
                @Override
                public void onNoMultiClick(View v) {
                    showCancellationConfirmDialog();
                }
            });
        }

        // 加载头像
        loadAvatarUrl();
    }

    private void loadAvatarUrl() {
        String avatarUrl = SharedPreferencesUtil.getUserAvatar();
        if (!avatarUrl.isEmpty()) {
            // 处理本地文件路径
            Object loadUrl = avatarUrl;
            if (avatarUrl.startsWith("file://")) {
                loadUrl = new File(avatarUrl.substring(7));
            }

            Glide.with(this)
                    .load(loadUrl)
                    .placeholder(R.drawable.ic_account_delete)
                    .error(R.drawable.ic_account_delete)
                    .transform(new CenterCrop(), new RoundedCorners(UserUtil.dp2px(this, 12.8f)))
                    .into(ivAvatar);
        }
    }
    
    @Override
    protected void setupDataBinding() {
        // 双向绑定
        DataBindingUtils.bindTextView(tvMobile, viewModel.getPhone(), this);
        DataBindingUtils.bindTextView(tvGetCode, viewModel.getCountdownText(), this);
        DataBindingUtils.bindEditText(etVerification, viewModel.getVerificationCode(), this);
    }
    
    @Override
    protected void setupObservers() {
        // 加载用户信息
        viewModel.loadUserPhone();
        // 观察是否可以获取验证码
        viewModel.getCanGetCode().observe(this, canGet -> {
            tvGetCode.setEnabled(canGet);
        });
        // 按钮高亮/变灰联动，enabled+alpha
        viewModel.getNextEnabled().observe(this, enabled -> {
            btnCancelAccount.setEnabled(enabled);
            btnCancelAccount.setAlpha(enabled ? 1.0f : 0.6f);
        });
        // 观察注销结果
        viewModel.getCancellationResult().observe(this, success -> {
            if (success != null && success) {
                // 清除用户数据并跳转到登录页
                viewModel.clearUserData();
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

    }
    
    private void setupAgreementText() {
        String fullText = "阅读并同意【灵犀 账号注销协议】";
        SpannableString spannableString = new SpannableString(fullText);
        
        int start = fullText.indexOf("【");
        int end = fullText.indexOf("】") + 1;
        
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                showAgreementDialog();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
                ds.setColor(Color.parseColor("#1890FF"));
            }
        };
        
        spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvAgreement.setText(spannableString);
        tvAgreement.setMovementMethod(LinkMovementMethod.getInstance());
        // 移除点击高亮效果
        tvAgreement.setHighlightColor(Color.TRANSPARENT);
    }
    
    private void showAgreementDialog() {
        // 跳转到WebView页面显示注销协议
        WebViewActivity.start(this, Constants.UNSUBSCRIBE_AGREEMENT_URL, "灵犀账号注销协议");
    }
    
    private void showCancellationConfirmDialog() {
        CommonDialog.showAccountDeletionDialog(this, new CommonDialog.OnDialogClickListener() {
            @Override
            public void onConfirm() {
                viewModel.cancelAccount();
            }

            @Override
            public void onCancel() {
                // 用户取消，不做任何操作
            }
        });
    }
    
    private void updateCheckboxState() {
        cbAgreement.setImageResource(isAgreementChecked ?
                R.drawable.ic_checkbox_checked : R.drawable.ic_checkbox_unchecked);
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
}