package com.fxzs.lingxiagent.lingxi.float_manager;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.cmdc.ai.assist.AIAssistantManager;
import com.cmdc.ai.assist.api.ASRIntelligentDialogue;
import com.cmdc.ai.assist.constraint.AsrResult;
import com.cmdc.ai.assist.constraint.DialogueResult;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.AsrManager;
import com.fxzs.lingxiagent.util.ZInputMethod;
import com.fxzs.lingxiagent.util.audio.TTSManager;
import com.fxzs.lingxiagent.view.common.NoMultiClickListener;
import com.fxzs.lingxiagent.view.widget.WaveFloatView;

import java.lang.ref.WeakReference;

import timber.log.Timber;

/**
 * 创建者：ZyOng
 * 描述：
 * 创建时间：2026/1/15 上午11:15
 */
public class WakeVoice2 extends ConstraintLayout {
    private final String TAG = "WakeVoice2";

    private EditText wakeEd;
    private Context context;
    private ImageView ivSend,ivVoice;
    private WaveFloatView btnWxVoice2;
    private String curAsrResult = "";
    private final Handler decibelHandler = new DecibelHandler(this);
    private final Handler uilHandler = new Handler(Looper.getMainLooper());
    private WakeVoiceCallback callback;

    private ASRIntelligentDialogue realtimeAsr;

    private boolean isRecord = true;

    private AsrManager asrManager;

    public WakeVoice2(Context context) {
        this(context, null);
    }

    public WakeVoice2(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WakeVoice2(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context.getApplicationContext();
        initView(this.context);
    }

    private void initView(Context context) {
        LayoutInflater.from(context).inflate(R.layout.wake_nexus2, this, true);
        wakeEd = findViewById(R.id.wakeEd);
        ivSend = findViewById(R.id.iv_send);
        ivVoice = findViewById(R.id.iv_voice);
        btnWxVoice2 = findViewById(R.id.btn_wx_voice2);
        initListener();
        btnWxVoice2.start();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Timber.tag(TAG).e("onAttachedToWindow");
        toggleAsrRecognition();
//        initAsrManger();
    }

    private void initListener() {
        wakeEd.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                isRecord = false;
                Timber.tag(TAG).e("afterTextChanged isRecord ="+isRecord );
                if (!wakeEd.getText().toString().isEmpty()){
                    btnWxVoice2.setVisibility(View.GONE);
                    ivVoice.setVisibility(View.GONE);
                    ivSend.setVisibility(View.VISIBLE);
                }else {
                    ivVoice.setVisibility(View.VISIBLE);
                    btnWxVoice2.setVisibility(View.GONE);
                    ivSend.setVisibility(View.GONE);
                }
                closeAsr();
            }
          return  false;
        });



        btnWxVoice2.setOnClickListener(new NoMultiClickListener() {
            @Override
            public void onNoMultiClick(View v) {
                isRecord = false;
                btnWxVoice2.setVisibility(View.GONE);
                ivVoice.setVisibility(View.VISIBLE);
                ivSend.setVisibility(View.GONE);
                closeAsr();
            }
        });

        ivVoice.setOnClickListener(new NoMultiClickListener() {
            @Override
            public void onNoMultiClick(View v) {
                isRecord = true;
                wakeEd.setHint("我在听，请说");
                ZInputMethod.hideKeyboard(context, ivVoice.getWindowToken());
                wakeEd.setText("");
                btnWxVoice2.setVisibility(View.VISIBLE);
                ivVoice.setVisibility(View.GONE);
                ivSend.setVisibility(View.GONE);
                toggleAsrRecognition();
            }
        });

        ivSend.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null){
                    callback.onSuccessMsg(wakeEd.getText().toString());
                    closeAsr();
                }

            }
        });


        wakeEd.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 文本改变前
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 文本改变中
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 文本改变后，s 是当前文本内容

                String input = s.toString();
                Timber.tag(TAG).e("afterTextChanged"+input+ "isRecord ="+isRecord );

                uilHandler.post(() -> {
                    if (isRecord  && !input.isEmpty()){
                        btnWxVoice2.setVisibility(View.GONE);
                        ivVoice.setVisibility(View.GONE);
                        if (isRecord){
                            btnWxVoice2.setVisibility(View.GONE);
                        }
                        ivSend.setVisibility(View.VISIBLE);
                    } else if (!isRecord && input.isEmpty()){
                        ivVoice.setVisibility(View.VISIBLE);
                        ivSend.setVisibility(View.GONE);
                        if (!"点击图标说话".contentEquals(wakeEd.getHint())) {
                            try {
                                wakeEd.post(() -> wakeEd.setHint("点击图标说话"));
                            } catch (Exception e) {
                                Timber.tag(TAG).e("afterTextChanged error=%s", e.getMessage() );
                                e.printStackTrace();
                            }
                        }
                    } else if (!isRecord && !TextUtils.isEmpty(input)){
                        ivVoice.setVisibility(View.GONE);
                        ivSend.setVisibility(View.VISIBLE);
                    }

                });

            }
        });

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
        if (btnWxVoice2 !=null){
            btnWxVoice2.stop();
        }
        Timber.tag(TAG).e("onDetachedFromWindow");
    }



    public void release() {
        closeAsr();
        if (!TTSManager.getInstance().isStop()) {
            TTSManager.getInstance().stop();
        }
        decibelHandler.removeCallbacksAndMessages(null);
        uilHandler.removeCallbacksAndMessages(null);
        callback = null;
    }


