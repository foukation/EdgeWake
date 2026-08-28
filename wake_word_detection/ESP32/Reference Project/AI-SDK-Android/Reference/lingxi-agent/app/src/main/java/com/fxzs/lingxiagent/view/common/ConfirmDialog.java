package com.fxzs.lingxiagent.view.common;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Html;
import android.text.Spanned;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fxzs.lingxiagent.R;

public class ConfirmDialog {
    
    public interface OnConfirmDialogListener {
        void onConfirm();
        void onCancel();
    }
    
    private Dialog dialog;
    private OnConfirmDialogListener listener;
    
    private int confirmBtnBgResId = R.drawable.bg_button_gray_rounded_8dp;
    private int cancelBtnBgResId = R.drawable.bg_button_gray_rounded_8dp;
    private int confirmBtnTextColor = 0xFF1C77FF;
    private int cancelBtnTextColor = 0xFF1E1E1E;
    private float titleLineSpacing = 1.4f;
    private float subtitleLineSpacing = 1.4f;

    public ConfirmDialog(Context context) {
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_confirm_exit);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setCancelable(false);
        // 设置窗口背景为透明
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setAttributes(params);
        }

        setupClickListeners();
    }

    // 无参构造函数，用于特殊弹窗
    private ConfirmDialog() {
        // 空构造函数，用于createAccountDeletionDialog方法
    }
    
    private void setupClickListeners() {
        LinearLayout btnCancel = dialog.findViewById(R.id.btn_cancel);
        LinearLayout btnExit = dialog.findViewById(R.id.btn_exit);
        
        btnCancel.setOnClickListener(v -> {
            dismiss();
            if (listener != null) {
                listener.onCancel();
            }
        });
        
        btnExit.setOnClickListener(v -> {
            dismiss();
            if (listener != null) {
                listener.onConfirm();
            }
        });
    }
    
    public ConfirmDialog setTitle(String title) {
        TextView tvTitle = dialog.findViewById(R.id.tv_dialog_title);
        if (tvTitle != null) {
            tvTitle.setText(title);
        }
        return this;
    }
    
    public ConfirmDialog setSubtitle(String subtitle) {
        TextView tvSubtitle = dialog.findViewById(R.id.tv_dialog_subtitle);
        if (tvSubtitle != null) {
            tvSubtitle.setText(subtitle);
        }
        return this;
    }
    
    public ConfirmDialog setSubtitleHtml(String htmlSubtitle) {
        TextView tvSubtitle = dialog.findViewById(R.id.tv_dialog_subtitle);
        if (tvSubtitle != null) {
            Spanned spanned = Html.fromHtml(htmlSubtitle);
            tvSubtitle.setText(spanned);
        }
        return this;
    }
    
    public ConfirmDialog setCancelText(String cancelText) {
        LinearLayout btnCancel = dialog.findViewById(R.id.btn_cancel);
        if (btnCancel != null) {
            TextView textView = (TextView) btnCancel.getChildAt(0);
            if (textView != null) {
                textView.setText(cancelText);
            }
        }
        return this;
    }
    
    public ConfirmDialog setConfirmText(String confirmText) {
        LinearLayout btnExit = dialog.findViewById(R.id.btn_exit);
        if (btnExit != null) {
            TextView textView = (TextView) btnExit.getChildAt(0);
            if (textView != null) {
                textView.setText(confirmText);
            }
        }
        return this;
    }
    
    public ConfirmDialog setOnConfirmDialogListener(OnConfirmDialogListener listener) {
        this.listener = listener;
        return this;
    }
    
    public ConfirmDialog setConfirmBtnBg(int resId) {
        this.confirmBtnBgResId = resId;
        LinearLayout btnExit = dialog.findViewById(R.id.btn_exit);
        if (btnExit != null) btnExit.setBackgroundResource(resId);
        return this;
    }
    public ConfirmDialog setCancelBtnBg(int resId) {
        this.cancelBtnBgResId = resId;
        LinearLayout btnCancel = dialog.findViewById(R.id.btn_cancel);
        if (btnCancel != null) btnCancel.setBackgroundResource(resId);
        return this;
    }
    public ConfirmDialog setConfirmBtnTextColor(int color) {
        this.confirmBtnTextColor = color;
        LinearLayout btnExit = dialog.findViewById(R.id.btn_exit);
        if (btnExit != null && btnExit.getChildCount() > 0) {
            ((TextView) btnExit.getChildAt(0)).setTextColor(color);
        }
        return this;
    }
    public ConfirmDialog setCancelBtnTextColor(int color) {
        this.cancelBtnTextColor = color;
        LinearLayout btnCancel = dialog.findViewById(R.id.btn_cancel);
        if (btnCancel != null && btnCancel.getChildCount() > 0) {
            ((TextView) btnCancel.getChildAt(0)).setTextColor(color);
        }
        return this;
    }
    public ConfirmDialog setTitleLineSpacing(float multiplier) {
        this.titleLineSpacing = multiplier;
        TextView tvTitle = dialog.findViewById(R.id.tv_dialog_title);
        if (tvTitle != null) tvTitle.setLineSpacing(0, multiplier);
        return this;
    }
    public ConfirmDialog setSubtitleLineSpacing(float multiplier) {
        this.subtitleLineSpacing = multiplier;
        TextView tvSubtitle = dialog.findViewById(R.id.tv_dialog_subtitle);
        if (tvSubtitle != null) tvSubtitle.setLineSpacing(0, multiplier);
        return this;
    }
    
    public void show() {
        // 检查是否是注销账号弹窗
        if (dialog.findViewById(R.id.tv_title) != null && dialog.findViewById(R.id.tv_message) != null) {
            // 这是注销账号弹窗，设置点击事件
            setupAccountDeletionClickListeners();
        } else {
            // 每次show时刷新样式，防止复用时未生效
            setConfirmBtnBg(confirmBtnBgResId);
            setCancelBtnBg(cancelBtnBgResId);
            setConfirmBtnTextColor(confirmBtnTextColor);
            setCancelBtnTextColor(cancelBtnTextColor);
            setTitleLineSpacing(titleLineSpacing);
            setSubtitleLineSpacing(subtitleLineSpacing);
        }

        if (dialog != null && !dialog.isShowing()) {
            dialog.show();
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

    /**
     * 创建注销账号确认弹窗
     */
    public static ConfirmDialog createAccountDeletionDialog(Context context) {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.dialog = new Dialog(context);
        confirmDialog.dialog.setContentView(R.layout.dialog_account_deletion);

        // 设置窗口背景为透明，避免四角黑色底色
        if (confirmDialog.dialog.getWindow() != null) {
            confirmDialog.dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            // 设置窗口属性
            android.view.WindowManager.LayoutParams params = confirmDialog.dialog.getWindow().getAttributes();
            params.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
            confirmDialog.dialog.getWindow().setAttributes(params);
        }

        confirmDialog.dialog.setCancelable(true);
        return confirmDialog;
    }

    private void setupAccountDeletionClickListeners() {
        android.widget.TextView btnCancel = dialog.findViewById(R.id.btn_cancel);
        android.widget.TextView btnConfirm = dialog.findViewById(R.id.btn_confirm);

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                dismiss();
                if (listener != null) {
                    listener.onCancel();
                }
            });
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                dismiss();
                if (listener != null) {
                    listener.onConfirm();
                }
            });
        }
    }

    /**
     * 设置注销账号弹窗的标题和内容
     */
    public ConfirmDialog setAccountDeletionContent(String title, String message) {
        TextView tvTitle = dialog.findViewById(R.id.tv_title);
        TextView tvMessage = dialog.findViewById(R.id.tv_message);

        if (tvTitle != null) {
            tvTitle.setText(title);
        }
        if (tvMessage != null) {
            tvMessage.setText(message);
        }
        return this;
    }
}