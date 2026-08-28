/*
package com.fxzs.lingxiagent.model.intention.repository;

import android.os.StrictMode;
import android.util.Log;

import com.fxzs.lingxiagent.model.intention.api.IntentionApiService;
import com.fxzs.lingxiagent.model.intention.api.IntentionV2ApiService;
import com.fxzs.lingxiagent.model.intention.api.IntentionV3ApiService;
import com.fxzs.lingxiagent.model.intention.api.IntentionV4ApiService;
import com.fxzs.lingxiagent.model.intention.api.IntentionV5ApiService;
import com.fxzs.lingxiagent.model.intention.dto.ClientActionsMulRequest;
import com.fxzs.lingxiagent.model.intention.dto.ClientActionsRequest;
import com.fxzs.lingxiagent.model.intention.dto.ClientApiActionsRes;
import com.fxzs.lingxiagent.model.intention.dto.ClientApiActionsResMul;
import com.fxzs.lingxiagent.model.intention.dto.ClientApiAppListRes;
import com.fxzs.lingxiagent.model.intention.dto.ClientApiGetTokenRes;
import com.fxzs.lingxiagent.model.intention.dto.ClientTimeData;
import com.fxzs.lingxiagent.model.intention.dto.ClientTimeRes;
import com.fxzs.lingxiagent.model.intention.dto.ImageAssistantRequest;
import com.fxzs.lingxiagent.model.intention.dto.ImageAssistantResponse;
import com.fxzs.lingxiagent.model.intention.dto.IsOcrResult;
import com.fxzs.lingxiagent.model.intention.dto.LLmQueryParams;
import com.fxzs.lingxiagent.model.intention.dto.LlmQueryResult;
import com.fxzs.lingxiagent.model.intention.dto.MedicineRequest;
import com.fxzs.lingxiagent.model.intention.dto.OcrResult;
import com.fxzs.lingxiagent.model.intention.dto.PromptRequest;
import com.fxzs.lingxiagent.model.intention.dto.QueryParams;
import com.fxzs.lingxiagent.model.intention.dto.TripContentRes;
import com.fxzs.lingxiagent.model.intention.dto.TripCreateRequest;
import com.fxzs.lingxiagent.model.intention.dto.TripCreateRes;
import com.fxzs.lingxiagent.model.intention.dto.TripDelRes;
import com.fxzs.lingxiagent.model.network.RetrofitClient;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IntentionRepositoryImpl {

    private static final String SIGN_CODE = "blue-agent";
    private static final String TAG = IntentionRepositoryImpl.class.getSimpleName();

    private static IntentionRepositoryImpl instance;
    private final IntentionApiService intentionApiService;
    private final IntentionV2ApiService intentionV2ApiService;
    private final IntentionV3ApiService intentionV3ApiService;
    private final IntentionV4ApiService intentionV4ApiService;
    private final IntentionV5ApiService intentionV5ApiService;

    public IntentionRepositoryImpl() {
        this.intentionApiService = RetrofitClient.getInstance().createServiceV1(IntentionApiService.class);
        this.intentionV2ApiService = RetrofitClient.getInstance().createServiceV1(IntentionV2ApiService.class);
        this.intentionV3ApiService = RetrofitClient.getInstance().createServiceV1(IntentionV3ApiService.class);
        this.intentionV4ApiService = RetrofitClient.getInstance().createServiceV1(IntentionV4ApiService.class);
        this.intentionV5ApiService = RetrofitClient.getInstance().createServiceV1(IntentionV5ApiService.class);
    }

    public static synchronized IntentionRepositoryImpl getInstance() {
        if (instance == null) {
            instance = new IntentionRepositoryImpl();
        }
        return instance;
    }

    */
