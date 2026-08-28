package com.fxzs.lingxiagent.model.billing.repository;

import com.cmdc.ai.assist.constraint.AIAssistConfig;
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.AIServiceManager;
import com.fxzs.lingxiagent.model.billing.api.BillingApiService;
import com.fxzs.lingxiagent.model.billing.api.ThirdPartyApiService;
import com.fxzs.lingxiagent.model.billing.callback.BillingCallback;
import com.fxzs.lingxiagent.model.billing.model.BaseResponse;
import com.fxzs.lingxiagent.model.billing.model.CheckDeviceResponse;
import com.fxzs.lingxiagent.model.billing.model.DeviceResponse;
import com.fxzs.lingxiagent.model.billing.model.ServicePackageResponse;
import com.fxzs.lingxiagent.model.billing.model.SyncDeviceRequest;
import com.fxzs.lingxiagent.model.billing.model.TokenResponse;
import com.fxzs.lingxiagent.model.billing.network.ApiClient;
import com.fxzs.lingxiagent.model.billing.network.ThirdPartyClient;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class BillingRepository {

    private final BillingApiService apiService;

    // 缓存
    private String cachedToken;
    private List<SyncDeviceRequest> cachedDevices;

    public BillingRepository() {
        apiService = ApiClient.getService();
    }

    public void start(BillingCallback callback, boolean needPackageInfo) {

        // step1 验证设备是否注册于计费平台
        apiService.checkServiceDevice()
                .enqueue(new Callback<BaseResponse<CheckDeviceResponse>>() {

                    @Override
                    public void onResponse(
                            Call<BaseResponse<CheckDeviceResponse>> call,
                            Response<BaseResponse<CheckDeviceResponse>> response) {

                        Timber.tag("BillingRepository").d("checkServiceDevice onResponse: %s", response.body().getData().isExists());

                        if (response.body().getData() == null) {

                            callback.onFail("api1 empty");
                            return;
                        }

                        CheckDeviceResponse device =
                                response.body().getData();

                        // exists=false
                        if (!device.isExists()) {
                            callback.onNoDevice();
                            return;
                        }

                        /* // 已有缓存，直接同步
                        if (cachedToken != null
                                && !cachedToken.isEmpty()
                                && cachedDevices != null
                                && !cachedDevices.isEmpty()) {

                            Timber.tag("BillingRepository")
                                    .d("use cached token and devices");

                            syncDevices(
                                    cachedDevices,
                                    cachedToken,
                                    callback,
                                    needPackageInfo
                            );
                            return;
                        }*/

                        // step2 获取token
                        getToken(callback, needPackageInfo);
                    }

                    @Override
                    public void onFailure(
                            Call<BaseResponse<CheckDeviceResponse>> call,
                            Throwable t) {

                        callback.onFail(t.getMessage());
                    }
                });
    }

    private void getToken(BillingCallback callback, boolean needPackageInfo) {

        apiService.getToken()
                .enqueue(new Callback<BaseResponse<TokenResponse>>() {

                    @Override
                    public void onResponse(
                            Call<BaseResponse<TokenResponse>> call,
                            Response<BaseResponse<TokenResponse>> response) {

                        Timber.tag("BillingRepository").d("getToken onResponse: %s", response.body().getData().getToken());

                        if (response.body().getData() == null) {
                            callback.onFail("token empty");
                            return;
                        }

                        String token =
                                response.body().getData().getToken();

                        // 缓存 token
                        cachedToken = token;

                        // step3 获取设备信息
                        getDevices(token, callback, needPackageInfo);
                    }

                    @Override
                    public void onFailure(
                            Call<BaseResponse<TokenResponse>> call,
                            Throwable t) {

                        callback.onFail(t.getMessage());
                    }
                });
    }

    private void getDevices(String token,
                            BillingCallback callback,
                            boolean needPackageInfo) {

        apiService.getDevices()
                .enqueue(new Callback<BaseResponse<List<DeviceResponse>>>() {

                    @Override
                    public void onResponse(
                            Call<BaseResponse<List<DeviceResponse>>> call,
                            Response<BaseResponse<List<DeviceResponse>>> response) {
                        List<DeviceResponse> list = response.body() != null
                                ? response.body().getData()
                                : null;

                        if (list == null) {
                            Timber.tag("BillingRepository").d("getDevices data = null");
                            return;
                        }

                        List<SyncDeviceRequest> SyncDeviceRequestList = new ArrayList<>();

                        for (int i = 0; i < list.size(); i++) {
                            DeviceResponse item = list.get(i);
                            SyncDeviceRequestList.add(new SyncDeviceRequest(
                                    item.getProductId(),
                                    item.getDeviceNo(),
                                    item.getDeviceNo(),
                                    System.currentTimeMillis() / 1000
                            ));
                        }

                        // 缓存设备
                        cachedDevices = SyncDeviceRequestList;

                        // step3 同步设备
                        syncDevices(SyncDeviceRequestList, token, callback, needPackageInfo);
                    }

                    @Override
                    public void onFailure(
                            Call<BaseResponse<List<DeviceResponse>>> call,
                            Throwable t) {

                        callback.onFail(t.getMessage());
                    }
                });
    }

    private void syncDevices(List<SyncDeviceRequest> deviceList,
                             String token,
                             BillingCallback callback,
                             boolean needPackageInfo) {

        ThirdPartyApiService thirdService =
                ThirdPartyClient.getService(token);

        Timber.tag("BillingRepository").d("syncDevices request size=%s", deviceList.size());

        for (int i = 0; i < deviceList.size(); i++) {
            SyncDeviceRequest item = deviceList.get(i);
            Timber.tag("BillingRepository").d("syncDevices req[" + i + "] = "
                    + "modelId=" + item.getDeviceModelId()
                    + ", deviceNo=" + item.getDeviceNo()
                    + ", deviceName=" + item.getDeviceName()
                    + ", time=" + item.getBindTime());
        }

        Gson gson = new Gson();

        String deviceListJson = gson.toJson(deviceList);

        thirdService.syncDevices(deviceList)
                .enqueue(new Callback<BaseResponse<Object>>() {

                    @Override
                    public void onResponse(
                            Call<BaseResponse<Object>> call,
                            Response<BaseResponse<Object>> response) {

                        Timber.tag("BillingRepository").d("syncDevices success: %s", response.body().getMsg());

                        if (!response.body().getSuccess()) {
                            callback.onFail("syncDevices fail");
                            return;
                        }

                        if (!needPackageInfo) {
                            callback.onSuccess();
                            return;
                        }

                        try {
                            String encodedDeviceStr = URLEncoder.encode(
                                    deviceListJson,
                                    StandardCharsets.UTF_8.toString()
                            );

                            // step5 获取服务包信息
                            getPackages(token, callback, encodedDeviceStr);

                        } catch (UnsupportedEncodingException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<BaseResponse<Object>> call,
                            Throwable t) {

                        callback.onFail(t.getMessage());
                    }
                });
    }

    private void getPackages(String token,
                             BillingCallback callback,
                             String encodedDeviceStr) {

        ThirdPartyApiService thirdService =
                ThirdPartyClient.getService(token);

        thirdService.getServicePackages()
                .enqueue(new Callback<BaseResponse<Object>>() {

                    @Override
                    public void onResponse(
                            Call<BaseResponse<Object>> call,
                            Response<BaseResponse<Object>> response) {


                        if (response.body().getData() == null) {
                            callback.onSentPackageInfo("[未开通]", token, encodedDeviceStr);
                            return;
                        }

                        AIAssistConfig aiAssistConfig = AIServiceManager.Companion.getInstance().getAiAssistConfig();
                        String curDeviceNo = aiAssistConfig.getDeviceNo();

                        Object data = response.body().getData();

                        String json = new Gson().toJson(data);

                        ServicePackageResponse packageResponse =
                                new Gson().fromJson(json, ServicePackageResponse.class);

                        List<ServicePackageResponse.PackageItem> packages =
                                packageResponse.getPackages();

                        Timber.tag("BillingRepository").d("getPackages onResponse: %s", json);

                        ServicePackageResponse.PackageItem valid = null;

                        for (ServicePackageResponse.PackageItem item : packages) {
                            if (item != null && curDeviceNo.equals(item.getDeviceNo())) {
                                valid = item;
                                break;
                            }
                        }

                        String result;

                        if (valid == null) {
                            result = "[未开通]";
                        } else {
                            switch (valid.getStatus()) {

                                case "not_subscribed":
                                    result = "[未开通]";
                                    break;

                                case "subscribed":
                                    result = "[已开通 " + valid.getPackageName() + "]";
                                    break;

                                case "expiring_soon":
                                    result = "[" + valid.getPackageName() + " 即将到期]";
                                    break;

                                case "expired":
                                    result = "[" + valid.getPackageName() + " 已过期]";
                                    break;

                                default:
                                    result = "[" + valid.getPackageName() + " 未开通]";
                                    break;
                            }
                        }

                        Timber.tag("BillingRepository").d("billExtra=%s", result);

                        callback.onSentPackageInfo(result, token, encodedDeviceStr);
                    }

                    @Override
                    public void onFailure(
                            Call<BaseResponse<Object>> call,
                            Throwable t) {

                        callback.onFail(t.getMessage());
                    }
                });
    }

    public void clearCache() {
        cachedToken = null;
        cachedDevices = null;

        Timber.tag("BillingRepository")
                .d("Billing cache cleared");
    }
}