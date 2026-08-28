package com.fxzs.lingxiagent.view.user;

import android.app.Dialog;
import android.content.Context;

import androidx.annotation.NonNull;

import com.fxzs.lingxiagent.view.common.CommonDialog;

/**
 * 退出登录弹窗
 * @deprecated 建议直接使用 CommonDialog.showLogoutDialog()
 */
@Deprecated
public class LogoutDialog {

    public interface OnLogoutListener {
        void onConfirm();
        void onCancel();
    }

    /**
     * 显示退出登录弹窗
     * @param context 上下文
     * @param listener 点击监听器
     * @return Dialog实例
     */
    public static Dialog show(@NonNull Context context, OnLogoutListener listener) {
        return CommonDialog.showLogoutDialog(context, new CommonDialog.OnDialogClickListener() {
            @Override
            public void onConfirm() {
                if (listener != null) {
                    listener.onConfirm();
                }
            }

            @Override
            public void onCancel() {
                if (listener != null) {
                    listener.onCancel();
                }
            }
        });
    }
}