package com.fxzs.lingxiagent.model.billing.network;

import com.fxzs.lingxiagent.model.billing.api.ThirdPartyApiService;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ThirdPartyClient {

    private static final String BASE_URL =
            "https://ivs.chinamobiledevice.com:31557/";

    public static ThirdPartyApiService getService(String token) {

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new TokenInterceptor(token))
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(ThirdPartyApiService.class);
    }
}