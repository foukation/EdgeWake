package com.fxzs.lingxiagent.model.scene.repository;

import android.content.Context;

import com.cmdc.ai.assist.constraint.AIAssistConfig;
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.AIServiceManager;
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.LocalModule;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.model.honor.dto.MessageRole;
import com.fxzs.lingxiagent.model.network.SignVerifier;
import com.fxzs.lingxiagent.model.scene.api.SceneApiService;
import com.fxzs.lingxiagent.model.scene.dto.DeltaContentData;
import com.fxzs.lingxiagent.model.scene.dto.SceneResponse;
import com.fxzs.lingxiagent.network.ZNet.RetrofitClient;
import com.fxzs.lingxiagent.util.AesUtil;
import com.fxzs.lingxiagent.util.DeviceUUIDGenerator;
import com.fxzs.lingxiagent.util.GMapHelper;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.util.ZUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.internal.http2.StreamResetException;
import timber.log.Timber;

/**
 * 认证仓库实现
 */
public class SceneRepositoryImpl implements SceneRepository {

    private static final String TAG = "SceneRepositoryImpl";
    private final SceneApiService sceneApiService;
    private final Context context;
    private LocalModule currentModule;
    private JSONArray messages = new JSONArray();
    private String sessionId;
    private boolean interruptMessage = false;
    private Disposable sseDisposable;

    public SceneRepositoryImpl(Context context) {
        this.context = context;
        this.sceneApiService = RetrofitClient.createSceneApi();
        updateSession();
    }

    public void updateRequestInfo(LocalModule mainCurModule, long conversationId) {
        if (!mainCurModule.equals(this.currentModule)) {
            this.currentModule = mainCurModule;
            updateSession();
        }
        this.sessionId = String.valueOf(conversationId);
    }

    public void updateSession() {
        this.sessionId = String.valueOf(System.currentTimeMillis() + (System.nanoTime() % 1_000_000));
        this.messages = new JSONArray();
        this.interruptMessage = false;
    }

    public void updateMessages(String role, String content) {
        try {
            JSONObject message = new JSONObject();
            message.put("role", role);
            message.put("content", content);
            messages.put(message);
        } catch (Exception e) {
            Timber.tag(TAG).d( "updateMessages error %s", e);
        }
    }

    @Override
    public void sendStreamRequest(String inputString, SceneStreamHandler handler) {
        this.interruptMessage = false;
        String timestamp = String.valueOf(System.currentTimeMillis() + (System.nanoTime() % 1_000_000));
        try {
            JSONObject requestBodyJson = createRequestBody(inputString, sessionId, timestamp);
            RequestBody requestBody = RequestBody.create(
                    MediaType.parse("application/json"),
                    requestBodyJson.toString()
            );

            // 创建请求头
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Channel", "LxApp");
            headers.put("X-Request-Source", "lingxiapp_main");
            headers.put("deviceIdentifier", DeviceUUIDGenerator.getDeviceUUID(context));

            String phone = AesUtil.decrypt(SharedPreferencesUtil.getUserPhone(), SignVerifier.getClientId());
            String token = AesUtil.aes128Encryt(phone, Constants.TOKEN_SHA256_KEY);
            headers.put("X-User-Token", "ZD-MPN" + token);

            // 发送请求
            Observable<ResponseBody> responseBodyObservable = sceneApiService.sendStreamRequest(headers, requestBody);
            Observable<String> sseObservable = parseSseStream(responseBodyObservable);
            // 流结束
            sseDisposable = sseObservable
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            data -> {
                                // 处理接收到的 SSE 数据
                                Timber.tag(TAG).d( "subscribe " + data);
                                handleDataChunk(data, handler);
                            },
                            throwable -> {
                                // 处理错误
                                handler.onError("网络请求失败: " + throwable.getMessage());
                            },
                            handler::onStreamComplete
                    );
        } catch (Exception e) {
            handler.onError("请求创建失败: " + e.getMessage());
        }
    }

    public Observable<String> parseSseStream(Observable<ResponseBody> responseBodyObservable) {
        return responseBodyObservable
                .subscribeOn(Schedulers.io())
                .flatMap(responseBody -> Observable.create(emitter -> {
                    BufferedReader reader = null;
                    try {
                        reader = new BufferedReader(new InputStreamReader(responseBody.byteStream()));
                        String line;
                        while (!emitter.isDisposed() && (line = reader.readLine()) != null) {
                            if (line.contains("[DONE]")) {
                                emitter.onComplete();
                            } else if (line.startsWith("data:")) {

                                String data = line.substring(5).trim();
                                if (!data.isEmpty()) {
                                    emitter.onNext(data);
                                }
                            }
                        }
                    } catch (Exception e) {
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
                            } catch (Exception ignored) {
                            }
                        }
                        responseBody.close();
                    }
                }));
    }

    private void handleDataChunk(String dataStr, SceneStreamHandler handler) {
        try {
            if (dataStr.contains("\"error\":")) {
                ZUtils.showToast("请求失败 " + dataStr);
                handler.onError(dataStr);
            }
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(DeltaContentData.class, new ContentDataDeserializer())
                    .create();
            SceneResponse resp = gson.fromJson(dataStr, SceneResponse.class);
            handler.onDataChunk(resp);
        } catch (Exception e) {
            e.printStackTrace();
            Timber.tag(TAG).d( "数据解析失败 " + e.getMessage());
            handler.onError("数据解析失败: " + e.getMessage());
        }
    }

    public void interruptMessage() {
        this.interruptMessage = true;
        if (sseDisposable != null && !sseDisposable.isDisposed()) {
            sseDisposable.dispose();
        }
    }

    private JSONObject createRequestBody(String inputString, String sessionId, String ts) {
        try {
            JSONObject data = new JSONObject();
            data.put("reqId", ts);
            data.put("model", "九天⼤模型");
            data.put("deviceId", DeviceUUIDGenerator.getDeviceUUID(context));
            data.put("stream", true);
            if (currentModule == LocalModule.MGVIDOE || currentModule == LocalModule.MEDIA ) {
//                data.put("backend_service", "zdTravel");
                data.put("backend_service", "mgVideo");
            } else if (currentModule == LocalModule.FINANCE) {
                //金融助手
                data.put("backend_service", "jkReport");
            } else {
                //通信助手
                data.put("backend_service", "zxTel");
            }

            JSONObject endpoint = new JSONObject();
            JSONObject location = new JSONObject();
            location.put("longitude", String.valueOf(GMapHelper.getInstance().getLongitude()));
            location.put("latitude", String.valueOf(GMapHelper.getInstance().getLatitude()));
            location.put("locationSystem", "GCJ02");
            endpoint.put("location", location);
            endpoint.put("system", "Android");
            data.put("endpoint", endpoint);

            JSONObject session = new JSONObject();
            session.put("sessionId", sessionId);
            session.put("attributes", "{\"key\":\"value\"}");
            data.put("session", session);

            updateMessages(MessageRole.USER.getAlias(), inputString);
            data.put("messages", messages);

            Timber.tag(TAG).d( "请求体: %s" + data.toString());
            return data;
        } catch (Exception e) {
            Timber.tag(TAG).d( "createRequestBody error " + e);
            return new JSONObject();

        }
    }
}