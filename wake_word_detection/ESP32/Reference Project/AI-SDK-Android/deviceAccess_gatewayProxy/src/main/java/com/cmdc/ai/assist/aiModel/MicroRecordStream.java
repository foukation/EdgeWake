package com.cmdc.ai.assist.aiModel;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import androidx.annotation.NonNull;
import com.cmdc.ai.assist.api.IRecordStream;
import java.io.IOException;
import java.io.InputStream;

import timber.log.Timber;

public class MicroRecordStream extends InputStream implements IRecordStream {

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

    @Override
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
    public int read(@NonNull byte[] buffer) {
        try {
            return super.read(buffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
     * 非阻塞读取存根实现。
     * <p>
     * 非持久化场景下不需要"追积压"能力，直接返回 0 即可，等价于 Kotlin 接口
     * {@link IRecordStream#readNonBlocking(byte[], int, int)} 的默认实现。
     * <p>
     * 之所以需要在 Java 类里显式实现：Kotlin 接口的默认方法默认不会编译成 Java
     * default method，Java 类视其为抽象方法，必须 override 才能编译通过。
     *
     * @param b   存储读取数据的字节数组
     * @param off 写入起始偏移
     * @param len 期望读取的字节数
     * @return 始终返回 0，表示"无积压可追"
     */
    @Override
    public int readNonBlocking(byte[] b, int off, int len) {
        return 0;
    }

    /**
     * 关闭录音流。
     * close 可能被主动结束、取消和 WebSocket 关闭回调重复触发，因此必须保证重复调用安全。
     */
    @Override
    public synchronized void close() {
        if (audioRecord == null || isClosed) {
            return;
        }

        try {
            if (isStarted) {
                audioRecord.stop();
            }
        } catch (IllegalStateException e) {
            Timber.tag(TAG).e(e);
        } finally {
            audioRecord.release();
            isStarted = false;
            isClosed = true;
            audioRecord = null;
            Timber.tag(TAG).d(" MyMicrophoneInputStream close");
        }
    }
}
