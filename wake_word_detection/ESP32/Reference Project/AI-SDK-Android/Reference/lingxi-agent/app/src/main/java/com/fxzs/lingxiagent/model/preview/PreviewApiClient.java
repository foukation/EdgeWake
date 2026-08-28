package com.fxzs.lingxiagent.model.preview;

import com.fxzs.lingxiagent.model.common.Constants;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
//import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.fxzs.lingxiagent.network.CustomLoggingInterceptor;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import retrofit2.converter.gson.GsonConverterFactory;

public class PreviewApiClient {

    private static PreviewApiService apiService;

    public static PreviewApiService getApiService() {
        if (apiService == null) {
            synchronized (PreviewApiClient.class) {
                if (apiService == null) {
                    apiService = new Retrofit.Builder()
                            .baseUrl("https://wps.shanghaijimu.com")
                            .client(getOkHttpClient())
                            .addConverterFactory(GsonConverterFactory.create())
                            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                            .build()
                            .create(PreviewApiService.class);
                }
            }
        }
        return apiService;
    }

    private static OkHttpClient getOkHttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request request = original.newBuilder()
                            .header("Authorization", "Bearer 587bba73918144e1b3844f8661c3dda0")
                            .header("Referer", "https://mobile-web.jmkjsh.com/")
                            .header("Accept", "application/json, text/plain, */*")
                            .method(original.method(), original.body())
                            .build();
                    return chain.proceed(request);
                })
                .addInterceptor(logging)
                .connectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(Constants.READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(Constants.WRITE_TIMEOUT, TimeUnit.SECONDS)
                .build();
    }
}
