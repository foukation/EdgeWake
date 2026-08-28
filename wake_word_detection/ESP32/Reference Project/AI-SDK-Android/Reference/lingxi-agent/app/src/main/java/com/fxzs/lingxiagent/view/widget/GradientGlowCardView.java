package com.fxzs.lingxiagent.view.widget;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.fxzs.lingxiagent.R;

/**
 * 创建者：ZyOng
 * 描述：
 * 创建时间：2026/2/28 16:11
 */
public class GradientGlowCardView extends FrameLayout {

    private Paint glowPaint;
    private Paint bgPaint;

    private RectF rectF = new RectF();

    private float cornerRadius;
    private float glowWidth;

    public GradientGlowCardView(Context context) {
        this(context, null);
    }

    public GradientGlowCardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GradientGlowCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setWillNotDraw(false);

        cornerRadius = context.getResources().getDimension(R.dimen.dp_16);     // 16dp 圆角
        glowWidth = context.getResources().getDimension(R.dimen.dp_2);        // 光晕宽度

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(glowWidth);

        // 模糊扩散（真实光晕）
        glowPaint.setMaskFilter(new BlurMaskFilter(
                glowWidth,
                BlurMaskFilter.Blur.NORMAL
        ));

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.parseColor("#F4F5F7"));

        // 需要软件层支持 BlurMaskFilter
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        rectF.set(
                glowWidth,
                glowWidth,
                w - glowWidth,
                h - glowWidth
        );

        // 创建环绕渐变（对角渐变更自然）
        LinearGradient gradient = new LinearGradient(
                0, 0,
                w, h,
                new int[]{
                        Color.parseColor("#7B61FF"),  // 紫
                        Color.parseColor("#4FC3F7"),  // 蓝
                        Color.parseColor("#7B61FF")   // 回到紫
                },
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP
        );

        glowPaint.setShader(gradient);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {

        // 1️⃣ 画渐变发光描边
        canvas.drawRoundRect(
                rectF,
                cornerRadius,
                cornerRadius,
                glowPaint
        );

        super.dispatchDraw(canvas);
    }
}
