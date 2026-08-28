package com.fxzs.lingxiagent.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.AsrManager;
import com.fxzs.lingxiagent.model.chat.callback.DialogEditCallback;
import com.fxzs.lingxiagent.model.chat.callback.NexusVoiceCallback;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.util.ZUtil.DialogUtils;
import com.fxzs.lingxiagent.util.audio.TTSManager;
import com.fxzs.lingxiagent.view.common.AutoRecordView;
import com.fxzs.lingxiagent.view.common.GlobalToast;

import timber.log.Timber;

/**
 * 创建者：ZyOng
 * 描述：
 * 创建时间：2026/1/15 上午11:15
 */
public class NexusVoice extends ConstraintLayout {
    private final String TAG = "NexusVoice";
    private View ll_bottom_edit;
    private TextView tv_ed_fake;
    private LinearLayout ll_edit;
    private LinearLayout ll_bottom_voice;

    private EditText ed;
    ImageView iv_voice;
    ImageView iv_keyboard;
    ImageView iv_send;
    View ll_mode;
    TextView tv_mode;
    TextView tv_press;
    TextView tv_voice_hint;
    View rl_voice;
    View iv_logo;
    View iv_edit_open;
    private Context context;
    private String content = "";

    boolean isVoice = true;

    private int[] location = new int[2];
    private int viewLeft;
    private int viewRight;
    private int viewTop;
    private int viewBottom;
    private boolean isInArea = true;
    private long startTime = 0L;
    private final long MIN_DURATION_MS = 1000L;
    private AutoRecordView voiceRecordView;
    private final int PRESS_DOWN = 1;
    private final int PRESS_UP = 2;
    private final int PRESS_MOVE = 3;
    private AsrManager asrManager;
    private ViewTreeObserver.OnGlobalLayoutListener globalLayoutListener;
    private boolean isDestroyed = false;


    private NexusVoiceCallback callback;

    public NexusVoice(Context context) {
        this(context, null);
    }

    public NexusVoice(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NexusVoice(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        initView(context);
    }

    private void initView(Context context) {
        LayoutInflater.from(context).inflate(R.layout.voice_nexus, this, true);
        tv_ed_fake = findViewById(R.id.tv_ed_fake);
        ll_edit = findViewById(R.id.ll_edit);
        ll_bottom_edit = findViewById(R.id.ll_bottom_edit);
        ll_bottom_voice = findViewById(R.id.ll_bottom_voice);
        ed = findViewById(R.id.ed);
        iv_voice = findViewById(R.id.iv_voice);
        iv_keyboard = findViewById(R.id.iv_keyboard);
        iv_send = findViewById(R.id.iv_send);
        tv_mode = findViewById(R.id.tv_mode);
        tv_press = findViewById(R.id.tv_press);
        tv_voice_hint = findViewById(R.id.tv_voice_hint);
        rl_voice = findViewById(R.id.rl_voice);
        iv_logo = findViewById(R.id.iv_logo);
        iv_edit_open = findViewById(R.id.iv_edit_open);
        voiceRecordView = findViewById(R.id.voiceRecordView);
        ShadowUtils.applyDefaultShadow(ll_bottom_edit, context);
        initListener();
        setOnListenSoft();
        initAsrManger();
    }

    private void initListener() {
        tv_ed_fake.setOnClickListener(view -> {
            ed.setVisibility(View.VISIBLE);
            ZInputMethod.openInputMethod(ed);
        });

        iv_send.setOnClickListener(view -> {
            ZUtils.print("点击发送");
            sendCommon("");
        });


        iv_voice.setOnClickListener(view -> {
            switchMode(1,0);
            // 获取视图在屏幕中的位置

            ll_bottom_edit.getLocationOnScreen(location);
            viewLeft = location[0];
            viewTop = location[1];
            viewRight = viewLeft + ll_bottom_edit.getWidth();
            viewBottom = viewTop + ll_bottom_edit.getHeight();
        });

        ll_bottom_edit.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Timber.tag("TouchEvent").d("onLongClick ====== >");
                if (isVoice) {
                    return true;
                }
                return false;
            }
        });


        ll_bottom_edit.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (!isVoice) {
                    return false;
                }
                if (!TTSManager.getInstance().isStop()) {
                    TTSManager.getInstance().stop();
                }
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                != PackageManager.PERMISSION_GRANTED) {
                            // 获取当前Activity的context来请求权限
                            Activity activity = findActivityFromContext(context);
                            if (activity != null) {
                                AppPermissionRequestManager.requestAudioPermission(activity, 1, AppPermissionRequestManager.PERMISSION_AUDIO_MESSAGE_ASR);
                            } else {
                                Timber.e("Cannot find activity context for permission request");
                            }
                            return false;
                        }
                        isInArea = true;
                        boolean isNetworkAvailable = NetworkUtils.isNetworkAvailable(context);
                        if (!isNetworkAvailable) {
                            isInArea = false;
                            GlobalToast.show(context, "网络错误，请检查网络连接", GlobalToast.Type.ERROR,2000);
                            return false;
                        }

                        startTime = SystemClock.elapsedRealtime();
                        voiceStatusHandle(PRESS_DOWN, false, false);
                        break;

                    case MotionEvent.ACTION_UP:
                        // 手指松开
                        ll_bottom_voice.setVisibility(View.GONE);
                        ll_bottom_edit.setVisibility(View.VISIBLE);

                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            Timber.tag("SuperEditUtils").e("ACTION_UP == 当前有录音权限");
                            long duration = SystemClock.elapsedRealtime() - startTime;
                            if (duration < MIN_DURATION_MS) {
                                GlobalToast.show( context, context.getString(R.string.record_toast), GlobalToast.Type.ERROR,2000);
                            }
                        }
                        voiceStatusHandle(PRESS_UP, isInArea, false);
                        break;

                    case MotionEvent.ACTION_MOVE:
                        // 获取屏幕坐标
                        float rawX = motionEvent.getRawX();
                        float rawY = motionEvent.getRawY();

