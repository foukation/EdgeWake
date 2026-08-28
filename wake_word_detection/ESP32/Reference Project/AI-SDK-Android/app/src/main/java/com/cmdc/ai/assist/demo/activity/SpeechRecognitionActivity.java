package com.cmdc.ai.assist.demo.activity;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import androidx.annotation.Nullable;
import com.cmdc.ai.assist.AIAssistantManager;
import com.cmdc.ai.assist.api.AIFoundationKit;
import com.cmdc.ai.assist.api.SpeechRecognition;
import com.cmdc.ai.assist.constraint.SpeechRecognitionData;
import com.cmdc.ai.assist.demo.R;
import timber.log.Timber;

import java.nio.ByteBuffer;

public class SpeechRecognitionActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = SpeechRecognitionPersistentActivity.class.getSimpleName();
    private Button button;
    private SpeechRecognition speechRecognition;
    private SpeechRecognition.ASRListener asrListener;
    private AIFoundationKit aiFoundationKit;
    private long connectStartMs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_realtime_layout);
        initData();
        initUi();
    }

    private void initData() {
        aiFoundationKit = (AIFoundationKit) AIAssistantManager.Companion.getInstance().aiFoundationKit();
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

        if (speechRecognition != null) {
            speechRecognition.release();
            Timber.tag(TAG).d("%s  finish", asrListener.hashCode());
            speechRecognition = null;
            asrListener = null;
        }

        if (button.getText().equals("开始识别")) {
            button.setText("停止识别");
            start();
        } else {
            button.setText("开始识别");
        }
    }

    private void start() {
        if (speechRecognition == null) {
            speechRecognition = aiFoundationKit.speechRecognitionHelp();
        }
        if (asrListener == null) {
            asrListener = new SpeechRecognition.ASRListener() {

                @Override
                public void onMessageReceived(SpeechRecognitionData message) {
                    // 接收翻译结果
                    Timber.tag(TAG).d("%s%s%s", this.hashCode(), "  speechRecognitionPersistent  ", message != null ? message.toString() : "null");
                }

                @Override
                public void onMessageReceived(ByteBuffer bytes) {
                    // 返回 pcm 格式的 tts 音频播报数据
                    Timber.tag(TAG).d("%s%s%s", this.hashCode(), "  speechRecognitionPersistent  ", bytes != null ? bytes.toString() : "null");
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    // 当连接关闭时调用
                    Timber.tag(TAG).d("%s%s%s", this.hashCode(), "  speechRecognitionPersistent  ", "onClose");
                    if (this == asrListener) {
                        button.setText("开始识别");
                    }
                }

                @Override
                public void onError(Exception ex) {
                    // 当发生错误时调用
                    Timber.tag(TAG).e("%s%s%s", this.hashCode(), "  speechRecognitionPersistent  onError ", ex.toString());
                    if (this == asrListener) {
                        button.setText("开始识别");
                    }
                }
            };
        }
        speechRecognition.setListener(asrListener);
        speechRecognitionPersistent();
    }

    /**
     * 语音识别（持续识别）
     */
    private void speechRecognitionPersistent() {
        Timber.tag(TAG).d("%s  startRecognition", asrListener.hashCode());
        connectStartMs = SystemClock.elapsedRealtime();
        /*speechRecognitionPersistent.startRecognition();*/
        speechRecognition.startRecognition();
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
        if (speechRecognition != null) {
            speechRecognition.release();  // 需要添加这个方法
            speechRecognition = null;
            button.setText("开始识别");
        }
    }
}
