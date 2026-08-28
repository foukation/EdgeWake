package com.fxzs.lingxiagent.util.markdown;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.ZUtil.CodeBlockPlugin;
import com.fxzs.lingxiagent.util.ZUtil.SyntaxHighlighter;
import com.fxzs.lingxiagent.view.code.CodeFullscreenActivity;

/**
 * A compound view for rendering a code block with header toolbar (language, copy, fullscreen)
 * matching the style of item_code_block.xml used in Markwon path.
 */
public class CodeBlockContainerView extends LinearLayout {

    private TextView tvLanguage;
    private TextView tvCodeContent;
    private CardView cvCopy;
    private CardView cvFullscreen;

    private final SyntaxHighlighter syntaxHighlighter;
    private String language = "code";

    public CodeBlockContainerView(Context context) {
        this(context, null);
    }

    public CodeBlockContainerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.item_code_block, this, true);
        tvLanguage = findViewById(R.id.tv_language);
        tvCodeContent = findViewById(R.id.tv_code_content);
        cvCopy = findViewById(R.id.cv_copy);
        cvFullscreen = findViewById(R.id.cv_fullscreen);
        syntaxHighlighter = new SyntaxHighlighter(context);

        // actions
        cvCopy.setOnClickListener(v -> {
            CharSequence code = tvCodeContent.getText();
            CodeBlockPlugin.copyCodeToClipboard(getContext(), code != null ? code.toString() : "");
        });
        cvFullscreen.setOnClickListener(v -> {
            CharSequence code = tvCodeContent.getText();
            CodeFullscreenActivity.start(getContext(), code != null ? code.toString() : "", language);
        });
    }

    public void setCodeText(CharSequence text) {
        setCodeText(text, language);
    }

    public void setCodeText(CharSequence text, @Nullable String lang) {
        if (lang == null || lang.isEmpty()) {
            lang = "code";
        }
        this.language = lang;
        tvLanguage.setText(lang.toLowerCase());
        try {
            SpannableStringBuilder out = syntaxHighlighter.highlight(text != null ? text.toString() : "", lang);
            tvCodeContent.setText(out);
        } catch (Throwable t) {
            tvCodeContent.setText(text);
        }
    }

    public void appendCodeText(CharSequence increment) {
        if (increment == null || increment.length() == 0) return;
        // Simple append without re-highlighting to keep it smooth in streaming
        tvCodeContent.append(increment);
    }

    public CharSequence getCodeText() {
        return tvCodeContent.getText();
    }
}

