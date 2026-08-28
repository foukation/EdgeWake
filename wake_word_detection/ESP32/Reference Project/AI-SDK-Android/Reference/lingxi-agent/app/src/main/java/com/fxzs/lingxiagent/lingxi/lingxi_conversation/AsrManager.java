package com.fxzs.lingxiagent.lingxi.lingxi_conversation;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.cmdc.ai.assist.api.AIFoundationKit;
import com.cmdc.ai.assist.api.SpeechRecognitionPersistentAliYun;
import com.cmdc.ai.assist.constraint.SpeechRecognitionPersistentAliYunData;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;

import java.nio.ByteBuffer;

import timber.log.Timber;

/**
 * 创建者：ZyOng
 * 描述：封装语音识别
 * 创建时间：2025/8/15 下午5:01
 */
public class AsrManager {

    private static final String TAG = "AsrManager";

    private AIFoundationKit aiFoundationKit;
    private SpeechRecognitionPersistentAliYun speechRecognitionPersistent;
    private boolean isVoiceCancel = false;
    private String curAsrResult = "";
    private SpeechRecognitionPersistentAliYunData mSpeechRecognitionPersistentData;

    private AsrResultListener resultListener;

    private Integer asrType;
    private boolean isCancelVoice = false;//取消识别
    private int vadTime = 800;//默认vad 时间 唤醒asr
    private boolean isEndClose = true;//判断结束时机 close  true ，唤醒asr,通过isFinal判断结束时机 false
    private boolean isRecognizing = false;

    private static int activeRecognizingCount = 0;

    public static boolean isRecognizing() {
        return activeRecognizingCount > 0;
    }

    private void markRecognizingStarted() {
        Timber.tag(TAG).d("isRecognizing : %s", isRecognizing);
        if (!isRecognizing) {
            isRecognizing = true;
            activeRecognizingCount = 1;
        }
    }

    private void markRecognizingEnded() {
        Timber.tag(TAG).d("isRecognizing : %s，activeRecognizingCount = %s", isRecognizing,activeRecognizingCount);
        if (isRecognizing) {
            isRecognizing = false;
            activeRecognizingCount = 0;
        }
    }

    public interface AsrResultListener {
        void onFinalResult(String text);

        void onPartialResult(String text);

        void onError(String errorMsg);

        /**
         * 目前主要用于处理网络异常语音识别执行onClose方法
         * @param text
         */
        void onCloseError(String text);
    }

    public AsrManager() {
        aiFoundationKit = new AIFoundationKit();
    }

    private void initAsrType() {
        String languageCode = SharedPreferencesUtil.getLanguageCode();
        boolean isNumeric = languageCode.matches("\\d+");
        if (isNumeric)
            asrType = Integer.parseInt(languageCode);
    }

    public void setResultListener(AsrResultListener listener) {
        this.resultListener = listener;
    }

    public void toggleRecognition() {
        initAsrType();
//        stopRecognition();
        startRecognition();
    }

