package com.fxzs.lingxiagent.view.meeting;

import android.content.Intent;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.fxzs.lingxiagent.JumpParameterManager;
import com.fxzs.lingxiagent.MainActivity;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.audio.TTSManager;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.viewmodel.chat.VMChat;

import timber.log.Timber;


public class MeetingContainActivity extends BaseActivity {

    MeetingFragment meetingFragment;


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

        findViewById(R.id.back).setOnClickListener(view -> backToMain());



            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            meetingFragment = new MeetingFragment();
            transaction.add(R.id.fragment_container, meetingFragment);
            transaction.commit();

    }
    private void backToMain() {
        if (JumpParameterManager.INSTANCE.isMainActivityInStack(this)) {
            // 存在 → 直接 finish，系统自动返回动画
            finish();
        } else {
            // 不存在 → 跳 Main，用系统返回动画
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            finish();
        }
    }
    @Override
    protected void setupObservers() {
        // 监听 ViewModel 的 LiveData

    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Timber.tag("SuperChatContainActivity").d( "onActivityResult called - requestCode: " + requestCode + ", resultCode: " + resultCode);
        if(meetingFragment != null){
            meetingFragment.onActivityResult( requestCode,  resultCode,  data);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        // 切换界面时停止TTS播放
        TTSManager.Companion.getInstance().stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 确保销毁时停止TTS播放
        TTSManager.Companion.getInstance().stop();
    }
}
