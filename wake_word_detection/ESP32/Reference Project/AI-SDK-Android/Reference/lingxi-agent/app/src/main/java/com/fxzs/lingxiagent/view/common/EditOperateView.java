package com.fxzs.lingxiagent.view.common;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.MediaRecorder;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.fxzs.lingxiagent.R;
import com.fxzs.smartassist.util.ZUtil.SizeUtils;
import com.fxzs.smartassist.util.ZUtil.VibrateUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;


public class EditOperateView extends androidx.appcompat.widget.AppCompatEditText {

    private OnFinishedRecordListener finishedListener;

    private TextView mStateTV;
    private ImageView ivBg;

    private VoiceButton wxVoiceButton;

    private float downY;

    private boolean isVibrate = false;
    private boolean isInside;
    private int action;
    private float touchX;
    private float touchY;
    private int viewX;
    private int viewY;
    private int moveY = 0;

    private int viewHeight;
    private int viewWidth;
    private boolean isEditStatus = true;
    private boolean isLongBtn = false;
    private int bottomHeight = 0;
    private int voiceBtnStatus = 0; //1 正在录音 2、取消录音
    private View rootView;
    private boolean runningObtainDecibelThread = true;
    private final Handler handler = new Handler();
    private File voiceFile;


    public EditOperateView(Context context) {
        this(context, null);

    }

    public EditOperateView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);

    }

    public EditOperateView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }


    public void setOnFinishedRecordListener(OnFinishedRecordListener listener) {
        finishedListener = listener;
    }


    private BottomSheetDialog recordDialog;
    private GestureDetector gestureDetector;
    private int[] locationVoice = new int[2];


    @SuppressLint("HandlerLeak")
    private void init() {
        setFocusable(true);
        setFocusableInTouchMode(true);
        setClickable(true);

        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent event) {
                isEditStatus = false;
                isLongBtn = true;
                if (event.getAction() == MotionEvent.ACTION_DOWN) {

                    voiceBtnStatus = 1;
                    //按下的时候，重新生成一个语音保存的地址，避免一直读写一个文件，可以引起错误
                    downY = event.getY();
                    initDialogAndStartRecord();
//                    LogUtils.INSTANCE.d("录音长按");
                    if (!isVibrate) {
                        VibrateUtils.vibrate(getContext(), 50);
                    }
                }

            }

            @Override
            public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
//                LogUtils.INSTANCE.d("录音长按点击" + e.getAction());
                return false;
            }
        });

        post(() -> {
            viewWidth = getWidth();
            viewHeight = getHeight();
            monitorGlobal();
        });

        voiceFile =  new File(getContext().getCacheDir(), "temp.3gp");
        bottomHeight = SizeUtils.dpToPx(26);

    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isEditStatus) {
            if (!wasKeyboardOpen && TextUtils.isEmpty(getText().toString())) {
                gestureDetector.onTouchEvent(event);
            }
            return super.onTouchEvent(event);
        }

        action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
//                LogUtils.INSTANCE.d("按下按下" + isEditStatus);

                //按下的时候，重新生成一个语音保存的地址，避免一直读写一个文件，可以引起错误
                downY = event.getY();
//                setText("松开发送");
                initDialogAndStartRecord();
                if (!isVibrate) {
                    VibrateUtils.vibrate(getContext(), 50);
                    isVibrate = true;

                }
                break;
            case MotionEvent.ACTION_MOVE:
                moveY = (int) (event.getY() - downY);
                isInside = isTouchInsideView(event, locationVoice);