//                        // 获取视图在屏幕中的位置
                        ll_bottom_edit.getLocationOnScreen(location);
                        int viewLeft = location[0];
                        int viewTop = location[1];
                        int viewRight = viewLeft + ll_bottom_edit.getWidth();
                        int viewBottom = viewTop + ll_bottom_edit.getHeight();


                        // 判断是否在视图范围内
                        if (rawX < viewLeft || rawX > viewRight || rawY < viewTop || rawY > viewBottom) {
                            isInArea = false;
                        } else {
                            isInArea = true;
                        }

                        voiceStatusHandle(PRESS_MOVE, false, isInArea);
                        break;

                    case MotionEvent.ACTION_CANCEL:
                        // 触摸取消（例如被父视图拦截）
                        isInArea = false;
                        ll_bottom_voice.setVisibility(View.GONE);
                        ll_bottom_edit.setVisibility(View.VISIBLE);
                        break;
                }
                return false; // 返回 true 表示消费事件
            }
        });
        iv_keyboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchMode(0,0);
            }
        });
        iv_edit_open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO 点击弹窗
                DialogUtils.showInputDialog(context, ed.getEditableText().toString(), new DialogEditCallback() {
                    @Override
                    public void callback(String result) {
                        sendCommon(result);
                    }

                    @Override
                    public void onCancel(String result) {
                        DialogEditCallback.super.onCancel(result);
                        ed.setText(result);
                    }
                });
            }
        });
        ed.addTextChangedListener(new TextWatcher() {
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
                int lineCount = ed.getLineCount();
                // 比如，实时显示输入内容
                ZUtils.setTextColor(context, tv_ed_fake, input.length() > 0 ? R.color.text_black : R.color.text_hint_color);
                if (input.length() == 0) {
                    tv_ed_fake.setText("发消息...");
                } else {
                    tv_ed_fake.setText(input);
                }
                if (input.length() > 0) {
                    iv_voice.setVisibility(View.INVISIBLE);
                    iv_send.setVisibility(View.VISIBLE);
//                    setEditVisible();
                } else {
                    if (isVoice) {
                        iv_voice.setVisibility(View.GONE);
                    } else {
                        iv_voice.setVisibility(View.VISIBLE);
                    }

                    iv_send.setVisibility(View.GONE);
                }
                iv_edit_open.setVisibility(View.GONE);

            }
        });
        ed.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
