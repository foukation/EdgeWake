package com.fxzs.lingxiagent.model.network;

import com.fxzs.lingxiagent.lingxi.lingxi_conversation.AIServiceManager;
import com.fxzs.lingxiagent.model.common.Constants;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

/**
 * 统一头部接口
 */
public class UnityHeaderInterceptor implements Interceptor {
    
    private static final String TAG = "UnityHeaderInterceptor";
    
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request.Builder requestBuilder = originalRequest.newBuilder();
        String url = originalRequest.url().toString();
        boolean noAuthRequired = url.contains(Constants.BASE_URL);
        if (noAuthRequired){
            AIServiceManager.Companion.getInstance().getHeaderInfo(requestBuilder);
        }
        Timber.tag(TAG).i("请求地址 ："+url);
        return chain.proceed(requestBuilder.build());
    }
}