package com.fxzs.lingxiagent.lingxi.translate;

import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.fxzs.lingxiagent.BuildConfig;
import com.fxzs.lingxiagent.lingxi.translate.audio.PcmAudioRecorder;
import com.fxzs.lingxiagent.lingxi.translate.model.TranslationItem;
import com.fxzs.lingxiagent.lingxi.translate.websocket.AsrWebSocketClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import timber.log.Timber;

/**
 * WebSocket同传翻译管理器
 * 使用WebSocket进行实时语音识别和翻译
 * 新增：支持PCM音频本地保存到手机应用私有目录
 */
public class WebSocketTranslationManager implements
        AsrWebSocketClient.AsrWebSocketListener,
        PcmAudioRecorder.AudioDataListener {

    private static final String TAG = "WSTranslationManager";

    private Context context;
    private AsrWebSocketClient webSocketClient;
    private PcmAudioRecorder audioRecorder;
    private TranslationCallback callback;

    // 本地缓存列表和去重集合
    private List<TranslationItem> localCacheList = new CopyOnWriteArrayList<>();
    private Set<String> processedSeIds = new HashSet<>();

    // 最后处理的项目（用于中间结果更新）
    private String currentMidResultSeId = "";
    private String lastBroadcastMidAsr = "";
    private String lastBroadcastFinalAsr = "";
    private String lastBroadcastTranslation = "";

    // ===================== 音频本地存储相关 =====================
    private FileOutputStream pcmFileOutputStream;
    private File currentPcmFile;
    // 是否开启音频本地保存开关
    private boolean enableSaveAudioLocal = BuildConfig.FLAVOR.contains("Beta");

    /** 停录后等待服务端返回尾部 FIN，再断开 WebSocket */
    private static final long DISCONNECT_DELAY_MS = 5000;
    private static final int KEEPALIVE_PACKET_BYTES = 1280;
    private static final byte[] SILENCE_PACKET = new byte[KEEPALIVE_PACKET_BYTES];
    private final Object disconnectLock = new Object();
    private Thread pendingDisconnectThread;
    /** 停录后正在等待尾部 FIN，此期间忽略 Idle timeout */
    private volatile boolean drainingAfterStop = false;

    /** WS 连接建立前先录，最多缓存 2s 音频 */
    private static final int WS_RING_BUFFER_PACKETS = 50;
    private static final int MAX_IDLE_RECONNECT_ATTEMPTS = 5;
    private final Object audioSendLock = new Object();
    private final List<byte[]> wsRingBuffer = new ArrayList<>();
    private volatile boolean wsReadyForAudio = false;
    private volatile boolean sessionStopping = false;
    private volatile boolean suppressDisconnectStop = false;

    private String lastSourceLanguage;
    private String lastTargetLanguage;
    private int idleReconnectAttempts;

    public interface TranslationCallback {
        void onTranslationStarted();
        void onTranslationStopped();
        void onError(String error);
    }

    public WebSocketTranslationManager(Context context, TranslationCallback callback) {
        this.context = context;
        this.callback = callback;
        this.webSocketClient = new AsrWebSocketClient(this, context);
        this.audioRecorder = new PcmAudioRecorder(context, this);
    }

    /**
     * 开始翻译
     * @param sourceLanguage 源语言代码
     * @param targetLanguage 目标语言代码
     */
    public void startTranslation(String sourceLanguage, String targetLanguage) {
        Timber.tag(TAG).d( "Starting translation: " + sourceLanguage + " -> " + targetLanguage);

        try {
            cancelPendingDisconnect();
            resetAudioSendState();
            sessionStopping = false;
            idleReconnectAttempts = 0;
            lastSourceLanguage = sourceLanguage;
            lastTargetLanguage = targetLanguage;

            // 清空中间结果状态
            currentMidResultSeId = "";
            lastBroadcastMidAsr = "";
            lastBroadcastFinalAsr = "";
            lastBroadcastTranslation = "";

            // 先开麦再连 WS，避免握手期间丢失开头语音
            if (!audioRecorder.startRecording()) {
                if (callback != null) {
                    callback.onError("无法启动音频录制");
                }
                return;
            }

            webSocketClient.connect(sourceLanguage, targetLanguage);

        } catch (Exception e) {
            Timber.tag(TAG).e( "Error starting translation", e);
            if (callback != null) {
                callback.onError("启动翻译失败: " + e.getMessage());
            }
        }
    }

    /**
     * 停止翻译
     */
    public void stopTranslation() {
        Timber.tag(TAG).d( "Stopping translation");
        sessionStopping = true;

        try {
            // 停止录音
            if (audioRecorder.isRecording()) {
                audioRecorder.stopRecording();
            }

            // 发送音频结束信号，延迟断开以收取尾部 FIN
            webSocketClient.sendEndOfAudio();
            scheduleDelayedDisconnect();

            if (callback != null) {
                callback.onTranslationStopped();
            }

        } catch (Exception e) {
            Timber.tag(TAG).e( "Error stopping translation", e);
        }
    }

    private void scheduleDelayedDisconnect() {
        cancelPendingDisconnect();
        drainingAfterStop = true;
        Thread thread = new Thread(() -> {
            try {
                long deadline = System.currentTimeMillis() + DISCONNECT_DELAY_MS;
                while (System.currentTimeMillis() < deadline) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    // 停录后仍需发静音包保活，否则服务端会报 20314 Idle timeout
                    if (webSocketClient.isConnected()) {
                        webSocketClient.sendAudioData(SILENCE_PACKET);
                    }
                    Thread.sleep(40);
                }
                synchronized (disconnectLock) {
                    if (Thread.currentThread() != pendingDisconnectThread) {
                        return;
                    }
                    pendingDisconnectThread = null;
                }
                Timber.tag(TAG).d("Delayed disconnect after stop");
                webSocketClient.disconnect();
            } catch (InterruptedException e) {
                Timber.tag(TAG).d("Delayed disconnect cancelled");
            } finally {
                drainingAfterStop = false;
            }
        }, "WSDisconnectDelay");
        synchronized (disconnectLock) {
            pendingDisconnectThread = thread;
        }
        thread.start();
    }

    private void cancelPendingDisconnect() {
        synchronized (disconnectLock) {
            drainingAfterStop = false;
            if (pendingDisconnectThread != null) {
                pendingDisconnectThread.interrupt();
                pendingDisconnectThread = null;
            }
        }
    }

    /**
     * 暂停录音（保持连接）
     */
    public void pauseRecording() {
        if (audioRecorder.isRecording()) {
            audioRecorder.pauseRecording();
        }
    }

    /**
     * 恢复录音
     */
    public void resumeRecording() {
        if (audioRecorder.isRecording()) {
            audioRecorder.resumeRecording();
        }
    }

    /**
     * 检查是否正在翻译
     */
    public boolean isTranslating() {
        return webSocketClient.isConnected() || audioRecorder.isRecording();
    }

    /**
     * 获取本地缓存的翻译项目列表
     */
    public List<TranslationItem> getLocalCacheList() {
        return new ArrayList<>(localCacheList);  // 返回副本以避免外部修改
    }

    /**
     * 获取缓存列表大小
     */
    public int getCacheSize() {
        return localCacheList.size();
    }

    /**
     * 清空本地缓存
     */
    public void clearLocalCache() {
        localCacheList.clear();
        processedSeIds.clear();
        currentMidResultSeId = "";
        lastBroadcastMidAsr = "";
        lastBroadcastFinalAsr = "";
        lastBroadcastTranslation = "";
        Timber.tag(TAG).d( "Local cache cleared");
    }

    // ===================== 音频存储对外方法 =====================
    public void setEnableSaveAudioLocal(boolean enable) {
        this.enableSaveAudioLocal = enable;
    }

    public boolean isEnableSaveAudioLocal() {
        return enableSaveAudioLocal;
    }

    public File getCurrentPcmAudioFile() {
        return currentPcmFile;
    }

    // ============= AsrWebSocketClient.AsrWebSocketListener 实现 =============

    private void resetAudioSendState() {
        synchronized (audioSendLock) {
            wsRingBuffer.clear();
            wsReadyForAudio = false;
        }
    }

    private void pauseAudioSend() {
        synchronized (audioSendLock) {
            wsReadyForAudio = false;
        }
    }

    private boolean isRecoverableIdleTimeout(String error) {
        return error != null
                && (error.contains("Idle timeout") || error.contains("20314"));
    }

    private boolean tryReconnectAfterIdle(String reason) {
        if (sessionStopping || !audioRecorder.isRecording()) {
            return false;
        }
        if (lastSourceLanguage == null || lastTargetLanguage == null) {
            return false;
        }
        if (idleReconnectAttempts >= MAX_IDLE_RECONNECT_ATTEMPTS) {
            Timber.tag(TAG).e("Idle timeout reconnect exceeded max attempts");
            return false;
        }
        idleReconnectAttempts++;
        Timber.tag(TAG).w("Server idle timeout (%s), reconnecting %d/%d, keep recording",
                reason, idleReconnectAttempts, MAX_IDLE_RECONNECT_ATTEMPTS);
        pauseAudioSend();
        suppressDisconnectStop = true;
        webSocketClient.disconnect();
        webSocketClient.connect(lastSourceLanguage, lastTargetLanguage);
        return true;
    }

    private void flushWsRingBufferLocked() {
        for (byte[] packet : wsRingBuffer) {
            webSocketClient.sendAudioData(packet);
        }
        wsRingBuffer.clear();
    }

    @Override
    public void onConnected() {
        Timber.tag(TAG).d("WebSocket connected, flushing pre-buffered audio");
        synchronized (audioSendLock) {
            flushWsRingBufferLocked();
            wsReadyForAudio = true;
        }
    }

    @Override
    public void onDisconnected() {
        Timber.tag(TAG).d("WebSocket disconnected");
        pauseAudioSend();

        if (suppressDisconnectStop) {
            suppressDisconnectStop = false;
            return;
        }
        if (sessionStopping) {
            if (audioRecorder.isRecording()) {
                audioRecorder.stopRecording();
            }
        }
    }

    @Override
    public void onTranscriptionResult(String text, boolean isFinal) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (isFinal) {
            if (text.equals(lastBroadcastFinalAsr)) {
                return;
            }
            lastBroadcastFinalAsr = text;
            lastBroadcastMidAsr = "";
            Timber.tag(TAG).d("Transcription result: " + text + " (final: true)");
            sendAsrFinalResult(text);
        } else {
            if (text.equals(lastBroadcastMidAsr)) {
                return;
            }
            lastBroadcastMidAsr = text;
            Timber.tag(TAG).d("Transcription result: " + text + " (final: false)");
            sendAsrMidResult(text);
        }
    }

    @Override
    public void onTranslationResult(String translatedText) {
        if (translatedText == null || translatedText.isEmpty()) {
            return;
        }
        if (translatedText.equals(lastBroadcastTranslation)) {
            return;
        }
        lastBroadcastTranslation = translatedText;
        Timber.tag(TAG).d("Translation result: " + translatedText);
        sendTranslationResult(translatedText);
    }

    @Override
    public void onTranslationItems(List<TranslationItem> items) {
        Timber.tag(TAG).d("Received " + items.size() + " translation items");

        boolean hasUpdates = false;

        // 处理每个接收到的项目
        for (TranslationItem item : items) {
            String seId = item.getSeId();
            boolean isEnd = item.isEnd();

            if ("current_message".equals(seId)) {
                // 中间结果 (type="MID") - 覆盖当前正在编辑的消息
                TranslationItem currentItem = findItemBySeId("current_message");

                if (currentItem == null) {
                    localCacheList.add(item);
                    Timber.tag(TAG).d("Added new MID message: " + item.getSourceText());
                    hasUpdates = true;
                } else {
                    boolean contentChanged = !equalsText(currentItem.getSourceText(), item.getSourceText())
                            || !equalsText(currentItem.getTargetText(), item.getTargetText());
                    if (contentChanged) {
                        updateExistingItem(currentItem, item);
                        Timber.tag(TAG).d("Updated MID message: " + item.getSourceText());
                        hasUpdates = true;
                    }
                }

            } else if (seId.startsWith("fin_")) {
                // 最终结果 (type="FIN") - 当前消息结束

                // 1. 先移除中间结果的占位符
                TranslationItem currentItem = findItemBySeId("current_message");
                if (currentItem != null) {
                    localCacheList.remove(currentItem);
                    processedSeIds.remove("current_message");
                    Timber.tag(TAG).d("Removed MID placeholder");
                }

                // 2. 添加最终结果作为新的一条消息
                localCacheList.add(item);
                processedSeIds.add(seId);
                lastBroadcastMidAsr = "";
                lastBroadcastFinalAsr = "";
                lastBroadcastTranslation = "";
                Timber.tag(TAG).d("Added FIN message: " + seId + " - " + item.getSourceText() + " -> " + item.getTargetText());

                hasUpdates = true;

            } else {
                // 其他格式（兼容旧协议）
                if (!processedSeIds.contains(seId)) {
                    localCacheList.add(item);
                    processedSeIds.add(seId);
                    hasUpdates = true;
                    Timber.tag(TAG).d("Added item (old protocol): " + seId);
                } else {
                    TranslationItem existingItem = findItemBySeId(seId);
                    if (existingItem != null) {
                        boolean contentChanged = !existingItem.getSourceText().equals(item.getSourceText()) ||
                                !existingItem.getTargetText().equals(item.getTargetText()) ||
                                existingItem.isEnd() != item.isEnd();

                        if (contentChanged) {
                            updateExistingItem(existingItem, item);
                            hasUpdates = true;
                            Timber.tag(TAG).d("Updated item (old protocol): " + seId);
                        }
                    }
                }
            }
        }

        // 如果有更新，通知界面刷新
        if (hasUpdates) {
            // 通知界面更新
            sendCacheListUpdate();
        }
    }

    /**
     * 根据SeId查找项目
     */
    private TranslationItem findItemBySeId(String seId) {
        for (TranslationItem item : localCacheList) {
            if (seId.equals(item.getSeId())) {
                return item;
            }
        }
        return null;
    }

    private static boolean equalsText(String a, String b) {
        if (a == null) {
            return b == null || b.isEmpty();
        }
        if (b == null) {
            return a.isEmpty();
        }
        return a.equals(b);
    }

    /**
     * 更新现有项目
     */
    private void updateExistingItem(TranslationItem existingItem, TranslationItem newItem) {
        existingItem.setSourceText(newItem.getSourceText());
        existingItem.setTargetText(newItem.getTargetText());
        existingItem.setIsEnd(newItem.isEnd());
        existingItem.setEndTime(newItem.getEndTime());
        existingItem.setSeVer(newItem.getSeVer());
    }

    /**
     * 发送缓存列表更新通知
     */
    private void sendCacheListUpdate() {
        Intent broadcastIntent = new Intent("com.fxzs.lingxiagent.CACHE_LIST_UPDATE");
        broadcastIntent.putExtra("cache_size", localCacheList.size());
        LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);
    }

    @Override
    public void onError(String error) {
        if (drainingAfterStop && isRecoverableIdleTimeout(error)) {
            Timber.tag(TAG).d("Idle timeout during post-stop drain, ignored");
            return;
        }

        if (tryReconnectAfterIdle(error)) {
            return;
        }

        Timber.tag(TAG).e("WebSocket error: " + error);
        sessionStopping = true;
        resetAudioSendState();

        if (audioRecorder.isRecording()) {
            audioRecorder.stopRecording();
        }

        if (callback != null) {
            callback.onError("翻译错误: " + error);
        }
    }

    // ============= PcmAudioRecorder.AudioDataListener 实现 =============

    @Override
    public void onAudioData(byte[] audioData) {
        synchronized (audioSendLock) {
            if (!wsReadyForAudio) {
                wsRingBuffer.add(audioData.clone());
                while (wsRingBuffer.size() > WS_RING_BUFFER_PACKETS) {
                    wsRingBuffer.remove(0);
                }
            } else {
                webSocketClient.sendAudioData(audioData);
            }
        }

        if (enableSaveAudioLocal && pcmFileOutputStream != null) {
            try {
                pcmFileOutputStream.write(audioData);
            } catch (IOException e) {
                Timber.tag(TAG).e(e, "写入PCM音频数据失败");
            }
        }
    }

    @Override
    public void onRecordingStarted() {
        Timber.tag(TAG).d( "Audio recording started");
        if (callback != null) {
            callback.onTranslationStarted();
        }

        // 开启本地存储，创建PCM录音文件
        if (enableSaveAudioLocal) {
            try {
                File saveDir = new File(context.getExternalFilesDir(null), "asr_audio_record");
                if (!saveDir.exists()) {
                    saveDir.mkdirs();
                }
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                currentPcmFile = new File(saveDir, "record_" + timeStamp + ".pcm");
                pcmFileOutputStream = new FileOutputStream(currentPcmFile);
                Timber.tag(TAG).d("PCM音频已保存至：" + currentPcmFile.getAbsolutePath());
            } catch (IOException e) {
                Timber.tag(TAG).e(e, "创建音频存储文件失败");
                pcmFileOutputStream = null;
                currentPcmFile = null;
            }
        }
    }

    @Override
    public void onRecordingStopped() {
        Timber.tag(TAG).d( "Audio recording stopped");
        // 关闭文件输出流
        if (pcmFileOutputStream != null) {
            try {
                pcmFileOutputStream.flush();
                pcmFileOutputStream.close();
                Timber.tag(TAG).d("PCM录音保存完成，路径：" + (currentPcmFile != null ? currentPcmFile.getAbsolutePath() : ""));
            } catch (IOException e) {
                Timber.tag(TAG).e(e, "关闭音频文件流出错");
            } finally {
                pcmFileOutputStream = null;
            }
        }
    }

    @Override
    public void onRecordingError(String error) {
        Timber.tag(TAG).e( "Audio recording error: " + error);

        // 异常时关闭音频文件流
        if (pcmFileOutputStream != null) {
            try {
                pcmFileOutputStream.close();
            } catch (IOException e) {
                Timber.tag(TAG).e(e, "异常关闭音频流失败");
            } finally {
                pcmFileOutputStream = null;
            }
        }

        // 断开WebSocket连接
        webSocketClient.disconnect();

        if (callback != null) {
            callback.onError("音频录制错误: " + error);
        }
    }

    // ============= 广播发送方法（兼容现有接口） =============

    private void sendAsrMidResult(String text) {
        Timber.tag(TAG).d( "Sending MID_RESULT broadcast: " + text);
        Intent broadcastIntent = new Intent("com.fxzs.lingxiagent.KEY_MID_RESULT");
        broadcastIntent.putExtra("mid_result", text);
        LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);
    }

    private void sendAsrFinalResult(String text) {
        Timber.tag(TAG).d( "Sending FINAL_RESULT broadcast: " + text);
        Intent broadcastIntent = new Intent("com.fxzs.lingxiagent.KEY_FINAL_RESULT");
        broadcastIntent.putExtra("final_result", text);
        LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);
    }

    private void sendTranslationResult(String translatedText) {
        Timber.tag(TAG).d( "Sending textReply broadcast: " + translatedText);
        Intent broadcastIntent = new Intent("com.fxzs.lingxiagent.textReply");
        broadcastIntent.putExtra("message", translatedText);
        broadcastIntent.putExtra("shouldMerge", true);
        broadcastIntent.putExtra("isShowBtnParam", false);
        LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);
    }

    /**
     * 释放资源
     */
    public void release() {
        Timber.tag(TAG).d( "Releasing resources");
        sessionStopping = true;
        cancelPendingDisconnect();
        resetAudioSendState();

        // 先停止录音（会触发 onRecordingStopped 关闭文件流）
        if (audioRecorder != null) {
            audioRecorder.stopRecording();
        }

        // 确保文件流已关闭（兜底）
        closePcmStream();

        // 断开WebSocket
        if (webSocketClient != null) {
            webSocketClient.release();
        }

        if (audioRecorder != null) {
            audioRecorder.release();
        }
    }

    private void closePcmStream() {
        if (pcmFileOutputStream != null) {
            try {
                pcmFileOutputStream.flush();
                pcmFileOutputStream.close();
            } catch (IOException e) {
                Timber.tag(TAG).e(e, "关闭音频流异常");
            } finally {
                pcmFileOutputStream = null;
            }
        }
    }
}