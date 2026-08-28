package com.fxzs.lingxiagent.view.common;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.fxzs.lingxiagent.R;

public class LoginPromptDialog extends Dialog {
    
    private TextView tvRegister;
    private ImageView ivClose;
    private TextView tvTitle;
    private TextView tvOneClickLogin;
    private TextView tvSwitchLogin;
    private CheckBox cbAgreement;
    private TextView tvAgreement;
    private LinearLayout llContent;
    
    private OnLoginPromptListener listener;
    
    public interface OnLoginPromptListener {
        void onOneClickLogin();
        void onSwitchLogin();
        void onRegister();
        void onClose();
        void onAgreementClick();
    }
    
    public LoginPromptDialog(@NonNull Context context) {
        super(context, R.style.TransparentDialog);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_login_prompt);
        
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            // 根据截图调整位置，大约在状态栏+导航栏下方
            params.y = dpToPx(120); // 距离顶部120dp
            window.setAttributes(params);
            
            window.setDimAmount(0.0f); // 不显示背景遮罩
        }
        
        setCancelable(true);
        setCanceledOnTouchOutside(true);
        
        initViews();
        setListeners();
    }
    
    private void initViews() {
        tvRegister = findViewById(R.id.tv_register);
        ivClose = findViewById(R.id.iv_close);
        tvTitle = findViewById(R.id.tv_title);
        tvOneClickLogin = findViewById(R.id.tv_one_click_login);
        tvSwitchLogin = findViewById(R.id.tv_switch_login);
        cbAgreement = findViewById(R.id.cb_agreement);
        tvAgreement = findViewById(R.id.tv_agreement);
        llContent = findViewById(R.id.ll_content);
        
        // 设置标题文本
        tvTitle.setText("登录可体验完整功能");
    }
    
    private void setListeners() {
        ivClose.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClose();
            }
            dismiss();
        });
        
        tvRegister.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRegister();
            }
        });
        
        tvOneClickLogin.setOnClickListener(v -> {
            if (cbAgreement.isChecked()) {
                if (listener != null) {
                    listener.onOneClickLogin();
                }
            } else {
                showAgreementHint();
            }
        });
        
        tvSwitchLogin.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSwitchLogin();
            }
        });
        
        tvAgreement.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAgreementClick();
            }
        });
        
        findViewById(R.id.view_outside).setOnClickListener(v -> {
            // 点击外部区域不做任何操作
        });
    }
    
    private void showAgreementHint() {
        // TODO: 显示需要同意协议的提示
    }
    
    public void setOnLoginPromptListener(OnLoginPromptListener listener) {
        this.listener = listener;
    }
    
    private int dpToPx(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }
}