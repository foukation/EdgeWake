package com.fxzs.lingxiagent.view.common;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.fxzs.lingxiagent.R;

/**
 * 语音输入对话框
 * 显示语音识别状态和结果
 */
public class VoiceInputDialog extends Dialog {
    
    private ImageView ivVoiceAnimation;
    private TextView tvStatus;
    private TextView tvResult;
    private TextView tvCancel;
    private AnimationDrawable voiceAnimation;
    
    private OnVoiceInputListener listener;
    
    public interface OnVoiceInputListener {
        void onCancel();
    }
    
    public VoiceInputDialog(@NonNull Context context) {
        super(context, R.style.TransparentDialog);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_voice_input);
        
        // 设置对话框窗口属性
        Window window = getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(layoutParams);
        }
        
        initViews();
    }
    
    private void initViews() {
        ivVoiceAnimation = findViewById(R.id.iv_voice_animation);
        tvStatus = findViewById(R.id.tv_status);
        tvResult = findViewById(R.id.tv_result);
        tvCancel = findViewById(R.id.tv_cancel);
        
        // 设置语音动画
        ivVoiceAnimation.setBackgroundResource(R.drawable.voice_animation);
        voiceAnimation = (AnimationDrawable) ivVoiceAnimation.getBackground();
        
        // 设置取消按钮
        tvCancel.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCancel();
            }
            dismiss();
        });
        
        // 点击空白处取消
        findViewById(R.id.ll_dialog_content).setOnClickListener(v -> {
            // 不做任何操作，防止点击内容区域关闭对话框
        });
        
        findViewById(R.id.fl_dialog_root).setOnClickListener(v -> {
            if (listener != null) {
                listener.onCancel();
            }
            dismiss();
        });
    }
    
    public void setOnVoiceInputListener(OnVoiceInputListener listener) {
        this.listener = listener;
    }
    
    /**
     * 显示准备状态
     */
    public void showReady() {
        tvStatus.setText("请说话...");
        tvResult.setVisibility(View.GONE);
        startAnimation();
    }
    
    /**
     * 显示正在识别状态
     */
    public void showListening() {
        tvStatus.setText("正在识别...");
        tvResult.setVisibility(View.GONE);
    }
    
    /**
     * 显示部分结果
     */
    public void showPartialResult(String text) {
        tvStatus.setText("正在识别...");
        tvResult.setText(text);
        tvResult.setVisibility(View.VISIBLE);
    }
    
    /**
     * 显示最终结果
     */
    public void showResult(String text) {
        tvStatus.setText("识别完成");
        tvResult.setText(text);
        tvResult.setVisibility(View.VISIBLE);
        stopAnimation();
    }
    
    /**
     * 显示错误
     */
    public void showError(String error) {
        tvStatus.setText(error);
        tvResult.setVisibility(View.GONE);
        stopAnimation();
    }
    
    /**
     * 开始动画
     */
    private void startAnimation() {
        if (voiceAnimation != null && !voiceAnimation.isRunning()) {
            voiceAnimation.start();
        }
    }
    
    /**
     * 停止动画
     */
    private void stopAnimation() {
        if (voiceAnimation != null && voiceAnimation.isRunning()) {
            voiceAnimation.stop();
        }
    }
    
    @Override
    public void dismiss() {
        stopAnimation();
        super.dismiss();
    }
}