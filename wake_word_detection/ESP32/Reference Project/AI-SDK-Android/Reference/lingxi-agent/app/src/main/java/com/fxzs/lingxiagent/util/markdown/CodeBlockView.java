package com.fxzs.lingxiagent.util.markdown;

import android.content.Context;
import android.graphics.Typeface;
import android.text.ClipboardManager;
import android.text.SpannableStringBuilder;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.fxzs.lingxiagent.util.ZUtil.SyntaxHighlighter;

public class CodeBlockView extends HorizontalScrollView {
    private final TextView codeText;
    private final SyntaxHighlighter syntaxHighlighter;

    public CodeBlockView(Context context) {
        this(context, null);
    }

    public CodeBlockView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setFillViewport(true);
        setOverScrollMode(OVER_SCROLL_IF_CONTENT_SCROLLS);

        syntaxHighlighter = new SyntaxHighlighter(context);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (12 * context.getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);
        container.setBackgroundColor(0xFFF6F6F6);

        codeText = new TextView(context);
        codeText.setTypeface(Typeface.MONOSPACE);
        codeText.setTextSize(13);
        codeText.setLineSpacing(0, 1.15f);
        codeText.setTextColor(0xFF333333);
        codeText.setGravity(Gravity.START);

        container.addView(codeText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        addView(container, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        ));

        // 长按复制
        container.setOnLongClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setText(codeText.getText());
            return true;
        });
    }

    public void setCodeText(CharSequence text) {
        codeText.setText(text);
    }

    public void setCodeText(CharSequence text, @Nullable String language) {
        if (language == null || language.isEmpty()) {
            setCodeText(text);
            return;
        }
        try {
            SpannableStringBuilder out = syntaxHighlighter.highlight(text.toString(), language);
            codeText.setText(out);
        } catch (Throwable t) {
            setCodeText(text);
        }
    }

    public void appendCodeText(CharSequence increment) {
        codeText.append(increment);
    }

    public CharSequence getCodeText() {
        return codeText.getText();
    }


}

