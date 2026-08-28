package com.fxzs.lingxiagent.model.network;

import static com.fxzs.lingxiagent.model.common.Constants.CLIENT_ID;

import androidx.annotation.NonNull;

import com.fxzs.lingxiagent.lingxi.lingxi_conversation.AIServiceManager;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

/**
 * 认证拦截器 - 自动添加Token和签名到请求头
 */
public class AuthInterceptor implements Interceptor {
    
    private static final String TAG = "AuthInterceptor";
    
    @NonNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request.Builder requestBuilder = originalRequest.newBuilder();
        // 添加通用请求头
       /* requestBuilder.header(Constants.HEADER_CLIENT_ID, Constants.CLIENT_ID);*/
        requestBuilder.header(Constants.HEADER_PROJECT_CODE, Constants.PROJECT_CODE);
        requestBuilder.header(Constants.HEADER_VERSION, "3");
        requestBuilder.header(Constants.HEADER_TIME, String.valueOf(System.currentTimeMillis()));
        requestBuilder.header(Constants.HEADER_BILL_CLIENT_ID, CLIENT_ID);

        String url = originalRequest.url().toString();
        // headers中，除了登录、检测升级及验证码接口不需要携带token，其他都需要携带token
        boolean noAuthRequired = url.contains("/app-api/member/auth/send-sms-code")// 发送验证码
                || url.contains("/app-api/member/auth/login-by-cmi")// 一键登录
                || url.contains("/app-api/member/auth/login")// 手机+密码登录
                || url.contains("/app-api/member/auth/sms-login")// 手机+验证码登录
                || url.contains("/app-api/member/auth/register")// 手机+密码注册账号
                || url.contains("/app-api/member/auth/refresh-token");// 刷新token

        // 如果需要认证且有Token，添加认证头
        if (!noAuthRequired) {
            String token = SharedPreferencesUtil.getToken();
            if (!token.isEmpty()) {
                requestBuilder.header(Constants.HEADER_AUTHORIZATION, Constants.HEADER_BEARER + token);
                Timber.tag(TAG).d("Added auth header for URL: %s", url);
            } else {
                Timber.tag(TAG).d( "No token available for URL: %s", url);
            }
        } else {
            Timber.tag(TAG).d("No auth required for URL: %s", url);
        }

        AIServiceManager.Companion.getInstance().getHeaderInfo(requestBuilder);
        // APP版本升级，添加签名
        boolean appUpgradeRequired = url.contains("open-api/ota/v2/app-check-upgrade");
        if (appUpgradeRequired) {
            requestBuilder.removeHeader("Authorization");
        }
        return chain.proceed(requestBuilder.build());
    }
}