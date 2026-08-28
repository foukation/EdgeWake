package com.fxzs.lingxiagent.view.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.fxzs.lingxiagent.R;

public class ChatTextActionDialog extends Dialog {

    public interface OnActionClickListener {
        void onLikeClick();
        void onDislikeClick();
        void onCopyClick();
        void onSelectClick();
        void onReadClick();
        void onShareClick();
        void onCollectClick();
        void onMoreClick();
        void onDeleteClick(); // 添加删除记录回调
    }

    private OnActionClickListener listener;
    private boolean isCard = false;

    public ChatTextActionDialog(@NonNull Context context) {
        super(context);
        init();
    }

    public ChatTextActionDialog(@NonNull Context context, boolean isCard) {
        super(context);
        this.isCard = isCard;
        init();
    }

    public ChatTextActionDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        init();
    }

    private void init() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.chat_dialog_layout);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            getWindow().setDimAmount(0.2f);
        }

        setCanceledOnTouchOutside(true);

        if (isCard) {
            findViewById(R.id.tv_copy).setVisibility(View.GONE);
            findViewById(R.id.tv_read).setVisibility(View.GONE);
            findViewById(R.id.tv_collect).setVisibility(View.GONE);
        } else {
            findViewById(R.id.tv_copy).setOnClickListener(v -> {
                if (listener != null) listener.onCopyClick();
                dismiss();
            });

            findViewById(R.id.tv_read).setOnClickListener(v -> {
                if (listener != null) listener.onReadClick();
                dismiss();
            });

            findViewById(R.id.tv_collect).setOnClickListener(v -> {
                if (listener != null) listener.onCollectClick();
                dismiss();
            });
        }

        findViewById(R.id.tv_share).setOnClickListener(v -> {
            if (listener != null) listener.onShareClick();
            dismiss();
        });

        findViewById(R.id.tv_delete).setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick();
            dismiss();
        });
    }

    public void setOnActionClickListener(OnActionClickListener listener) {
        this.listener = listener;
    }

    /**
     * 在指定位置显示对话框
     * @param x X坐标
     * @param y Y坐标
     */
    public void showAtLocation(int x, int y) {
        show();

        Window window = getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.CENTER | Gravity.RIGHT;
            params.x = x;
            params.y = y;
            window.setAttributes(params);
        }
    }
}