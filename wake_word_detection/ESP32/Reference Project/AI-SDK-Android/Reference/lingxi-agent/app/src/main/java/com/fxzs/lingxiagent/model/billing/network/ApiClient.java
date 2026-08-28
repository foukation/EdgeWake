package com.fxzs.lingxiagent.model.billing.network;

import com.fxzs.lingxiagent.model.billing.api.BillingApiService;
import com.fxzs.lingxiagent.network.ZNet.RetrofitClient;
import retrofit2.Retrofit;

public class ApiClient {

    public static BillingApiService getService() {
        Retrofit retrofit = RetrofitClient.getInstance().getRetrofit();
        return retrofit.create(BillingApiService.class);
    }
}