package com.fxzs.lingxiagent.model.billing.callback;

public interface BillingCallback {

    void onSuccess();

    void onSentPackageInfo(String status, String token, String device);

    void onFail(String msg);

    void onNoDevice();

}