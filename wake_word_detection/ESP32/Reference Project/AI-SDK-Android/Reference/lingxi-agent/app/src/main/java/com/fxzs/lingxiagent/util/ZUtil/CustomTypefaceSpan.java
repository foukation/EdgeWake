package com.fxzs.lingxiagent.util.ZUtil;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.TypefaceSpan;

/**
 * 自定义字体Span
 * 可以设置自定义字体的同时保留原有的样式（如加粗、斜体）
 */
public class CustomTypefaceSpan extends TypefaceSpan {
    
    private final Typeface customTypeface;
    
    public CustomTypefaceSpan(Typeface typeface) {
        super("");
        this.customTypeface = typeface;
    }
    
    @Override
    public void updateDrawState(TextPaint ds) {
        applyCustomTypeface(ds, customTypeface);
    }
    
    @Override
    public void updateMeasureState(TextPaint paint) {
        applyCustomTypeface(paint, customTypeface);
    }
    
    private static void applyCustomTypeface(Paint paint, Typeface tf) {
        if (tf == null) {
            return;
        }
        
        int oldStyle;
        Typeface old = paint.getTypeface();
        if (old == null) {
            oldStyle = 0;
        } else {
            oldStyle = old.getStyle();
        }
        
        int fake = oldStyle & ~tf.getStyle();
        if ((fake & Typeface.BOLD) != 0) {
            paint.setFakeBoldText(true);
        }
        
        if ((fake & Typeface.ITALIC) != 0) {
            paint.setTextSkewX(-0.25f);
        }
        
        paint.setTypeface(tf);
    }
    
    /**
     * 创建自定义字体Span
     * @param typeface 要应用的字体
     * @return CustomTypefaceSpan实例
     */
    public static CustomTypefaceSpan create(Typeface typeface) {
        return new CustomTypefaceSpan(typeface);
    }
}
