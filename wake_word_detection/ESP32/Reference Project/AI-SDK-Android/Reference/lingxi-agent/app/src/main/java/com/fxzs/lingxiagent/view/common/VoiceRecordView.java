package com.fxzs.lingxiagent.view.common;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.fxzs.lingxiagent.R;
import com.fxzs.smartassist.util.ZUtil.SizeUtils;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import timber.log.Timber;

/**
 * 创建者：ZyOng
 * 描述：
 * 创建时间：2025/7/31 下午2:56
 */

public class VoiceRecordView extends FrameLayout {
    private static final String TAG = "VoiceRecordView";

    public interface RecordListener {
        void onStartRecord();
        void onStopRecord();
        boolean onCancelRecord();
    }

    private TextView mStateTV;
    private ImageView ivBg;
    private int[] locationVoice = new int[2];
    private int bottomPadding = 0;
    private VoiceButton wxVoiceButton;
    private volatile MediaRecorder mRecorder;
    private File voiceFile;
    private RecordListener recordListener;
    private boolean runningObtainDecibelThread = true;
    private final Handler decibelHandler = new DecibelHandler(this);
    private ConstraintLayout flLayout;
    private View view;
    private boolean isRecording = false;

    public VoiceRecordView(Context context) {
        super(context);
        init(context);
    }

    public VoiceRecordView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VoiceRecordView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // 使用更可靠的文件路径
        voiceFile = new File(getContext().getExternalCacheDir(), "temp_record_" + System.currentTimeMillis() + ".3gp");
        // 确保目录存在
        File parentDir = voiceFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        if (bottomPadding == 0){
            bottomPadding = SizeUtils.dpToPx(8);
        }
        view = LayoutInflater.from(context).inflate(R.layout.dialog_record, this, true);
        mStateTV = view.findViewById(R.id.rc_audio_state_text);
        ivBg = view.findViewById(R.id.iv_bg);
        wxVoiceButton = view.findViewById(R.id.btn_wx_voice);

        mStateTV.setVisibility(View.VISIBLE);
        mStateTV.setText("松手发送,上移取消");
        mStateTV.setTextColor(ContextCompat.getColor(context, R.color.color_A0A0A0));
        ivBg.setImageResource(R.drawable.shape_voice_bg);

        flLayout = view.findViewById(R.id.fl_layout);
        flLayout.setPadding(0, 0, 0, bottomPadding);

