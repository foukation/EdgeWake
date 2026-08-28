package com.fxzs.lingxiagent.lingxi.float_manager;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.view.common.GlobalToast;

import timber.log.Timber;

/**
 * 创建者：ZyOng
 * 描述：
 * 创建时间：2026/1/27 14:57
 */
public class FloatingWindowManager {

    private static FloatingWindowManager instance;

    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private View floatView;
    private WakeVoice2 wakeVoice;
    private Context context;
    private ConstraintLayout clParent;
    private WakeVoiceCallback callback;

    private FloatingWindowManager(Context context) {
        this.context = context.getApplicationContext();
        windowManager = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);
        initLayoutParams();
    }

    public static FloatingWindowManager getInstance(Context context) {
        if (instance == null) {
            synchronized (FloatingWindowManager.class) {
                if (instance == null) {
                    instance = new FloatingWindowManager(context);
                }
            }
        }
        return instance;
    }

    /**
     * 初始化参数
     */
    private void initLayoutParams() {
        int height = (int) context.getResources().getDimension(R.dimen.dp_100);
        int width = (int) context.getResources().getDimension(R.dimen.dp_343);
        layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );

        layoutParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
//        layoutParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
        layoutParams.gravity = Gravity.BOTTOM | Gravity.CENTER;
    }

    /**
     * 显示悬浮窗
     */
    public void show() {
        if (floatView != null) return;

        floatView = LayoutInflater.from(context).inflate(R.layout.floating_wakeup, null);
        // 点击窗外关闭浮窗
        floatView.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_OUTSIDE) {
                hide();
                return true;
            }
            return false;
        });
        floatView.setOnClickListener(v ->
                hide()
        );
        wakeVoice = floatView.findViewById(R.id.nexus_voice);
        wakeVoice.setCallback(callback);
        windowManager.addView(floatView, layoutParams);

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            // 获取当前Activity的context来请求权限
            GlobalToast.show(context, "请先获取录音权限", GlobalToast.Type.ERROR,2000);
        }
    }

    public void startVoice(){
        if (wakeVoice != null){
            wakeVoice.startVoice();
        }

    }

    public void setCallback(WakeVoiceCallback callback) {
        // 设置回调接口
        this.callback = callback;
    }
    /**
     * 隐藏悬浮窗
     */
    public void hide() {
        if (floatView != null && windowManager != null) {
            try {
                windowManager.removeViewImmediate(floatView);
            } catch (Exception e) {
                Timber.tag("FloatWindow").w(e, "Failed to remove floating view");
            }
            floatView = null;
        }
        if (wakeVoice != null){
            wakeVoice.release();
            wakeVoice = null;
        }


    }


    /**
     * 是否已经显示
     */
    public boolean isShowing() {
        return floatView != null;
    }


}

