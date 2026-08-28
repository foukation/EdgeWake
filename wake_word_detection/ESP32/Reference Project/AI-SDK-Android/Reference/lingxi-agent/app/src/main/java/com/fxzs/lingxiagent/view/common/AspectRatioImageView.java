package com.fxzs.lingxiagent.view.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

import com.fxzs.lingxiagent.R;

import timber.log.Timber;

/**
 * 自定义ImageView，支持保持宽高比的自适应缩放
 * 特别适用于键盘弹起时的图片自动缩放场景
 */
public class AspectRatioImageView extends AppCompatImageView {
    
    private float aspectRatio = 0f; // 宽高比 (width/height)
    private boolean adjustViewBounds = true;
    private int maxWidth = Integer.MAX_VALUE;
    private int maxHeight = Integer.MAX_VALUE;
    
    public AspectRatioImageView(Context context) {
        super(context);
        init(context, null);
    }
    
    public AspectRatioImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }
    
    public AspectRatioImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }
    
    private void init(Context context, AttributeSet attrs) {
        try {
            if (attrs != null) {
                TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AspectRatioImageView);
                aspectRatio = a.getFloat(R.styleable.AspectRatioImageView_aspectRatio, 0f);
                adjustViewBounds = a.getBoolean(R.styleable.AspectRatioImageView_adjustViewBounds, true);
                maxWidth = a.getDimensionPixelSize(R.styleable.AspectRatioImageView_maxWidth, Integer.MAX_VALUE);
                maxHeight = a.getDimensionPixelSize(R.styleable.AspectRatioImageView_maxHeight, Integer.MAX_VALUE);
                a.recycle();
            }
        } catch (Exception e) {
            // 如果属性解析失败，使用默认值
            Timber.tag("AspectRatioImageView").w("Failed to parse attributes, using defaults", e);
            aspectRatio = 0f;
            adjustViewBounds = true;
            maxWidth = Integer.MAX_VALUE;
            maxHeight = Integer.MAX_VALUE;
        }

        // 设置默认的ScaleType为fitCenter，保持图片完整显示
        setScaleType(ScaleType.FIT_CENTER);
        setAdjustViewBounds(adjustViewBounds);
    }
    
    /**
     * 设置宽高比
     * @param aspectRatio 宽高比 (width/height)，例如：16/9f = 1.78f
     */
    public void setAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
        requestLayout();
    }
    
    /**
     * 获取当前宽高比
     */
    public float getAspectRatio() {
        return aspectRatio;
    }
    
    /**
     * 设置最大宽度
     */
    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
        requestLayout();
    }
    
    /**
     * 设置最大高度
     */
    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
        requestLayout();
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        try {
            if (aspectRatio > 0) {
                int widthMode = MeasureSpec.getMode(widthMeasureSpec);
                int heightMode = MeasureSpec.getMode(heightMeasureSpec);
                int widthSize = MeasureSpec.getSize(widthMeasureSpec);
                int heightSize = MeasureSpec.getSize(heightMeasureSpec);

                int width, height;
            
            if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
                // 两个维度都是精确值，选择较小的约束来保持比例
                width = widthSize;
                height = heightSize;
                
                float currentRatio = (float) width / height;
                if (currentRatio > aspectRatio) {
                    // 当前比例比目标比例宽，以高度为准
                    width = (int) (height * aspectRatio);
                } else {
                    // 当前比例比目标比例高，以宽度为准
                    height = (int) (width / aspectRatio);
                }
            } else if (widthMode == MeasureSpec.EXACTLY) {
                // 宽度确定，根据比例计算高度
                width = widthSize;
                height = (int) (width / aspectRatio);
            } else if (heightMode == MeasureSpec.EXACTLY) {
                // 高度确定，根据比例计算宽度
                height = heightSize;
                width = (int) (height * aspectRatio);
            } else {
                // 两个维度都不确定，使用默认测量
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                return;
            }
            
            // 应用最大值限制
            if (width > maxWidth) {
                width = maxWidth;
                height = (int) (width / aspectRatio);
            }
            if (height > maxHeight) {
                height = maxHeight;
                width = (int) (height * aspectRatio);
            }
            
                setMeasuredDimension(width, height);
            } else {
                // 没有设置宽高比，使用默认测量
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        } catch (Exception e) {
            // 如果测量过程中出现异常，使用默认测量
            Timber.tag("AspectRatioImageView").w("Error in onMeasure, falling back to default", e);
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }
    
    /**
     * 根据图片的原始尺寸自动设置宽高比
     */
    public void setAspectRatioFromDrawable() {
        if (getDrawable() != null) {
            int intrinsicWidth = getDrawable().getIntrinsicWidth();
            int intrinsicHeight = getDrawable().getIntrinsicHeight();
            if (intrinsicWidth > 0 && intrinsicHeight > 0) {
                setAspectRatio((float) intrinsicWidth / intrinsicHeight);
            }
        }
    }
}
