package com.fxzs.lingxiagent.view.common;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.SplashActivity;
import com.fxzs.lingxiagent.model.common.Constants;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通用弹窗组件
 * 支持自定义标题、内容、按钮文字和样式
 */
public class CommonDialog {
    
    public interface OnDialogClickListener {
        void onConfirm();
        void onCancel();
    }
    
    public static class Builder {
        private Context context;
        private String title = "提示";
        private String message = "";
        private boolean isHtmlMessage = false;
        private String confirmText = "确认";
        private String cancelText = "取消";
        private boolean confirmTextRed = false;
        private OnDialogClickListener listener;
        private boolean cancelable = true;
        private int cancelBtnVisible;

        public Builder(Context context) {
            this.context = context;
        }
        
        /**
         * 设置标题
         */
        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }
        
        /**
         * 设置内容消息
         */
        public Builder setMessage(String message) {
            this.message = message;
            this.isHtmlMessage = false;
            return this;
        }

        /**
         * 设置HTML格式的内容消息
         */
        public Builder setHtmlMessage(String htmlMessage) {
            this.message = htmlMessage;
            this.isHtmlMessage = true;
            return this;
        }
        
        /**
         * 设置确认按钮文字
         */
        public Builder setConfirmText(String confirmText) {
            this.confirmText = confirmText;
            return this;
        }

        public Builder setCancelVisible(int visible) {
            this.cancelBtnVisible = visible;
            return this;
        }

        /**
         * 设置取消按钮文字
         */
        public Builder setCancelText(String cancelText) {
            this.cancelText = cancelText;
            return this;
        }
        
        /**
         * 设置确认按钮是否为红色（警告样式）
         */
        public Builder setConfirmTextRed(boolean isRed) {
            this.confirmTextRed = isRed;
            return this;
        }
        
        /**
         * 设置点击监听器
         */
        public Builder setOnClickListener(OnDialogClickListener listener) {
            this.listener = listener;
            return this;
        }
        
        /**
         * 设置是否可取消
         */
        public Builder setCancelable(boolean cancelable) {
            this.cancelable = cancelable;
            return this;
        }
        
        /**
         * 创建并显示弹窗
         */
        public Dialog show() {
            Dialog dialog = new Dialog(context);
            dialog.setContentView(R.layout.dialog_common);
            
            // 设置窗口背景为透明
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                dialog.getWindow().setAttributes(params);
            }
            
            // 初始化控件
            TextView tvTitle = dialog.findViewById(R.id.tv_title);
            TextView tvMessage = dialog.findViewById(R.id.tv_message);
            TextView btnCancel = dialog.findViewById(R.id.btn_cancel);
            TextView btnConfirm = dialog.findViewById(R.id.btn_confirm);
            View middle_line = dialog.findViewById(R.id.middle_line);

            // 设置内容
            if (tvTitle != null) tvTitle.setText(title);
            if (tvMessage != null) {
                if (isHtmlMessage) {
                    tvMessage.setText(Html.fromHtml(message));
                    tvMessage.setMovementMethod(LinkMovementMethod.getInstance());
                } else {
                    tvMessage.setText(message);
                }
            }
            btnCancel.setVisibility(cancelBtnVisible);
            middle_line.setVisibility(cancelBtnVisible);
            if (btnCancel != null) btnCancel.setText(cancelText);
            if (btnConfirm != null) btnConfirm.setText(confirmText);
            
            // 设置确认按钮颜色
            if (btnConfirm != null) {
                if (confirmTextRed) {
                    btnConfirm.setTextColor(Color.parseColor("#FF4444"));
                } else {
                    btnConfirm.setTextColor(Color.parseColor("#1C77FF"));
                }
            }
            