//                    sendText("");
                    sendCommon("");
                    return true;
                }
                return false;
            }
        });

    }

    public void sendCommon(String result) {
        boolean isNetworkAvailable = NetworkUtils.isNetworkAvailable(context);
        if (!isNetworkAvailable) {
            GlobalToast.show((Activity) context, "网络错误，请检查网络连接", GlobalToast.Type.ERROR);
            return;
        }


        if (!TextUtils.isEmpty(result.trim())) {
            content = result;
        } else {
            content = ed.getText().toString();
            if (content.trim().isEmpty()) {
                ZUtils.showToast("请输入有效内容");
                return;
            }
        }

        if (content.length() > Constants.DIALOG_INPUT_NUMBER) {
            GlobalToast.show((Activity) context, context.getString(R.string.dialog_input_content_hint), GlobalToast.Type.ERROR);
            return;
        }

        ed.setText("");
        ZInputMethod.hideKeyboard(context, ll_bottom_edit.getWindowToken());
        //键盘收起期间如果请求网络数据会导致UI 500ms 的卡顿，先执行收起，延时100ms执行网络请求
//            root_view.postDelayed(() -> callback.send(content, selectOptionModel), 100);
        if (callback != null){
            callback.onSuccessMsg(content);
        }
    }


    public void switchMode(int mode,int type) {
        //mode:0-文字输入，1-语音模式
        if (mode == 0) {
            isVoice = false;
            iv_keyboard.setVisibility(View.GONE);
            tv_press.setVisibility(View.GONE);
            iv_logo.setVisibility(View.GONE);

            String content = ed.getText().toString();
            if (content.isEmpty()) {
                iv_voice.setVisibility(View.VISIBLE);
            } else {
                iv_voice.setVisibility(View.INVISIBLE);
            }
            tv_ed_fake.setVisibility(View.VISIBLE);
//            if (type == 0){
//                ZInputMethod.openInputMethod(ed);
//            }

        } else if (mode == 1) {

            isVoice = true;
            iv_keyboard.setVisibility(View.VISIBLE);
            tv_press.setVisibility(View.VISIBLE);
//            iv_logo.setVisibility(View.VISIBLE);
            iv_voice.setVisibility(View.INVISIBLE);
            tv_ed_fake.setVisibility(View.GONE);
            ed.setVisibility(View.GONE);
            ZInputMethod.hideKeyboard(context, ll_bottom_edit.getWindowToken());
        }
    }


    public void setOnListenSoft() {
        globalLayoutListener = () -> {
            Rect rect = new Rect();
            getWindowVisibleDisplayFrame(rect);
            int screenHeight = getRootView().getHeight();

            int keyHeight = screenHeight - rect.bottom;
            if (keyHeight < screenHeight * 0.15) {
                Timber.tag("NexusVoice").d("隐藏了.");
                if (isVoice) {
                    tv_ed_fake.setVisibility(View.GONE);
                } else {
                    tv_ed_fake.setVisibility(View.VISIBLE);
                }
                ll_edit.setVisibility(View.GONE);


            } else {
                Timber.tag("NexusVoice").d("弹出.");
                tv_ed_fake.setVisibility(View.GONE);
                ll_edit.setVisibility(View.VISIBLE);
            }
        };

        getViewTreeObserver().addOnGlobalLayoutListener(globalLayoutListener);

    }

    private void voiceStatusHandle(int type, boolean isInArea, boolean status) {
        if (type == PRESS_DOWN) {
            if (voiceRecordView != null && voiceRecordView.startRecording()) {
                voiceRecordView.show();
                voiceRecordView.switchVoiceStatus(true);
            }
            TTSManager.Companion.getInstance().stop();
//            if (vmChat.pressDownBusinessProcessFlow()) return;
            toggleAsrRecognition();
        } else if (type == PRESS_MOVE) {
            if (voiceRecordView != null) {
                voiceRecordView.switchVoiceStatus(status);
            }
        } else if (type == PRESS_UP) {
            if (voiceRecordView != null) {
                voiceRecordView.stopRecording();
            }
//            if (vmChat.pressUpBusinessProcessFlow()) {
//                ll_bottom.setVisibility(View.VISIBLE);
//                return;
//            }
            if (!isInArea) {
                closeAsr();
            } else {
                cancelAsr();
            }
        }
    }

    private void toggleAsrRecognition() {
        if (asrManager == null) {
            Timber.tag(TAG).d("asrManager = %s", asrManager);
            initAsrManger();
        }
        asrManager.toggleRecognition();
    }

    private void initAsrManger() {
        asrManager = new AsrManager();
        asrManager.setResultListener(new AsrManager.AsrResultListener() {
            @Override
            public void onFinalResult(String text) {
                Timber.tag(TAG).d("识别结果: %s", text);
                if (isDestroyed) return;
//                sendCommon(text);
                setVoiceInputContent(text);
            }

            @Override
            public void onPartialResult(String text) {
                Timber.tag(TAG).d("onPartialResult partial: %s", text);

            }

            @Override
            public void onError(String errorMsg) {
                Timber.tag(TAG).e("ASR error: %s", errorMsg);
                if ( callback != null && !isDestroyed){
                    callback.onErrorMsg("未识别到文字");
                }
//                getActivity().runOnUiThread(() -> GlobalToast.show(requireActivity(), "未识别到文字", GlobalToast.Type.ERROR));
            }

            @Override
            public void onCloseError(String text) {
                Timber.tag(TAG).e("ASR onCloseError: %s", text);
                if ( callback != null && !isDestroyed){//需要判断网络情况
                    callback.onErrorMsg(text);
                }
//                getActivity().runOnUiThread(() -> {
//                    boolean isNetworkAvailable = NetworkUtils.isNetworkAvailable(context);
//                    if (!isNetworkAvailable) {
//                        GlobalToast.show(context, "网络错误，请检查网络连接", GlobalToast.Type.ERROR);
//                    } else {
//                        GlobalToast.show(context, "未识别到文字", GlobalToast.Type.ERROR);
//                    }
//                });
            }
        });
    }

    public void closeAsr() {
        if (asrManager == null) {
            Timber.tag(TAG).d("asrManager = %s", asrManager);
            return;
        }
        asrManager.stopRecognition();

    }

    public void cancelAsr() {
        if (asrManager == null) {
            Timber.tag(TAG).d("asrManager = %s", asrManager);
            return;
        }
        asrManager.cancelRecognition();
    }

    public void setCallback(NexusVoiceCallback callback) {
        // 设置回调接口
        this.callback = callback;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        release();
    }

    private void removeListeners() {
        if (globalLayoutListener != null && getViewTreeObserver().isAlive()) {
            getViewTreeObserver().removeOnGlobalLayoutListener(globalLayoutListener);
            globalLayoutListener = null;
        }

        ll_bottom_edit.setOnTouchListener(null);
        ll_bottom_edit.setOnLongClickListener(null);

        iv_voice.setOnClickListener(null);
        iv_keyboard.setOnClickListener(null);
        iv_send.setOnClickListener(null);
        iv_edit_open.setOnClickListener(null);
        tv_ed_fake.setOnClickListener(null);
    }


    private void release() {
        isDestroyed = true;
        if (asrManager != null) {
            asrManager.stopRecognition();
            asrManager.onDestroy();
            asrManager = null;
        }

        if (!TTSManager.getInstance().isStop()) {
            TTSManager.getInstance().stop();
        }

        callback = null;
        removeListeners();
    }

    /**
     * 从Context中查找Activity实例
     */
    private Activity findActivityFromContext(Context context) {
        if (context instanceof Activity) {
            return (Activity) context;
        } else if (context instanceof android.view.ContextThemeWrapper) {
            Context baseContext = ((android.view.ContextThemeWrapper) context).getBaseContext();
            if (baseContext instanceof Activity) {
                return (Activity) baseContext;
            }
        }
        // 尝试通过Window获取Activity
        if (getContext() instanceof Activity) {
            return (Activity) getContext();
        }
        return null;
    }

    public void setVoiceInputContent(String content){
        if (ed != null){
            ed.setText(content);
            ed.setSelection(ed.getText().length());
        }
        switchMode(0,1);
    }


}
