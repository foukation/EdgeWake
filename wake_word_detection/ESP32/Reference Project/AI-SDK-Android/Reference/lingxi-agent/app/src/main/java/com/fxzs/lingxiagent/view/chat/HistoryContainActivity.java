package com.fxzs.lingxiagent.view.chat;

import android.content.Intent;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.viewmodel.chat.VMChat;
import com.fxzs.lingxiagent.viewmodel.history.VMHistory;


public class HistoryContainActivity extends BaseActivity {


    int default_tab = VMHistory.TAB_CHAT;
    private boolean is_tab_hide = false;

    @Override
    protected int getLayoutResource() {
        return R.layout.act_history_container;
    }

    @Override
    protected Class getViewModelClass() {
        return VMChat.class;
    }

    @Override
    protected void setupDataBinding() {

    }

    @Override
    protected void initializeViews() {
        init();
    }



    public void init() {

        ZUtils.setStatusBarWhite(this);
        findViewById(R.id.back).setOnClickListener(view -> finish());

        if (getIntent() != null) {
            default_tab = getIntent().getIntExtra("default_tab",VMHistory.TAB_CHAT);
            is_tab_hide = getIntent().getBooleanExtra("is_tab_hide",false);
        }


            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();

        HistoryBottomSheetFragment bottomSheet = HistoryBottomSheetFragment.newInstance(default_tab,is_tab_hide);
            transaction.add(R.id.fragment_container, bottomSheet);
            transaction.commit();

    }
    @Override
    protected void setupObservers() {
        // 监听 ViewModel 的 LiveData

    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();


    }
}