            // 设置点击事件
            if (btnCancel != null) {
                btnCancel.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onCancel();
                    }
                    dialog.dismiss();
                });
            }
            
            if (btnConfirm != null) {
                btnConfirm.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onConfirm();
                    }
                    dialog.dismiss();
                });
            }
            
            dialog.setCancelable(cancelable);
            dialog.setCanceledOnTouchOutside(cancelable);
            dialog.show();
            
            return dialog;
        }
    }
    
    /**
     * 快速创建注销账号确认弹窗
     */
    public static Dialog showAccountDeletionDialog(Context context, OnDialogClickListener listener) {
        return new Builder(context)
                .setTitle("安全提示")
                .setMessage("注销账号后，将不会保留当前账号的所有数据，请谨慎操作")
                .setConfirmText("确认注销")
                .setConfirmTextRed(true)
                .setCancelText("取消")
                .setOnClickListener(listener)
                .show();
    }
    
    /**
     * 快速创建退出登录确认弹窗
     */
    public static Dialog showLogoutDialog(Context context, OnDialogClickListener listener) {
        return new Builder(context)
                .setTitle("确认退出登录？")
                .setMessage("退出登录不会丢失任何数据，你仍可以登录此账号")
                .setConfirmText(context.getString(R.string.logout))
                .setConfirmTextRed(true)
                .setCancelText(context.getString(android.R.string.cancel))
                .setOnClickListener(listener)
                .show();
    }
    
    /**
     * 快速创建通用确认弹窗
     */
    public static Dialog showConfirmDialog(Context context, String title, String message,
                                         String confirmText, OnDialogClickListener listener) {
        return new Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setConfirmText(confirmText)
                .setOnClickListener(listener)
                .show();
    }
    public static Dialog showConfirmOneBtnDialog(Context context, String title, String message,
                                         String confirmText, OnDialogClickListener listener) {
        return new Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setCancelVisible(View.GONE)
                .setConfirmText(confirmText)
                .setOnClickListener(listener)
                .show();
    }

    /**
     * 快速创建支持HTML内容的确认弹窗
     */
    public static Dialog showHtmlConfirmDialog(Context context, String title, String htmlMessage,
                                             String confirmText, OnDialogClickListener listener) {
        return new Builder(context)
                .setTitle(title)
                .setHtmlMessage(htmlMessage)
                .setConfirmText(confirmText)
                .setOnClickListener(listener)
                .show();
    }

    /**
     * 快速创建协议确认弹窗（带有可点击的服务协议和隐私政策链接）
     */
    public static Dialog showAgreementDialog(Context context,String content, String title,OnDialogClickListener listener) {
        // 创建可点击的文本
        String confirm = context.getString(R.string.first_agree);
        SpannableString spannableString = new SpannableString(content);
        Map<String, Runnable> keywordActions = getStringRunnableMap(context);

        for (Map.Entry<String, Runnable> entry : keywordActions.entrySet()) {
            String keyword = entry.getKey();
            Runnable action = entry.getValue();
            addClickableSpans(spannableString, content, keyword, createClickableSpan(context, action));
        }

        if (content.contains("中国移动认证服务条款")) {
            confirm = context.getString(R.string.agree);
        }

        // 创建弹窗
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_common);

        // 设置窗口背景为透明
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setAttributes(params);
        }

        // 初始化控件
        TextView tvTitle = dialog.findViewById(R.id.tv_title);
        TextView tvMessage = dialog.findViewById(R.id.tv_message);
        TextView btnCancel = dialog.findViewById(R.id.btn_cancel);
        TextView btnConfirm = dialog.findViewById(R.id.btn_confirm);
        View vwBg = dialog.findViewById(R.id.vw_bg);
        if (context instanceof SplashActivity){
            vwBg.setBackgroundColor(Color.parseColor("#80ffffff"));
        }else {
            vwBg.setBackgroundColor(Color.parseColor("#33000000"));
        }


        // 设置内容
        if (tvTitle != null) tvTitle.setText(title);
        if (tvMessage != null) {
            tvMessage.setText(spannableString);
            tvMessage.setMovementMethod(LinkMovementMethod.getInstance());
            tvMessage.setGravity(Gravity.LEFT);
        }
        if (btnCancel != null) btnCancel.setText(context.getString(R.string.disagree));
        if (btnConfirm != null) {
            btnConfirm.setText(confirm);
            btnConfirm.setTextColor(Color.parseColor("#1C77FF"));
        }

        // 设置点击事件
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                dialog.dismiss();
                if (listener != null) listener.onCancel();
            });
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();
                if (listener != null) listener.onConfirm();
            });
        }
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.show();
        return dialog;
    }

    /**
     * 快速创建警告弹窗（红色确认按钮）
     */
    public static Dialog showWarningDialog(Context context, String title, String message, 
                                         String confirmText, OnDialogClickListener listener) {
        return new Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setConfirmText(confirmText)
                .setConfirmTextRed(true)
                .setOnClickListener(listener)
                .show();
    }

    @NonNull
    private static Map<String, Runnable> getStringRunnableMap(Context context) {
        Map<String, Runnable> keywordActions = new LinkedHashMap<>();
        keywordActions.put("用户协议", () ->
                WebViewActivity.start(context, Constants.USER_AGREEMENT, context.getString(R.string.user_agreement))
        );
        keywordActions.put("隐私政策", () ->
                WebViewActivity.start(context, Constants.PRIVACY_POLICY_DETAILED, context.getString(R.string.privacy_policy))
        );
        keywordActions.put("中国移动认证服务条款", () ->
                WebViewActivity.start(context, Constants.CM_CONTACT_URL, context.getString(R.string.cm_certification_clause))
        );
        return keywordActions;
    }

    private static void addClickableSpans(
            SpannableString spannable,
            String content,
            String keyword,
            ClickableSpan span
    ) {
        Pattern pattern = Pattern.compile(Pattern.quote(keyword)); // 转义特殊字符
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            spannable.setSpan(
                    createClickableSpan(span),
                    matcher.start(),
                    matcher.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }

    private static ClickableSpan createClickableSpan(Context context, Runnable action) {
        return new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                if (action != null) action.run();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.parseColor("#1C77FF"));
                ds.setUnderlineText(true);
            }
        };
    }


    private static ClickableSpan createClickableSpan(ClickableSpan original) {
        return new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                original.onClick(widget);
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                original.updateDrawState(ds);
            }
        };
    }


}