/*
     * 调用其他接口，首先需获取公网IP
     * *//*

    public void handlerRequestPublicIp() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            URL url = new URL("http://checkip.amazonaws.com");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String publicIp = reader.readLine().trim();
            Timber.tag(TAG).d( "handlerRequestPublicIp publicIp " + publicIp);
            SharedPreferencesUtil.updateClientIP(publicIp);
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    */
/*
     * 获取设备Id
     * *//*

    public void handlerRequestDeviceId() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            URL url = new URL("http://checkip.amazonaws.com");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String publicIp = reader.readLine().trim();
            Timber.tag(TAG).d( "handlerRequestDeviceId publicIp " + publicIp);
            SharedPreferencesUtil.updateClientIP(publicIp);
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    */
/*
     * 获取公网IP后需获取客户端token，然后才能调用其他接口
     * *//*

    public void handlerRequestClientToken(final TokenCallback callback) {
        intentionApiService.getClientToken(SIGN_CODE)
                .enqueue(new Callback<ClientApiGetTokenRes>() {
                    @Override
                    public void onResponse(Call<ClientApiGetTokenRes> call, Response<ClientApiGetTokenRes> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ClientApiGetTokenRes res = response.body();
                            if (res.code == 200) {
                                String clientToken = res.data;
                                Timber.tag(TAG).d( "handlerRequestClientToken clientToken " + clientToken);
                                SharedPreferencesUtil.updateIntentionToken(clientToken);
                                callback.onSuccess();
                            } else {
                                SharedPreferencesUtil.updateIntentionToken("");
                                callback.onError("获取token失败: " + res.msg);
                            }
                        } else {
                            SharedPreferencesUtil.updateIntentionToken("");
                            callback.onError("HTTP错误: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<ClientApiGetTokenRes> call, Throwable t) {
                        callback.onError("网络错误: " + t.getMessage());
                        Timber.tag(TAG).e( "onFailure " + t.getMessage());
                    }
                });
    }

    */
/*
     * 获取客户端适配App信息
     * *//*

    public void handlerRequestClientAppList(final ApiCallback<ClientApiAppListRes> callback) {
        intentionApiService.getAppList(1, 50, 1)
                .enqueue(new Callback<ClientApiAppListRes>() {
                    @Override
                    public void onResponse(Call<ClientApiAppListRes> call, Response<ClientApiAppListRes> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Timber.tag(TAG).d( "handlerRequestClientAppList isSuccessful " + response.body());
                            ClientApiAppListRes res = response.body();
                            if (res.code == 200) {
                                callback.onSuccess(res);
                            } else {
                                callback.onError("获取应用列表失败: " + res.msg);
                            }
                        } else {
                            callback.onError("HTTP错误: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<ClientApiAppListRes> call, Throwable t) {
                        callback.onError("网络错误: " + t.getMessage());
                        Timber.tag(TAG).e( "onFailure " + t.getMessage());
                    }
                });
    }

    */
/*
     * 获取客户端Action信息
     * *//*

    public void handlerRequestClientActions(String inputStr, final ApiCallback<ClientApiActionsRes> callback) {
        intentionV4ApiService.getClientActions(new ClientActionsRequest(inputStr))
                .enqueue(new Callback<ClientApiActionsRes>() {
                    @Override
                    public void onResponse(Call<ClientApiActionsRes> call, Response<ClientApiActionsRes> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Timber.tag(TAG).d( "handlerRequestClientActions isSuccessful " + response.body());
                            ClientApiActionsRes res = response.body();
                            if (res.code == 200 && res.data.intents != null) {
                                callback.onSuccess(res);
                            } else {
                                callback.onError("获取操作失败: " + res.msg);
                            }
                        } else {
                            callback.onError("HTTP错误: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<ClientApiActionsRes> call, Throwable t) {
                        callback.onError("网络错误: " + t.getMessage());
                        Timber.tag(TAG).e( "onFailure " + t.getMessage());
                    }
                });
    }

    */
/*
     * 获取客户端Action（多轮对话）信息
     * *//*

    public void handlerRequestClientActionsMul(String inputStr, String sessionID,
                                                      final ApiCallback<ClientApiActionsResMul> callback) {
        intentionApiService.getClientActionsMul(new ClientActionsMulRequest(inputStr, sessionID))
                .enqueue(new Callback<ClientApiActionsResMul>() {
                    @Override
                    public void onResponse(Call<ClientApiActionsResMul> call, Response<ClientApiActionsResMul> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Timber.tag(TAG).d( "handlerRequestClientActionsMul isSuccessful " + response.body());
                            ClientApiActionsResMul res = response.body();
                            if (res.code == 200) {
                                callback.onSuccess(res);
                            } else {
                                callback.onError("获取多轮操作失败: " + res.msg);
                            }
                        } else {
                            callback.onError("HTTP错误: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<ClientApiActionsResMul> call, Throwable t) {
                        callback.onError("网络错误: " + t.getMessage());
                        Timber.tag(TAG).e( "onFailure " + t.getMessage());
                    }
                });
    }

    */
