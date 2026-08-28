package com.fxzs.lingxiagent.model.billing.network;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class TokenInterceptor implements Interceptor {

    private final String token;

    public static final String X_APP_ID =
            "APP1773900596930";

    public TokenInterceptor(String token) {
        this.token = token;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {

        Request request = chain.request()
                .newBuilder()
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("x-app-id", X_APP_ID)
                .addHeader("Content-Type", "application/json")
                .build();

        return chain.proceed(request);
    }
}