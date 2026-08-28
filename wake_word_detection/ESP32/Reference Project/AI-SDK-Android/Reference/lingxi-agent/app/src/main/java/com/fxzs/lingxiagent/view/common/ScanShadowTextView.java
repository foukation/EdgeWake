package com.fxzs.lingxiagent.view.common;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

/**
 * 白色文字，居中显示，带黑色阴影条从左到右扫过的效果。
 */
public class ScanShadowTextView extends AppCompatTextView {

    private final Paint scanPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect textBounds = new Rect();
    private LinearGradient gradient;
    private float scanXFraction = -1f; // [-1, 1] 左到右
    private ValueAnimator animator;

    public ScanShadowTextView(Context context) {
        super(context);
        init();
    }

    public ScanShadowTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ScanShadowTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setTextColor(Color.BLACK);
        setShadowLayer(8f, 0f, 0f, Color.WHITE);
        scanPaint.setStyle(Paint.Style.FILL);
        setupAnimator();
    }

    private void setupAnimator() {
        animator = ValueAnimator.ofFloat(-1f, 1f);
        animator.setDuration(2800);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.addUpdateListener(a -> {
            scanXFraction = (float) a.getAnimatedValue();
            invalidate();
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (animator != null && !animator.isStarted()) {
            animator.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.cancel();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateGradient(w, h);
    }

    private void updateGradient(int w, int h) {
        if (w <= 0 || h <= 0) return;
        // 先生成一个占位渐变；实际位置和宽度在 onDraw 中按条带宽度重建
        int transparent = Color.TRANSPARENT;
        int dark = 0xCCFFFFFF; // 更亮的半透明白
        gradient = new LinearGradient(
                0, 0, 1, 0,
                new int[]{transparent, dark, transparent},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP
        );
        scanPaint.setShader(gradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 计算扫描条应绘制的位置，使其在文字区域上方扫过
        CharSequence text = getText();
        if (text == null || text.length() == 0) return;

        getPaint().getTextBounds(text.toString(), 0, text.length(), textBounds);

        int viewWidth = getWidth();
        int viewHeight = getHeight();

        float textWidth = getPaint().measureText(text.toString());
        Paint.FontMetrics fm = getPaint().getFontMetrics();
        float textHeight = fm.bottom - fm.top;

        float leftPadding = (viewWidth - textWidth) / 2f;
        float topPadding = (viewHeight - textHeight) / 2f;

        // 以首字符宽度作为单位，条带宽度 = 半个字宽
        float firstCharWidth = text.length() > 0 ? getPaint().measureText(text, 0, 1) : textWidth;
        float bandWidth = Math.max(1f, firstCharWidth * 0.5f);
        float centerX = leftPadding + textWidth * (scanXFraction + 1f) / 2f; // -1->左, 1->右

        float left = centerX - bandWidth / 2f;
        float right = centerX + bandWidth / 2f;
        float top = topPadding;
        float bottom = topPadding + textHeight;

        if (bandWidth > 0f) {
            // 针对当前条带宽度与位置，重建渐变，使中间最暗、两侧透明
            int transparent = Color.TRANSPARENT;
            int dark = 0xCCFFFFFF; // 更亮的半透明白
            gradient = new LinearGradient(
                    left, 0, right, 0,
                    new int[]{transparent, dark, transparent},
                    new float[]{0f, 0.5f, 1f},
                    Shader.TileMode.CLAMP
            );
            scanPaint.setShader(gradient);
            canvas.drawRect(left, top, right, bottom, scanPaint);
        }
    }
}


