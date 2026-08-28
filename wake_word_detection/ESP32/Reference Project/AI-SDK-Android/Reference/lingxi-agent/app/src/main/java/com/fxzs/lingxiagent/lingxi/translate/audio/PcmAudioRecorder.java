package com.fxzs.lingxiagent.lingxi.translate.audio;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

/**
 * PCM音频录制器
 * 按照ASR接口要求录制音频：16000Hz, 16bits, 单声道, PCM格式
 */
public class PcmAudioRecorder {

    private static final String TAG = "PcmAudioRecorder";

    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private static final int PACKET_DURATION_MS = 40;
    private static final int PACKET_SIZE_BYTES = SAMPLE_RATE * 2 * PACKET_DURATION_MS / 1000;

    private static final int LEVEL_LOG_INTERVAL_PACKETS = 1000 / PACKET_DURATION_MS;
    private static final int LOW_LEVEL_THRESHOLD = 100;

    /** 高于此电平才跳过增益，避免 Pad 短段偏亮被误判为“强麦” */
    private static final float GAIN_SKIP_THRESHOLD = 2500f;
    /** 目标平均电平，接近脚本推流参考 ~5000 */
    private static final float GAIN_TARGET_LEVEL = 4000f;
    private static final float MAX_SOFTWARE_GAIN = 8f;
    /** 动态压缩：先压高峰再软限幅，减轻 Simba→Sibon 类失真 */
    private static final float COMPRESS_THRESHOLD = 8000f;
    private static final float COMPRESS_RATIO = 3f;
    private static final float SOFT_LIMIT_START = 24000f;
    private static final float SOFT_LIMIT_RATIO = 0.35f;
    /** 高通 ~80Hz，去房间低频 rumble */
    private static final float HIGH_PASS_ALPHA = 0.98f;

    private static final int CALIBRATION_PACKETS = 50;
    private static final int EARLY_CALIBRATION_PACKETS = 10;
    private static final float EARLY_LOCK_AVG = 400f;
    private static final int MAX_PRELOCK_PACKETS = CALIBRATION_PACKETS;

    private Context context;
    private AudioRecord audioRecord;
    private int activeAudioSource = MediaRecorder.AudioSource.MIC;
    private AutomaticGainControl automaticGainControl;
    private NoiseSuppressor noiseSuppressor;
    private AcousticEchoCanceler acousticEchoCanceler;
    private Thread recordingThread;
    private AtomicBoolean isRecording = new AtomicBoolean(false);
    private AtomicBoolean isPaused = new AtomicBoolean(false);

    private float sessionGain = 1f;
    private float calibrationSum;
    private int calibrationCount;
    private boolean sessionGainLocked;
    private boolean gainBoostLogged;
    private final List<byte[]> preLockBuffer = new ArrayList<>();

    private float highPassPrevIn;
    private float highPassPrevOut;

    private AudioDataListener audioDataListener;

    public interface AudioDataListener {
        void onAudioData(byte[] audioData);
        void onRecordingStarted();
        void onRecordingStopped();
        void onRecordingError(String error);
    }

    public PcmAudioRecorder(Context context, AudioDataListener listener) {
        this.context = context;
        this.audioDataListener = listener;
        initAudioRecord();
    }

