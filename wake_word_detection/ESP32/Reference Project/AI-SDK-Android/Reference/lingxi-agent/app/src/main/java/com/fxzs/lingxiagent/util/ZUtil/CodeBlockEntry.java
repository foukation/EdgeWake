package com.fxzs.lingxiagent.util.ZUtil;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.view.code.CodeFullscreenActivity;

import org.commonmark.node.FencedCodeBlock;

import io.noties.markwon.Markwon;
import io.noties.markwon.recycler.MarkwonAdapter;

/**
 * 代码块Entry，用于自定义代码块的渲染
 */
public class CodeBlockEntry extends MarkwonAdapter.Entry<FencedCodeBlock, CodeBlockEntry.Holder> {
    
    private final Context context;
    
    public static CodeBlockEntry create(Context context) {
        return new CodeBlockEntry(context);
    }
    
    private CodeBlockEntry(Context context) {
        this.context = context;
    }
    
    public int layoutResId() {
        return R.layout.item_code_block;
    }
    
    @Override
    public Holder createHolder(LayoutInflater inflater, ViewGroup parent) {
        View view = inflater.inflate(layoutResId(), parent, false);
        return new Holder(view);
    }

    @Override
    public void bindHolder(@NonNull Markwon markwon, @NonNull Holder holder, @NonNull FencedCodeBlock fencedCodeBlock) {
        // 获取代码内容和语言
        String codeContent = fencedCodeBlock.getLiteral();
        String language = fencedCodeBlock.getInfo();
        
        if (codeContent == null) {
            codeContent = "";
        }
        if (language == null || language.isEmpty()) {
            language = "code";
        }
        
        // 设置内容
        holder.tvLanguage.setText(language.toLowerCase());

        // 应用语法高亮
        SyntaxHighlighter highlighter = new SyntaxHighlighter(context);
        SpannableStringBuilder highlightedCode = highlighter.highlight(codeContent, language);
        holder.tvCodeContent.setText(highlightedCode);
        
        // 设置复制按钮点击事件
        final String finalCodeContent = codeContent;
        holder.cvCopy.setOnClickListener(v ->
            CodeBlockPlugin.copyCodeToClipboard(context, finalCodeContent));

        // 设置全屏按钮点击事件
        final String finalLanguage = language;
        holder.cvFullscreen.setOnClickListener(v ->
            CodeFullscreenActivity.start(context, finalCodeContent, finalLanguage));
    }



    /**
     * ViewHolder for code block
     */
    public static class Holder extends MarkwonAdapter.Holder {

        final TextView tvLanguage;
        final TextView tvCodeContent;
        final ImageView ivCopy;
        final ImageView ivFullscreen;
        final CardView cvCopy;
        final CardView cvFullscreen;

        public Holder(@NonNull View itemView) {
            super(itemView);
            tvLanguage = itemView.findViewById(R.id.tv_language);
            tvCodeContent = itemView.findViewById(R.id.tv_code_content);
            ivCopy = itemView.findViewById(R.id.iv_copy);
            ivFullscreen = itemView.findViewById(R.id.iv_fullscreen);
            cvCopy = itemView.findViewById(R.id.cv_copy);
            cvFullscreen = itemView.findViewById(R.id.cv_fullscreen);
        }
    }
}
