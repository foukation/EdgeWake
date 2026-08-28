package com.fxzs.lingxiagent.model.deepresearch.repository;

import android.content.Context;
import android.os.Build;

import com.cmdc.ai.assist.constraint.AIAssistConfig;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.AIServiceManager;
import com.fxzs.lingxiagent.model.deepresearch.api.DeepResearchApiService;
import com.fxzs.lingxiagent.model.deepresearch.dto.TripDeepResearchRes;
import com.fxzs.lingxiagent.model.user.UserUtil;
import com.fxzs.lingxiagent.network.ZNet.RetrofitClient;
import com.fxzs.lingxiagent.util.DeviceUUIDGenerator;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.util.SignatureGenerator;
import com.fxzs.lingxiagent.util.SignatureUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tencent.cos.xml.utils.StringUtils;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import okhttp3.internal.http2.StreamResetException;
import timber.log.Timber;

public class DeepResearchImpl implements DeepResearchRepository{
    private static final String TAG = "DeepResearchImpl";
    private final DeepResearchApiService deepResearchApiService;
    private final Context context;
    private Disposable sseDisposable;
    private static final Gson gson = new GsonBuilder().create();

    public DeepResearchImpl(Context context) {
        this.context = context;
        this.deepResearchApiService = RetrofitClient.createDeepResearchApi();
    }

    @Override
    public Disposable sendStreamRequest(String inputString, String req_id, DeepResearchStreamHandler handler) {

        String query = inputString;
        String device_id = DeviceUUIDGenerator.getDeviceUUID(context);
        AIAssistConfig aiAssistConfig = AIServiceManager.Companion.getInstance().getAiAssistConfig();

        String timestamp = String.valueOf(System.currentTimeMillis());
        String signature = SignatureUtil.setMd5Signature(
                aiAssistConfig.getDeviceSecret() + timestamp
        );

        // 创建请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("token", SignatureGenerator.generateSignature(req_id, device_id));
        headers.put("clientId", "lingxi_android");

        headers.put("ts", timestamp);
        headers.put("sign", signature);

        if (aiAssistConfig != null){
            headers.put("deviceNo", aiAssistConfig.getDeviceNo());
            headers.put("deviceId", aiAssistConfig.getDeviceId());
            headers.put("productKey", aiAssistConfig.getProductKey());
            headers.put("productId", aiAssistConfig.getProductId());
        }

        // 发送请求
        Observable<ResponseBody> responseBodyObservable;
        String phoneNumber = UserUtil.formatReallyPhone(SharedPreferencesUtil.getUserPhone());
        String devicesInfo = StringUtils.isEmpty(Build.MODEL)
                ? (context.getResources().getBoolean(R.bool.isTablet) ? "Pad" : "Phone")
                : Build.MODEL;
        Timber.tag(TAG).i("设备型号: %s", devicesInfo);
        responseBodyObservable = deepResearchApiService.sendStreamDeepResearchRequest(headers, req_id,query,device_id,devicesInfo,phoneNumber);

        Observable<String> sseObservable = parseSseStream(responseBodyObservable);
        // 流结束
        sseDisposable = sseObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        data -> {
                            Timber.tag(TAG).d("SSE数据%s：", String.valueOf(data));
                            // 处理接收到的 SSE 数据
                            handleDataChunk(data, handler);
                        },
                        throwable -> {
                            // 处理错误
                            handler.onError("网络请求失败: " + throwable.getMessage());
                        },
                        handler::onStreamComplete
                );
        return sseDisposable;
    }

    private JSONObject createRequestBody(String inputString, String ts) {

        try {
            JSONObject data = new JSONObject();
            data.put("req_id", ts);
            data.put("query", inputString);
            data.put("device_id",  DeviceUUIDGenerator.getDeviceUUID(context));
            return data;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    public Observable<String> parseSseStream(Observable<ResponseBody> responseBodyObservable) {
        return responseBodyObservable
                .subscribeOn(Schedulers.io())
                .flatMap(responseBody -> Observable.create(emitter -> {
                    BufferedReader reader = null;
                    try {
                        reader = new BufferedReader(new InputStreamReader(responseBody.byteStream()));
                        String line = "";
                        while (!emitter.isDisposed() && (line = reader.readLine()) != null) {
//                            {"think": "用户", "web_search": null, "report": null}
                            if (line.contains("Download link sent.")) {
                                emitter.onComplete();
                            } else if (line.startsWith("data:")) {
                                String data = line.substring(5).trim();
                                if (!data.isEmpty()) {
                                    emitter.onNext(data);
                                }
                            } else {
                                if (!line.isEmpty()) {
                                    emitter.onNext(line);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Timber.tag(TAG).d("SSE异常：" + e.getMessage());
                        if (!emitter.isDisposed()) {
                            // 忽略 StreamResetException
                            if (!(e instanceof StreamResetException && e.getMessage().contains("CANCEL"))) {
                                emitter.onError(e);

                            } else {
                                emitter.onComplete();
                            }
                        }
                    } finally {
                        // 确保资源关闭
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (Exception ignored) {}
                        }
                        responseBody.close();
                    }
                }));
    }
    private void handleDataChunk(String dataStr, DeepResearchStreamHandler handler) {
        try {
            TripDeepResearchRes resp = gson.fromJson(dataStr, TripDeepResearchRes.class);
            handler.onDataChunk(resp);
        } catch (Exception e) {
            handler.onError("数据解析失败: " + e.getMessage());
        }
    }
    public void interruptMessage() {
        Timber.tag(TAG).d("中断消息");
        if (sseDisposable != null && !sseDisposable.isDisposed()) {
            sseDisposable.dispose();
        }
    }
}