/*
     * 获取泛化时间
     * *//*

    public void handlerRequestClientNormalizeTime(String timeDescription,
                                                         final ApiCallback<ClientTimeData> callback) {
        intentionApiService.normalizeTime(timeDescription)
                .enqueue(new Callback<ClientTimeRes>() {
                    @Override
                    public void onResponse(Call<ClientTimeRes> call, Response<ClientTimeRes> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Timber.tag(TAG).d( "handlerRequestClientNormalizeTime isSuccessful " + response.body());
                            ClientTimeRes res = response.body();
                            if (res.code == 200) {
                                callback.onSuccess(res.data);
                            } else {
                                callback.onError("获取时间失败: " + res.msg);
                            }
                        } else {
                            callback.onError("HTTP错误: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<ClientTimeRes> call, Throwable t) {
                        callback.onError("网络错误: " + t.getMessage());
                        Timber.tag(TAG).e( "onFailure " + t.getMessage());
                    }
                });
    }

    */
/*
     * 行程创建
     * *//*

    public void createTrip(String inputStr, final ApiCallback<TripCreateRes> callback) {
        intentionApiService.createTrip(new TripCreateRequest(inputStr))
                .enqueue(new Callback<TripCreateRes>() {
                    @Override
                    public void onResponse(Call<TripCreateRes> call, Response<TripCreateRes> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Timber.tag(TAG).d( "createTrip isSuccessful " + response.body());
                            callback.onSuccess(response.body());
                        } else {
                            callback.onError("创建行程失败，状态码: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<TripCreateRes> call, Throwable t) {
                        callback.onError("网络错误: " + t.getMessage());
                        Timber.tag(TAG).e( "onFailure " + t.getMessage());
                    }
                });
    }

    */
/*
     * 获取行程列表
     * *//*

    public void getTripList(final ApiCallback<TripContentRes> callback) {
        intentionApiService.getTripList(1, 50)
                .enqueue(new Callback<TripContentRes>() {
                    @Override
                    public void onResponse(Call<TripContentRes> call, Response<TripContentRes> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Timber.tag(TAG).d( "getTripList isSuccessful " + response.body());
                            callback.onSuccess(response.body());
                        } else {
                            callback.onError("获取行程列表失败，状态码: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<TripContentRes> call, Throwable t) {
                        callback.onError("网络错误: " + t.getMessage());
                        Timber.tag(TAG).e( "onFailure " + t.getMessage());
                    }
                });
    }

    */
/*
     * 删除行程
     * *//*

    public void delTrip(String delId, final ApiCallback<TripDelRes> callback) {
        intentionApiService.deleteTrip(delId)
                .enqueue(new Callback<TripDelRes>() {
                    @Override
                    public void onResponse(Call<TripDelRes> call, Response<TripDelRes> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Timber.tag(TAG).d( "delTrip isSuccessful " + response.body());
                            callback.onSuccess(response.body());
                        } else {
                            callback.onError("删除行程失败，状态码: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<TripDelRes> call, Throwable t) {
                        callback.onError("网络错误: " + t.getMessage());
                        Timber.tag(TAG).e( "onFailure " + t.getMessage());
                    }
                });
    }

    */
/*
     * OCR处理
     * *//*

    public void handlerOCR(QueryParams params, final ApiCallback<OcrResult> callback) {
        intentionV5ApiService.handleOcr(params)
                .enqueue(new Callback<OcrResult>() {
                    @Override
                    public void onResponse(Call<OcrResult> call, Response<OcrResult> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Timber.tag(TAG).d( "handlerOCR isSuccessful " + response.body());
                            callback.onSuccess(response.body());
                        } else {
                            callback.onError("OCR处理失败，状态码: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<OcrResult> call, Throwable t) {
                        callback.onError("网络错误: " + t.getMessage());
                        Timber.tag(TAG).e( "onFailure " + t.getMessage());
                    }
                });
    }

    */
