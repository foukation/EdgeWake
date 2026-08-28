package com.fxzs.lingxiagent.util.ZUtil;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;

import androidx.annotation.NonNull;

import org.commonmark.node.Code;
import org.commonmark.node.Emphasis;
import org.commonmark.node.Heading;
import org.commonmark.node.Link;
import org.commonmark.node.StrongEmphasis;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.MarkwonConfiguration;
import io.noties.markwon.MarkwonSpansFactory;
import io.noties.markwon.RenderProps;
import io.noties.markwon.SpanFactory;
import io.noties.markwon.core.CorePlugin;
import io.noties.markwon.core.CoreProps;
import io.noties.markwon.core.MarkwonTheme;

/**
 * 豆包风格的 Markdown 主题插件
 * 提供与豆包界面一致的样式配置，包括字体大小、颜色和间距
 */
public class ModernMarkdownThemePlugin extends AbstractMarkwonPlugin {

    private final Context context;

    public ModernMarkdownThemePlugin(Context context) {
        this.context = context;
    }

    @Override
    public void configureTheme(@NonNull MarkwonTheme.Builder builder) {
        final float density = context.getResources().getDisplayMetrics().density;
        final float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;

        // 豆包风格颜色配置
        builder
                // 链接颜色 - 豆包类似的蓝色
                .linkColor(Color.parseColor("#165DFF"))

                // 代码块样式 - 更柔和的背景
                .codeBlockBackgroundColor(Color.parseColor("#F5F7FA"))
                .codeBlockTextColor(Color.parseColor("#333333"))
                .codeBlockMargin((int) (12 * density))

                // 内联代码样式
                .codeBackgroundColor(Color.parseColor("#e6e6e6"))
                .codeTextColor(Color.parseColor("#D81F26"))
                .codeTextSize((int) (15 * scaledDensity))

                // 引用块样式
                .blockQuoteColor(Color.parseColor("#165DFF"))
                .blockQuoteWidth((int) (3 * density))

                // 标题样式优化 - 这里也缩小了标题倍数
                .headingBreakColor(Color.parseColor("#333333"))
                .headingBreakHeight((int) (1 * density))
                // 标题大小倍数缩小一圈
                .headingTextSizeMultipliers(new float[]{1.4f, 1.3f, 1.2f, 1.1f, 1.05f, 1.0f})
                // 增加标题的段落间距，改善与正文的视觉分隔
                .headingTypeface(Typeface.DEFAULT_BOLD)

                // 分割线样式
                .thematicBreakColor(Color.parseColor("#E5E6EB"))
                .thematicBreakHeight((int) (1 * density))

                // 列表样式
                .listItemColor(Color.parseColor("#333333"))
                .bulletListItemStrokeWidth((int) (1.5f * density))

                // 间距配置 - 更舒适的阅读体验
                .blockMargin((int) (32 * density))
                // 标题间距配置 - 增加标题下方的分隔线高度来增加间距
                .headingBreakHeight((int) (8 * density));
    }

    @Override
    public void configure(@NonNull Registry registry) {
        // 配置全局文本样式，设置与豆包一致的字体大小和行间距
        registry.require(CorePlugin.class, corePlugin -> {
            corePlugin.addOnTextAddedListener((markdown, text, start) -> {
                // 设置字体大小为17sp，接近豆包的主要字体大小
                markdown.setSpans(start, new AbsoluteSizeSpan(14, true));

                // 设置自定义字体，同时保留样式（如加粗）
                android.graphics.Typeface customTypeface = FontCacheManager.getInstance().getTypeface(context, "fonts/jjyh.ttf");
                markdown.setSpans(start, CustomTypefaceSpan.create(customTypeface));

                // 设置正文内容对齐方式，让文字平铺整个容器
                markdown.setSpans(start, new AlignmentSpan.Standard(Layout.Alignment.ALIGN_NORMAL));

                // 行间距现在在 MarkdownUtils 中通过 TextView.setLineSpacing() 统一设置
                // 这样可以避免标题换行时行间距过大的问题
            });
        });
    }

    @Override
    public void configureSpansFactory(@NonNull MarkwonSpansFactory.Builder builder) {
        // 自定义span样式
        builder
                // 强调文本样式
                .setFactory(Emphasis.class, new SpanFactory() {
                    @Override
                    public Object getSpans(@NonNull MarkwonConfiguration configuration, @NonNull RenderProps renderProps) {
                        return new StyleSpan(Typeface.ITALIC);
                    }
                })

                // 加粗文本样式
                .setFactory(StrongEmphasis.class, (configuration, renderProps) -> {
                    return new StyleSpan(Typeface.BOLD);
                })

                // 标题样式增强 - 增大与正文的间距
                .setFactory(Heading.class, new SpanFactory() {
                    @Override
                    public Object getSpans(@NonNull MarkwonConfiguration configuration, @NonNull RenderProps renderProps) {
                        final int level = renderProps.get(CoreProps.HEADING_LEVEL, 1);
                        final float density = context.getResources().getDisplayMetrics().density;

                        // 根据标题级别设置不同的颜色
                        int color;
                        switch (level) {
                            case 1:
                                color = Color.parseColor("#333333");
                                break;
                            case 2:
                                color = Color.parseColor("#333333");
                                break;
                            case 3:
                                color = Color.parseColor("#333333");
                                break;
                            default:
                                color = Color.parseColor("#333333");
                                break;
                        }

                        // 定义标题大小倍数 - 这里也缩小了标题倍数
                        float[] headingSizes = {1.2f, 1.15f, 1.1f, 1.05f, 1.0f, 0.95f};
                        float sizeMultiplier = level <= headingSizes.length ? headingSizes[level - 1] : 1.0f;

                        return new Object[]{
                                new StyleSpan(Typeface.BOLD),
                                new ForegroundColorSpan(color),
                                new RelativeSizeSpan(sizeMultiplier),
                                // 标题靠左对齐
                                new AlignmentSpan.Standard(Layout.Alignment.ALIGN_NORMAL),
                                // 添加标题与正文之间的间距
                                HeadingPaddingSpan.create(level, density)
                        };
                    }
                })

                // 内联代码样式增强
                .setFactory(Code.class, new SpanFactory() {
                    @Override
                    public Object getSpans(@NonNull MarkwonConfiguration configuration, @NonNull RenderProps renderProps) {
                        return new Object[]{
                                new BackgroundColorSpan(Color.parseColor("#F5F7FA")),
                                new ForegroundColorSpan(Color.parseColor("#D81F26")),
                                new TypefaceSpan("monospace"),
                                new RelativeSizeSpan(0.95f)
                        };
                    }
                })

                // 链接样式增强
                .setFactory(Link.class, new SpanFactory() {
                    @Override
                    public Object getSpans(@NonNull MarkwonConfiguration configuration, @NonNull RenderProps renderProps) {
                        return new Object[]{
                                new ForegroundColorSpan(Color.parseColor("#165DFF")),
                                new UnderlineSpan()
                        };
                    }
                });
    }
}