package com.fxzs.lingxiagent.view.user;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.common.DataBindingUtils;
import com.fxzs.lingxiagent.viewmodel.user.VMAccountSafety;

public class AccountSafetyActivity extends BaseActivity<VMAccountSafety> {
    
    private ImageView ivBack;
    private LinearLayout layoutChangePassword;
    private LinearLayout layoutChangeMobile;
    private LinearLayout layoutDeleteAccount;
    private TextView tvMobile;
    
    @Override
    protected int getLayoutResource() {
        return R.layout.activity_account_safety;
    }
    
    @Override
    protected Class<VMAccountSafety> getViewModelClass() {
        return VMAccountSafety.class;
    }
    
    @Override
    protected void initializeViews() {
        // 初始化控件
        ivBack = findViewById(R.id.iv_back);
        layoutChangePassword = findViewById(R.id.layout_change_password);
        layoutChangeMobile = findViewById(R.id.layout_change_mobile);
        layoutDeleteAccount = findViewById(R.id.layout_delete_account);
        tvMobile = findViewById(R.id.tv_mobile);
        
        // 设置点击事件
        ivBack.setOnClickListener(v -> finish());
        
        layoutChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChangePasswordActivity.class);
            startActivity(intent);
        });
        
        layoutChangeMobile.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChangeMobileActivity.class);
            startActivity(intent);
        });
        
        layoutDeleteAccount.setOnClickListener(v -> {
            Intent intent = new Intent(this, AccountCancellationActivity.class);
            startActivity(intent);
        });
    }
    
    @Override
    protected void setupDataBinding() {
        // 绑定手机号显示
        DataBindingUtils.bindTextView(tvMobile, viewModel.getMobile(), this);
    }
    
    @Override
    protected void setupObservers() {
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

        // 加载用户信息
        viewModel.loadUserInfo();
    }

}