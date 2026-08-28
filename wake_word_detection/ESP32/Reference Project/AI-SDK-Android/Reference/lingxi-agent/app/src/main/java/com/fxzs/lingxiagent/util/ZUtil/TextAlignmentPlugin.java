package com.fxzs.lingxiagent.util.ZUtil;

import android.content.Context;
import android.text.Layout;
import android.text.style.AlignmentSpan;

import androidx.annotation.NonNull;

import org.commonmark.node.BlockQuote;
import org.commonmark.node.Code;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Paragraph;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.MarkwonConfiguration;
import io.noties.markwon.MarkwonSpansFactory;
import io.noties.markwon.RenderProps;
import io.noties.markwon.SpanFactory;

/**
 * 文本对齐插件
 * 为不同类型的Markdown内容设置合适的对齐方式
 */
public class TextAlignmentPlugin extends AbstractMarkwonPlugin {
    
    private final Context context;
    
    public TextAlignmentPlugin(Context context) {
        this.context = context;
    }
    
    @Override
    public void configureSpansFactory(@NonNull MarkwonSpansFactory.Builder builder) {
        // 段落对齐 - 正文内容平铺整个容器
        builder.setFactory(Paragraph.class, new SpanFactory() {
            @Override
            public Object getSpans(@NonNull MarkwonConfiguration configuration, @NonNull RenderProps renderProps) {
                // 正文段落使用自然对齐，配合TextView的justification设置
                return new AlignmentSpan.Standard(Layout.Alignment.ALIGN_NORMAL);
            }
        });
        
        // 注意：标题对齐在 ModernMarkdownThemePlugin 中处理，这里不重复设置
        // 避免覆盖标题的加粗、大小等样式
        
        // 代码块对齐 - 靠左对齐
        builder.setFactory(FencedCodeBlock.class, new SpanFactory() {
            @Override
            public Object getSpans(@NonNull MarkwonConfiguration configuration, @NonNull RenderProps renderProps) {
                // 代码块使用左对齐，保持代码格式
                return new AlignmentSpan.Standard(Layout.Alignment.ALIGN_NORMAL);
            }
        });
        
        builder.setFactory(IndentedCodeBlock.class, new SpanFactory() {
            @Override
            public Object getSpans(@NonNull MarkwonConfiguration configuration, @NonNull RenderProps renderProps) {
                // 缩进代码块使用左对齐
                return new AlignmentSpan.Standard(Layout.Alignment.ALIGN_NORMAL);
            }
        });
        
        // 内联代码对齐 - 跟随父容器
        builder.setFactory(Code.class, new SpanFactory() {
            @Override
            public Object getSpans(@NonNull MarkwonConfiguration configuration, @NonNull RenderProps renderProps) {
                // 内联代码跟随父容器的对齐方式
                return new AlignmentSpan.Standard(Layout.Alignment.ALIGN_NORMAL);
            }
        });
        
        // 引用块对齐 - 可以使用轻微的两端对齐
        builder.setFactory(BlockQuote.class, new SpanFactory() {
            @Override
            public Object getSpans(@NonNull MarkwonConfiguration configuration, @NonNull RenderProps renderProps) {
                // 引用块使用自然对齐
                return new AlignmentSpan.Standard(Layout.Alignment.ALIGN_NORMAL);
            }
        });
    }
    
    /**
     * 创建文本对齐插件实例
     * @param context 上下文
     * @return TextAlignmentPlugin实例
     */
    public static TextAlignmentPlugin create(Context context) {
        return new TextAlignmentPlugin(context);
    }
}
