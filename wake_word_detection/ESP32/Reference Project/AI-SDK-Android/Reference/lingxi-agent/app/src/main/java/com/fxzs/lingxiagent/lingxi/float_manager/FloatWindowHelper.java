package com.fxzs.lingxiagent.lingxi.float_manager;

import android.Manifest;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.fxzs.lingxiagent.IYAApplication;
import com.fxzs.lingxiagent.MainActivity;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.WakeVoiceActivity;
import com.fxzs.lingxiagent.util.AppManager;
import com.fxzs.lingxiagent.util.NetworkUtils;
import com.fxzs.lingxiagent.view.agent.AgentContainActivity;

import timber.log.Timber;

public class FloatWindowHelper {
    private static final String TAG = "FloatWindowHelper";

    // 安全全局 Handler
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    // 悬浮窗单例
    private static FloatingWindowManager floatingWindowManager;

    // ------------------------------
    // 外部调用：显示悬浮窗
    // ------------------------------
    public static void showFloatWindow(Context context, WakeVoiceCallback callback) {
        if (floatingWindowManager == null) {
            floatingWindowManager = FloatingWindowManager.getInstance(context.getApplicationContext());
        }

        if (floatingWindowManager.isShowing()) {
            return;
        }

        floatingWindowManager.setCallback(callback);
        mainHandler.post(() -> {
            if (floatingWindowManager != null) {
                floatingWindowManager.show();
            }
        });
    }

    // ------------------------------
    // 外部调用：隐藏悬浮窗
    // ------------------------------
    public static void hideFloatWindow() {
        if (floatingWindowManager != null) {
            floatingWindowManager.hide();
        }
    }

    // ------------------------------
    // 释放资源（在 Application 销毁时调用）
    // ------------------------------
    public static void release() {
        hideFloatWindow();
        floatingWindowManager = null;
    }

    // ------------------------------
    // 判断是否正在显示
    // ------------------------------
    public static boolean isShowing() {
        return floatingWindowManager != null && floatingWindowManager.isShowing();
    }

    // ------------------------------
    // 默认回调（完整迁移了 WakeUpService 逻辑）
    // ------------------------------
    public static WakeVoiceCallback createDefaultCallback(Context context) {
        return new WakeVoiceCallback() {
            @Override
            public void onSuccessMsg(String content) {
                Timber.tag(TAG).d("语音识别成功：%s", content);
                mainHandler.post(() -> {
                    launchAppToForeground(context, content);
                    hideFloatWindow();
                });
            }

            @Override
            public void onErrorMsg(String error, String content, int code) {
                Timber.tag(TAG).e("语音识别失败：%s %s", error, content);
                mainHandler.post(() -> {
//                    if (code == AsrErrorCode.Companion.getERROR_TIMEOUT() ||
//                            code == AsrErrorCode.Companion.getERROR_NETWORK()) {
//                        Toast.makeText(context, "网络连接失败，请重试", Toast.LENGTH_SHORT).show();
//                        hideFloatWindow();
//                        return;
//                    }
//
//                    if (code == AsrErrorCode.Companion.getERROR_PERMISSION()) {
//                        Toast.makeText(context, "无录音权限", Toast.LENGTH_SHORT).show();
//                        hideFloatWindow();
//                        return;
//                    }


//                    if (floatingWindowManager != null) {
//                        Toast.makeText(context, "识别失败，请重试", Toast.LENGTH_SHORT).show();
//                        floatingWindowManager.startVoice();
//                    } else {
//                        Timber.tag(TAG).e("floatingWindowManager == null");
//                    }

                    boolean isNetworkAvailable = NetworkUtils.isNetworkAvailable(context);
                    if (!isNetworkAvailable) {
                        Toast.makeText(context, "网络连接失败，请重试", Toast.LENGTH_SHORT).show();
                    } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(context, "无录音权限", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "识别失败，请重试", Toast.LENGTH_SHORT).show();
                    }
                    hideFloatWindow();
                });
            }
        };
    }

    public static  void launchAppToForeground(Context context,String content) {
        try {
            Timber.tag(TAG).d("当前activity%s", IYAApplication.getInstance().getCurrentActivity());
            Intent intent = new Intent();
            if (IYAApplication.getInstance().getCurrentActivity() instanceof MainActivity){
                ((MainActivity) IYAApplication.getInstance().getCurrentActivity()).onReceiveFloatContent(content);
            }
            else if (IYAApplication.getInstance().getCurrentActivity() instanceof AgentContainActivity){
                ((AgentContainActivity) IYAApplication.getInstance().getCurrentActivity()).onReceiveFloatContent(content);
            }
            else if (IYAApplication.getInstance().getCurrentActivity() != null && AppManager.isActivityInStack(MainActivity.class)){
                Timber.tag(TAG).d("当前存在MainActivity");

                intent.setClass(context, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("formFloatContent", content);
                ActivityOptions options = ActivityOptions.makeCustomAnimation(
                        context,
                        R.anim.wake_enter,
                        R.anim.wake_exit
                );
                context.startActivity(intent, options.toBundle());
            }
            else {
                intent.setClass(context, WakeVoiceActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.putExtra("formFloatContent", content);
                ActivityOptions options = ActivityOptions.makeCustomAnimation(
                        context,
                        R.anim.wake_enter,
                        R.anim.wake_exit
                );
                context.startActivity(intent, options.toBundle());
            }


        } catch (Exception e) {
            Timber.tag(TAG).e("bring to front failed%s", e.getMessage());
        }
    }




}