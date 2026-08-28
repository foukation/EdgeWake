package com.fxzs.lingxiagent.view.common;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.viewmodel.main.VMEmpty;

public class WebViewActivity extends BaseActivity<VMEmpty> {

    private static final String EXTRA_URL = "extra_url";
    private static final String EXTRA_TITLE = "extra_title";

    private ImageView ivBack;
    private TextView tvTitle;
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_webview;
    }

    @Override
    protected Class<VMEmpty> getViewModelClass() {
        return VMEmpty.class;
    }

    @Override
    protected void setupDataBinding() {

    }

    @Override
    protected void initializeViews() {
        ivBack = findViewById(R.id.iv_back);
        tvTitle = findViewById(R.id.tv_title);
        webView = findViewById(R.id.webview);

        // 设置返回按钮
        ivBack.setOnClickListener(v -> finish());

        // 获取传递的数据
        String url = getIntent().getStringExtra(EXTRA_URL);
        String title = getIntent().getStringExtra(EXTRA_TITLE);

        // 设置标题
        if (title != null) {
            tvTitle.setText(title);
        }

        webView.setBackgroundColor(0);
        webView.getBackground().setAlpha(0);
        // 配置WebView
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);

        // 设置WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.endsWith(".apk")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (url.startsWith("http")) {
                    view.loadUrl(url);
                } else if (url.startsWith("alipays://")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    } catch (ActivityNotFoundException e) {
                        GlobalToast.show(WebViewActivity.this, "请安装支付宝", GlobalToast.Type.ERROR);
                    } catch (Exception e) {
                        // 如果无法打开支付宝App，打开浏览器
                        try {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            startActivity(browserIntent);
                            return true;
                        } catch (Exception ex) {
                            e.printStackTrace();
                        }
                    }
                }
                return true;
            }
        });

        // 加载URL
        if (url != null) {
            webView.loadUrl(url);
        }
    }

    @Override
    protected void setupObservers() {

    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * 启动WebView Activity的静态方法
     */
    public static void start(Context context, String url, String title) {
        Intent intent = new Intent(context, WebViewActivity.class);
        intent.putExtra(EXTRA_URL, url);
        intent.putExtra(EXTRA_TITLE, title);
        context.startActivity(intent);
    }
}