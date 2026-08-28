package com.fxzs.lingxiagent.view.chat.delegate;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.main.utils.ScreenUtils;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.model.honor.dto.HtmlInfo;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;

import java.net.URLDecoder;

import timber.log.Timber;

/**
 * WebView卡片委托
 * 负责处理使用WebView显示的智能体卡片，包括：
 * - WebView配置和设置
 * - SSL证书处理
 * - URL重定向和跳转处理
 * - 页面高度动态调整
 * - 复杂URL解析和处理
 */
public class WebViewCardDelegate extends CardMessageDelegate {
    
    private static final String TAG = "WebViewCardDelegate";
    
    public WebViewCardDelegate() {
        super(ChatAdapter.TYPE_ASSISTANT_CARD, R.layout.lingxi_card_webview);
    }
    
    @Override
    protected RecyclerView.ViewHolder createViewHolder(View view) {
        return new ChatAdapter.ChatViewHolder(view);
    }
    
    @Override
    protected void onBindViewHolderInternal(RecyclerView.ViewHolder holder, ChatMessage message, 
                                           int position, ChatAdapterContext context) {
        ChatAdapter.ChatViewHolder cardHolder = (ChatAdapter.ChatViewHolder) holder;
        
        Timber.tag(TAG).d( "setAssistantCard: position=" + position);
        
        // 获取卡片信息
        HtmlInfo cardInfo = message.getCardInfo();
        if (cardInfo == null) {
            Timber.tag(TAG).w("CardInfo is null, cannot display WebView card");
            return;
        }
        
        // 设置WebView配置和内容
        setupWebView(cardHolder, cardInfo, context);
    }
    
    @Override
    protected Class<? extends RecyclerView.ViewHolder> getExpectedViewHolderClass() {
        return ChatAdapter.ChatViewHolder.class;
    }
    
    /**
     * 设置WebView的配置和内容加载
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(ChatAdapter.ChatViewHolder holder, HtmlInfo cardInfo, ChatAdapterContext context) {
        if (holder.webView == null) {
            Timber.tag(TAG).w("WebView is null");
            return;
        }
        
        // 配置WebView设置，完全保持与原有逻辑一致
        WebSettings settings = holder.webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        // 设置WebViewClient处理页面加载和URL跳转
        holder.webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                
                // 动态调整WebView高度
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
                int height = 500; // 默认高度
                if (cardInfo.getHeight() != -1) {
                    height = cardInfo.getHeight();
                }
                params.height = ScreenUtils.INSTANCE.dpToPx(height, view.getContext());
                view.setLayoutParams(params);
                
                Timber.tag(TAG).d( "onPageFinished: Set WebView height to " + height + "dp");
            }
            
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String webUrl = request.getUrl().toString();
                return handleUrlLoading(view.getContext(), webUrl);
            }
            
            @SuppressLint("WebViewClientOnReceivedSslError")
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                // 忽略SSL证书错误，保持与原有逻辑一致
                handler.proceed();
                Timber.tag(TAG).d( "onReceivedSslError: Proceeding despite SSL error");
            }
        });
        
        // 加载URL
        String fullUrl = cardInfo.getUrl() + cardInfo.getParams();
        holder.webView.loadUrl(fullUrl);
        Timber.tag(TAG).d( "setupWebView: Loading URL: " + fullUrl);
    }
    
    /**
     * 处理URL加载和跳转逻辑，完全保持与原有代码一致
     */
    private boolean handleUrlLoading(Context context, String webUrl) {
        try {
            if (webUrl.contains("bdhonorbrowser://v1")) {
                // 处理特殊的浏览器协议URL
                String webUrlResult = processSpecialUrl(webUrl);
                
                if (webUrlResult.contains("travel_planner")) {
                    // 直接使用系统浏览器打开旅行规划链接
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(webUrlResult));
                    context.startActivity(intent);
                } else {
                    // 使用应用内WebView打开
                    startWebViewActivity(context, webUrlResult, "灵犀-出行规划");
                }
            } else {
                // 普通URL使用应用内WebView打开
                startWebViewActivity(context, webUrl, "灵犀-出行规划");
            }
            
            Timber.tag(TAG).d( "handleUrlLoading: Handled URL: " + webUrl);
            return true;
            
        } catch (Exception e) {
            Timber.tag(TAG).e( "handleUrlLoading: Error processing URL: " + webUrl, e);
            return false;
        }
    }
    
    /**
     * 处理特殊协议URL的解析
     */
    private String processSpecialUrl(String webUrl) {
        try {
            String commonUrl = webUrl.replace("bdhonorbrowser://v1/browser/open?upgrade=1&url=", "");
            return URLDecoder.decode(commonUrl);
        } catch (Exception e) {
            Timber.tag(TAG).e( "processSpecialUrl: Error decoding URL", e);
            return webUrl;
        }
    }
}