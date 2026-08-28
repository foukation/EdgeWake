package com.fxzs.lingxiagent.view.common;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.fxzs.lingxiagent.R;

public class GlobalToast {
    private static String content = "";
    private static final Handler handler = new Handler(Looper.getMainLooper());
    public enum Type {
        SUCCESS, ERROR, NORMAL, LOADING
    }

    public static Toast show(@NonNull Activity activity, @NonNull String message, @NonNull Type type) {
        return show(activity, message, type, 2000);
    }

    public static Toast show(@NonNull Activity activity, @NonNull String message, @NonNull Type type, int durationMs) {
        if (content.equals(message)){
            return null;
        }
        content = message;
        Toast toast = new Toast(activity.getApplicationContext());
        activity.runOnUiThread(() -> {
            LayoutInflater inflater = activity.getLayoutInflater();
            View layout = inflater.inflate(R.layout.toast_global, null);
            ImageView ivIcon = layout.findViewById(R.id.iv_toast_icon);
            TextView tvMsg = layout.findViewById(R.id.tv_toast_message);
            tvMsg.setText(message);

            switch (type) {
                case SUCCESS:
                    ivIcon.setImageResource(R.drawable.ic_toast_succ);
                    ivIcon.setVisibility(View.VISIBLE);
                    break;
                case ERROR:
                    ivIcon.setImageResource(R.drawable.ic_toast_err);
                    ivIcon.setVisibility(View.VISIBLE);
                    break;
                case LOADING:
                    ivIcon.setImageResource(R.drawable.toast_loading);
                    ivIcon.setVisibility(View.VISIBLE);
                    try {
                        Animation rotateAnimation = AnimationUtils.loadAnimation(activity, R.anim.loading_rotation);
                        ivIcon.startAnimation(rotateAnimation);
                    } catch (Exception e) {
                        // 如果动画加载失败，忽略异常
                    }
                    break;
                case NORMAL:
                default:
                    ivIcon.setVisibility(View.GONE);
                    break;
            }

            toast.setView(layout);
            toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, getNavBarHeight(activity) + dp2px(activity, 12));
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.show();

            // 手动控制时长
//            if (durationMs > 2000) {
            handler.postDelayed(() -> {
                    toast.cancel();
                    content = "";
                }, durationMs);
//            }
        });
        return toast;
    }

    private static int getNavBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static Toast show(@NonNull Context context, @NonNull String message, @NonNull Type type, int durationMs) {
        if (content.equals(message)){
            return null;
        }
        content = message;
        Toast toast = new Toast(context);
        handler.post(() -> {
            LayoutInflater inflater = LayoutInflater.from(context.getApplicationContext());
            View layout = inflater.inflate(R.layout.toast_global, null);
            ImageView ivIcon = layout.findViewById(R.id.iv_toast_icon);
            TextView tvMsg = layout.findViewById(R.id.tv_toast_message);
            tvMsg.setText(message);

            switch (type) {
                case SUCCESS:
                    ivIcon.setImageResource(R.drawable.ic_toast_succ);
                    ivIcon.setVisibility(View.VISIBLE);
                    break;
                case ERROR:
                    ivIcon.setImageResource(R.drawable.ic_toast_err);
                    ivIcon.setVisibility(View.VISIBLE);
                    break;
                case LOADING:
                    ivIcon.setImageResource(R.drawable.toast_loading);
                    ivIcon.setVisibility(View.VISIBLE);
                    try {
                        Animation rotateAnimation = AnimationUtils.loadAnimation(context, R.anim.loading_rotation);
                        ivIcon.startAnimation(rotateAnimation);
                    } catch (Exception e) {
                        // 如果动画加载失败，忽略异常
                    }
                    break;
                case NORMAL:
                default:
                    ivIcon.setVisibility(View.GONE);
                    break;
            }

            toast.setView(layout);
            toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, getNavBarHeight(context) + dp2px(context, 12));
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.show();

            // 手动控制时长
//            if (durationMs > 2000) {
            handler.postDelayed(() -> {
                toast.cancel();
                content = "";
            }, durationMs);
//            }
        });
        return toast;
    }


    private static int dp2px(Context context, float dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }
} 