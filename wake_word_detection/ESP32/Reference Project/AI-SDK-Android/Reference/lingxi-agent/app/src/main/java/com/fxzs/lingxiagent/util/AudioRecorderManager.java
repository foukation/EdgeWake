package com.fxzs.lingxiagent.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.ActivityCompat;

import com.fxzs.smartassist.model.meeting.callback.OnAmplitudeListener;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

/**
 * 音频录制管理器
 * 负责管理音频录制的启动、暂停、停止以及文件保存
 *
 * 音频格式针对腾讯云语音识别API优化：
 * - 格式: M4A (AAC编码)
 * - 采样率: 16kHz (腾讯云推荐)
 * - 比特率: 64kbps (平衡质量与文件大小)
 * - 声道: 单声道 (减少文件大小)
 * - 位深度: 16bit (AAC编码器默认)
 *
 * 参考文档: https://cloud.tencent.com/document/product/1093/37823
 */
public class AudioRecorderManager {
    private static final String TAG = "AudioRecorderManager";
    private static AudioRecorderManager manager;

    // 音频录制器
    private AudioRecord audioRecord;
    private MediaCodec mediaCodec;
    private MediaMuxer mediaMuxer;

    // 录音文件路径
    private String audioFilePath;

    // 录音状态
    private boolean isRecording = false;
    private boolean isPaused = false;
    private boolean isMuxerStarted = false;

    // 上下文
    private Context context;
    private long recordingStartTime;
    private long recordingCount = 0;

    // 音频质量配置 - 针对腾讯云语音识别优化
    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int SAMPLE_RATE = 16000; // 腾讯云推荐16kHz采样率
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO; // 单声道
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT; // 16bit PCM

    // AAC编码参数
    private static final String MIME_TYPE = "audio/mp4a-latm";
    private static final int BIT_RATE = 64000; // 64kbps
    private static final int AAC_PROFILE = 2; // AAC LC

    // 兼容参数：部分设备不支持16kHz，必要时降级至更通用的 44.1kHz
    private static final int FALLBACK_SAMPLE_RATE = 44100;
    private static final int FALLBACK_BIT_RATE = 96000;

    // 缓冲区大小
    private int bufferSize;

    // 录音线程
    private RecordingThread recordingThread;

    // MediaMuxer相关
    private int audioTrackIndex = -1;

    // 录音文件目录
    private static final String AUDIO_DIR_NAME = "MeetingRecordings";

    private Runnable amplitudeRunnable;
    private OnAmplitudeListener amplitudeListener;
    private Handler handler = new Handler(Looper.getMainLooper());

    public static AudioRecorderManager getInstance() {
        if (manager == null) {
            synchronized (AudioRecorderManager.class) {
                if (manager == null) {
                    manager = new AudioRecorderManager();
                }
            }
        }
        return manager;
    }

    public void init(Context context) {
        // 使用Application Context避免内存泄漏
        if (context != null) {
            this.context = context.getApplicationContext();
            Timber.tag(TAG).i("AudioRecorderManager 初始化成功");
        } else {
            Timber.tag(TAG).e("初始化失败：Context 为 null");
        }
    }

    /**
     * 检查上下文是否已初始化
     */
    private boolean isContextInitialized() {
        if (context == null) {
            Timber.tag(TAG).e("Context 未初始化，请先调用 init() 方法");
            return false;
        }
        return true;
    }

    /**
     * 开始录音
     * @param meetingTitle 会议标题，用于生成文件名
     * @return 是否成功开始录音
     */
    public boolean startRecording(String meetingTitle) {
        // 检查上下文是否已初始化
        if (!isContextInitialized()) {
            return false;
        }

        // 检查会议标题是否为空
        if (meetingTitle == null || meetingTitle.trim().isEmpty()) {
            Timber.tag(TAG).e("会议标题不能为空");
            return false;
        }

        return startRecordingInternal(meetingTitle, false);
    }

