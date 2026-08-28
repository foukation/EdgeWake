package com.fxzs.lingxiagent.lingxi.translate.websocket;

import static com.fxzs.lingxiagent.model.common.Constants.CLIENT_ID;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.cmdc.ai.assist.constraint.AIAssistConfig;
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.AIServiceManager;
import com.fxzs.lingxiagent.lingxi.translate.model.TranslationItem;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.model.honor.repository.BenefitCode;
import com.fxzs.lingxiagent.util.BillDialogHelper;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import timber.log.Timber;

/**
 * ASR WebSocket客户端
 * 用于实时语音识别和翻译
 * 基于灵犀同声传译协议实现
 */
public class AsrWebSocketClient {

    private static final String TAG = "AsrWebSocketClient";
    // 使用配置类管理URL
    private static final String BASE_URL = WebSocketConfig.getWebSocketUrl();
    
    private WebSocket webSocket;
    private OkHttpClient client;
    private AsrWebSocketListener listener;
    private String sourceLanguage;
    private String targetLanguage;
    private boolean startConfigSent = false;
    private volatile boolean connected = false;
    
    // 用于累积同一句话的中间结果
    private String currentAsrText = "";
    private String currentTransText = "";
    
    // 用于检测无响应超时
    private long lastTranslationTime = 0;
    private long audioStartTime = 0;

    private static final long NO_RESPONSE_TIMEOUT = 10000; // 10秒无翻译结果视为超时
    private final Context context;
    private boolean hasReceivedResult = false;
    private boolean noResponseWarningSent = false;
    /** 开始送音频后，从未收到任何识别结果 */
    private static final long INITIAL_NO_RESULT_TIMEOUT_MS = 15000;
    /** 已有识别结果后，长时间无新结果（如电影静音段） */
    private static final long GAP_NO_RESULT_TIMEOUT_MS = 20000;
    
    public interface AsrWebSocketListener {
        void onConnected();
        void onDisconnected();
        void onTranscriptionResult(String text, boolean isFinal);
        void onTranslationResult(String translatedText);
        void onTranslationItems(List<TranslationItem> items);
        void onError(String error);
    }
    
    public AsrWebSocketClient(AsrWebSocketListener listener, Context context) {
        this.listener = listener;
        this.context = context;
        this.client = new OkHttpClient.Builder()
                .readTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(30, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS)
                .pingInterval(20, TimeUnit.SECONDS)
                .build();
    }
    