//    private void toggleAsrRecognition() {
//        Timber.tag(TAG).e("初始化收音");
//        closeAsr();
//        startAsr();
//    }

    private void toggleAsrRecognition() {
        if (asrManager == null) {
            Timber.tag(TAG).d("asrManager 初始化收音 = %s", asrManager);
            initAsrManger2();
        }
        asrManager.toggleRecognition();
    }

    public void closeAsr() {
        if (realtimeAsr != null) {
            realtimeAsr.release();
            realtimeAsr = null;
        }

        if (asrManager != null) {
            asrManager.onDestroy();
            asrManager = null;
        }
    }

    private void startAsr() {
        curAsrResult = "";
        if (realtimeAsr == null) {
            realtimeAsr = (ASRIntelligentDialogue) AIAssistantManager.Companion.getInstance().asrIntelligentDialogueHelp();
        }

        realtimeAsr.setListener(new ASRIntelligentDialogue.RealtimeAsrListener() {

            @Override
            public void onConnected() {
                Timber.tag(TAG).e("onConnected");
            }

            @Override
            public void onAsrMidResult(@NonNull AsrResult asrResult) {
                Timber.tag(TAG).e("onAsrMidResult = %s", asrResult.getText());
                if (!isRecord) {
                    return;
                }
                uilHandler.post(() -> {
                    if (wakeEd != null && !TextUtils.isEmpty(asrResult.getText())) {

                        wakeEd.setText(asrResult.getText());
                        wakeEd.setSelection(wakeEd.getText().length());

                    }
                });
            }

            @Override
            public void onAsrFinalResult(@NonNull AsrResult asrResult) {
                Timber.tag(TAG).e("onAsrFinalResult = %s", asrResult.getText());
                if (!isRecord) {
                    return;
                }
                uilHandler.post(() -> {
                    curAsrResult = asrResult.getText();
                    if (wakeEd != null && !TextUtils.isEmpty(asrResult.getText())) {

                        wakeEd.setText(asrResult.getText());
                        wakeEd.setSelection(wakeEd.getText().length());


                    }

                    if (!TextUtils.isEmpty(asrResult.getText())) {
                        if (callback != null) {
                            closeAsr();
                            callback.onSuccessMsg(asrResult.getText());
                        }
                    }
                });
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
        private final WeakReference<WakeVoice2> ref;
        public DecibelHandler(WakeVoice2 view) {
            super(Looper.getMainLooper());
            ref = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            WakeVoice2 view = ref.get();
            if (view == null ) return;
            sendEmptyMessageDelayed(0, 500);
        }
    }


    /**
     * 需要自动取消识别
     */
    private void initAsrManger2() {
        asrManager = new AsrManager();
        asrManager.setVadTime(2000);
        asrManager.callbackSite(false);
        asrManager.setResultListener(new AsrManager.AsrResultListener() {
            @Override
            public void onFinalResult(String text) {
                Timber.tag(TAG).e("onAsrFinalResult = %s", text);
                if (!isRecord) {
                    return;
                }
                uilHandler.post(() -> {
                    curAsrResult = text;
                    if (wakeEd != null && !TextUtils.isEmpty(text)) {

                        wakeEd.setText(text);
                        wakeEd.setSelection(wakeEd.getText().length());


                    }

                    if (!TextUtils.isEmpty(text)) {
                        if (callback != null) {
                            closeAsr();
                            callback.onSuccessMsg(text);
                        }
                    }
                });

            }

            @Override
            public void onPartialResult(String text) {
                Timber.d("onPartialResult partial: %s", text);
//                cancelAsr();

            }

            @Override
            public void onError(String errorMsg) {
                Timber.e("ASR error: %s", errorMsg);
                if (callback != null){
                    callback.onErrorMsg(errorMsg,curAsrResult,0);
//                    callback.onErrorMsg(errorMsg,curAsrResult,code);
                }
//                getActivity().runOnUiThread(() -> GlobalToast.show(requireActivity(), "未识别到文字", GlobalToast.Type.ERROR));
            }

            @Override
            public void onCloseError(String text) {
                Timber.e("ASR onCloseError: %s", text);
                if (callback != null){
                    callback.onErrorMsg(text,curAsrResult,0);
//                    callback.onErrorMsg(errorMsg,curAsrResult,code);
                }
            }
        });
    }


    public void cancelAsr() {
        if (asrManager == null) {
            Timber.tag(TAG).d("asrManager = %s", asrManager);
            return;
        }
        asrManager.cancelRecognition();
    }


}
