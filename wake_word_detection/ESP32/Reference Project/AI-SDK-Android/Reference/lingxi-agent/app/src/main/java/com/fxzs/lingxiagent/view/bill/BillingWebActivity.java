package com.fxzs.lingxiagent.view.bill;

import static com.fxzs.lingxiagent.model.billing.network.TokenInterceptor.X_APP_ID;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.fxzs.lingxiagent.R;
import java.net.URLEncoder;
import timber.log.Timber;

public class BillingWebActivity extends AppCompatActivity {

    private ImageView ivBack;
    private TextView tvTitle;
    private WebView webView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_billing_web);

        initView();
        initWebView();
        loadUrl();
    }

    private void initView() {
        ivBack = findViewById(R.id.iv_back);
        tvTitle = findViewById(R.id.tv_title);
        webView = findViewById(R.id.web_view);
        progressBar = findViewById(R.id.progress_bar);

        tvTitle.setText("灵犀权益包");

        ivBack.setOnClickListener(v -> {
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                finish();
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {

        WebSettings settings = webView.getSettings();

        // JS
        settings.setJavaScriptEnabled(true);

        // H5 storage
        settings.setDomStorageEnabled(true);

        // 自适应
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        // 支持缩放
        settings.setSupportZoom(false);

        // cache
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // 允许混合内容
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // file
        settings.setAllowFileAccess(true);

        // 自动播放
        settings.setMediaPlaybackRequiresUserGesture(false);

        // 调试
        WebView.setWebContentsDebuggingEnabled(true);

        // WebViewClient
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }
        });

        // Chrome
        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onProgressChanged(WebView view, int newProgress) {

                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                }
            }
        });
    }

    private void loadUrl() {

        String base =
                "https://ivs.chinamobiledevice.com:31557/billing-web/service/paymethod";

        // token
        String billToken = getIntent().getStringExtra("billToken");

        // deviceList json
        String deviceList = getIntent().getStringExtra("deviceList");

        String parentOrigin = "https://ivs.chinamobiledevice.com:11443";
        try {
            String url = base
                    + "?&parentOrigin=" + URLEncoder.encode(parentOrigin, "UTF-8")
                    + "&app_id=" + URLEncoder.encode(X_APP_ID, "UTF-8")
                    + "&pc_token=" + URLEncoder.encode(billToken, "UTF-8")
                    + "&device_list=" + deviceList;

            Timber.tag("BillingWebActivity").d("webviewUrl：" + url);

            webView.loadUrl(url);

        } catch (Exception e) {
            e.printStackTrace();
        }
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
    protected void onDestroy() {

        if (webView != null) {

            webView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            webView.clearHistory();
            webView.destroy();
        }

        super.onDestroy();
    }
}