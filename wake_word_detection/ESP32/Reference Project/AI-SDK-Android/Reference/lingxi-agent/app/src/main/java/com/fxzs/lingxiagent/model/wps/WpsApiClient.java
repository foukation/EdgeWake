package com.fxzs.lingxiagent.model.wps;

import com.fxzs.lingxiagent.model.common.Constants;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
//import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.fxzs.lingxiagent.network.CustomLoggingInterceptor;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class WpsApiClient {

    private static WpsApiService apiService;

    public static WpsApiService getApiService() {
        if (apiService == null) {
            synchronized (WpsApiClient.class) {
                if (apiService == null) {

                    apiService = new Retrofit.Builder()
                            .baseUrl(Constants.WPS_BASE_URL)
                            .client(getOkHttpClient())
                            .addConverterFactory(GsonConverterFactory.create())
                            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                            .build()
                            .create(WpsApiService.class);
                }
            }
        }
        return apiService;
    }

    private static OkHttpClient getOkHttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        // 使用自定义日志拦截器，打印完整的请求和响应内容
        CustomLoggingInterceptor customLoggingInterceptor = new CustomLoggingInterceptor();

        return new OkHttpClient.Builder()
                .addInterceptor(new WpsAuthInterceptor())
                .addInterceptor(logging)
                .connectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(Constants.READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(Constants.WRITE_TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(customLoggingInterceptor) // 添加自定义日志拦截器
                .build();
    }
}
