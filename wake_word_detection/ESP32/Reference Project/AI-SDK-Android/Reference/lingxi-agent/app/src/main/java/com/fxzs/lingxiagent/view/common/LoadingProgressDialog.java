package com.fxzs.lingxiagent.view.common;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.callback.RequestCallback;

public class LoadingProgressDialog {
    
    private Dialog dialog;
    private Context context;
    
    public LoadingProgressDialog(Context context) {
        this.context = context;
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_loading_progress);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setCancelable(false);
    }
    
    public LoadingProgressDialog setMessage(String message) {
        // 由于原始布局的TextView没有ID，我们通过遍历布局来查找TextView
        TextView messageView = dialog.findViewById(R.id.tv);
//                findTextViewInLayout();
        if (messageView != null) {
            messageView.setText(message);
        }
        return this;
    }
    public void setCancel(RequestCallback callback){
        LinearLayout ll_cancel = dialog.findViewById(R.id.ll_cancel);
        ll_cancel.setVisibility(View.VISIBLE);
        ll_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(callback != null){
                    callback.callback("");
                }
                dialog.dismiss();
            }
        });
    }
    
    private TextView findTextViewInLayout() {
        try {
            // 获取dialog的根布局
            android.view.ViewGroup rootView = (android.view.ViewGroup) dialog.findViewById(android.R.id.content);
            if (rootView != null) {
                return findTextViewRecursively(rootView);
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return null;
    }
    
    private TextView findTextViewRecursively(android.view.ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            android.view.View child = parent.getChildAt(i);
            if (child instanceof TextView) {
                return (TextView) child;
            } else if (child instanceof android.view.ViewGroup) {
                TextView result = findTextViewRecursively((android.view.ViewGroup) child);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
    
    public LoadingProgressDialog setCancelable(boolean cancelable) {
        dialog.setCancelable(cancelable);
        return this;
    }
    
    public void show() {
        if (dialog != null && !dialog.isShowing()) {
            dialog.show();
            startLoadingAnimation();
        }
    }
    
    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            try {
                dialog.dismiss();
            } catch (Exception e) {
                // 忽略对话框清理异常
            }
        }
    }
    
    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }
    
    private void startLoadingAnimation() {
        FrameLayout loadingContainer = dialog.findViewById(R.id.loading_container);
        if (loadingContainer != null) {
            try {
                Animation rotateAnimation = AnimationUtils.loadAnimation(context, R.anim.loading_rotation);
                loadingContainer.startAnimation(rotateAnimation);
            } catch (Exception e) {
                // 如果动画加载失败，忽略异常
            }
        }
    }
}