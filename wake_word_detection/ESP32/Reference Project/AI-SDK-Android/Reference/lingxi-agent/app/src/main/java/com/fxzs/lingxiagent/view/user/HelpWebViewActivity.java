package com.fxzs.lingxiagent.view.user;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.viewmodel.user.VMHelpWebView;

public class HelpWebViewActivity extends BaseActivity<VMHelpWebView> {
    
    private ImageView ivBack;
    private TextView tvTitle;
    private TextView tvFeedback;
    private WebView webView;
    
    @Override
    protected int getLayoutResource() {
        return R.layout.activity_help_webview;
    }
    
    @Override
    protected Class<VMHelpWebView> getViewModelClass() {
        return VMHelpWebView.class;
    }
    
    @Override
    protected void initializeViews() {
        ivBack = findViewById(R.id.iv_back);
        tvTitle = findViewById(R.id.tv_title);
        tvFeedback = findViewById(R.id.tv_feedback);
        webView = findViewById(R.id.webview);
        
        tvTitle.setText("帮助与反馈");
        
        // 配置WebView
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        
        // 加载帮助页面URL
        String helpUrl = viewModel.getHelpUrl();
        webView.loadUrl(helpUrl);
        
        // 设置点击事件
        ivBack.setOnClickListener(v -> finish());
        
        // 反馈文字点击事件
        tvFeedback.setOnClickListener(v -> {
            Intent intent = new Intent(this, FeedbackActivity.class);
            startActivity(intent);
        });
    }
    
    @Override
    protected void setupDataBinding() {
        // WebView页面不需要特殊的数据绑定
    }
    
    @Override
    protected void setupObservers() {
        // 观察加载状态
        viewModel.getLoadingState().observe(this, isLoading -> {
            if (isLoading != null) {
                // 加载状态处理可以在这里添加
            }
        });
    }
    
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 设置状态栏颜色为白色，与背景一致，并保证内容不被遮挡
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(Color.parseColor("#FFFFFF"));
            getWindow().getDecorView().postDelayed(() -> {
                getWindow().setStatusBarColor(Color.parseColor("#FFFFFF"));
            }, 100);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }
}