    /**
     * 连接WebSocket
     * @param sourceLanguage 源语言代码
     * @param targetLanguage 目标语言代码
     */
    public void connect(String sourceLanguage, String targetLanguage) {
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        this.startConfigSent = false;
        this.connected = false;
        this.currentAsrText = "";
        this.currentTransText = "";
        this.lastTranslationTime = 0;
        this.audioStartTime = 0;
        this.hasReceivedResult = false;
        this.noResponseWarningSent = false;
        
        Timber.tag(TAG).d("Connecting to: " + BASE_URL);
        Timber.tag(TAG).d("Language: " + sourceLanguage + " -> " + targetLanguage);

        AIAssistConfig aiAssistConfig = AIServiceManager.Companion.getInstance().getAiAssistConfig();
        String deviceId = aiAssistConfig.getDeviceId();
        String deviceNo = aiAssistConfig.getDeviceNo();
        String productId = aiAssistConfig.getProductId();
        String sign = aiAssistConfig.getSn();

        String userId = String.valueOf(SharedPreferencesUtil.getUserId());

        String wssUrl = Uri.parse(BASE_URL)
                .buildUpon()
                .appendQueryParameter("deviceId", deviceId)
                .appendQueryParameter("deviceNo", deviceNo)
                .appendQueryParameter("sign", sign)
                .appendQueryParameter("ts", String.valueOf(System.currentTimeMillis()))
                .appendQueryParameter("productId", productId)
                .appendQueryParameter("userId", userId)
                .appendQueryParameter("clientId", "lingxi_android")
                .build()
                .toString();
        
        Request request = new Request.Builder()
                .url(wssUrl)
                .build();
                
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                connected = true;
                Timber.tag(TAG).d("WebSocket connected, sending START config");
                sendStartConfig(webSocket);
            }
            
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Timber.tag(TAG).d("Received message: " + text);
                try {
                    JSONObject object = new JSONObject(text);

                    String msg = object.getString("msg");
                    int code = object.getInt("code");
                    if (BenefitCode.isBenefitError(String.valueOf(code))) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (listener != null) {
                                listener.onDisconnected();
                            }
                            if (context instanceof Activity) {

                                Activity activity = (Activity) context;

                                if (!activity.isFinishing()
                                        && !activity.isDestroyed()) {

                                    BillDialogHelper.showBillDialog(
                                            activity,
                                            msg,
                                            activity::finish
                                    );
                                }
                            }
                        });
                    } else {
                        handleTextMessage(text);
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

            }
            
            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                Timber.tag(TAG).d("Received binary data: " + bytes.size() + " bytes");
                // 二进制数据可能是TTS音频，暂时忽略
            }
            
            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                connected = false;
                Timber.tag(TAG).e("WebSocket error: " + t.getMessage(), t);
                if (listener != null) {
                    listener.onError(t.getMessage());
                }
            }
            
            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Timber.tag(TAG).d("WebSocket closing: " + reason);
                webSocket.close(1000, null);
            }
            
            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                connected = false;
                Timber.tag(TAG).d("WebSocket closed: " + reason);
                if (listener != null) {
                    listener.onDisconnected();
                }
            }
        });
    }
    
    /**
     * 发送START配置消息
     */
    private void sendStartConfig(WebSocket webSocket) {
        try {
            JSONObject config = new JSONObject();
            config.put("type", "START");
            config.put("from", sourceLanguage);
            config.put("to", targetLanguage);
            config.put("format", WebSocketConfig.AudioConfig.FORMAT);
            config.put("sampling_rate", WebSocketConfig.AudioConfig.SAMPLING_RATE);
            config.put("tts_speaker", WebSocketConfig.TtsConfig.SPEAKER);
            config.put("return_target_tts", WebSocketConfig.TtsConfig.RETURN_TARGET_TTS);
            
            String configStr = config.toString();
            
            // 详细日志：显示发送的语言代码
            Timber.tag(TAG).d("=== 发送START配置 ===");
            Timber.tag(TAG).d("源语言代码: " + sourceLanguage);
            Timber.tag(TAG).d("目标语言代码: " + targetLanguage);
            Timber.tag(TAG).d("完整配置: " + configStr);
            
            webSocket.send(configStr);
            startConfigSent = true;
            
            Timber.tag(TAG).d("START config sent: " + configStr);
            
            // 通知连接成功，可以开始发送音频
            if (listener != null) {
                listener.onConnected();
            }
        } catch (Exception e) {
            Timber.tag(TAG).e("Failed to send START config: " + e.getMessage(), e);
            if (listener != null) {
                listener.onError("配置发送失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 发送音频数据
     * @param audioData PCM音频数据（16kHz, 单声道, 16-bit, Little-Endian）
     */
    public void sendAudioData(byte[] audioData) {
        if (webSocket != null && audioData != null && audioData.length > 0 && startConfigSent) {
            // 记录第一次发送音频的时间
            if (audioStartTime == 0) {
                audioStartTime = System.currentTimeMillis();
                Timber.tag(TAG).d("🎤 开始发送音频数据，语言对：" + sourceLanguage + " -> " + targetLanguage);
            }

            checkNoResponseTimeout();
            
            ByteString byteString = ByteString.of(audioData);
            webSocket.send(byteString);
            // 减少日志输出频率，避免刷屏
            if (audioData.length > 0) {
                Timber.tag(TAG).v("Sent audio data: " + audioData.length + " bytes");
            }
        }
    }
    
    private void checkNoResponseTimeout() {
        if (audioStartTime == 0 || noResponseWarningSent) {
            return;
        }

        long now = System.currentTimeMillis();
        if (!hasReceivedResult) {
            long elapsedMs = now - audioStartTime;
            if (elapsedMs < INITIAL_NO_RESULT_TIMEOUT_MS) {
                return;
            }
            Timber.tag(TAG).w("⚠️ 已开始发送音频 " + (elapsedMs / 1000)
                    + " 秒，仍未收到任何识别结果");
            logNoResponseHints();
            noResponseWarningSent = true;
            return;
        }

        long gapMs = now - lastTranslationTime;
        if (gapMs < GAP_NO_RESULT_TIMEOUT_MS) {
            return;
        }
        Timber.tag(TAG).w("⚠️ 距上次识别结果已 " + (gapMs / 1000)
                + " 秒无新内容（可能为静音/BGM 段落）");
        logNoResponseHints();
        noResponseWarningSent = true;
    }

    private void logNoResponseHints() {
        Timber.tag(TAG).w("⚠️ 语言对：" + sourceLanguage + " -> " + targetLanguage);
        Timber.tag(TAG).w("⚠️ 可能原因：音量过低、环境噪音、服务端 VAD 未检出语音、或网络延迟");
    }

    private void markResultReceived() {
        lastTranslationTime = System.currentTimeMillis();
        hasReceivedResult = true;
        noResponseWarningSent = false;
    }

    /**
     * 检查音频质量
     */
    private void checkAudioQuality(byte[] audioData) {
        try {
            // 计算音频能量（音量）
            long sum = 0;
            int sampleCount = audioData.length / 2;
            
            for (int i = 0; i < audioData.length - 1; i += 2) {
                // Little-Endian: 低字节在前，高字节在后
                short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
                sum += Math.abs(sample);
            }
            
            long avgLevel = sum / sampleCount;
            
            Timber.tag(TAG).d("🔊 音频质量检测：平均音量 = " + avgLevel);
            
            if (avgLevel < 100) {
                Timber.tag(TAG).w("⚠️ 音频音量过低！当前: " + avgLevel + ", 建议: > 100");
                Timber.tag(TAG).w("⚠️ 请检查：1) 麦克风权限 2) 录音音量 3) 环境噪音");
            } else if (avgLevel > 20000) {
                Timber.tag(TAG).w("⚠️ 音频音量过高！当前: " + avgLevel + ", 可能失真");
            } else {
                Timber.tag(TAG).d("✅ 音频音量正常");
            }
            
        } catch (Exception e) {
            Timber.tag(TAG).e("检查音频质量失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送结束标志
     * 注意：新协议中不需要发送空包，直接关闭连接即可
     */
    public void sendEndOfAudio() {
        if (webSocket != null) {
            Timber.tag(TAG).d("Audio stream ended, will close connection");
            // 新协议中音频结束后服务端会自动检测，不需要发送特殊标记
        }
    }
    
    /**
     * 断开WebSocket连接
     */
    public void disconnect() {
        if (webSocket != null) {
            connected = false;
            webSocket.close(1000, "Client disconnect");
            webSocket = null;
        }
    }
    
    /**
     * 检查连接状态
     */
    public boolean isConnected() {
        return connected && webSocket != null;
    }
    
    /**
     * 处理文本消息
     * 新协议格式：
     * {
     *   "code": 0,
     *   "data": {
     *     "status": "TRN" | "END",
     *     "result": {
     *       "type": "FIN",
     *       "sentence": "原文",
     *       "sentence_trans": "译文",
     *       "provider": "提供商"
     *       // 或中间结果
     *       "asr": "识别中...",
     *       "asr_trans": "翻译中..."
     *     }
     *   }
     * }
     */
    private void handleTextMessage(String message) {
        try {
            JSONObject json = new JSONObject(message);
            
            // 检查是否为新协议格式
            if (json.has("code")) {
                handleNewProtocol(json);
            }
            // 兼容旧协议格式（List格式）
            else if (json.has("List")) {
                handleOldProtocol(json);
            }
            // 其他未知格式
            else {
                Timber.tag(TAG).w("Unknown message format: " + message);
            }
            
        } catch (Exception e) {
            Timber.tag(TAG).e("Error parsing message: " + message, e);
            if (listener != null) {
                listener.onError("解析服务器响应失败");
            }
        }
    }
    
    /**
     * 处理新协议格式（文档标准格式）
     */
    private void handleNewProtocol(JSONObject json) {
        try {
            int code = json.getInt("code");
            
            // 检查错误码
            if (code != 0) {
                String errorMsg = json.optString("msg", "服务器错误");
                Timber.tag(TAG).e("Server error: code=" + code + ", msg=" + errorMsg);
                if (listener != null) {
                    listener.onError(errorMsg);
                }
                return;
            }
            
            // 解析data字段
            if (!json.has("data")) {
                Timber.tag(TAG).w("Response missing 'data' field");
                return;
            }
            
            JSONObject data = json.getJSONObject("data");
            String status = data.optString("status", "");
            
            // 如果没有status字段，可能是连接确认消息
            if (status.isEmpty() && data.has("provider")) {
                String provider = data.optString("provider", "");
                String message = data.optString("message", "");
                Timber.tag(TAG).d("Provider info: " + provider + " - " + message);
                return;
            }
            
            if ("TRN".equals(status)) {
                // 翻译进行中
                handleTranslationResult(data);
            } else if ("END".equals(status)) {
                // 会话结束
                Timber.tag(TAG).d("Session ended by server");
                if (listener != null) {
                    listener.onDisconnected();
                }
            } else if ("STA".equals(status)) {
                // 连接确认消息，忽略
                Timber.tag(TAG).d("Connection confirmed (STA)");
            } else {
                Timber.tag(TAG).w("Unknown status: " + status);
                // 打印完整消息以便调试
                Timber.tag(TAG).w("Full message: " + data.toString());
            }
            
        } catch (Exception e) {
            Timber.tag(TAG).e("Error handling new protocol: " + e.getMessage(), e);
        }
    }
    
    /**
     * 处理翻译结果
     * 
     * 逻辑说明：
     * 1. type="MID" (中间结果) - 同一条消息的多次更新，使用固定的seId="current_message"
     * 2. type="FIN" (最终结果) - 当前消息结束，生成唯一的seId，标记isEnd=true
     * 3. 下一个MID - 重新使用seId="current_message"，开始新的一条消息
     */
    private void handleTranslationResult(JSONObject data) {
        try {
            if (!data.has("result")) {
                return;
            }
            
            JSONObject result = data.getJSONObject("result");
            String type = result.optString("type", "");
            
            if ("FIN".equals(type)) {
                markResultReceived();

                // 最终结果 - 当前消息结束
                String sentence = result.optString("sentence", "");
                String sentenceTrans = result.optString("sentence_trans", "");
                String provider = result.optString("provider", "");
                
                Timber.tag(TAG).d("FIN - Original: " + sentence);
                Timber.tag(TAG).d("FIN - Translation: " + sentenceTrans);
                Timber.tag(TAG).d("FIN - Provider: " + provider);
                
                if (listener != null) {
                    if (!sentence.isEmpty()) {
                        listener.onTranscriptionResult(sentence, true);
                    }
                    if (!sentenceTrans.isEmpty()) {
                        listener.onTranslationResult(sentenceTrans);
                    }
                }
                
                // 转换为TranslationItem格式（最终结果，使用唯一的seId）
                List<TranslationItem> items = new ArrayList<>();
                TranslationItem item = new TranslationItem(
                    "fin_" + System.currentTimeMillis(), // 唯一的seId
                    1, // seVer
                    sentence,
                    sentenceTrans,
                    -1, // startTime
                    -1, // endTime
                    true, // isEnd = true 表示最终结果
                    "" // audio
                );
                items.add(item);
                
                if (listener != null) {
                    listener.onTranslationItems(items);
                }
                
                // 清空累积的中间结果，准备接收下一条消息
                currentAsrText = "";
                currentTransText = "";
                
            } else {
                // 中间结果 - 同一条消息的更新，使用固定的seId
                String asr = result.optString("asr", "");
                String asrTrans = result.optString("asr_trans", "");

                boolean asrChanged = !asr.isEmpty() && !asr.equals(currentAsrText);
                boolean transChanged = !asrTrans.isEmpty() && !asrTrans.equals(currentTransText);
                if (!asrChanged && !transChanged) {
                    return;
                }

                markResultReceived();

                if (!asr.isEmpty()) {
                    currentAsrText = asr;
                    Timber.tag(TAG).d("MID - ASR: " + asr);
                    if (listener != null) {
                        listener.onTranscriptionResult(asr, false);
                    }
                }
                
                if (!asrTrans.isEmpty()) {
                    currentTransText = asrTrans;
                    Timber.tag(TAG).d("MID - Translation: " + asrTrans);
                    if (listener != null) {
                        listener.onTranslationResult(asrTrans);
                    }
                }
                
                // 转换为TranslationItem格式（中间结果，使用固定的seId="current_message"）
                if (!asr.isEmpty() || !asrTrans.isEmpty()) {
                    List<TranslationItem> items = new ArrayList<>();
                    TranslationItem item = new TranslationItem(
                        "current_message", // 固定的seId，用于覆盖显示
                        1,
                        asr,
                        asrTrans,
                        -1,
                        -1,
                        false, // isEnd = false 表示中间结果
                        ""
                    );
                    items.add(item);
                    
                    if (listener != null) {
                        listener.onTranslationItems(items);
                    }
                }
            }
            
        } catch (Exception e) {
            Timber.tag(TAG).e("Error handling translation result: " + e.getMessage(), e);
        }
    }
    
    /**
     * 处理旧协议格式（兼容保留）
     */
    private void handleOldProtocol(JSONObject json) {
        try {
            Timber.tag(TAG).d("Handling old protocol format");
            
            JSONArray list = json.getJSONArray("List");
            List<TranslationItem> translationItems = new ArrayList<>();
            
            for (int i = 0; i < list.length(); i++) {
                JSONObject result = list.getJSONObject(i);
                
                String seId = result.optString("SeId", "");
                int seVer = result.optInt("SeVer", 0);
                String sourceText = result.optString("SourceText", "");
                String targetText = result.optString("TargetText", "");
                int startTime = result.optInt("StartTime", -1);
                int endTime = result.optInt("EndTime", -1);
                boolean isEnd = result.optBoolean("IsEnd", false);
                String audio = result.optString("Audio", "");
                
                TranslationItem item = new TranslationItem(seId, seVer, sourceText, targetText, 
                                                           startTime, endTime, isEnd, audio);
                translationItems.add(item);
                
                Timber.tag(TAG).d("Parsed item " + i + " - SeId: " + seId + ", Source: " + sourceText + 
                        ", Target: " + targetText + ", IsEnd: " + isEnd);
            }
            
            if (listener != null && !translationItems.isEmpty()) {
                listener.onTranslationItems(translationItems);
            }
            
        } catch (Exception e) {
            Timber.tag(TAG).e("Error handling old protocol: " + e.getMessage(), e);
        }
    }
    
    /**
     * 资源清理
     */
    public void release() {
        disconnect();
        if (client != null) {
            client.dispatcher().executorService().shutdown();
        }
    }
}