package com.fxzs.lingxiagent.util.markdown;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

/**
 * Styles used by ChatMarkdownRenderer to approximate ModernMarkdownThemePlugin
 */
public final class MarkdownStyle {
    private MarkdownStyle() {}

    // Base text
    public static final int BODY_COLOR = Color.parseColor("#333333");
    public static final float BODY_TEXT_SIZE_SP = 16f;
    public static final float COT_TEXT_SIZE_SP = 14f;//cot 字体大小
    public static final float LINE_SPACING_MULT = 1.2f;

    // Headings relative multipliers (level 1..6)
    private static final float[] HEADING_MULT = new float[]{1.2f, 1.15f, 1.10f, 1.05f, 1.0f, 0.95f};

    // Link
    public static final int LINK_COLOR = Color.parseColor("#165DFF");

    // Inline code
    public static final int INLINE_CODE_BG = Color.parseColor("#F5F7FA");
    public static final int INLINE_CODE_FG = Color.parseColor("#D81F26");

    // Blockquote
    public static final int QUOTE_BAR_COLOR = Color.parseColor("#165DFF");
    public static final int QUOTE_BAR_WIDTH_DP = 3;

    // HR
    public static final int HR_COLOR = Color.parseColor("#E5E6EB");

    // Table
    public static final int TABLE_HEADER_BG = Color.parseColor("#F2F2F2");
    public static final int TABLE_ROW_HEIGHT_DP = 50; // 要求行高
    public static final int TABLE_ROW_BG_ODD = Color.parseColor("#FFFFFFFF");
    public static final int TABLE_ROW_BG_EVEN = Color.parseColor("#FAFAFA");

    public static void applyBody(TextView tv) {
        tv.setTextSize(BODY_TEXT_SIZE_SP);
        tv.setLineSpacing(0, LINE_SPACING_MULT);
        tv.setTextColor(BODY_COLOR);
        // Enable links by default; safe for paragraph/list/headings
        tv.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public static void applyHeading(TextView tv, int level) {
        float mult = 1.0f;
        if (level >= 1 && level <= HEADING_MULT.length) mult = HEADING_MULT[level - 1];
        tv.setTextSize(BODY_TEXT_SIZE_SP * mult);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setLineSpacing(0, LINE_SPACING_MULT);
        tv.setTextColor(BODY_COLOR);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
    }

    // 参考旧版 HeadingPaddingSpan 与主题的间距策略，给出直出容器的标题上下间距（dp）
    public static int headingTopMarginDp(int level) {
        // 顶部间距稍小，随级别降低而减小
        int base = 10; // dp
        float mult = Math.max(0.5f, (7 - level) / 6.0f);
        return (int) (base * mult);
    }

    public static int headingBottomMarginDp(int level) {
        // 底部间距稍大，增强与正文分隔
        int base = 14; // dp
        float mult = Math.max(0.5f, (7 - level) / 6.0f);
        return (int) (base * mult);
    }

    public static int dp(Context ctx, int v) {
        float d = ctx.getResources().getDisplayMetrics().density;
        return (int) (v * d + 0.5f);
    }
}

