package com.fxzs.lingxiagent.lingxi.translate;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.fxzs.lingxiagent.JumpParameterManager;
import com.fxzs.lingxiagent.MainActivity;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.AppPermissionRequestManager;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.viewmodel.translate.VMSimultaneousTranslate;

/**
 * 同声传译主页面
 * 提供聆听模式和对话模式两种翻译模式的入口
 */
public class SimultaneousTranslateActivity extends BaseActivity<VMSimultaneousTranslate> {

    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 1;
    private ImageView ivBack;
    private LinearLayout llListenMode;
    private LinearLayout llDialogMode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_simultaneous_translate;
    }

    @Override
    protected Class<VMSimultaneousTranslate> getViewModelClass() {
        return VMSimultaneousTranslate.class;
    }

    @Override
    protected void setupDataBinding() {
        // 暂不需要数据绑定
    }

    @Override
    protected void initializeViews() {
        ivBack = findViewById(R.id.iv_back);
        llListenMode = findViewById(R.id.ll_listen_mode);
        llDialogMode = findViewById(R.id.ll_dialog_mode);

        // 返回按钮点击事件
        ivBack.setOnClickListener(v -> backToMain());

        // 聆听模式点击事件
        llListenMode.setOnClickListener(v -> {
            Intent intent = new Intent(this, ListenModeActivity.class);
            startActivity(intent);
        });

        // 对话模式点击事件
        llDialogMode.setOnClickListener(v -> {
            Intent intent = new Intent(this, DialogModeActivity.class);
            startActivity(intent);
        });
        checkAudioPermission();
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
        // 暂不需要观察者
    }

    @Override
    protected void handleLoadingState(boolean loading) {
        // 暂不需要加载状态处理
    }


    private void checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            AppPermissionRequestManager.requestAudioPermission(this, PERMISSION_REQUEST_RECORD_AUDIO,AppPermissionRequestManager.PERMISSION_AUDIO_MESSAGE_SI);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                ZUtils.showToast("需要录音权限才能使用功能");
            }
        }


    }
}