/*
     * OCR附加处理
     * *//*

    public void isAdditionOcr(QueryParams params, final ApiCallback<IsOcrResult> callback) {
        intentionV5ApiService.isAdditionOcr(params)
                .enqueue(new Callback<IsOcrResult>() {
                    @Override
                    public void onResponse(Call<IsOcrResult> call, Response<IsOcrResult> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Timber.tag(TAG).d( "isAdditionOcr isSuccessful " + response.body());
                            callback.onSuccess(response.body());
                        } else {
                            callback.onError("OCR附加处理失败，状态码: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<IsOcrResult> call, Throwable t) {
                        callback.onError("网络错误: " + t.getMessage());
                        Timber.tag(TAG).e( "onFailure " + t.getMessage());
                    }
                });
    }

    */
/*
     * LLM处理
     * *//*

    public void handlerLlmAction(LLmQueryParams params, final ApiCallback<LlmQueryResult> callback) {
        intentionV2ApiService.handleLlmAction(params)
                .enqueue(new Callback<LlmQueryResult>() {
                    @Override
                    public void onResponse(Call<LlmQueryResult> call, Response<LlmQueryResult> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Timber.tag(TAG).d( "handlerLlmAction isSuccessful " + response.body());
                            callback.onSuccess(response.body());
                        } else {
                            callback.onError("LLM处理失败，状态码: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<LlmQueryResult> call, Throwable t) {
                        callback.onError("网络错误: " + t.getMessage());
                        Timber.tag(TAG).e( "onFailure " + t.getMessage());
                    }
                });
    }

    */
/* 图片信息提取 *//*

    public void imageInformationExtraction(
            ImageAssistantRequest request,
            ApiCallback<ImageAssistantResponse> callback) {

        intentionApiService.imageInformationExtraction(request)
            .enqueue(new Callback<ImageAssistantResponse>() {
            @Override
            public void onResponse(Call<ImageAssistantResponse> call,
                                   Response<ImageAssistantResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Timber.tag(TAG).e( "onResponse Image recognition response: " + response.body());
                    callback.onSuccess(response.body());
                } else {
                    String error = "Request failed: " + response.code();
                    Timber.tag(TAG).e( "onResponse " + error);
                    callback.onError(error);
                }
            }

            @Override
            public void onFailure(Call<ImageAssistantResponse> call, Throwable t) {
                String error = "Network error: " + t.getMessage();
                Timber.tag(TAG).e( "onFailure " + t.getMessage());
                callback.onError(error);
            }
        });
    }

    */
/* 问答 *//*

    public void questionAnswer(
            PromptRequest request,
            ApiCallback<PromptRequest.PromptResponse> callback) {
        intentionV3ApiService.questionAnswer(request)
                .enqueue(new Callback<PromptRequest.PromptResponse>() {
            @Override
            public void onResponse(Call<PromptRequest.PromptResponse> call,
                                   Response<PromptRequest.PromptResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Timber.tag(TAG).e( "onResponse QA response: " + response.body());
                    callback.onSuccess(response.body());
                } else {
                    String error = "Request failed: " + response.code();
                    Timber.tag(TAG).e( "onResponse " + error);
                    callback.onError(error);
                }
            }

            @Override
            public void onFailure(Call<PromptRequest.PromptResponse> call, Throwable t) {
                String error = "Network error: " + t.getMessage();
                Timber.tag(TAG).e( "onFailure " + error);
                callback.onError(error);
            }
        });
    }

    */
/* 用药提醒 *//*

    public void medicationReminder(
            MedicineRequest request,
            ApiCallback<MedicineRequest.MedicineResponse> callback) {
        intentionV3ApiService.medicationReminder(request)
            .enqueue(new Callback<MedicineRequest.MedicineResponse>() {
            @Override
            public void onResponse(Call<MedicineRequest.MedicineResponse> call,
                                   Response<MedicineRequest.MedicineResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Timber.tag(TAG).e( "onResponse Medication response: " + response.body());
                    callback.onSuccess(response.body());
                } else {
                    String error = "Request failed: " + response.code();
                    Timber.tag(TAG).e( "onResponse " + error);
                    callback.onError(error);
                }
            }

            @Override
            public void onFailure(Call<MedicineRequest.MedicineResponse> call, Throwable t) {
                String error = "Network error: " + t.getMessage();
                Timber.tag(TAG).e( "onFailure " + error);
                callback.onError(error);
            }
        });
    }
}
*/
