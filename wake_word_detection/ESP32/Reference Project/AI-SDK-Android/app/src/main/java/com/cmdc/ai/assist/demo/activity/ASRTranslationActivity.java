package com.cmdc.ai.assist.demo.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;

import com.cmdc.ai.assist.AIAssistantManager;
import com.cmdc.ai.assist.api.ASRTranslation;
import com.cmdc.ai.assist.constraint.Language;
import com.cmdc.ai.assist.constraint.TranslationData;
import com.cmdc.ai.assist.constraint.TranslationTypeCode;
import com.cmdc.ai.assist.demo.R;

import java.nio.ByteBuffer;

import timber.log.Timber;

public class ASRTranslationActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = ASRTranslationActivity.class.getSimpleName();
    private Button button;
    private ASRTranslation asrTranslation;
    private ASRTranslation.ASRTranslationListener asrTranslationListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_realtime_layout);
        initData();
        initUi();
    }

    private void initData() {
        asrTranslation = (ASRTranslation) AIAssistantManager.Companion.getInstance().asrTranslationHelp();
    }

    private void initUi() {
        View rootView = findViewById(android.R.id.content);
        rootView.setOnClickListener(this);
        initButtons();
    }

    private void initButtons() {
        button = findViewById(R.id.asr_control);
        button.setText("开始识别");
        button.setOnClickListener(view -> toggleAsrRecognition());
        findViewById(R.id.open_activity_control).setVisibility(View.GONE);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == android.R.id.content) {
            toggleAsrRecognition();
        }
    }

    /**
     * 切换语音识别状态
     * 开始/停止语音识别，并处理相关资源
     */
    private void toggleAsrRecognition() {

        if (asrTranslation != null) {
            asrTranslation.release();
            Timber.tag(TAG).d("%s  cancelRecognition", asrTranslationListener.hashCode());
            asrTranslation = null;
            asrTranslationListener = null;
        }

        if (button.getText().equals("开始识别")) {
            button.setText("停止识别");
            start();
        } else {
            button.setText("开始识别");
        }
    }

    private void start() {
        if (asrTranslation == null) {
            asrTranslation = (ASRTranslation) AIAssistantManager.Companion.getInstance().asrTranslationHelp();
        }
        if (asrTranslationListener == null) {
            asrTranslationListener = new ASRTranslation.ASRTranslationListener() {
                @Override
                public void onMessageReceived(@Nullable TranslationData translationData) {
                    // 接收翻译结果
                    Timber.tag(TAG).d("%s%s%s", this.hashCode(), "  speechRecognitionTranslation  onMessageReceived  ", translationData != null ? translationData.getAsrResult() + " " + translationData.getTranslationResult() + " " + translationData.getType() : "null");
                }

                @Override
                public void onMessageReceived(ByteBuffer bytes) {
                    // 返回 pcm 格式的 tts 音频播报数据
                    Timber.tag(TAG).d("%s%s%s", this.hashCode(), "  speechRecognitionTranslation  onMessageReceived  ", bytes != null ? bytes.toString() : "null");
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    // 当连接关闭时调用
                    Timber.tag(TAG).d("%s%s%s", this.hashCode(), "  speechRecognitionTranslation  ", "onClose code：" + code + "  reason：" + reason + "  remote：" + remote);
                }

                @Override
                public void onError(Exception ex) {
                    // 当发生错误时调用
                    Timber.tag(TAG).e("%s%s%s", this.hashCode(), "  speechRecognitionTranslation  onError ", ex.toString());
                }
            };
        }
        speechRecognitionTranslation();
    }

    /**
     * 语音识别（持续识别）
     */
    private void speechRecognitionTranslation() {
        asrTranslation.setListener(asrTranslationListener);
        Timber.tag(TAG).d("%s  startRecognition", asrTranslationListener.hashCode());
        /*speechRecognitionPersistent.startRecognition();*/
        /*asrTranslation.startRecognition(TranslationTypeCode.ZH_TO_EN);*/
        asrTranslation.startRecognition(Language.CHINESE, Language.ENGLISH);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // 此处为android 6.0以上动态授权的回调，用户自行实现。
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (asrTranslation != null) {
            asrTranslation.release();  // 需要添加这个方法
            asrTranslation = null;
            button.setText("开始识别");
        }
    }
}
