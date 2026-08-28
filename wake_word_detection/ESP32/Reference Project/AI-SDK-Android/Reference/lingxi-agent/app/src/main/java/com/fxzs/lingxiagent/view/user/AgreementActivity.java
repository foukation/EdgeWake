package com.fxzs.lingxiagent.view.user;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.common.WebViewActivity;
import com.fxzs.lingxiagent.viewmodel.user.VMUserSettings;

public class AgreementActivity extends BaseActivity<VMUserSettings> {
    
    private ImageView ivBack;
    private LinearLayout rlAgreementPri1;
    private LinearLayout rlAgreementPri2;
    private LinearLayout rlAgreementUser;
    private LinearLayout rlAgreementShareList;
    private LinearLayout rlAgreementUserList;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_agreement_app;
    }

    @Override
    protected Class<VMUserSettings> getViewModelClass() {
         return VMUserSettings.class;
    }

    @Override
    protected void setupDataBinding() {

    }

    @Override
    protected void initializeViews() {
        // 设置状态栏颜色为白色，与背景一致，并保证内容不被遮挡
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(Color.parseColor("#FFFFFF"));
            getWindow().getDecorView().postDelayed(() -> {
                getWindow().setStatusBarColor(android.graphics.Color.parseColor("#FFFFFF"));
            }, 100);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
        // 初始化控件
        ivBack = findViewById(R.id.iv_back);
        rlAgreementPri1 = findViewById(R.id.rl_agreement_pri1);
        rlAgreementPri2 = findViewById(R.id.rl_agreement_pri2);
        rlAgreementUser = findViewById(R.id.rl_agreement_user);
        rlAgreementUserList = findViewById(R.id.rl_agreement_userlist);
        rlAgreementShareList = findViewById(R.id.rl_agreement_sharelist);
        // 设置点击事件
        ivBack.setOnClickListener(v -> finish());

        rlAgreementPri1.setOnClickListener(view -> {
            Intent intent = new Intent(view.getContext(), WebViewActivity.class);
            intent.putExtra("extra_url", Constants.PRIVACY_POLICY_DETAILED);
            intent.putExtra("extra_title", "隐私政策");
            startActivity(intent);
        });
        rlAgreementPri2.setOnClickListener(view -> {
            Intent intent = new Intent(view.getContext(), WebViewActivity.class);
            intent.putExtra("extra_url", Constants.PRIVACY_POLICY_SUMMARY);
            intent.putExtra("extra_title", "隐私政策");
            startActivity(intent);
        });
        rlAgreementUser.setOnClickListener(view -> {
            Intent intent = new Intent(view.getContext(), WebViewActivity.class);
            intent.putExtra("extra_url", Constants.USER_AGREEMENT);
            intent.putExtra("extra_title", "用户协议");
            startActivity(intent);
        });
        rlAgreementUserList.setOnClickListener(view -> {
            Intent intent = new Intent(view.getContext(), WebViewActivity.class);
            intent.putExtra("extra_url", Constants.PARTY_INFO_SHARE_LIST);
            intent.putExtra("extra_title", "收集个人信息明示清单");
            startActivity(intent);
        });
        rlAgreementShareList.setOnClickListener(view -> {
            Intent intent = new Intent(view.getContext(), WebViewActivity.class);
            intent.putExtra("extra_url", Constants.PERSONAL_INFO_LIST);
            intent.putExtra("extra_title", "第三方信息共享清单");
            startActivity(intent);
        });
    }
    
    @Override
    protected void setupObservers() {

    }
    

}