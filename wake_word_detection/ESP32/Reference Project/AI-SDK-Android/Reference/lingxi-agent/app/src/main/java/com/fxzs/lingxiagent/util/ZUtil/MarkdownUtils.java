package com.fxzs.lingxiagent.util.ZUtil;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.target.Target;
import com.fxzs.lingxiagent.util.markdown.MarkdownStyle;

import org.commonmark.node.ListItem;
import org.commonmark.node.StrongEmphasis;
import org.jetbrains.annotations.NotNull;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.MarkwonSpansFactory;
import io.noties.markwon.SoftBreakAddsNewLinePlugin;
import io.noties.markwon.core.CorePlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.image.AsyncDrawable;
import io.noties.markwon.image.ImagesPlugin;
import io.noties.markwon.image.glide.GlideImagesPlugin;
import io.noties.markwon.recycler.table.TableEntryPlugin;
import io.noties.markwon.syntax.Prism4jThemeDefault;
import io.noties.markwon.syntax.SyntaxHighlightPlugin;
import io.noties.prism4j.Prism4j;
import timber.log.Timber;

public class MarkdownUtils {
    private static final String TAG = "MarkdownUtils";
    private static final String FONT_PATH = "fonts/jjyh.ttf";

    public static Markwon.Builder createBuilder(Context context) {
        return Markwon.builder(context)
                .usePlugin(CorePlugin.create())
//                .usePlugin(MarkwonInlineParserPlugin.create())
//                .usePlugin(JLatexMathPlugin.create(30, builder -> {
//                    builder.inlinesEnabled(true);
//                    builder.errorHandler((latex, error) -> {
//                        Log.d("==================", latex + ",,,," + error.getMessage());
//                        return null;
//                    });
//
//                    builder.executorService(Executors.newCachedThreadPool());
//                }))
                .usePlugin(new ModernMarkdownThemePlugin(context))
                .usePlugin(TextAlignmentPlugin.create(context)) // 使用文本对齐插件
                // 图片：先用 ImagesPlugin 设置尺寸解析与占位，再用 GlideImagesPlugin 负责加载
                .usePlugin(ImagesPlugin.create())
                .usePlugin(GlideImagesPlugin.create(context))
                .usePlugin(GlideImagesPlugin.create(Glide.with(context)))
                .usePlugin(GlideImagesPlugin.create(new GlideImagesPlugin.GlideStore() {
                    @Override
                    public void cancel(Target<?> target) {
                        Glide.with(context).clear(target);
                    }

                    @Override
                    public RequestBuilder<Drawable> load(AsyncDrawable drawable) {
                        return Glide.with(context).load(drawable.getDestination());
                    }
                }))
                .usePlugin(TablePlugin.create(context))
                .textSetter(new Markwon.TextSetter() {
                    @Override
                    public void setText(@NonNull @NotNull TextView textView, @NonNull @NotNull Spanned spanned, @NonNull @NotNull TextView.BufferType bufferType, @NonNull @NotNull Runnable runnable) {
                        // 字体设置现在在 ModernMarkdownThemePlugin 中通过 Span 处理
                        // 设置自定义字体，同时保留样式（如加粗）
//                        android.graphics.Typeface customTypeface = FontCacheManager.getInstance().getTypeface(context, "fonts/jjyh.ttf");
//
//                        // 设置字体
//                        textView.setTypeface(customTypeface);

                        // 这样可以保留加粗等样式效果
                        textView.setTextSize(MarkdownStyle.COT_TEXT_SIZE_SP);

                        // 设置合适的行间距，避免标题换行时行间距过大
                        textView.setLineSpacing(0, 1.2f); // 1.2倍行间距

                        // 增加TextView的内边距，改善整体间距效果
                        int padding = (int) (4 * context.getResources().getDisplayMetrics().density);
                        textView.setPadding(textView.getPaddingLeft(), padding,
                                textView.getPaddingRight(), padding);

                        // 设置文本对齐方式：正文内容平铺整个容器
                        TextAlignmentHelper.setBodyAlignment(textView);

                        // 设置文本内容
                        textView.setText(spanned);

                        textView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
                        textView.setClickable(true);
                    }
                })
                .usePlugin(TableEntryPlugin.create(builder1 -> builder1
                        .tableHeaderRowBackgroundColor(0xDAD5DADC)
                        .tableBorderWidth(0)))
                .usePlugin(LinkWebPlugin.create(context))
                .usePlugin(TableEntryPlugin.create(builder1 -> builder1
                        .tableHeaderRowBackgroundColor(0xDAD5DADC)
                        .tableBorderWidth(0)))
                .usePlugin(new AbstractMarkwonPlugin() {
                    @Override
                    public void configureSpansFactory(@NonNull MarkwonSpansFactory.Builder builder) {
                        builder.setFactory(ListItem.class, (configuration, props) -> {
                            int gapWidth = configuration.theme().getBlockMargin();
                            return new Object[]{
                                    new MdSmallerBulletSpan(gapWidth, Color.parseColor("#FF7F7F7F"), 5)
                            };
                        });
                    }
                });
    }

