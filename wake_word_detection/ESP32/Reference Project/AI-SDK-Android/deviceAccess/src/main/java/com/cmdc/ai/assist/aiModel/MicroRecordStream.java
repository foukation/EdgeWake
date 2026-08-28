package com.cmdc.ai.assist.aiModel;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.io.InputStream;

import timber.log.Timber;

public class MicroRecordStream extends InputStream {

    private final String TAG = MicroRecordStream.class.getSimpleName();
    private static AudioRecord audioRecord;

    private static MicroRecordStream is;

    private volatile boolean isStarted = false;

    private volatile boolean isClosed;

    private MicroRecordStream() {

    }

    public static MicroRecordStream getInstance() {
        if (is == null) {
            synchronized (MicroRecordStream.class) {
                if (is == null) {
                    is = new MicroRecordStream();
                }
            }
        }
        is.isClosed = false;
        return is;
    }

    private void start() {
        Timber.tag(TAG).d(" MyMicrophoneInputStream start recoding!");
        if (audioRecord == null) {
            int bufferSize = AudioRecord.getMinBufferSize(16000,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            // 改进：使用2-3倍最小缓冲区大小，平衡延迟和稳定性
            int recommendedBufferSize = bufferSize * 6;  // 约0.6秒的音频数据
            // 16000 采样率 16bits 单声道
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    recommendedBufferSize);
        }
        if (audioRecord == null
                || audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            throw new IllegalStateException(
                    "startRecording() called on an uninitialized AudioRecord." + (audioRecord == null));
        }
        audioRecord.startRecording();
        isStarted = true;

        Timber.tag(TAG).d(" MyMicrophoneInputStream start recoding finished");
    }

    public void startRecording() {
        if (!isStarted && !isClosed) {
            Timber.tag(TAG).d("startRecording");
            start(); // 建议在CALLBACK_EVENT_ASR_READY事件中调用。
            isStarted = true;
        }
    }

    @Override
    public int read() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int read(byte[] b, int off, int len) {
        if (!isStarted && !isClosed) {
            Timber.tag(TAG).d("read");
            start(); // 建议在CALLBACK_EVENT_ASR_READY事件中调用。
            isStarted = true;
            return -2;
        }
        if (audioRecord == null) {
            return -1;
        }
        try {
            return audioRecord.read(b, off, len);
        } catch (Exception e) {
            Timber.tag(TAG).e(e);
            throw e;
        }
    }

    /**
     * 关闭录音流
     */
    @Override
    public void close() {
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release(); // 程序结束别忘记自行释放
            isStarted = false;
            isClosed = true;
            audioRecord = null;
            Timber.tag(TAG).d(" MyMicrophoneInputStream close");
        }
    }
}
