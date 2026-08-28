package com.fxzs.lingxiagent.view.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.fxzs.lingxiagent.R;

/**
 * 支持线性渐变文字颜色的 TextView
 */
public class GradientTextView extends AppCompatTextView {

    private int startColor = 0;
    private int endColor = 0;
    // 0: horizontal (left->right), 1: vertical (top->bottom)
    private int orientation = 0;

    private LinearGradient linearGradient;

    public GradientTextView(Context context) {
        super(context);
    }

    public GradientTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public GradientTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(@Nullable AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.GradientTextView);
            try {
                startColor = a.getColor(R.styleable.GradientTextView_startColor, startColor);
                endColor = a.getColor(R.styleable.GradientTextView_endColor, endColor);
                orientation = a.getInt(R.styleable.GradientTextView_gradientOrientation, orientation);
            } finally {
                a.recycle();
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        applyGradient(w, h);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        // 文字变化可能导致宽高变化，延后到下次布局再应用
        post(() -> applyGradient(getWidth(), getHeight()));
    }

    public void setStartColor(int color) {
        this.startColor = color;
        applyGradient(getWidth(), getHeight());
        invalidate();
    }

    public void setEndColor(int color) {
        this.endColor = color;
        applyGradient(getWidth(), getHeight());
        invalidate();
    }

    public void setOrientationHorizontal() { this.orientation = 0; applyGradient(getWidth(), getHeight()); }
    public void setOrientationVertical() { this.orientation = 1; applyGradient(getWidth(), getHeight()); }

    private void applyGradient(int w, int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        if (startColor == 0 || endColor == 0) {
            // 未设置颜色时不应用渐变
            getPaint().setShader(null);
            return;
        }

        float left = 0f;
        float top = 0f;
        float right = orientation == 0 ? w : 0f;
        float bottom = orientation == 1 ? h : 0f;

        linearGradient = new LinearGradient(
                left, top,
                right, bottom,
                new int[]{startColor, endColor},
                null,
                Shader.TileMode.CLAMP
        );
        getPaint().setShader(linearGradient);
        invalidate();
    }
}

