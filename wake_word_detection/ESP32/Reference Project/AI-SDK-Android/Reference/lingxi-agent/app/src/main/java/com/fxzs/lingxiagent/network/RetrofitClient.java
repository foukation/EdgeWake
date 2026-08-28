package com.fxzs.lingxiagent.network;

import com.fxzs.lingxiagent.lingxi.lingxi_conversation.AIServiceManager;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

public class RetrofitClient {

    private static RetrofitClient instance;
    private final Retrofit retrofit;
    private final Retrofit pptRetrofit;
    
    private RetrofitClient() {
        // 使用自定义日志拦截器，打印完整的请求和响应内容
        CustomLoggingInterceptor customLoggingInterceptor = new CustomLoggingInterceptor();
        
        // OkHttpClient配置
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new AuthInterceptor()) // 添加认证拦截器
                .addInterceptor(customLoggingInterceptor) // 添加自定义日志拦截器
                .build();
        
        // Retrofit配置
        retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // PPT专用Retrofit配置
        pptRetrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL_PPT)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
    
    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }
    
    public <T> T create(Class<T> service) {
        return retrofit.create(service);
    }

    public <T> T createPptService(Class<T> service) {
        return pptRetrofit.create(service);
    }
    
    /**
     * 认证拦截器，自动添加token
     */
    private static class AuthInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();
            
            // 获取token
            String token = SharedPreferencesUtil.getAccessToken();
            Timber.tag("TAG").d("token = "+token);
            // 构建新请求，添加必要的header
            Request.Builder requestBuilder = originalRequest.newBuilder()
                    .header("tenant-id", "1"); // 添加tenant-id header
            
            if (token != null && !token.isEmpty()) {
                // 添加Authorization header
                requestBuilder.header("Authorization", "Bearer " + token);
            }
            AIServiceManager.Companion.getInstance().getHeaderInfo(requestBuilder);
            Request newRequest = requestBuilder.build();
            return chain.proceed(newRequest);
        }
    }
}