    public void startRecognition() {
        Timber.tag(TAG).d("初始化语音识别");
        markRecognizingStarted();
        isVoiceCancel = false;
        isCancelVoice = false;
        if (aiFoundationKit == null) {
            aiFoundationKit = new AIFoundationKit();
        }
//        speechRecognitionPersistent = aiFoundationKit.speechRecognitionPersistentHelp();
        speechRecognitionPersistent = aiFoundationKit.speechRecognitionPersistentAliYunHelp();
        speechRecognitionPersistent.setListener(new SpeechRecognitionPersistentAliYun.ASRListener() {

            @Override
            public void onAudioEnergy(float v) {

            }

            @Override
            public void onConnected() {
                Timber.tag(TAG).d("speechRecognition_onConnected ，是否取消识别 = %s",isVoiceCancel);
                if (isVoiceCancel){//识别结束
                    curAsrResult = "";
                    if (resultListener != null) {
                        resultListener.onCloseError(curAsrResult);
                    }
                }
            }

            @Override
            public void onMessageReceived(@Nullable SpeechRecognitionPersistentAliYunData speechRecognitionPersistentData) {
                Timber.tag(TAG).d("speechRecognition_onMessageReceived : %s", speechRecognitionPersistentData + " isVoiceCancel=" + isVoiceCancel);

                if (speechRecognitionPersistentData == null) {
                    curAsrResult = "";
                    isVoiceCancel = false;
                    return;
                }
                mSpeechRecognitionPersistentData = speechRecognitionPersistentData;

                String type = speechRecognitionPersistentData.getType();
                SpeechRecognitionPersistentAliYunData.Data data = speechRecognitionPersistentData.getData();
                if (data != null && Boolean.FALSE.equals(data.isFinal())) {
                    Timber.tag(TAG).d("speechRecognition_识别过程结果内容 : %s, resultListener = %s", curAsrResult,resultListener);
                    if (resultListener != null) {
                        resultListener.onPartialResult(data.getText());
                    }
                    return;
                }
                if (data != null && Boolean.TRUE.equals(data.isFinal())) {//isFinal true 识别结束
                    curAsrResult += data.getText();
                    Timber.tag(TAG).d("speechRecognition_识别完成结果内容 : %s, isFinal = %s,resultListener = %s ， 是否取消识别isCancelVoice = %s,结束识别时机 = %s", curAsrResult,data.isFinal(),resultListener,isCancelVoice,isEndClose);
                    if (resultListener != null && !TextUtils.isEmpty(curAsrResult) && !isCancelVoice) {
//
                        if (!isEndClose){
                            resultListener.onFinalResult(curAsrResult);
                            isVoiceCancel = false;
                        }else {
                            resultListener.onPartialResult(curAsrResult);
                        }

                    }else {
                        if (resultListener != null && TextUtils.isEmpty(curAsrResult)) {
                            resultListener.onCloseError(curAsrResult);
                            isVoiceCancel = false;
                        }
                    }
                }


//                Timber.tag(TAG).d("speechRecognition_onMessageReceived_error : %s, error_message = %s", speechRecognitionPersistentData.getErrorNumber(),speechRecognitionPersistentData.getErrorMessage());
//                if ("error".equals(type) && resultListener != null){
//                    resultListener.onError(speechRecognitionPersistentData.getErrorMessage());
//                }
            }

            @Override
            public void onMessageReceived(@Nullable ByteBuffer byteBuffer) {
            }

            @Override
            public void onClose(int i, @Nullable String s, boolean b) {
                Timber.tag(TAG).e("speechRecognition_onClose    isVoiceCancel=%s    error=%s", isVoiceCancel, mSpeechRecognitionPersistentData != null ? mSpeechRecognitionPersistentData.getErrorNumber() : "null");
                markRecognizingEnded();
                if (!isEndClose){
                    isVoiceCancel = false;
                    return;
                }
                if (!TextUtils.isEmpty(curAsrResult) && isVoiceCancel) {
                    if (resultListener != null) {
                        resultListener.onFinalResult(curAsrResult);
                    }
                    isVoiceCancel = false;
                }else {
                    if (resultListener != null && TextUtils.isEmpty(curAsrResult)) {
                        resultListener.onCloseError(curAsrResult);
                    }
                }
            }

            @Override
            public void onError(@Nullable Exception e) {
                Timber.tag(TAG).d("speechRecognition_onError %s", e != null ? e.getMessage() : "unknown");
                if (resultListener != null && e != null) {
                    resultListener.onError(e.getMessage());
                }
            }

        });

        Timber.tag(TAG).d("speechRecognition_startRecognition()");
        curAsrResult = "";
        speechRecognitionPersistent.setVad(vadTime);
        if (asrType == null) {
            speechRecognitionPersistent.startRecognition();
            return;
        }
        Timber.tag(TAG).d("asrType : %s", asrType);
//        speechRecognitionPersistent.startRecognition(asrType);

        speechRecognitionPersistent.startRecognition();
    }

    public void stopRecognition() {
        if (speechRecognitionPersistent != null) {
            Timber.tag(TAG).d("speechRecognition_stop");
            speechRecognitionPersistent.cancel();//取消识别
            isVoiceCancel = false;
            isCancelVoice = true;
            speechRecognitionPersistent = null;
        }
        markRecognizingEnded();
    }

    public void cancelRecognition() {
        Timber.tag(TAG).d("speechRecognition_cancel%s", speechRecognitionPersistent);
        if (speechRecognitionPersistent != null) {
            Timber.tag(TAG).d("speechRecognition_cancel");
            speechRecognitionPersistent.finish();//识别结果
            isVoiceCancel = true;
        }
        markRecognizingEnded();
    }

    public void onDestroy() {
        if (speechRecognitionPersistent != null) {
            stopRecognition();
            speechRecognitionPersistent = null;
        } else {
            markRecognizingEnded();
        }
        if (aiFoundationKit != null) {
            aiFoundationKit = null;
        }

        if (resultListener != null) {
            resultListener = null;
        }
    }


    public void setVadTime(int time){
        this.vadTime = time;
    }

    public void callbackSite(boolean isEnd){
        this.isEndClose = isEnd;
    }

}

