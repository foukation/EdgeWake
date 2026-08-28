package com.fxzs.lingxiagent.view.common;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

import timber.log.Timber;

/**
 * 创建者：ZyOng
 * 描述：
 * 创建时间：2025/7/31 下午2:56
 */

public class AutoRecordView extends FrameLayout {
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
    private AudioRecord audioRecord;

    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private File outputFile;

    private Thread recordThread;

    public AutoRecordView(Context context) {
        super(context);
        init(context);
    }

    public AutoRecordView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AutoRecordView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {

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
//        if (isShown()) {
//        }

        setVisibility(GONE);
        if (recordListener != null) {
            recordListener.onStopRecord();
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

//        // 先进行检查
//        CheckResult checkResult = checkMediaRecorderAvailable();
//        if (!checkResult.isAvailable) {
//            toast("录音启动失败: " + checkResult.errorMessage);
//            Timber.tag(TAG).e( "MediaRecorder check failed: " + checkResult.errorMessage);
//            return false;
//        }

        if (isRecording) {
            Timber.tag(TAG).i( "Already recording");
            return false;
        }

//        AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
//        int result = am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
//
//
//        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
//        if (bufferSize == AudioRecord.ERROR_BAD_VALUE || bufferSize == AudioRecord.ERROR) {
//            Timber.tag(TAG).i( "Invalid buffer size");
//            return false;
//        }
//
//        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
//            return false;
//        }
//        audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,
//                SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize);

//        outputFile = createOutputFile("voice");
        isRecording = true;
//        recordThread = new Thread(() -> recordAudioToFile(bufferSize));
//        recordThread.start();
//        startRecordingSafe();
        runningObtainDecibelThread = true;
        decibelHandler.sendEmptyMessage(0);
        return true;
    }


    private boolean startRecordingSafe() {
        int retry = 0;
        while (retry < 3) {
            try {
                audioRecord.startRecording();
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    return true;
                }
            } catch (Exception e) {
                Timber.tag(TAG).e("AudioRecord start failed: %s", e.getMessage());
            }

            retry++;
            Timber.tag(TAG).i("Retry startRecording... (" + retry + ")");
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }
        return false;
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
        if (!isRecording) return;
        isRecording = false;
        runningObtainDecibelThread = false;
        decibelHandler.removeCallbacksAndMessages(null);
        try {
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            }
        } catch (Exception e) {
            Timber.tag(TAG).e( "Error stopping AudioRecord%s", e);
        }
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
        private final WeakReference<AutoRecordView> ref;

        public DecibelHandler(AutoRecordView view) {
            super(Looper.getMainLooper());
            ref = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            AutoRecordView view = ref.get();
            if (view == null || !view.runningObtainDecibelThread) return;

            view.wxVoiceButton.addVoiceSize(80);
//            try {
//                if (view.mRecorder != null && view.isRecording) {
//                    int amp = view.mRecorder.getMaxAmplitude();
//                    double db = 20 * Math.log10(amp + 1e-6);
//                    Timber.tag(TAG).i("分贝大小%s", db);
////                    view.wxVoiceButton.addVoiceSize((int) db);
//                }
//            } catch (IllegalStateException ignored) {
//                // 忽略状态异常，可能正在停止
//            }

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

    /**
     * 创建文件
     */
    private File createOutputFile(String fileName) {
        File dir = new File(getContext().getExternalFilesDir(null), "MeetingRecordings");
        if (!dir.exists()) dir.mkdirs();

        if (!fileName.endsWith(".pcm")) fileName += ".pcm";
        return new File(dir, fileName);
    }

    /**
     * 核心录音循环 + 实时音量计算
     */
    private void recordAudioToFile(int bufferSize) {
        byte[] buffer = new byte[bufferSize];

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputFile))) {
            audioRecord.startRecording();

            while (isRecording) {
                int read = audioRecord.read(buffer, 0, buffer.length);
                if (read > 0) {
                    bos.write(buffer, 0, read);

                    // 实时音量计算
                    double amplitude = calculateAmplitude(buffer, read);
                    double db = 20 * Math.log10(amplitude / 32767.0 + 1e-6);
                    Timber.tag(TAG).i("分贝大小%s", db);
                    // 主线程回调
                    wxVoiceButton.addVoiceSize((int) db);
                }
            }
        } catch (IOException e) {
            Timber.tag(TAG).e( "Error writing audio file%s", e);
        }
    }

    /**
     * 计算PCM数据的均方根振幅（RMS）
     */
    private double calculateAmplitude(byte[] buffer, int read) {
        long sum = 0;
        for (int i = 0; i < read; i += 2) {
            // 16bit PCM, little endian
            short sample = (short) ((buffer[i] & 0xFF) | (buffer[i + 1] << 8));
            sum += sample * sample;
        }
        double mean = sum / (read / 2.0);
        return Math.sqrt(mean);
    }
}