package com.fxzs.lingxiagent.view.drawing;

import android.content.Intent;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.viewmodel.chat.VMChat;

import timber.log.Timber;


public class DrawingContainActivity extends BaseActivity {

    DrawingNewFragment drawingNewFragment;


    @Override
    protected int getLayoutResource() {
        return R.layout.act_drawing_container;
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
            drawingNewFragment = new DrawingNewFragment();
            transaction.add(R.id.fragment_container, drawingNewFragment);
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
        if(drawingNewFragment != null){
            drawingNewFragment.onActivityResult( requestCode,  resultCode,  data);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();


    }
}
