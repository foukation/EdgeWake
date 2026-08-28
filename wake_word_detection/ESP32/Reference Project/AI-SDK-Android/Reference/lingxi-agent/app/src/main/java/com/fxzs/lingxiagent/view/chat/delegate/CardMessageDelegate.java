package com.fxzs.lingxiagent.view.chat.delegate;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.fxzs.lingxiagent.view.common.WebViewActivity;

import timber.log.Timber;

/**
 * 卡片消息委托基类
 * 提供卡片消息的通用功能，包括：
 * - WebView页面跳转的通用方法
 * - Intent创建和启动的辅助方法
 * - 卡片交互的标准接口
 * 
 * 各种具体的卡片类型可以继承此基类或直接使用BaseViewTypeDelegate
 */
public abstract class CardMessageDelegate extends BaseViewTypeDelegate {
    
    private static final String TAG = "CardMessageDelegate";
    
    public CardMessageDelegate(int viewType, int layoutRes) {
        super(viewType, layoutRes);
    }
    
    /**
     * 创建WebView页面跳转Intent的通用方法
     */
    protected void startWebViewActivity(Context context, String url, String title) {
        try {
            Intent intent = new Intent(context, WebViewActivity.class);
            intent.putExtra("extra_url", url);
            intent.putExtra("extra_title", title != null ? title : "灵犀");
            context.startActivity(intent);
            Timber.tag(TAG).d( "startWebViewActivity: Started WebView with URL: " + url);
        } catch (Exception e) {
            Timber.tag(TAG).e( "startWebViewActivity: Failed to start WebView", e);
        }
    }
    
    /**
     * 创建系统设置页面跳转Intent的通用方法
     */
    protected void startSystemSettings(Context context, String action) {
        try {
            Intent intent = new Intent(action);
            context.startActivity(intent);
            Timber.tag(TAG).d( "startSystemSettings: Started system settings with action: " + action);
        } catch (Exception e) {
            Timber.tag(TAG).e( "startSystemSettings: Failed to start system settings", e);
        }
    }
    protected void startSystemSettings(Context context, String action,String  url) {
        try {
            Intent intent = new Intent(action);
            intent.setData(Uri.parse(url));
            context.startActivity(intent);
            Timber.tag(TAG).d( "startSystemSettings: Started system settings with action: " + action);
        } catch (Exception e) {
            Timber.tag(TAG).e( "startSystemSettings: Failed to start system settings", e);
        }
    }

    /**
     * 创建通用Intent的辅助方法
     */
    protected Intent createIntent(Context context, Class<?> targetClass) {
        return new Intent(context, targetClass);
    }
    
    /**
     * 设置Intent参数的辅助方法
     */
    protected Intent setIntentExtras(Intent intent, String urlKey, String url, String titleKey, String title) {
        if (url != null) {
            intent.putExtra(urlKey, url);
        }
        if (title != null) {
            intent.putExtra(titleKey, title);
        }
        return intent;
    }
    
    /**
     * 安全启动Activity的方法
     */
    protected void safeStartActivity(Context context, Intent intent) {
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Timber.tag(TAG).e( "safeStartActivity: Failed to start activity", e);
        }
    }
}