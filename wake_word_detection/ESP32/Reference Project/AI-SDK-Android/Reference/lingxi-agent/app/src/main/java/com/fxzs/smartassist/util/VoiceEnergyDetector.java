package com.fxzs.smartassist.util;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

public class VoiceEnergyDetector {
    private static final String TAG = "VoiceEnergyDetector";
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_DURATION_MS = 100; // 100ms 缓冲区
    private static final int SAMPLE_PER_MS = SAMPLE_RATE / 1000;
    private static final int BUFFER_SIZE_SAMPLES = SAMPLE_PER_MS * BUFFER_DURATION_MS;
    private static final int BUFFER_SIZE_BYTES = BUFFER_SIZE_SAMPLES * 2; // 16bit 单声道
    private AudioRecord audioRecord;
    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private int bufferSize;

    public interface OnVolumeChangedListener {
        void onVolumeChanged(double dB);
    }

    public VoiceEnergyDetector() {
        initAudioRecorder();
    }

    @SuppressLint("MissingPermission")
    private void initAudioRecorder() {
        // 计算有效缓冲区大小
        int minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
        );
        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Timber.tag(TAG).d("Invalid min buffer size");
            return;
        }
        bufferSize = Math.max(minBufferSize, BUFFER_SIZE_BYTES); // 取较大值保证可用性

        try {
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
            );
        } catch (IllegalArgumentException e) {
            Timber.tag(TAG).d("AudioRecord initialization"+e.toString());
        }
    }
    public void stopListening() {
        isRecording.set(false);
        try {
            if (audioRecord != null) {
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
                audioRecord.release();
                audioRecord = null;
            }
        } catch (Exception e) {
            Timber.tag(TAG).d("Stop recording failed"+e.toString());
        }
    }


}