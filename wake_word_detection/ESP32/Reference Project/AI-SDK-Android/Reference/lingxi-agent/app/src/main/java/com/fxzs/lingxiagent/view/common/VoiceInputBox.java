package com.fxzs.lingxiagent.view.common;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import androidx.core.content.ContextCompat;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.AudioRecorderManager;

public class VoiceInputBox extends RelativeLayout {
    
    private EditText etInput;
    private ImageButton btnVoice;
    private AudioRecorderManager audioRecorderManager;
    private OnVoiceInputListener voiceInputListener;
    private OnTextInputListener textInputListener;
    private boolean isRecording = false;
    
    public interface OnVoiceInputListener {
        void onVoiceRecordStart();
        void onVoiceRecordStop(String audioFilePath);
        void onVoiceRecordCancel();
        void onPermissionRequired();
    }
    
    public interface OnTextInputListener {
        void onTextChanged(String text);
        void onSendClicked(String text);
    }
    
    public VoiceInputBox(Context context) {
        super(context);
        init(context);
    }
    
    public VoiceInputBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public VoiceInputBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.voice_input_box, this, true);
        
        etInput = findViewById(R.id.et_input);
        btnVoice = findViewById(R.id.btn_voice);
        
//        audioRecorderManager = new AudioRecorderManager(context);
        
        setupTextInput();
        setupVoiceInput();
    }
    
    private void setupTextInput() {
        etInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (textInputListener != null) {
                    textInputListener.onTextChanged(s.toString());
                }
                updateVoiceButtonState(s.toString().trim().isEmpty());
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        etInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                String text = etInput.getText().toString().trim();
                if (!text.isEmpty() && textInputListener != null) {
                    textInputListener.onSendClicked(text);
                    etInput.setText("");
                }
                return true;
            }
            return false;
        });
    }
    
    private void setupVoiceInput() {
        // 初始化默认状态为语音输入
        updateVoiceButtonState(true);
    }
    
    private void updateVoiceButtonState(boolean showVoice) {
        if (showVoice) {
            btnVoice.setImageResource(R.drawable.ic_voice_input);
            btnVoice.setContentDescription("语音输入");
            btnVoice.setEnabled(true);
            btnVoice.setOnClickListener(v -> {
                if (!isRecording) {
                    startVoiceRecording();
                } else {
                    stopVoiceRecording();
                }
            });
        } else {
            btnVoice.setImageResource(R.drawable.selector_send_button);
            btnVoice.setContentDescription("发送消息");
            String text = etInput.getText().toString().trim();
            btnVoice.setEnabled(!text.isEmpty());
            btnVoice.setOnClickListener(v -> {
                String currentText = etInput.getText().toString().trim();
                if (!currentText.isEmpty() && textInputListener != null) {
                    textInputListener.onSendClicked(currentText);
                    etInput.setText("");
                }
            });
        }
    }
    
    private void startVoiceRecording() {
        if (!checkRecordPermission()) {
            if (voiceInputListener != null) {
                voiceInputListener.onPermissionRequired();
            }
            return;
        }
        
        String fileName = "voice_" + System.currentTimeMillis();
        boolean success = audioRecorderManager.startRecording(fileName);
        
        if (success) {
            isRecording = true;
            btnVoice.setImageResource(R.drawable.ic_voice_wave);
            btnVoice.setContentDescription("正在录音，点击停止");
            
            if (voiceInputListener != null) {
                voiceInputListener.onVoiceRecordStart();
            }
        }
    }
    
    private void stopVoiceRecording() {
        if (isRecording) {
            String audioFilePath = audioRecorderManager.stopRecording();
            isRecording = false;
            
            btnVoice.setImageResource(R.drawable.ic_voice_input);
            btnVoice.setContentDescription("语音输入");
            
            if (voiceInputListener != null) {
                if (audioFilePath != null && !audioFilePath.isEmpty()) {
                    voiceInputListener.onVoiceRecordStop(audioFilePath);
                } else {
                    voiceInputListener.onVoiceRecordCancel();
                }
            }
        }
    }
    
    private boolean checkRecordPermission() {
        return ContextCompat.checkSelfPermission(getContext(), 
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }
    
    public void setOnVoiceInputListener(OnVoiceInputListener listener) {
        this.voiceInputListener = listener;
    }
    
    public void setOnTextInputListener(OnTextInputListener listener) {
        this.textInputListener = listener;
    }
    
    public void setText(String text) {
        etInput.setText(text);
    }
    
    public String getText() {
        return etInput.getText().toString();
    }
    
    public void setHint(String hint) {
        etInput.setHint(hint);
    }
    
    public void clearText() {
        etInput.setText("");
    }
    
    public void setEnabled(boolean enabled) {
        etInput.setEnabled(enabled);
        btnVoice.setEnabled(enabled);
    }
    
    public void requestInputFocus() {
        etInput.requestFocus();
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (isRecording) {
            audioRecorderManager.stopRecording();
        }
    }
}