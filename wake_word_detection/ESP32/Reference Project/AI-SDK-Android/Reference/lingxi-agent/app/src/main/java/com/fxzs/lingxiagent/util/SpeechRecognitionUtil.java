package com.fxzs.lingxiagent.util;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;

import timber.log.Timber;

/**
 * 语音识别工具类
 * 封装Android原生语音识别功能，提供简单易用的接口
 */
public class SpeechRecognitionUtil {
    
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    
    private Activity activity;
    private SpeechRecognizer speechRecognizer;
    private OnSpeechRecognitionListener listener;
    private boolean isListening = false;
    
    /**
     * 语音识别回调接口
     */
    public interface OnSpeechRecognitionListener {
        void onResult(String text);
        void onError(String errorMessage);
        void onReadyForSpeech();
        void onBeginningOfSpeech();
        void onEndOfSpeech();
        void onPartialResult(String partialText);
    }
    
    public SpeechRecognitionUtil(Activity activity) {
        this.activity = activity;
        initSpeechRecognizer();
    }
    
    /**
     * 设置语音识别监听器
     */
    public void setOnSpeechRecognitionListener(OnSpeechRecognitionListener listener) {
        this.listener = listener;
    }
    
    /**
     * 初始化语音识别器
     */
    private void initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
            // 语音识别不可用
            Timber.tag("SpeechRecognitionUtil").e( "Speech recognition is not available on this device");
            return;
        }
        
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    if (listener != null) {
                        listener.onReadyForSpeech();
                    }
                }
                
                @Override
                public void onBeginningOfSpeech() {
                    if (listener != null) {
                        listener.onBeginningOfSpeech();
                    }
                }
                
                @Override
                public void onRmsChanged(float rmsdB) {
                    // 音量变化
                }
                
                @Override
                public void onBufferReceived(byte[] buffer) {
                    // 接收到音频数据
                }
                
                @Override
                public void onEndOfSpeech() {
                    isListening = false;
                    if (listener != null) {
                        listener.onEndOfSpeech();
                    }
                }
                
                @Override
                public void onError(int error) {
                    isListening = false;
                    String errorMessage = getErrorMessage(error);
                    if (listener != null) {
                        listener.onError(errorMessage);
                    }
                }
                
                @Override
                public void onResults(Bundle results) {
                    isListening = false;
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String text = matches.get(0);
                        if (listener != null) {
                            listener.onResult(text);
                        }
                    }
                }
                
                @Override
                public void onPartialResults(Bundle partialResults) {
                    ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String text = matches.get(0);
                        if (listener != null) {
                            listener.onPartialResult(text);
                        }
                    }
                }
                
                @Override
                public void onEvent(int eventType, Bundle params) {
                    // 其他事件
                }
            });
        } catch (Exception e) {
            Timber.tag("SpeechRecognitionUtil").e( "Failed to initialize speech recognizer"+ e);
            speechRecognizer = null;
        }
    }
    
    /**
     * 开始语音识别
     */
    public void startListening() {
        if (!checkPermission()) {
            requestPermission();
            return;
        }
        
        if (isListening) {
            return;
        }
        
        if (speechRecognizer == null) {
            initSpeechRecognizer();
        }
        
        if (speechRecognizer == null) {
            // 语音识别不可用
            if (listener != null) {
                listener.onError("语音识别服务不可用");
            }
            return;
        }
        
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        
        isListening = true;
        speechRecognizer.startListening(intent);
    }
    
    /**
     * 停止语音识别
     */
    public void stopListening() {
        if (speechRecognizer != null && isListening) {
            isListening = false;
            speechRecognizer.stopListening();
        }
    }
    
    /**
     * 取消语音识别
     */
    public void cancelListening() {
        if (speechRecognizer != null && isListening) {
            isListening = false;
            speechRecognizer.cancel();
        }
    }
    
    /**
     * 销毁语音识别器
     */
    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        listener = null;
        isListening = false;
    }
    
    /**
     * 检查录音权限
     */
    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * 请求录音权限
     */
    private void requestPermission() {
//        ActivityCompat.requestPermissions(activity,
//                new String[]{Manifest.permission.RECORD_AUDIO},
//                REQUEST_RECORD_AUDIO_PERMISSION);
        AppPermissionRequestManager.requestAudioPermission(activity, REQUEST_RECORD_AUDIO_PERMISSION,AppPermissionRequestManager.PERMISSION_AUDIO_MESSAGE_ASR);
    }
    
    /**
     * 处理权限请求结果
     */
    public void onRequestPermissionsResult(int requestCode, int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening();
            } else {
                if (listener != null) {
                    listener.onError("需要录音权限才能使用语音输入");
                }
            }
        }
    }
    
    /**
     * 获取错误信息
     */
    private String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "音频录制错误";
            case SpeechRecognizer.ERROR_CLIENT:
                return "客户端错误";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "权限不足";
            case SpeechRecognizer.ERROR_NETWORK:
                return "网络错误";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "网络超时";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "未识别到语音";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "识别服务忙";
            case SpeechRecognizer.ERROR_SERVER:
                return "服务器错误";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "语音输入超时";
            default:
                return "未知错误";
        }
    }
    
    /**
     * 是否正在监听
     */
    public boolean isListening() {
        return isListening;
    }
}