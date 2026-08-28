package com.cmdc.ai.assist.aiModel;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import androidx.annotation.NonNull;
import com.cmdc.ai.assist.api.IRecordStream;
import java.io.IOException;
import java.io.InputStream;

import timber.log.Timber;

public class MicroRecordStreamModify extends InputStream implements IRecordStream {

    private final String TAG = MicroRecordStreamModify.class.getSimpleName();
    private static AudioRecord audioRecord;

    private static MicroRecordStreamModify is;

    private volatile boolean isStarted = false;

    private volatile boolean isClosed;

    private MicroRecordStreamModify() {

    }

    public static MicroRecordStreamModify getInstance() {
        if (is == null) {
            synchronized (MicroRecordStreamModify.class) {
                if (is == null) {
                    is = new MicroRecordStreamModify();
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
            // 将硬件 ring buffer 扩大到约 2 秒，避免弱网建联期间上层来不及读取而溢出，
            // 进而被底层覆盖最旧数据导致句首丢字。
            // 2 秒大小 = 采样率 16000 × 16bit(2字节) × 单声道 × 2 秒 = 64000 字节。
            // 同时与 AudioRecord.getMinBufferSize 返回值取较大者，确保满足最低要求。
            int recommendedBufferSize = Math.max(bufferSize, 64000);
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
     * 非阻塞读取录音数据。
     * <p>
     * 与 {@link #read(byte[], int, int)} 的区别：
     * <ul>
     *   <li>硬件 ring buffer 有数据则立刻返回已读取字节数；</li>
     *   <li>没有数据则立刻返回 0，不会阻塞等待麦克风采集；</li>
     *   <li>不负责惰性启动 AudioRecord，仅在已 start 且未 close 时才工作。</li>
     * </ul>
     * 主要用于上层在阻塞读一帧之后，连续追赶建联期间堆积的音频，
     * 避免使用 Thread.sleep 死板节拍导致积压永远追不上。
     *
     * @param b   存储读取数据的字节数组
     * @param off 写入起始偏移
     * @param len 期望读取的字节数
     * @return 实际读取的字节数；缓冲无数据或未就绪时返回 0；异常返回 0
     */
    @Override
    public int readNonBlocking(byte[] b, int off, int len) {
        if (!isStarted || isClosed || audioRecord == null) {
            return 0;
        }
        try {
            // AudioRecord.READ_NON_BLOCKING 自 API 23 起支持。
            int n = audioRecord.read(b, off, len, AudioRecord.READ_NON_BLOCKING);
            // 负数为错误码（如 ERROR_INVALID_OPERATION），统一规整为 0，上层按“缓冲已空”处理。
            return Math.max(n, 0);
        } catch (Exception e) {
            Timber.tag(TAG).e(e);
            return 0;
        }
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