    public static Markwon createMarkwon(Context context) {
        Markwon.Builder builder = createBuilder(context);

        // 尝试添加语法高亮，如果失败则忽略
        try {
            final Prism4j prism4j = new Prism4j(SimplePrismGrammarLocator.getInstance());
            builder.usePlugin(SyntaxHighlightPlugin.create(prism4j, Prism4jThemeDefault.create()));
        } catch (Exception e) {
            // 如果语法高亮失败，继续使用基础功能
            Timber.tag(TAG).w("Failed to initialize syntax highlighting: " + e.getMessage());
        }

        return builder.build();
    }

    public static Markwon createMdForLx(Context context) {
        Markwon.Builder builder = createBuilder(context)
                .usePlugin(new AbstractMarkwonPlugin() {
                    @Override
                    public void configureSpansFactory(@NonNull MarkwonSpansFactory.Builder builder) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            builder.appendFactory(StrongEmphasis.class, (configuration, props) -> new Object[]{
                                    new StyleSpan(Typeface.BOLD),
                            });
                        }
                    }
                })
                .usePlugin(SoftBreakAddsNewLinePlugin.create());
        return builder.build();
    }

    /**
     * 创建优化的Markdown渲染器
     */
    public static MarkdownRenderer createRenderer(Context context) {
        return new MarkdownRenderer(context);
    }

    /**
     * 便捷方法：智能渲染Markdown到TextView
     */
    public static void renderSmart(Context context, String markdown, TextView textView) {
        MarkdownRenderer renderer = createRenderer(context);
        if (markdown != null && markdown.length() > 1000) {
            renderer.renderLargeMarkdown(markdown, textView, null);
        } else {
            renderer.renderWithCache(markdown, textView);
        }
    }

    /**
     * 预加载字体，建议在应用启动时调用
     *
     * @param context 应用上下文
     */
    public static void preloadFonts(Context context) {
        FontCacheManager.getInstance().preloadFont(context, FONT_PATH);
        Timber.tag(TAG).d("Font preloading initiated");
    }

    /**
     * 清理字体缓存，建议在内存紧张时调用
     */
    public static void clearFontCache() {
        FontCacheManager.getInstance().clearCache();
        Timber.tag(TAG).d("Font cache cleared");
    }

    /**
     * 验证自定义字体是否正确加载
     *
     * @param context 应用上下文
     */
    public static void verifyCustomFont(Context context) {
        Timber.tag(TAG).d("=== Custom Font Verification ===");

        // 检查字体是否在缓存中
        FontCacheManager fontManager = FontCacheManager.getInstance();
        boolean isCached = fontManager.isFontCached(FONT_PATH);
        Timber.tag(TAG).d("Font cached: " + isCached);

        // 尝试加载字体
        Typeface customFont = fontManager.getTypeface(context, FONT_PATH);
        Timber.tag(TAG).d("Font loaded: " + (customFont != null));
        Timber.tag(TAG).d("Is default font: " + (customFont == Typeface.DEFAULT));

        if (customFont != null && customFont != Typeface.DEFAULT) {
            Timber.tag(TAG).d("✅ Custom font is working!");
        } else {
            Timber.tag(TAG).w("❌ Custom font may not be working properly");
        }

        Timber.tag(TAG).d("================================");
    }
}
