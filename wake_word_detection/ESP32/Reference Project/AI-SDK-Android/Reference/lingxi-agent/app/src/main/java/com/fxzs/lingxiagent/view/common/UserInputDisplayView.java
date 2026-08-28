package com.fxzs.lingxiagent.view.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fxzs.lingxiagent.R;

import timber.log.Timber;

public class UserInputDisplayView extends LinearLayout {
    private static final String TAG = "UserInputDisplayView";
    
    public enum DisplayMode {
        COMPACT,    // 短文本，固定宽度
        NORMAL,     // 标准文本，单行或双行
        EXPANDED    // 长文本，多行显示
    }
    
    private TextView tvUserInput;
    private DisplayMode displayMode = DisplayMode.NORMAL;
    
    public UserInputDisplayView(@NonNull Context context) {
        super(context);
        init(context, null);
    }
    
    public UserInputDisplayView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }
    
    public UserInputDisplayView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }
    
    private void init(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.view_user_input_display, this, true);
        
        tvUserInput = findViewById(R.id.tv_user_input);
        
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.UserInputDisplayView);
            String text = typedArray.getString(R.styleable.UserInputDisplayView_inputText);
            if (text != null) {
                setText(text);
            }
            typedArray.recycle();
        }
        
        // 设置默认属性
        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(android.view.Gravity.CENTER_VERTICAL);
        setBackgroundResource(R.drawable.bg_user_input_gradient);
    }
    
    public void setText(String text) {
        if (tvUserInput != null) {
            tvUserInput.setText(text);
            adjustDisplayMode(text);
        }
    }
    
    private void adjustDisplayMode(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        
        int textLength = text.length();
        Timber.tag(TAG).d( "调整显示模式，文本长度: " + textLength);
        
        if (textLength <= 10) {
            setDisplayMode(DisplayMode.COMPACT);
        } else if (textLength <= 50) {
            setDisplayMode(DisplayMode.NORMAL);
        } else {
            setDisplayMode(DisplayMode.EXPANDED);
        }
    }
    
    public void setDisplayMode(DisplayMode mode) {
        this.displayMode = mode;
        updateLayoutForMode();
    }
    
    private void updateLayoutForMode() {
        float density = getResources().getDisplayMetrics().density;
        
        switch (displayMode) {
            case COMPACT:
                Timber.tag(TAG).d( "设置紧凑模式");
                setMinimumWidth((int) (96 * density));
                setMinimumHeight((int) (48 * density));
                setPadding(
                    (int) (16 * density),
                    (int) (12 * density),
                    (int) (16 * density),
                    (int) (12 * density)
                );
                tvUserInput.setMaxLines(1);
                tvUserInput.setGravity(android.view.Gravity.CENTER);
                setGravity(android.view.Gravity.CENTER);
                break;
                
            case NORMAL:
                Timber.tag(TAG).d( "设置标准模式");
                setMinimumWidth((int) (303 * density));
                setMinimumHeight((int) (64 * density));
                setPadding(
                    (int) (16 * density),
                    (int) (12 * density),
                    (int) (16 * density),
                    (int) (12 * density)
                );
                tvUserInput.setMaxLines(2);
                tvUserInput.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
                setGravity(android.view.Gravity.CENTER_VERTICAL);
                break;
                
            case EXPANDED:
                Timber.tag(TAG).d( "设置展开模式");
                setMinimumWidth((int) (303 * density));
                setMinimumHeight((int) (144 * density));
                setPadding(
                    (int) (16 * density),
                    (int) (12 * density),
                    (int) (16 * density),
                    (int) (12 * density)
                );
                tvUserInput.setMaxLines(Integer.MAX_VALUE);
                tvUserInput.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
                setGravity(android.view.Gravity.CENTER_VERTICAL);
                break;
        }
        
        requestLayout();
    }
    
    public String getText() {
        return tvUserInput != null ? tvUserInput.getText().toString() : "";
    }
    
    public void setTextColor(int color) {
        if (tvUserInput != null) {
            tvUserInput.setTextColor(color);
        }
    }
    
    public void setTextSize(float size) {
        if (tvUserInput != null) {
            tvUserInput.setTextSize(size);
        }
    }
    
    public DisplayMode getDisplayMode() {
        return displayMode;
    }
    
    // 手动设置为右对齐样式（用于短文本）
    public void setCompactRightAlign() {
        setDisplayMode(DisplayMode.COMPACT);
        tvUserInput.setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
        setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
    }
}