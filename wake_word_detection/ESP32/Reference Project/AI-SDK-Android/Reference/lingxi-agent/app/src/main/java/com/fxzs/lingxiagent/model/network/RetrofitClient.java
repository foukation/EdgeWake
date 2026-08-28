package com.fxzs.lingxiagent.model.network;

import com.fxzs.lingxiagent.BuildConfig;
import com.fxzs.lingxiagent.model.auth.api.AuthApiService;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.model.user.dto.FeedbackDto;
import com.fxzs.lingxiagent.model.user.dto.FeedbackDtoDeserializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.Proxy;
import java.util.concurrent.TimeUnit;

import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit客户端配置
 */
public class RetrofitClient {
    private final Retrofit retrofit;
//    private final Retrofit retrofitV1,retrofitV2, retrofitV3, retrofitV4, retrofitV5;
    private final Retrofit streamingRetrofit, pptRetrofit, noPinnerRetrofit;
    private final AuthApiService authApiService;
    
    private RetrofitClient() {
        // 使用自定义日志拦截器，打印完整的请求和响应内容
        CustomLoggingInterceptor customLoggingInterceptor = new CustomLoggingInterceptor();

        // 配置标准OkHttpClient
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(Constants.READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(Constants.WRITE_TIMEOUT, TimeUnit.SECONDS)
                .certificatePinner(SecurityPolicy.getCertificatePinner())       // 证书固定
                .connectionSpecs(SecurityPolicy.getConnectionSpec())            // TLS强化
                .addInterceptor(new AuthInterceptor())                          // 认证拦截器
                .addInterceptor(customLoggingInterceptor);                      // 添加自定义日志拦截器
        // 生产环境禁用代理
        if (!BuildConfig.DEBUG) {
            builder.proxy(Proxy.NO_PROXY);
            builder.socketFactory(SecurityPolicy.getNoProxySocketFactory());
        }
        OkHttpClient okHttpClient = builder.build();

        // 配置意图识别接口标准OkHttpClient
        OkHttpClient intentionOkHttpClient = new OkHttpClient.Builder()
                .connectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(Constants.READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(Constants.WRITE_TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(new IntentionAuthInterceptor())
                .addInterceptor(customLoggingInterceptor) // 添加自定义日志拦截器
                .build();

        // 配置流式响应专用的OkHttpClient（更长的超时时间）
        OkHttpClient streamingOkHttpClient = new OkHttpClient.Builder()
                .connectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS) // 5分钟读取超时，适合流式响应
                .writeTimeout(Constants.WRITE_TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(new AuthInterceptor())
                .addInterceptor(customLoggingInterceptor) // 添加自定义日志拦截器
                .build();
        
        // 配置Gson
        // 先创建一个没有自定义deserializer的Gson实例
        Gson tempGson = new GsonBuilder().setLenient().create();
        
        // 然后创建带有自定义deserializer的Gson实例
        Gson gson = new GsonBuilder()
                .setLenient()
                .registerTypeHierarchyAdapter(FeedbackDto.class, new FeedbackDtoDeserializer())
                .create();

        // 创建标准Retrofit实例
        retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        // 创建标准Retrofit实例,对应http://36.213.71.163:11453/
//        retrofitV1 = new Retrofit.Builder()
//                .baseUrl(Constants.BASE_URL_V1)
//                .client(intentionOkHttpClient)
//                .addConverterFactory(GsonConverterFactory.create(gson))
//                .build();
//
//        // 创建标准Retrofit实例,对应http://36.213.71.163:11470/
//        retrofitV2 = new Retrofit.Builder()
//                .baseUrl(Constants.BASE_URL_V2)
//                .client(intentionOkHttpClient)
//                .addConverterFactory(GsonConverterFactory.create(gson))
//                .build();
//
//        // 创建标准Retrofit实例,对应http://36.213.71.163:11507/
//        retrofitV3 = new Retrofit.Builder()
//                .baseUrl(Constants.BASE_URL_V3)
//                .client(intentionOkHttpClient)
//                .addConverterFactory(GsonConverterFactory.create(gson))
//                .build();
//
//        // 创建标准Retrofit实例,对应http://36.213.71.163:11508/
//        retrofitV4 = new Retrofit.Builder()
//                .baseUrl(Constants.BASE_URL_V4)
//                .client(intentionOkHttpClient)
//                .addConverterFactory(GsonConverterFactory.create(gson))
//                .build();
//
//        // 创建标准Retrofit实例,对应http://36.213.71.200:5692
//        retrofitV5 = new Retrofit.Builder()
//                .baseUrl(Constants.BASE_URL_V4)
//                .client(intentionOkHttpClient)
//                .addConverterFactory(GsonConverterFactory.create(gson))
//                .build();

        // 创建不需要Pinner的Retrofit和OkHttpClient实例，如APP版本升级
        OkHttpClient.Builder noPinnerBuilder = okHttpClient.newBuilder()
                .addInterceptor(new AuthInterceptor())
                .addInterceptor(customLoggingInterceptor) // 添加自定义日志拦截器
                .certificatePinner(CertificatePinner.DEFAULT);// 恢复为默认值
        noPinnerRetrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .client(noPinnerBuilder.build())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        // 创建流式响应专用的Retrofit实例
        streamingRetrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .client(streamingOkHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        // 创建PPT专用的Retrofit实例
        pptRetrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL_PPT)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        // 创建API服务
        authApiService = retrofit.create(AuthApiService.class);
    }

    public static RetrofitClient getInstance() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static final RetrofitClient INSTANCE = new RetrofitClient();
    }
    
    public AuthApiService getAuthApiService() {
        return authApiService;
    }
    
    public <T> T createService(Class<T> serviceClass) {
        return retrofit.create(serviceClass);
    }

//    public <T> T createServiceV1(Class<T> serviceClass) {
//        return retrofitV1.create(serviceClass);
//    }

//    public <T> T createServiceV2(Class<T> serviceClass) {
//        return retrofitV2.create(serviceClass);
//    }
//
//    public <T> T createServiceV3(Class<T> serviceClass) {
//        return retrofitV3.create(serviceClass);
//    }
//
//    public <T> T createServiceV4(Class<T> serviceClass) {
//        return retrofitV4.create(serviceClass);
//    }
//
//    public <T> T createServiceV5(Class<T> serviceClass) {
//        return retrofitV5.create(serviceClass);
//    }

    public <T> T createNoPinnerService(Class<T> serviceClass) {
        return noPinnerRetrofit.create(serviceClass);
    }

    public <T> T createStreamingService(Class<T> serviceClass) {
        return streamingRetrofit.create(serviceClass);
    }

    public <T> T createPptService(Class<T> serviceClass) {
        return pptRetrofit.create(serviceClass);
    }
}