        setBackgroundColor(Color.TRANSPARENT); // 背景透明
        setVisibility(GONE);
    }

    public void setRecordListener(RecordListener listener) {
        this.recordListener = listener;
    }

    public void setBottomPadding(int px) {
        this.bottomPadding = px;
        if (view == null){
            return;
        }
        if (flLayout == null && view != null){
            flLayout = view.findViewById(R.id.fl_layout);
        }
        if (flLayout != null){
            flLayout.setPadding(0, 0, 0, bottomPadding);
        }
    }

    public void show() {
        if (!isShown()) {
            setVisibility(VISIBLE);
            if (recordListener != null) {
                recordListener.onStartRecord();
            }
        }
    }

    public void dismiss() {
        if (isShown()) {
            setVisibility(GONE);
            if (recordListener != null) {
                recordListener.onStopRecord();
            }
        }
    }

    public void switchVoiceStatus(boolean send) {
        if (!send) {
            mStateTV.setText("松开取消");
            mStateTV.setTextColor(ContextCompat.getColor(getContext(), R.color.color_EE3636));
            ivBg.setImageResource(R.drawable.shape_voice_cancel_bg);
        } else {
            mStateTV.setText("松手发送,上移取消");
            mStateTV.setTextColor(ContextCompat.getColor(getContext(), R.color.color_A0A0A0));
            ivBg.setImageResource(R.drawable.shape_voice_bg);
        }
    }

    public boolean isShowing() {
        return getVisibility() == VISIBLE;
    }

    public void initRecord(){
        if (mRecorder != null) {
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
        }
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.setOutputFile(voiceFile.getAbsolutePath());
        } catch (Exception e) {
            Timber.tag(TAG).e( "Init record set output file failed: "+ e);
            
        }
    }

    /**
     * MediaRecorder 可用性检查
     */
    private CheckResult checkMediaRecorderAvailable() {
        CheckResult result = new CheckResult();

        // 1. 检查权限
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            result.isAvailable = false;
            result.errorMessage = "缺少 RECORD_AUDIO 权限";
            result.errorCode = "PERMISSION_DENIED";
            return result;
        }

        // 2. 检查音频系统
        try {
            AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
            if (audioManager == null) {
                result.isAvailable = false;
                result.errorMessage = "音频服务不可用";
                result.errorCode = "AUDIO_SERVICE_UNAVAILABLE";
                return result;
            }

            // 检查音频模式
            int audioMode = audioManager.getMode();
            if (audioMode == AudioManager.MODE_IN_CALL || audioMode == AudioManager.MODE_IN_COMMUNICATION) {
                result.isAvailable = false;
                result.errorMessage = "设备正在通话中，无法录音";
                result.errorCode = "IN_CALL_MODE";
                return result;
            }
        } catch (Exception e) {
            Timber.tag(TAG).e( "Check audio system failed: "+ e);
            result.isAvailable = false;
            result.errorMessage = "音频系统异常";
            result.errorCode = "AUDIO_SYSTEM_ERROR";
            return result;
        }

        // 3. 检查麦克风硬件
        PackageManager pm = getContext().getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
            result.isAvailable = false;
            result.errorMessage = "设备没有麦克风";
            result.errorCode = "NO_MICROPHONE";
            return result;
        }

        // 4. 检查存储
        if (!voiceFile.getParentFile().canWrite()) {
            result.isAvailable = false;
            result.errorMessage = "存储不可写";
            result.errorCode = "STORAGE_NOT_WRITABLE";
            return result;
        }

        result.isAvailable = true;
        result.errorMessage = "MediaRecorder 可用";
        return result;
    }

    /**
     * 执行录音操作 - 增强版本
     */
    public boolean startRecording() {
        // 先进行检查
        CheckResult checkResult = checkMediaRecorderAvailable();
        if (!checkResult.isAvailable) {
            toast("录音启动失败: " + checkResult.errorMessage);
            Timber.tag(TAG).e( "MediaRecorder check failed: " + checkResult.errorMessage);
            return false;
        }

        // 使用重试机制
        for (int retry = 0; retry < 2; retry++) {
            try {
                if (doStartRecording()) {
                    return true;
                }

                // 第一次失败后延迟重试
                if (retry == 0) {
                    Timber.tag(TAG).w( "First attempt failed, retrying after delay...");
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

            } catch (Exception e) {
                Timber.tag(TAG).w( "Recording attempt " + (retry + 1) + " failed: ", e);
            }
        }

        toast("录音启动失败，请重试");
        return false;
    }

    /**
     * 实际执行录音启动
     */
    private boolean doStartRecording() {
        try {
            // 彻底清理之前的实例
            if (mRecorder != null) {
                try {
                    if (isRecording) {
                        mRecorder.stop();
                    }
                } catch (Exception e) {
                    // 忽略停止异常
                    Timber.tag(TAG).d( "MediaRecorder stop..."+e);
                }
                mRecorder.release();
                mRecorder = null;
            }

            // 创建新的 MediaRecorder 实例
            mRecorder = new MediaRecorder();

            // 严格按照顺序配置
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOutputFile(voiceFile.getAbsolutePath());

            Timber.tag(TAG).d( "MediaRecorder configuring completed, preparing...");

            // 准备
            mRecorder.prepare();
            Timber.tag(TAG).d( "MediaRecorder prepared successfully");

//            // 短暂延迟后启动（避免状态机冲突）
//            try {
//                Thread.sleep(50);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }

            mRecorder.start();
            isRecording = true;
            Timber.tag(TAG).d( "MediaRecorder started successfully");

            runningObtainDecibelThread = true;
            decibelHandler.sendEmptyMessage(0);

            return true;

        } catch (Exception e) {
            Timber.tag(TAG).e( "doStartRecording failed: "+ e);
            handleStartFailure(e);
            return false;
        }
    }

    /**
     * 处理启动失败
     */
    private void handleStartFailure(Exception e) {
        // 释放资源
        if (mRecorder != null) {
            try {
                mRecorder.release();
            } catch (Exception ex) {
                Timber.tag(TAG).e( "Release MediaRecorder failed: "+ ex);
            }
            mRecorder = null;
        }
        isRecording = false;

        // 分析错误原因
        String errorMsg = analyzeError(e);
        Timber.tag(TAG).e( "Recording start failure: " + errorMsg);
    }

    /**
     * 分析错误原因
     */
    private String analyzeError(Exception e) {
        String errorMsg = e.getMessage() != null ? e.getMessage() : "未知错误";

        if (e instanceof IllegalStateException) {
            return "录音状态异常，请确保正确调用生命周期方法";
        } else if (e instanceof IOException) {
            return "文件操作失败，请检查存储权限和空间";
        } else if (e instanceof RuntimeException) {
            if (errorMsg.contains("start failed")) {
                return "麦克风被其他应用占用，请关闭其他录音应用";
            } else if (errorMsg.contains("Permission")) {
                return "录音权限未生效，请重新授权";
            }
        }

        return "录音设备异常: " + errorMsg;
    }

    /**
     * 快速检查 MediaRecorder 是否可用
     */
    public boolean isMediaRecorderAvailable() {
        return checkMediaRecorderAvailable().isAvailable;
    }

    /**
     * 获取详细的 MediaRecorder 状态信息
     */
    public String getMediaRecorderStatus() {
        CheckResult result = checkMediaRecorderAvailable();
        if (result.isAvailable) {
            return "录音功能正常";
        } else {
            return String.format("录音不可用: %s (错误码: %s)",
                    result.errorMessage, result.errorCode);
        }
    }

    /**
     * 取消录音对话框和停止录音
     */
    public void stopRecording() {
        runningObtainDecibelThread = false;
        decibelHandler.removeCallbacksAndMessages(null);
        switchVoiceStatus(true);
        Timber.tag(TAG).d( "MediaRecorder stopRecording");
        if (mRecorder != null) {
            try {
                if (isRecording) {
                    mRecorder.stop();
                    Timber.tag(TAG).d( "MediaRecorder stopped successfully");
                }
            } catch (RuntimeException e) {
                Timber.tag(TAG).e( "Stop MediaRecorder failed: "+ e);
            } finally {
                try {
                    mRecorder.reset();
                    mRecorder.release();
                } catch (Exception e) {
                    Timber.tag(TAG).e( "Release MediaRecorder failed: "+ e);
                }
                mRecorder = null;
            }
        }
        isRecording = false;
        this.dismiss();
        if (wxVoiceButton != null) {
            wxVoiceButton.removeAllMessage();
        }
    }

    /**
     * 检查结果内部类
     */
    private static class CheckResult {
        boolean isAvailable;
        String errorMessage;
        String errorCode;

        CheckResult() {
            this.isAvailable = false;
            this.errorMessage = "未检查";
            this.errorCode = "UNCHECKED";
        }
    }

    private static class DecibelHandler extends Handler {
        private final WeakReference<VoiceRecordView> ref;

        public DecibelHandler(VoiceRecordView view) {
            super(Looper.getMainLooper());
            ref = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            VoiceRecordView view = ref.get();
            if (view == null || !view.runningObtainDecibelThread) return;

            try {
                if (view.mRecorder != null && view.isRecording) {
                    int amp = view.mRecorder.getMaxAmplitude();
                    double db = 20 * Math.log10(amp + 1e-6);
                    view.wxVoiceButton.addVoiceSize((int) db);
                }
            } catch (IllegalStateException ignored) {
                // 忽略状态异常，可能正在停止
            }

            sendEmptyMessageDelayed(0, 500);
        }
    }

    private void toast(String content) {
        GlobalToast.show((AppCompatActivity)getContext(), content, GlobalToast.Type.ERROR);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopRecording();
        decibelHandler.removeCallbacksAndMessages(null);
    }

    /**
     * 获取录音状态
     */
    public boolean isRecording() {
        return isRecording;
    }

    /**
     * 获取录音文件
     */
    public File getVoiceFile() {
        return voiceFile;
    }
}