//                Log.d("测试测试","isInside = "+isInside);
                if (mStateTV != null && wxVoiceButton != null && moveY < 0 && moveY < -50 && !isInside && voiceBtnStatus == 1) {
                    mStateTV.setText("松开取消");
                    mStateTV.setTextColor(ContextCompat.getColor(getContext(), R.color.color_EE3636));
//                    LogUtils.INSTANCE.d("录音松开取消");
                    ivBg.setImageResource(R.drawable.shape_voice_cancel_bg);
                    if (!isVibrate) {
                        VibrateUtils.vibrate(getContext(), 50);
                        isVibrate = true;
                    }

                    voiceBtnStatus = 2;

//            wxVoiceButton.setCancel(true);
                } else if (mStateTV != null && wxVoiceButton != null && isInside && voiceBtnStatus == 2) {
                    mStateTV.setText("松手发送,上移取消");
                    mStateTV.setTextColor(ContextCompat.getColor(getContext(), R.color.color_A0A0A0));
//                    LogUtils.INSTANCE.d("录音松手发送,上移取消");
                    ivBg.setImageResource(R.drawable.shape_voice_bg);
                    isVibrate = false;
//            wxVoiceButton.setCancel(false);
                    voiceBtnStatus = 1;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
//                this.setText("按住录音");

                if (moveY < 0 && moveY < -50) {  //当手指向上滑，会cancel
//                    LogUtils.INSTANCE.d("按下关闭== 取消录音");
                    if (finishedListener != null) {
                        finishedListener.onCancelRecord();
                    }
                } else {
                    if (finishedListener != null) {
//                        LogUtils.INSTANCE.d("按下关闭== 完成录音");
                        finishedListener.onCompleteRecord();
                    }
                }
                isVibrate = false;
                voiceBtnStatus = 0;
                showEditLayout();
                stopRecording();
                break;
        }

        return true;
    }


    /**
     * 初始化录音对话框 并 开始录音
     */
    private void initDialogAndStartRecord() {
        recordDialog = new BottomSheetDialog(getContext(), R.style.like_toast_dialog_style);
//        recordDialog = new Dialog(getContext(), R.style.like_toast_dialog_style);
        // view = new ImageView(getContext());
        View view = View.inflate(getContext(), R.layout.dialog_record, null);
        wxVoiceButton = view.findViewById(R.id.btn_wx_voice);
        mStateTV = view.findViewById(R.id.rc_audio_state_text);
        ivBg = view.findViewById(R.id.iv_bg);
        mStateTV.setVisibility(View.VISIBLE);
        mStateTV.setText("松手发送,上移取消");
        mStateTV.setTextColor(ContextCompat.getColor(getContext(), R.color.color_A0A0A0));
        voiceBtnStatus = 1;
        ivBg.setImageResource(R.drawable.shape_voice_bg);
        ConstraintLayout frameLayout = view.findViewById(R.id.fl_layout);
        recordDialog.setContentView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        frameLayout.setPadding(0, 0, 0, bottomHeight);
        if (finishedListener != null) {
            finishedListener.onStartRecord();
        }
        if (startRecording()){
            recordDialog.show();
//            wxVoiceButton.addCheckVoice();
            mStateTV.post(() -> {
                getLocationOnScreen(locationVoice);

            });
        }else {
            toast("启动收音弹框失败");
        }

    }


    private void toast(String content) {
        GlobalToast.show((Activity)getContext(), content, GlobalToast.Type.ERROR);
    }


    public interface OnFinishedRecordListener {
        void onStartRecord();

        void onStopRecord();//录音结束

        void onCancelRecord();//取消录音

        void onCompleteRecord();//完成

        void onChangeSoftKeyboard(boolean open, int height);
    }


    public void setEditStatus() {
        this.isEditStatus = !isEditStatus;
        switchEditUi();
    }

    public void switchEditUi() {
//        LogUtils.INSTANCE.d("当前键盘状态" + isEditStatus);
        setLongClickable(isEditStatus);
        if (!isEditStatus) {
            setText("按住说话");
            setTypeface(Typeface.DEFAULT_BOLD);
            setGravity(Gravity.CENTER);
            setFocusable(false);
            setClickable(true);
        } else {
            setHintTextColor(getContext().getColor(R.color.color_999999));
            setHint(getContext().getResources().getString(R.string.text_input_hint));
            setText("");
            setTypeface(Typeface.DEFAULT);
            setGravity(Gravity.CENTER | Gravity.START);
            requestFocus();
            setCursorVisible(true);
            setFocusable(true);
            setFocusableInTouchMode(true);
            setClickable(false);
        }
    }

    public Boolean isEditStatus() {
        return isEditStatus;
    }


    private void showEditLayout() {
//        LogUtils.INSTANCE.d("isLongBtn" + isLongBtn + "  isEditStatus=" + isEditStatus);

        if (this.isLongBtn && !this.isEditStatus) {//编辑长按
            setFocusable(true);
            setFocusableInTouchMode(true);
            setClickable(true);
            clearFocus();
            setCursorVisible(false);
            this.isLongBtn = false;
            this.isEditStatus = true;
            return;
        }

        if (!this.isLongBtn && !this.isEditStatus) {//点击说话
            setFocusable(false);
            setFocusableInTouchMode(false);
            setClickable(true);
            clearFocus();
            setCursorVisible(false);
            return;
        }

        if (!this.isLongBtn && this.isEditStatus) {//编辑
            setFocusable(true);
            setFocusableInTouchMode(true);
            setClickable(true);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec); // 如果你不处理尺寸
    }

    private boolean wasKeyboardOpen = false;


    /**
     * 键盘弹出不能长按录音
     */
    public void monitorGlobal() {
        //键盘弹出
        rootView = getRootView().findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);

    }

    public void detach(){
        if (rootView == null){
            return;
        }
        rootView.getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);
    }


    private final ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {

        @Override
        public void onGlobalLayout() {
            Rect rect = new Rect();
            rootView.getWindowVisibleDisplayFrame(rect);
            int screenHeight = rootView.getRootView().getHeight();
            int heightDiff = screenHeight - rect.height();
            int keyHeight = (int) (screenHeight * 0.15);
            boolean isKeyboardOpen = heightDiff > keyHeight;

            if (isKeyboardOpen != wasKeyboardOpen) {
                wasKeyboardOpen = isKeyboardOpen;

                if (isKeyboardOpen) {
                    setFocusable(true);
                    setFocusableInTouchMode(true);
                    setCursorVisible(true);
//                    LogUtils.INSTANCE.d("键盘打开");
                    if (finishedListener != null) {
                        finishedListener.onChangeSoftKeyboard(true,heightDiff);
                    }

                } else {
//                    LogUtils.INSTANCE.d("键盘关闭");
                    clearFocus();
                    setCursorVisible(false);
                    finishedListener.onChangeSoftKeyboard(false,0);
                }
            }
        }
    };


    /**
     * 移动到取消为止
     *
     * @param event
     */
    public boolean isTouchInsideView(MotionEvent event, int[] location) {
        viewX = location[0];
        viewY = location[1];
        touchX = event.getRawX();  // 手指的屏幕 X 坐标
        touchY = event.getRawY();  // 手指的屏幕 Y 坐标
//        Log.d("测试测试","当前位置"+touchX  + "  View位置="+viewY  + "  viewHeight"+(viewY + viewHeight)  );
        return touchX >= viewX && touchX <= (viewX + viewWidth) && touchY >= viewY && touchY <= (viewY + viewHeight);
    }


    public void setBottomHeight(int bottomHeight) {
        this.bottomHeight = bottomHeight;
    }

    private volatile MediaRecorder mRecorder;

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {

            if (mRecorder == null || !runningObtainDecibelThread) {
//                Timber.d("分贝大小:获取失败");
                return;
            }

            try {
                int amplitude = mRecorder.getMaxAmplitude(); // 1~32767
                double db = 20 * Math.log10(amplitude + 1e-6); // 防止 log(0)
//                Timber.e("获取分贝大小==: %s",db);
                wxVoiceButton.addVoiceSize((int) db);
            } catch (IllegalStateException e) {
//                Timber.e("获取分贝失败: %s", e.getMessage());
            }
            handler.postDelayed(this, 500);
        }
    };




    /**
     * 执行录音操作
     */
    //int num = 0 ;
    private boolean startRecording() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            toast("录音启动失败[" +"缺少 RECORD_AUDIO 权限"+"]");
            return false;
        }
        if (mRecorder != null) {
            mRecorder.reset();
        } else {
            mRecorder = new MediaRecorder();
        }
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {

            mRecorder.setOutputFile(voiceFile.getAbsolutePath());
            mRecorder.prepare();
            mRecorder.start();
        } catch (Exception e) {
            e.printStackTrace();
            mRecorder.release();
            mRecorder = null;
            toast("录音启动失败[" + e.getMessage() + "]");
            return false;
        }
        runningObtainDecibelThread = true;
        handler.post(updateRunnable);
        return true;
    }

    /**
     * 取消录音对话框和停止录音
     */
    private void stopRecording() {
        runningObtainDecibelThread = false;
        handler.removeCallbacks(updateRunnable);

        if (mRecorder != null) {
            try {
                mRecorder.stop();//停止时没有prepare，就会报stop failed
                mRecorder.reset();
                mRecorder.release();
                mRecorder = null;
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        if (recordDialog != null) {
            recordDialog.dismiss();
            recordDialog = null;
        }
        if (wxVoiceButton != null) {
            wxVoiceButton.removeAllMessage();
            wxVoiceButton = null;
        }
    }

}
