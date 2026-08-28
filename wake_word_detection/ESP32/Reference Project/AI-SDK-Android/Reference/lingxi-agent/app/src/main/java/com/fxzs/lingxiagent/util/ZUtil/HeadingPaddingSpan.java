package com.fxzs.lingxiagent.util.ZUtil;

import android.graphics.Paint;
import android.text.style.LineHeightSpan;

/**
 * 标题填充间距Span
 * 专门用于增加标题与正文之间的间距
 * 只在标题的最后一行添加底部间距，避免影响标题内部行间距
 */
public class HeadingPaddingSpan implements LineHeightSpan {
    
    private final int bottomPadding;
    private boolean isLastLine = false;
    
    public HeadingPaddingSpan(int bottomPadding) {
        this.bottomPadding = bottomPadding;
    }
    
    @Override
    public void chooseHeight(CharSequence text, int start, int end, int spanstartv, int v, Paint.FontMetricsInt fm) {
        if (fm != null) {
            // 只在最后一行添加底部间距
            // 通过检查是否到达文本末尾来判断是否为最后一行
            if (end >= text.length() || text.charAt(end - 1) == '\n') {
                fm.descent += bottomPadding;
                fm.bottom += bottomPadding;
            }
        }
    }
    
    /**
     * 创建标题填充间距Span
     * @param level 标题级别 (1-6)
     * @param density 屏幕密度
     * @return HeadingPaddingSpan实例
     */
    public static HeadingPaddingSpan create(int level, float density) {
        // 根据标题级别计算底部间距
        // 一级标题间距最大，六级标题间距最小
        int baseBottomPadding = (int) (10 * density); // 基础间距24dp
        float multiplier = (7 - level) / 6.0f; // 级别越高倍数越大
        multiplier = Math.max(0.5f, multiplier); // 最小倍数为0.5
        
        int bottomPadding = (int) (baseBottomPadding * multiplier);
        
        return new HeadingPaddingSpan(bottomPadding);
    }
}
