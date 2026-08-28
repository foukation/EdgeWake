package com.fxzs.lingxiagent.model.network;

import com.fxzs.lingxiagent.lingxi.lingxi_conversation.AIServiceManager;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

/**
 * 意图相关接口认证拦截器 - 自动添加Token和X-Client-Ip到请求头
 */
public class IntentionAuthInterceptor implements Interceptor {
    
    private static final String TAG = "AuthInterceptor";
    
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request.Builder requestBuilder = originalRequest.newBuilder();
        
        String url = originalRequest.url().toString();
        requestBuilder.header(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);

        // 这些接口不需要认证
        boolean noAuthRequired = url.contains("/app-api/member/auth/send-sms-code");
        
        // 如果需要认证且有Token，添加认证头
        if (!noAuthRequired) {
            String token = SharedPreferencesUtil.getIntentionToken();
            Timber.tag(TAG).d( "Token used: %s", token);
            if (!token.isEmpty()) {
                requestBuilder.header(Constants.HEADER_AUTHORIZATION, Constants.HEADER_BEARER + token);
                Timber.tag(TAG).d( "Added auth header for URL: %s", url);
            } else {
                Timber.tag(TAG).d( "No token available for URL: %s", url);
            }
            String clientIp = SharedPreferencesUtil.getClientIP();
            Timber.tag(TAG).d( "clientIp used: %s", clientIp);
            if (!clientIp.isEmpty()) {
                requestBuilder.header(Constants.X_CLIENT_IP, clientIp);
                Timber.tag(TAG).d( "Added clientIp auth header for URL: %s", url);
            } else {
                Timber.tag(TAG).d("No clientIp available for URL: %s", url);
            }
        } else {
            Timber.tag(TAG).d( "No auth required for URL: %s", url);
        }
        AIServiceManager.Companion.getInstance().getHeaderInfo(requestBuilder);
        return chain.proceed(requestBuilder.build());
    }
}