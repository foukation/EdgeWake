package com.fxzs.lingxiagent.lingxi.main.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fxzs.lingxiagent.R;

public class CustomToast {

    /**
     * 展示只有文字的toast
     */
    public static Toast showToast(Context context, String msg, int duration) {
        return showToastDuration(context, msg, -1, duration, false, false);
    }

    /**
     * 展示图标 +文字的toast
     */
    public static Toast showToast(Context context, String msg, int imageviewID, int duration) {
        return showToastDuration(context, msg, imageviewID, duration, false, false);
    }

    /**
     * loading动画定制toast，如版本检测功能
     */
    public static Toast showLoadingToast(Context context, String msg, int imageviewID, int duration) {
        return showToastDuration(context, msg, imageviewID, duration, false, true);
    }

    /**
     * 喜欢定制toast，展示加入收藏功能
     */
    public static Toast showLikeToast(Context context, String msg, int imageviewID, int duration) {
        return showToastDuration(context, msg, imageviewID, duration, true, false);
    }

    private static Toast showToastDuration(Context context, String msg, int imageviewID, int duration, boolean showCollect, boolean showLoading) {
        Toast toast = new Toast(context);
        int yOffset = context.getResources().getDimensionPixelSize(R.dimen.dp_146);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, yOffset);
        @SuppressLint("InflateParams") View view = LayoutInflater.from(context).inflate(R.layout.toast_view, null);
        TextView tvMessage = view.findViewById(R.id.toast_message);
        tvMessage.setText(msg);
        if(imageviewID != -1) {
            ImageView icon = view.findViewById(R.id.toast_imageview);
            icon.setVisibility(View.VISIBLE);
            icon.setImageResource(imageviewID);
            if(showLoading) {
                AnimationDrawable animationDrawable = (AnimationDrawable) icon.getDrawable();
                // 开始动画
                animationDrawable.start();
            }
        }
        ImageView divider_collect = view.findViewById(R.id.divider_collect);
        TextView toast_message_collect = view.findViewById(R.id.toast_message_collect);
        if(showCollect) {
            divider_collect.setVisibility(View.VISIBLE);
            toast_message_collect.setVisibility(View.VISIBLE);
        }
        toast.setView(view);
        toast.setDuration(duration);
        return toast;
    }
}