    /**
     * 启动录音核心逻辑，必要时降级到兼容参数
     * @param meetingTitle 录音文件标题
     * @param useFallbackProfile 是否使用兼容参数
     */
    private boolean startRecordingInternal(String meetingTitle, boolean useFallbackProfile) {
        if (isRecording) {
            Timber.tag(TAG).w("Already recording");
            return false;
        }

        // 再次检查上下文
        if (context == null) {
            Timber.tag(TAG).e("Context 为 null，无法开始录音");
            return false;
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        String currentFilePath = null;

        try {
            currentFilePath = createAudioFilePath(meetingTitle);
            // 检查文件路径是否生成成功
            if (currentFilePath == null || currentFilePath.isEmpty()) {
                Timber.tag(TAG).e("无法创建录音文件路径");
                return false;
            }

            audioFilePath = currentFilePath;

            int sampleRate = useFallbackProfile ? FALLBACK_SAMPLE_RATE : SAMPLE_RATE;
            int bitRate = useFallbackProfile ? FALLBACK_BIT_RATE : BIT_RATE;

            // 计算缓冲区大小
            bufferSize = AudioRecord.getMinBufferSize(sampleRate, CHANNEL_CONFIG, AUDIO_FORMAT);
            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                bufferSize = sampleRate * 2; // 默认缓冲区大小
                Timber.tag(TAG).w("使用默认缓冲区大小: " + bufferSize);
            }

            // 创建AudioRecord实例
            audioRecord = new AudioRecord(AUDIO_SOURCE, sampleRate, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize);

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new RuntimeException("AudioRecord initialization failed");
            }

            // 初始化MediaCodec用于AAC编码
            mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
            MediaFormat format = MediaFormat.createAudioFormat(MIME_TYPE, sampleRate, 1);
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, AAC_PROFILE);
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize);

            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();

            // 初始化MediaMuxer用于封装M4A
            mediaMuxer = new MediaMuxer(currentFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            // 开始录音
            audioRecord.startRecording();
            isRecording = true;
            isPaused = false;
            recordingStartTime = System.currentTimeMillis();

            // 启动录音线程
            recordingThread = new RecordingThread();
            recordingThread.start();

            Timber.tag(TAG).i("录音开始 - %s配置，文件: %s",
                    useFallbackProfile ? "兼容" : "标准", currentFilePath);
            return true;

        } catch (IOException | RuntimeException e) {
            Timber.tag(TAG).e(e, "录音启动失败，当前配置: %s",
                    useFallbackProfile ? "兼容(44.1kHz/96kbps)" : "标准(16kHz/64kbps)");
            handleRecordingStartFailure(currentFilePath);
            if (!useFallbackProfile) {
                Timber.tag(TAG).w("尝试使用兼容参数重新启动录音");
                return startRecordingInternal(meetingTitle, true);
            }
            return false;
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "录音启动发生未知异常");
            handleRecordingStartFailure(currentFilePath);
            return false;
        }
    }

    /**
     * 录音线程
     */
    private class RecordingThread extends Thread {
        private static final long TIMEOUT_US = 10000L;

        @Override
        public void run() {
            byte[] buffer = new byte[bufferSize];
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            while (isRecording) {
                if (isPaused) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Timber.tag(TAG).i("录音线程被中断");
                        break;
                    }
                    continue;
                }

                // 读取PCM数据
                int bytesRead = audioRecord.read(buffer, 0, bufferSize);

                if (bytesRead > 0) {
                    try {
                        // 编码PCM数据为AAC
                        int inputBufferIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_US);
                        if (inputBufferIndex >= 0) {
                            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
                            if (inputBuffer != null) {
                                inputBuffer.put(buffer, 0, bytesRead);
                                long presentationTime = System.nanoTime() / 1000;
                                mediaCodec.queueInputBuffer(inputBufferIndex, 0, bytesRead,
                                        presentationTime, 0);
                            }
                        }

                        // 获取编码后的数据
                        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US);
                        while (outputBufferIndex >= 0) {
                            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                // 忽略编解码器配置数据
                                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US);
                                continue;
                            }

                            if (bufferInfo.size > 0) {
                                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
                                if (outputBuffer != null) {
                                    // 写入MediaMuxer
                                    if (isMuxerStarted && audioTrackIndex >= 0) {
                                        outputBuffer.position(bufferInfo.offset);
                                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                                        mediaMuxer.writeSampleData(audioTrackIndex, outputBuffer, bufferInfo);
                                    }
                                }
                            }
                            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US);
                        }

                        // 检查是否需要添加音轨
                        if (!isMuxerStarted) {
                            MediaFormat outputFormat = mediaCodec.getOutputFormat();
                            audioTrackIndex = mediaMuxer.addTrack(outputFormat);
                            mediaMuxer.start();
                            isMuxerStarted = true;
                            Timber.tag(TAG).i("MediaMuxer 启动，音轨索引: " + audioTrackIndex);
                        }

                    } catch (Exception e) {
                        Timber.tag(TAG).e(e, "音频编码失败");
                        break;
                    }
                } else if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                    Timber.tag(TAG).e("AudioRecord ERROR_INVALID_OPERATION");
                    break;
                } else if (bytesRead == AudioRecord.ERROR_BAD_VALUE) {
                    Timber.tag(TAG).e("AudioRecord ERROR_BAD_VALUE");
                    break;
                } else {
                    Timber.tag(TAG).w("读取音频数据返回: " + bytesRead);
                    // 继续尝试，不要立即退出
                }
            }

            // 停止编码
            if (mediaCodec != null) {
                try {
                    mediaCodec.stop();
                    mediaCodec.release();
                } catch (Exception e) {
                    Timber.tag(TAG).e(e, "释放MediaCodec失败");
                }
                mediaCodec = null;
            }

            // 停止MediaMuxer
            if (mediaMuxer != null) {
                try {
                    if (isMuxerStarted) {
                        mediaMuxer.stop();
                    }
                    mediaMuxer.release();
                } catch (Exception e) {
                    Timber.tag(TAG).e(e, "释放MediaMuxer失败");
                }
                mediaMuxer = null;
                isMuxerStarted = false;
                audioTrackIndex = -1;
            }

            Timber.tag(TAG).i("录音线程结束");
        }
    }

    /**
     * 创建音频文件路径
     * @param meetingTitle 会议标题
     * @return 文件路径
     */
    private String createAudioFilePath(String meetingTitle) {
        if (!isContextInitialized()) {
            return null;
        }

        try {
            // 获取应用私有目录
            File externalFilesDir = context.getExternalFilesDir(null);
            if (externalFilesDir == null) {
                Timber.tag(TAG).e("无法获取外部文件目录");
                return null;
            }

            File audioDir = new File(externalFilesDir, AUDIO_DIR_NAME);
            if (!audioDir.exists()) {
                boolean created = audioDir.mkdirs();
                if (!created) {
                    Timber.tag(TAG).e("创建录音目录失败: %s", audioDir.getAbsolutePath());
                    return null;
                }
                Timber.tag(TAG).i("创建录音目录: %s", audioDir.getAbsolutePath());
            }

            // 生成文件名：会议标题_时间戳.m4a
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String timestamp = sdf.format(new Date());

            // 清理文件名中的非法字符
            String cleanTitle = meetingTitle.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5\\-_]", "_");
            if (cleanTitle.isEmpty()) {
                cleanTitle = "Recording";
            }

            String fileName = cleanTitle + "_" + timestamp + ".m4a";
            File audioFile = new File(audioDir, fileName);

            String filePath = audioFile.getAbsolutePath();
            Timber.tag(TAG).i("生成的录音文件路径: " + filePath);
            return filePath;

        } catch (Exception e) {
            Timber.tag(TAG).e(e, "创建录音文件路径失败");
            return null;
        }
    }

    /**
     * 处理录音启动失败，释放资源并清理临时文件
     * @param failedFilePath 本次尝试生成的文件路径
     */
    private void handleRecordingStartFailure(String failedFilePath) {
        cleanupFailedRecordingFile(failedFilePath);
        isRecording = false;
        isPaused = false;
        audioFilePath = null;
        releaseRecorder();
    }

    /**
     * 删除启动失败产生的空文件，避免目录中堆积废文件
     * @param failedFilePath 需要删除的文件路径
     */
    private void cleanupFailedRecordingFile(String failedFilePath) {
        if (failedFilePath == null) {
            return;
        }
        File failedFile = new File(failedFilePath);
        if (failedFile.exists()) {
            boolean deleted = failedFile.delete();
            Timber.tag(TAG).w("删除启动失败的临时录音文件: %s, result=%s",
                    failedFilePath, deleted);
        }
    }

    public void setAmplitudeListener(OnAmplitudeListener listener) {
        this.amplitudeListener = listener;
    }

    /**
     * 暂停录音
     * @return 是否成功暂停
     */
    @SuppressLint("TimberArgCount")
    public boolean pauseRecording() {
        if (!isRecording || isPaused) {
            Timber.tag(TAG).w("Not recording or already paused");
            return false;
        }

        try {
            isPaused = true;
            if (audioRecord != null) {
                audioRecord.stop();
            }
            Timber.tag(TAG).i("Recording paused");
            return true;
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "Failed to pause recording");
            return false;
        }
    }

    /**
     * 恢复录音
     * @return 是否成功恢复
     */
    public boolean resumeRecording() {
        if (!isRecording || !isPaused) {
            Timber.tag(TAG).w("Not paused");
            return false;
        }

        try {
            if (audioRecord != null) {
                audioRecord.startRecording();
            }
            isPaused = false;
            Timber.tag(TAG).i("Recording resumed");
            return true;
        } catch (Exception e) {
            Timber.tag(TAG).e("Failed to resume recording%s", e);
            return false;
        }
    }

    /**
     * 停止录音并保存文件
     * @return 保存的音频文件路径，失败返回null
     */
    public String stopRecording() {
        if (!isRecording) {
            Timber.tag(TAG).w("Not recording");
            return null;
        }

        isRecording = false;

        try {
            // 等待录音线程结束
            if (recordingThread != null) {
                recordingThread.join(2000); // 等待最多2秒
            }

            // 停止AudioRecord
            if (audioRecord != null) {
                audioRecord.stop();
            }

            // 检查文件是否存在
            if (audioFilePath != null) {
                File audioFile = new File(audioFilePath);
                if (audioFile.exists() && audioFile.length() > 0) {
                    Timber.tag(TAG).i("录音停止并保存成功");
                    Timber.tag(TAG).i("文件路径: " + audioFilePath);
                    Timber.tag(TAG).i("文件大小: " + audioFile.length() + " bytes (" + (audioFile.length() / 1024) + " KB)");
                    Timber.tag(TAG).i("文件名: " + audioFile.getName());
                    Timber.tag(TAG).i("保存目录: " + audioFile.getParent());
                    return audioFilePath;
                } else {
                    Timber.tag(TAG).e("Audio file is empty or doesn't exist");
                    return null;
                }
            } else {
                Timber.tag(TAG).e("Audio file path is null");
                return null;
            }

        } catch (Exception e) {
            Timber.tag(TAG).e("Failed to stop recording%s", e);
            return null;
        } finally {
            isRecording = false;
            isPaused = false;
            releaseRecorder();
        }
    }

    /**
     * 取消录音（不保存文件）
     */
    @SuppressLint("TimberArgCount")
    public void cancelRecording() {
        if (!isRecording) {
            return;
        }

        isRecording = false;

        try {
            // 等待录音线程结束
            if (recordingThread != null) {
                recordingThread.join(1000);
            }
        } catch (InterruptedException e) {
            Timber.tag(TAG).e(e, "Interrupted while waiting for recording thread");
        }

        // 删除录音文件
        if (audioFilePath != null) {
            File audioFile = new File(audioFilePath);
            if (audioFile.exists()) {
                boolean deleted = audioFile.delete();
                Timber.tag(TAG).i("Recording cancelled and file deleted: %s", deleted);
            }
        }

        isRecording = false;
        isPaused = false;
        releaseRecorder();
    }

    /**
     * 释放录音器资源
     */
    private void releaseRecorder() {
        if (audioRecord != null) {
            try {
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
                audioRecord.release();
            } catch (Exception e) {
                Timber.tag(TAG).e("Error releasing recorder%s", e);
            }
            audioRecord = null;
        }

        if (mediaCodec != null) {
            try {
                mediaCodec.stop();
                mediaCodec.release();
            } catch (Exception e) {
                Timber.tag(TAG).e("Error releasing media codec"+ e);
            }
            mediaCodec = null;
        }

        if (mediaMuxer != null) {
            try {
                if (isMuxerStarted) {
                    mediaMuxer.stop();
                }
                mediaMuxer.release();
            } catch (Exception e) {
                Timber.tag(TAG).e("Error releasing media muxer"+ e);
            }
            mediaMuxer = null;
            isMuxerStarted = false;
            audioTrackIndex = -1;
        }

        recordingThread = null;
    }

    /**
     * 获取当前录音状态
     * @return 是否正在录音
     */
    public boolean isRecording() {
        return isRecording;
    }

    /**
     * 获取暂停状态
     * @return 是否暂停中
     */
    public boolean isPaused() {
        return isPaused;
    }

    /**
     * 获取当前录音文件路径
     * @return 文件路径
     */
    public String getCurrentAudioPath() {
        return audioFilePath;
    }

    /**
     * 获取录音文件保存目录
     * @return 目录路径
     */
    public String getRecordingsDirectory() {
        if (!isContextInitialized()) {
            return null;
        }

        File externalFilesDir = context.getExternalFilesDir(null);
        if (externalFilesDir == null) {
            return null;
        }

        File audioDir = new File(externalFilesDir, AUDIO_DIR_NAME);
        return audioDir.getAbsolutePath();
    }

    /**
     * 获取音频格式配置信息
     * @return 音频格式详细信息
     */
    public String getAudioFormatInfo() {
        return String.format(Locale.getDefault(),
                "音频格式配置 (腾讯云优化):\n" +
                        "格式: M4A (AAC编码)\n" +
                        "采样率: %dHz\n" +
                        "比特率: %dbps (%dkbps)\n" +
                        "声道数: %d (单声道)\n" +
                        "音频源: 麦克风",
                SAMPLE_RATE, BIT_RATE, BIT_RATE/1000, 1);
    }

    public long getRecordingStartTime() {
        return recordingStartTime;
    }

    public void setRecordingStartTime(long recordingStartTime) {
        this.recordingStartTime = recordingStartTime;
    }

    public long getRecordingCount() {
        return recordingCount;
    }

    public void setRecordingCount(long recordingCount) {
        this.recordingCount = recordingCount;
    }
}