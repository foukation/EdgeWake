package com.fxzs.lingxiagent.util.ZUtil;

import android.os.Build;
import android.text.Layout;
import android.widget.TextView;

/**
 * 文本对齐辅助工具类
 * 提供跨版本兼容的文本对齐解决方案
 */
public class TextAlignmentHelper {
    
    /**
     * 为TextView设置正文对齐方式
     * 正文内容应该平铺整个容器，实现更好的阅读体验
     * 
     * @param textView 目标TextView
     */
    public static void setBodyAlignment(TextView textView) {
        if (textView == null) {
            return;
        }
        
        // 对于Android O (API 26) 及以上版本，使用系统的两端对齐功能
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 设置两端对齐模式，让文字平铺整个容器宽度
            textView.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD);
        }
        
        // 设置文本方向为从左到右
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            textView.setTextDirection(TextView.TEXT_DIRECTION_LTR);
        }
        
        // 设置基础对齐方式为左对齐（作为fallback）
        textView.setTextAlignment(TextView.TEXT_ALIGNMENT_TEXT_START);
    }
    
    /**
     * 为TextView设置标题对齐方式
     * 标题应该靠左对齐，保持清晰的层次结构
     * 
     * @param textView 目标TextView
     */
    public static void setHeadingAlignment(TextView textView) {
        if (textView == null) {
            return;
        }
        
        // 标题不使用两端对齐，保持左对齐
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            textView.setJustificationMode(Layout.JUSTIFICATION_MODE_NONE);
        }
        
        // 设置文本方向为从左到右
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            textView.setTextDirection(TextView.TEXT_DIRECTION_LTR);
        }
        
        // 设置左对齐
        textView.setTextAlignment(TextView.TEXT_ALIGNMENT_TEXT_START);
    }
    
    /**
     * 为TextView设置代码块对齐方式
     * 代码块应该保持左对齐，不使用两端对齐
     * 
     * @param textView 目标TextView
     */
    public static void setCodeAlignment(TextView textView) {
        if (textView == null) {
            return;
        }
        
        // 代码块不使用两端对齐
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            textView.setJustificationMode(Layout.JUSTIFICATION_MODE_NONE);
        }
        
        // 设置文本方向为从左到右
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            textView.setTextDirection(TextView.TEXT_DIRECTION_LTR);
        }
        
        // 代码块左对齐
        textView.setTextAlignment(TextView.TEXT_ALIGNMENT_TEXT_START);
    }
    
    /**
     * 为TextView设置引用块对齐方式
     * 引用块使用左对齐，保持引用的清晰性
     * 
     * @param textView 目标TextView
     */
    public static void setQuoteAlignment(TextView textView) {
        if (textView == null) {
            return;
        }
        
        // 引用块可以使用轻微的两端对齐
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            textView.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD);
        }
        
        // 设置文本方向为从左到右
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            textView.setTextDirection(TextView.TEXT_DIRECTION_LTR);
        }
        
        // 基础对齐方式
        textView.setTextAlignment(TextView.TEXT_ALIGNMENT_TEXT_START);
    }
    
    /**
     * 检查当前设备是否支持文本两端对齐
     * 
     * @return true 如果支持两端对齐
     */
    public static boolean isJustificationSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }
    
    /**
     * 获取推荐的文本对齐模式描述
     * 
     * @return 对齐模式的描述字符串
     */
    public static String getAlignmentModeDescription() {
        if (isJustificationSupported()) {
            return "支持两端对齐 (Android O+)";
        } else {
            return "使用左对齐 (Android O以下)";
        }
    }
}
