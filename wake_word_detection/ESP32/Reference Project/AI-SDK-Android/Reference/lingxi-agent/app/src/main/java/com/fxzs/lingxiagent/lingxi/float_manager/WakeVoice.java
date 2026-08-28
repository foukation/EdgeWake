package com.fxzs.lingxiagent.lingxi.float_manager;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.cmdc.ai.assist.AIAssistantManager;
import com.cmdc.ai.assist.api.ASRIntelligentDialogue;
import com.cmdc.ai.assist.constraint.AsrResult;
import com.cmdc.ai.assist.constraint.DialogueResult;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.audio.TTSManager;
import com.fxzs.lingxiagent.view.common.VoiceButton;

import java.lang.ref.WeakReference;
import java.util.Random;

import timber.log.Timber;

/**
 * 创建者：ZyOng
 * 描述：
 * 创建时间：2026/1/15 上午11:15
 */
public class WakeVoice extends ConstraintLayout {
    private final String TAG = "WakeVoice";
    private View ll_bottom_edit;
    private LinearLayout ll_edit;

    private EditText ed;
    private Context context;
    private String curAsrResult = "";
    private final Handler decibelHandler = new DecibelHandler(this);
    private WakeVoiceCallback callback;

    private ASRIntelligentDialogue realtimeAsr;
    private VoiceButton wxVoiceButton;
    private AudioWaveGlowView wxVoiceButton2;

    public WakeVoice(Context context) {
        this(context, null);
    }

    public WakeVoice(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WakeVoice(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context.getApplicationContext();
        initView(this.context);
    }

    private void initView(Context context) {
        LayoutInflater.from(context).inflate(R.layout.wake_nexus, this, true);
        ll_bottom_edit = findViewById(R.id.ll_bottom_edit);
        wxVoiceButton = findViewById(R.id.btn_wx_voice);
        wxVoiceButton2 = findViewById(R.id.btn_wx_voice2);
        wxVoiceButton.setColor(ContextCompat.getColor(context, R.color.color_primary));
//        ShadowUtils.applyDefaultShadow(ll_bottom_edit, context,ContextCompat.getColor(context, R.color.float_color_primary),ContextCompat.getColor(context, R.color.transparent));
        initListener();
        float[] values = new float[36];
        Random random = new Random();
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(100);   // ≈ 60fps
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator animation) {
                for (int i = 0; i < values.length; i++) {
                    values[i] = 0.2f + random.nextFloat() * (1.0f - 0.2f);
                }
//                wxVoiceButton2.updateAmplitudes(values);
            }
        });
//        animator.start();

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        initAsrManger();
    }

    private void initListener() {

    }


    private void initAsrManger() {
        decibelHandler.sendEmptyMessage(0);
        toggleAsrRecognition();
    }


    public void setCallback(WakeVoiceCallback callback) {
        // 设置回调接口
        this.callback = callback;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        release();
        Timber.tag(TAG).e("onDetachedFromWindow");
    }



    public void release() {
        closeAsr();
        if (!TTSManager.getInstance().isStop()) {
            TTSManager.getInstance().stop();
        }
        decibelHandler.removeCallbacksAndMessages(null);
        if (wxVoiceButton != null) {
            wxVoiceButton.removeAllMessage();
        }
        callback = null;
    }


    private void toggleAsrRecognition() {
        Timber.tag(TAG).e("初始化收音");
        closeAsr();
        startAsr();
    }

    public void closeAsr() {
        if (realtimeAsr != null) {
            realtimeAsr.release();
            realtimeAsr = null;
        }
    }

    private void startAsr() {
        curAsrResult = "";
        if (realtimeAsr == null) {
            realtimeAsr = (ASRIntelligentDialogue) AIAssistantManager.Companion.getInstance().asrIntelligentDialogueHelp();
        }

        realtimeAsr.setListener(new ASRIntelligentDialogue.RealtimeAsrListener() {

            @Override
            public void onAsrMidResult(@NonNull AsrResult asrResult) {
                Timber.tag(TAG).e("onAsrMidResult = %s", asrResult.getText());
            }

            @Override
            public void onAsrFinalResult(@NonNull AsrResult asrResult) {
                Timber.tag(TAG).e("onAsrFinalResult = %s", asrResult.getText());
                curAsrResult = asrResult.getText();
                if (!TextUtils.isEmpty(asrResult.getText())){
                    if (callback != null){
                        closeAsr();
                        callback.onSuccessMsg(asrResult.getText());
                    }
                }
            }

            @Override
            public void onConnected() {
                Timber.tag(TAG).e("onConnected");
            }


            public void onDialogueResult(@NonNull DialogueResult result) {
                Timber.tag(TAG).e("onDialogueResult = %s", result);
            }

            @Override
            public void onError(int code, @NonNull String message) {
                Timber.tag(TAG).e("onError%s %s", message,code);
                if (callback != null){
                    callback.onErrorMsg(message,curAsrResult,code);
                }

//                if (message.contains("sent ping but didn't receive pong within 5000ms") || message.contains("failed to connect to")
//                        || message.contains("No address associated with hostname")) {
//                    if (callback != null){
//                        callback.onErrorMsg(message,curAsrResult,code);
//                    }
//                }

            }

            @Override
            public void onComplete() {
                Timber.tag(TAG).e("onComplete");
            }
        });
        realtimeAsr.startRecognition(context);
    }

    public void startVoice(){
        Timber.tag(TAG).e("开始收音");
//        realtimeAsr.startRecognition(context);
        toggleAsrRecognition();
    }


    private static class DecibelHandler extends Handler {
        private final WeakReference<WakeVoice> ref;
        public DecibelHandler(WakeVoice view) {
            super(Looper.getMainLooper());
            ref = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            WakeVoice view = ref.get();
            if (view == null ) return;
            view.wxVoiceButton.addVoiceSize(80);
            sendEmptyMessageDelayed(0, 500);
        }
    }


}
