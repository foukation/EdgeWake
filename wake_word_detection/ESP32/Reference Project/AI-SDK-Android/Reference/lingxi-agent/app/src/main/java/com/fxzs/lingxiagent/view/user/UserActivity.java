package com.fxzs.lingxiagent.view.user;

import android.view.View;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.viewmodel.user.VMUserProfile;

/**
 * 创建者：ZyOng
 * 描述：
 * 创建时间：2025/8/6 上午9:09
 */
public class UserActivity extends BaseActivity<VMUserProfile> {



    @Override
    protected int getLayoutResource() {
        return R.layout.activity_user;
    }

    @Override
    protected Class<VMUserProfile> getViewModelClass() {
        return VMUserProfile.class;
    }

    @Override
    protected void setupDataBinding() {

    }

    @Override
    protected void initializeViews() {

        findViewById(R.id.iv_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void setupObservers() {

    }
}
