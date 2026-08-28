package com.fxzs.lingxiagent.view.code;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.ZUtil.SyntaxHighlighter;

/**
 * 全屏代码查看Activity
 */
public class CodeFullscreenActivity extends AppCompatActivity {
    
    public static final String EXTRA_CODE_CONTENT = "code_content";
    public static final String EXTRA_LANGUAGE = "language";
    
    private TextView tvLanguageFullscreen;
    private TextView tvCodeContentFullscreen;
    private CardView cvBack;
    private CardView cvCopyFullscreen;
    private CardView cvShare;
    
    private String codeContent;
    private String language;
    
    public static void start(Context context, String codeContent, String language) {
        Intent intent = new Intent(context, CodeFullscreenActivity.class);
        intent.putExtra(EXTRA_CODE_CONTENT, codeContent);
        intent.putExtra(EXTRA_LANGUAGE, language);
        context.startActivity(intent);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_code_fullscreen);
        
        initViews();
        initData();
        setupListeners();
    }
    
    private void initViews() {
        tvLanguageFullscreen = findViewById(R.id.tv_language_fullscreen);
        tvCodeContentFullscreen = findViewById(R.id.tv_code_content_fullscreen);
        cvBack = findViewById(R.id.cv_back);
        cvCopyFullscreen = findViewById(R.id.cv_copy_fullscreen);
        cvShare = findViewById(R.id.cv_share);
    }
    
    private void initData() {
        codeContent = getIntent().getStringExtra(EXTRA_CODE_CONTENT);
        language = getIntent().getStringExtra(EXTRA_LANGUAGE);
        
        if (codeContent == null) {
            codeContent = "";
        }
        if (language == null) {
            language = "Code";
        }
        
        tvLanguageFullscreen.setText(language.toUpperCase());

        // 应用语法高亮
        SyntaxHighlighter highlighter = new SyntaxHighlighter(this);
        SpannableStringBuilder highlightedCode = highlighter.highlight(codeContent, language);
        tvCodeContentFullscreen.setText(highlightedCode);
    }
    
    private void setupListeners() {
        cvBack.setOnClickListener(v -> finish());

        cvCopyFullscreen.setOnClickListener(v -> copyCodeToClipboard());

        cvShare.setOnClickListener(v -> shareCode());
    }
    
    private void copyCodeToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Code", codeContent);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
    }
    
    private void shareCode() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, codeContent);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, language + " 内容");
        startActivity(Intent.createChooser(shareIntent, "分享内容"));
    }
}
