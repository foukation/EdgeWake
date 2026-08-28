package com.fxzs.lingxiagent.view.user;

import android.content.Intent;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.viewmodel.chat.VMChat;

import timber.log.Timber;


public class UserContainActivity extends BaseActivity {

    UserFragment userFragment;


    @Override
    protected int getLayoutResource() {
        return R.layout.act_super_chat_container;
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

        findViewById(R.id.back).setOnClickListener(view -> finish());



            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            userFragment = new UserFragment();
            transaction.add(R.id.fragment_container, userFragment);
            transaction.commit();

    }
    @Override
    protected void setupObservers() {
        // 监听 ViewModel 的 LiveData

    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Timber.tag("SuperChatContainActivity").d( "onActivityResult called - requestCode: " + requestCode + ", resultCode: " + resultCode);
        if(userFragment != null){
            userFragment.onActivityResult( requestCode,  resultCode,  data);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();


    }
}