    private void initAudioRecord() {
        if (context != null) {
            boolean hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED;
            if (!hasPermission) {
                Timber.tag(TAG).e("RECORD_AUDIO permission not granted");
                if (audioDataListener != null) {
                    audioDataListener.onRecordingError("No permission to record audio");
                }
                return;
            }
        }

        try {
            int bufferSizeInBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            bufferSizeInBytes = Math.max(bufferSizeInBytes * 4, PACKET_SIZE_BYTES * 4);

            releaseAudioEffects();
            if (audioRecord != null) {
                audioRecord.release();
                audioRecord = null;
            }

            int[] sources = {
                    MediaRecorder.AudioSource.CAMCORDER,
                    MediaRecorder.AudioSource.MIC
            };
            for (int source : sources) {
                audioRecord = createAudioRecord(source, bufferSizeInBytes);
                if (audioRecord != null && audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                    activeAudioSource = source;
                    applyPreferredInputDevice(audioRecord);
                    configureAudioPostProcessing();
                    Timber.tag(TAG).d("AudioRecord initialized. source=%s, buffer=%d",
                            audioSourceName(source), bufferSizeInBytes);
                    return;
                }
                releaseAudioRecordOnly();
            }

            Timber.tag(TAG).e("AudioRecord initialization failed for all sources");
            if (audioDataListener != null) {
                audioDataListener.onRecordingError("Failed to initialize audio recorder");
            }
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "Error initializing AudioRecord");
            if (audioDataListener != null) {
                audioDataListener.onRecordingError("Audio recorder initialization error: " + e.getMessage());
            }
        }
    }

    private AudioRecord createAudioRecord(int audioSource, int bufferSizeInBytes) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioFormat format = new AudioFormat.Builder()
                        .setEncoding(AUDIO_FORMAT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_CONFIG)
                        .build();
                return new AudioRecord.Builder()
                        .setAudioSource(audioSource)
                        .setAudioFormat(format)
                        .setBufferSizeInBytes(bufferSizeInBytes)
                        .build();
            }
            return new AudioRecord(
                    audioSource,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSizeInBytes
            );
        } catch (Exception e) {
            Timber.tag(TAG).w(e, "Failed to create AudioRecord source=%s", audioSourceName(audioSource));
            return null;
        }
    }

    private void applyPreferredInputDevice(AudioRecord record) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || context == null || record == null) {
            return;
        }
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) {
            return;
        }
        AudioDeviceInfo preferred = null;
        for (AudioDeviceInfo device : audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)) {
            int type = device.getType();
            if (type == AudioDeviceInfo.TYPE_BUILTIN_MIC
                    || type == AudioDeviceInfo.TYPE_USB_DEVICE
                    || type == AudioDeviceInfo.TYPE_WIRED_HEADSET) {
                preferred = device;
                if (type == AudioDeviceInfo.TYPE_BUILTIN_MIC) {
                    break;
                }
            }
        }
        if (preferred != null && record.setPreferredDevice(preferred)) {
            Timber.tag(TAG).d("Preferred input device: type=%d id=%d", preferred.getType(), preferred.getId());
        }
    }

    private static String audioSourceName(int source) {
        if (source == MediaRecorder.AudioSource.CAMCORDER) {
            return "CAMCORDER";
        }
        return "MIC";
    }

    /**
     * 聆听模式：保留外放/环境声 — NS/AEC 关；弱麦 Pad 开硬件 AGC 平滑拉升。
     */
    private void configureAudioPostProcessing() {
        if (audioRecord == null) {
            return;
        }
        int sessionId = audioRecord.getAudioSessionId();
        boolean enableAgc = shouldEnableHardwareAgc();
        automaticGainControl = configureEffect(
                AutomaticGainControl.isAvailable()
                        ? AutomaticGainControl.create(sessionId)
                        : null,
                "AGC",
                enableAgc);
        noiseSuppressor = configureEffect(
                NoiseSuppressor.isAvailable()
                        ? NoiseSuppressor.create(sessionId)
                        : null,
                "NoiseSuppressor",
                false);
        acousticEchoCanceler = configureEffect(
                AcousticEchoCanceler.isAvailable()
                        ? AcousticEchoCanceler.create(sessionId)
                        : null,
                "AEC",
                false);
    }

    private boolean shouldEnableHardwareAgc() {
        if (context == null) {
            return true;
        }
        Configuration config = context.getResources().getConfiguration();
        int screenLayout = config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        return screenLayout >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    private <T extends android.media.audiofx.AudioEffect> T configureEffect(
            T effect, String name, boolean enabled) {
        if (effect == null) {
            Timber.tag(TAG).d("%s not available on this device", name);
            return null;
        }
        try {
            effect.setEnabled(enabled);
            Timber.tag(TAG).d("%s %s", name, enabled ? "enabled" : "disabled");
        } catch (Exception e) {
            Timber.tag(TAG).w(e, "Failed to configure %s", name);
            effect.release();
            return null;
        }
        return effect;
    }

    private void releaseAudioEffects() {
        releaseEffect(automaticGainControl, "AGC");
        automaticGainControl = null;
        releaseEffect(noiseSuppressor, "NoiseSuppressor");
        noiseSuppressor = null;
        releaseEffect(acousticEchoCanceler, "AEC");
        acousticEchoCanceler = null;
    }

    private void releaseEffect(android.media.audiofx.AudioEffect effect, String name) {
        if (effect == null) {
            return;
        }
        try {
            effect.release();
        } catch (Exception e) {
            Timber.tag(TAG).w(e, "Failed to release %s", name);
        }
    }

    private void releaseAudioRecordOnly() {
        if (audioRecord != null) {
            try {
                audioRecord.release();
            } catch (Exception e) {
                Timber.tag(TAG).w(e, "Failed to release AudioRecord");
            }
            audioRecord = null;
        }
    }

    public boolean startRecording() {
        if (audioRecord == null) {
            initAudioRecord();
        }
        if (audioRecord == null || audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Timber.tag(TAG).e("AudioRecord not initialized");
            return false;
        }

        if (isRecording.get()) {
            Timber.tag(TAG).w("Recording already in progress");
            return true;
        }

        try {
            resetGainState();
            audioRecord.startRecording();
            isRecording.set(true);
            isPaused.set(false);

            recordingThread = new Thread(this::recordingLoop, "AudioRecorderThread");
            recordingThread.start();

            if (audioDataListener != null) {
                audioDataListener.onRecordingStarted();
            }

            Timber.tag(TAG).d("Recording started");
            return true;
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "Error starting recording");
            if (audioDataListener != null) {
                audioDataListener.onRecordingError("Failed to start recording: " + e.getMessage());
            }
            return false;
        }
    }

    public void pauseRecording() {
        isPaused.set(true);
        Timber.tag(TAG).d("Recording paused");
    }

    public void resumeRecording() {
        isPaused.set(false);
        Timber.tag(TAG).d("Recording resumed");
    }

    public void stopRecording() {
        if (!isRecording.get()) {
            return;
        }

        isRecording.set(false);

        try {
            if (recordingThread != null) {
                recordingThread.join(1000);
            }

            if (audioRecord != null && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord.stop();
            }

            if (audioDataListener != null) {
                audioDataListener.onRecordingStopped();
            }

            Timber.tag(TAG).d("Recording stopped");
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "Error stopping recording");
        }
    }

    private void recordingLoop() {
        byte[] buffer = new byte[PACKET_SIZE_BYTES];
        int packetCount = 0;

        while (isRecording.get()) {
            long cycleStart = System.currentTimeMillis();
            try {
                if (isPaused.get()) {
                    byte[] silenceBuffer = new byte[PACKET_SIZE_BYTES];
                    deliverPacket(silenceBuffer);
                    Thread.sleep(PACKET_DURATION_MS);
                    continue;
                }

                int bytesRead = audioRecord.read(buffer, 0, buffer.length);

                if (bytesRead > 0) {
                    List<byte[]> rawPackets = buildPackets(buffer, bytesRead);
                    for (byte[] rawPacket : rawPackets) {
                        deliverCalibratedPacket(rawPacket);

                        packetCount++;
                        if (packetCount % LEVEL_LOG_INTERVAL_PACKETS == 0) {
                            logAudioLevel(rawPacket, packetCount);
                        }
                    }
                } else if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                    Timber.tag(TAG).e("AudioRecord ERROR_INVALID_OPERATION");
                    break;
                } else if (bytesRead == AudioRecord.ERROR_BAD_VALUE) {
                    Timber.tag(TAG).e("AudioRecord ERROR_BAD_VALUE");
                    break;
                }

                long elapsed = System.currentTimeMillis() - cycleStart;
                long remaining = PACKET_DURATION_MS - elapsed;
                if (remaining > 0) {
                    Thread.sleep(remaining);
                }
            } catch (InterruptedException e) {
                Timber.tag(TAG).d("Recording thread interrupted");
                break;
            } catch (Exception e) {
                Timber.tag(TAG).e(e, "Error in recording loop");
                if (audioDataListener != null) {
                    audioDataListener.onRecordingError("Recording error: " + e.getMessage());
                }
                break;
            }
        }

        Timber.tag(TAG).d("Recording loop ended");
    }

    private List<byte[]> buildPackets(byte[] buffer, int bytesRead) {
        List<byte[]> packets = new ArrayList<>();
        if (bytesRead <= 0) {
            return packets;
        }
        int offset = 0;
        while (offset < bytesRead) {
            int remaining = bytesRead - offset;
            byte[] packet = new byte[PACKET_SIZE_BYTES];
            int copyLen = Math.min(remaining, PACKET_SIZE_BYTES);
            System.arraycopy(buffer, offset, packet, 0, copyLen);
            packets.add(packet);
            offset += copyLen;
        }
        return packets;
    }

    private void resetGainState() {
        sessionGain = 1f;
        calibrationSum = 0f;
        calibrationCount = 0;
        sessionGainLocked = false;
        gainBoostLogged = false;
        preLockBuffer.clear();
        highPassPrevIn = 0f;
        highPassPrevOut = 0f;
    }

    private void deliverCalibratedPacket(byte[] rawPacket) {
        if (rawPacket == null) {
            return;
        }

        if (!sessionGainLocked) {
            preLockBuffer.add(rawPacket);
            lockSessionGainIfReady(computePacketAvgLevel(rawPacket));
            if (!sessionGainLocked) {
                if (preLockBuffer.size() >= MAX_PRELOCK_PACKETS) {
                    lockSessionGainWithFallback();
                } else {
                    return;
                }
            }
            flushPreLockBuffer();
            return;
        }

        deliverPacket(processPacket(rawPacket));
    }

    private void flushPreLockBuffer() {
        for (byte[] pending : preLockBuffer) {
            deliverPacket(processPacket(pending));
        }
        preLockBuffer.clear();
    }

    private void deliverPacket(byte[] packet) {
        if (packet == null || audioDataListener == null) {
            return;
        }
        audioDataListener.onAudioData(packet);
    }

    private byte[] processPacket(byte[] packet) {
        if (packet == null) {
            return null;
        }
        byte[] out = packet.clone();
        for (int i = 0; i < out.length - 1; i += 2) {
            short sample = (short) ((out[i + 1] << 8) | (out[i] & 0xFF));
            short processed = processSample(sample);
            out[i] = (byte) (processed & 0xFF);
            out[i + 1] = (byte) ((processed >> 8) & 0xFF);
        }
        return out;
    }

    private short processSample(short sample) {
        short filtered = applyHighPass(sample);
        if (sessionGain <= 1.01f) {
            return filtered;
        }
        return amplifySample(filtered, sessionGain);
    }

    private short applyHighPass(short sample) {
        float x = sample;
        float y = HIGH_PASS_ALPHA * (highPassPrevOut + x - highPassPrevIn);
        highPassPrevIn = x;
        highPassPrevOut = y;
        int result = Math.round(y);
        return (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, result));
    }

    private void lockSessionGainWithFallback() {
        if (sessionGainLocked) {
            return;
        }
        float calibAvg = calibrationCount > 0 ? calibrationSum / calibrationCount : 0f;
        sessionGain = computeSessionGain(calibAvg);
        sessionGainLocked = true;
        Timber.tag(TAG).w("Session gain fallback lock: gain=%.2f calibAvg=%.0f packets=%d",
                sessionGain, calibAvg, calibrationCount);
    }

    private float computeSessionGain(float calibAvg) {
        if (calibAvg >= GAIN_SKIP_THRESHOLD) {
            return 1f;
        }
        float target = shouldEnableHardwareAgc() ? GAIN_TARGET_LEVEL * 0.85f : GAIN_TARGET_LEVEL;
        float maxGain = shouldEnableHardwareAgc() ? MAX_SOFTWARE_GAIN * 0.75f : MAX_SOFTWARE_GAIN;
        return Math.min(maxGain, target / Math.max(calibAvg, 30f));
    }

    private short amplifySample(short sample, float gain) {
        float amplified = sample * gain;
        float abs = Math.abs(amplified);
        if (abs > COMPRESS_THRESHOLD) {
            amplified = Math.copySign(
                    COMPRESS_THRESHOLD + (abs - COMPRESS_THRESHOLD) / COMPRESS_RATIO,
                    amplified);
            abs = Math.abs(amplified);
        }
        if (abs > SOFT_LIMIT_START) {
            amplified = Math.copySign(
                    SOFT_LIMIT_START + (abs - SOFT_LIMIT_START) * SOFT_LIMIT_RATIO,
                    amplified);
        }
        int result = Math.round(amplified);
        return (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, result));
    }

    private void lockSessionGainIfReady(int packetAvg) {
        if (sessionGainLocked) {
            return;
        }
        calibrationSum += packetAvg;
        calibrationCount++;

        float calibAvg = calibrationSum / calibrationCount;
        boolean fullCalibration = calibrationCount >= CALIBRATION_PACKETS;
        boolean earlyWeakMic = calibrationCount >= EARLY_CALIBRATION_PACKETS && calibAvg < EARLY_LOCK_AVG;
        if (!fullCalibration && !earlyWeakMic) {
            return;
        }

        sessionGain = computeSessionGain(calibAvg);
        sessionGainLocked = true;
        Timber.tag(TAG).d("Session gain locked: gain=%.2f calibAvg=%.0f packets=%d source=%s agc=%s",
                sessionGain, calibAvg, calibrationCount, audioSourceName(activeAudioSource),
                automaticGainControl != null);
        if (sessionGain > 1.05f) {
            gainBoostLogged = true;
        }
    }

    private int computePacketAvgLevel(byte[] audioData) {
        long sum = 0;
        int sampleCount = audioData.length / 2;
        for (int i = 0; i < audioData.length - 1; i += 2) {
            short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
            sum += Math.abs(sample);
        }
        return sampleCount > 0 ? (int) (sum / sampleCount) : 0;
    }

    private void logAudioLevel(byte[] rawPacket, int packetCount) {
        int rawLevel = computePacketAvgLevel(rawPacket);
        if (rawLevel < LOW_LEVEL_THRESHOLD) {
            Timber.tag(TAG).w("Audio level low: raw=%d gain=%.2f locked=%s (packet #%d)",
                    rawLevel, sessionGain, sessionGainLocked, packetCount);
        } else {
            Timber.tag(TAG).d("Audio level ok: raw=%d gain=%.2f locked=%s (packet #%d)",
                    rawLevel, sessionGain, sessionGainLocked, packetCount);
        }
    }

    public boolean isRecording() {
        return isRecording.get();
    }

    public boolean isPaused() {
        return isPaused.get();
    }

    public AudioConfig getAudioConfig() {
        return new AudioConfig(SAMPLE_RATE, 16, 1, PACKET_SIZE_BYTES, PACKET_DURATION_MS);
    }

    public void release() {
        stopRecording();
        releaseAudioEffects();
        releaseAudioRecordOnly();
    }

    public static class AudioConfig {
        public final int sampleRate;
        public final int bitsPerSample;
        public final int channels;
        public final int packetSizeBytes;
        public final int packetDurationMs;

        public AudioConfig(int sampleRate, int bitsPerSample, int channels,
                           int packetSizeBytes, int packetDurationMs) {
            this.sampleRate = sampleRate;
            this.bitsPerSample = bitsPerSample;
            this.channels = channels;
            this.packetSizeBytes = packetSizeBytes;
            this.packetDurationMs = packetDurationMs;
        }

        @Override
        public String toString() {
            return String.format("AudioConfig{rate=%dHz, bits=%d, channels=%d, packetSize=%d bytes, duration=%dms}",
                    sampleRate, bitsPerSample, channels, packetSizeBytes, packetDurationMs);
        }
    }
}
