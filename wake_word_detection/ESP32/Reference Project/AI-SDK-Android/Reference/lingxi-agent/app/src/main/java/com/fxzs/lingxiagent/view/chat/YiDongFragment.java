package com.fxzs.lingxiagent.view.chat;

import android.view.View;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.view.common.BaseFragment;
import com.fxzs.lingxiagent.viewmodel.agent.VMAgent;

public class YiDongFragment extends BaseFragment<VMAgent> {


    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_yidong;
    }

    @Override
    protected Class<VMAgent> getViewModelClass() {
        return VMAgent.class;
    }

    @Override
    protected void initializeViews(View view) {
    }

    @Override
    protected void setupDataBinding() {
        // 设置数据绑定
    }

    @Override
    protected void setupObservers() {
        // 观察数据变化
    }


}