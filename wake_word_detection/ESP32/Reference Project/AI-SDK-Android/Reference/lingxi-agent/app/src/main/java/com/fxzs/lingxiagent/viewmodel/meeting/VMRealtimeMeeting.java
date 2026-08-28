package com.fxzs.lingxiagent.viewmodel.meeting;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.model.chat.callback.StsCallback;
import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.meeting.dto.MeetingDto;
import com.fxzs.lingxiagent.model.meeting.dto.SoundRecordTaskResponseDto;
import com.fxzs.lingxiagent.model.meeting.repository.MeetingRepository;
import com.fxzs.lingxiagent.model.meeting.repository.MeetingRepositoryImpl;
import com.fxzs.lingxiagent.util.AudioRecorderManager;
import com.fxzs.lingxiagent.util.NetworkUtils;
import com.fxzs.lingxiagent.util.ZUtil.Constant;
import com.fxzs.lingxiagent.util.ZUtil.SessionUpload;
import com.tencent.cos.xml.exception.CosXmlClientException;
import com.tencent.cos.xml.exception.CosXmlServiceException;

import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

public class VMRealtimeMeeting extends BaseViewModel {
    private static final String TAG = "VMRealtimeMeeting";
    private static final int POLLING_INTERVAL = 2000; // 2秒
    private static final int MAX_POLLING_COUNT = 60; // 最多查询60次
    
    private final MeetingRepository repository;
    private AudioRecorderManager audioRecorderManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    
    // 状态数据
    private final MutableLiveData<Boolean> isRecording = new MutableLiveData<>(false);
    private final MutableLiveData<String> recordingFilePath = new MutableLiveData<>();
    private final MutableLiveData<String> meetingId = new MutableLiveData<>();
    private final MutableLiveData<String> selectedLanguage = new MutableLiveData<>("chinese_16k");
    private final MutableLiveData<String> meetingTitle = new MutableLiveData<>();
    
    // 进度状态
    private final MutableLiveData<String> progressMessage = new MutableLiveData<>();
    private final MutableLiveData<RecognitionResult> recognitionResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showNetworkReconnectDialog = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> showProgressDialog = new MutableLiveData<>(false);

    // 基础状态
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    // 轮询控制
    private final AtomicBoolean isPolling = new AtomicBoolean(false);
    private Runnable pollingRunnable;
    
    // 断网重连相关状态
    private ProcessStage currentStage = ProcessStage.NONE;
    private String uploadedFileUrl; // 已上传的文件URL
    private String currentTaskId; // 当前识别任务ID
    private boolean hasNetworkError = false; // 是否发生了网络错误
    private boolean isCreateNew = false; // 是否新建会议

    // 处理阶段枚举
    public enum ProcessStage {
        NONE,           // 未开始
        UPLOADING,      // 文件上传中
        UPLOADED,       // 文件已上传，准备创建会议
        MEETING_CREATED, // 会议已创建，准备启动识别
        RECOGNITION_STARTED, // 识别已启动，轮询中
        COMPLETED       // 已完成
    }

    public VMRealtimeMeeting(@NonNull Application application) {
        super(application);
        repository = new MeetingRepositoryImpl(application);
    }

    public AudioRecorderManager getAudioRecorderManager() {
        return audioRecorderManager;
    }

    // 初始化音频录制器
    public void initAudioRecorder(AudioRecorderManager manager) {
        this.audioRecorderManager = manager;
    }
    // 开始录音
    public void startRecording(String title/*, OnAmplitudeListener listener*/) {
        audioRecorderManager = AudioRecorderManager.getInstance();
        if (audioRecorderManager != null) {
            meetingTitle.setValue(title);
            boolean success = audioRecorderManager.startRecording(title);
//            boolean success = true;
            isRecording.setValue(success);
            if (!success) {
                setError("开始录音失败");
            }

        }
    }
    public void resumeRecording(String title) {

            meetingTitle.setValue(title);
            isRecording.setValue(true);


    }
    // Helper方法 - 删除重复定义，使用父类的protected方法
    
    // 停止录音
    public void stopRecording() {
        if (audioRecorderManager != null) {
            String filePath = audioRecorderManager.stopRecording();
            recordingFilePath.setValue(filePath);
            isRecording.setValue(false);
        }
    }

    public void setIsRecording(boolean recording) {
        isRecording.setValue(recording);
    }

    // 完整的结束会议流程
    public void endMeetingWithProgress() {
        Timber.tag(TAG).d( "开始结束会议流程");
        

        // 重置状态
        currentStage = ProcessStage.UPLOADING;
        hasNetworkError = false;
        uploadedFileUrl = null;
        currentTaskId = null;
        
        // 停止录音
        stopRecording();
        String audioFilePath = recordingFilePath.getValue();
        
        if (audioFilePath == null || audioFilePath.isEmpty()) {
            setError("录音文件路径为空");
            return;
        }
        
        // 开始上传流程
        progressMessage.setValue("正在上传录音文件...");
        setLoading(true);
        
        handler.postDelayed(() -> uploadAudioFile(audioFilePath), 500);
    }
    
    // 重新生成（断网重连）
    public void retryGenerateMeeting() {
        Timber.tag(TAG).d( "重新生成会议，当前阶段: " + currentStage);
        hasNetworkError = false;
        showNetworkReconnectDialog.setValue(false);

        switch (currentStage) {
            case UPLOADING:
                // 重新上传
                String audioFilePath = recordingFilePath.getValue();
                if (audioFilePath != null && !audioFilePath.isEmpty()) {
                    progressMessage.setValue("正在重新上传录音文件...");
                    setLoading(true);
                    uploadAudioFile(audioFilePath);
                } else {
                    handleRecognitionError("录音文件路径为空");
                }
                break;
                
            case UPLOADED:
                // 从创建会议开始
                if (uploadedFileUrl != null) {
                    progressMessage.setValue("正在创建会议记录...");
                    setLoading(true);
                    createMeetingAndStartRecognition(uploadedFileUrl, selectedLanguage.getValue());
                } else {
                    handleRecognitionError("文件URL为空");
                }
                break;
                
            case MEETING_CREATED:
                // 从启动语音识别开始
                if (uploadedFileUrl != null && meetingId.getValue() != null) {
                    progressMessage.setValue("正在启动语音识别...");
                    setLoading(true);
                    startVoiceRecognitionTask(uploadedFileUrl, selectedLanguage.getValue(), meetingId.getValue());
                } else {
                    handleRecognitionError("会议ID或文件URL为空");
                }
                break;
                
            case RECOGNITION_STARTED:
                // 重新开始轮询
                if (currentTaskId != null && meetingId.getValue() != null) {
                    progressMessage.setValue("正在查询识别结果...");
                    setLoading(true);
                    startPollingRecognitionResult(currentTaskId, meetingId.getValue());
                } else {
                    handleRecognitionError("任务ID或会议ID为空");
                }
                break;
                
            default:
                handleRecognitionError("无法重新生成，状态异常");
                break;
        }
    }
    
    // 上传音频文件
    private void uploadAudioFile(String filePath) {
        Timber.tag(TAG).d( "开始上传音频文件: " + filePath);
        currentStage = ProcessStage.UPLOADING;
        
        SessionUpload.upload(getApplication(), filePath, new StsCallback() {
            @Override
            public void progress(long percent) {

            }

            @Override
            public void callback(String fileUrl) {
                if (fileUrl != null && !fileUrl.isEmpty()) {
                    Timber.tag(TAG).d( "文件上传成功: " + fileUrl);
                    uploadedFileUrl = fileUrl; // 保存上传成功的URL
                    currentStage = ProcessStage.UPLOADED;
                    
                    handler.post(() -> {
                        progressMessage.setValue("文件上传成功，正在创建会议记录...");
                        createMeetingAndStartRecognition(fileUrl, selectedLanguage.getValue());
                    });
                } else {
                    Timber.tag(TAG).e( "文件上传失败: 返回URL为空");
                    handler.post(() -> handleNetworkError("文件上传失败"));
                }
            }

            @Override
            public void error(@Nullable CosXmlClientException clientException, @Nullable CosXmlServiceException serviceException) {

                setError("网络异常，请联网后重试");

                handler.post(() -> showProgressDialog.setValue(false));

            }
        });
    }
    
    // 创建会议记录并开始识别
    private void createMeetingAndStartRecognition(String fileUrl, String language) {
        Timber.tag(TAG).d( "创建会议记录 - 使用新的API调用顺序");
        
        // 步骤1: 调用addAiMeetingRecord创建会议记录
        MeetingDto meeting = new MeetingDto();
        // 使用fileUrl字段
        meeting.setFileUrl(fileUrl);
        // 使用name字段作为会议名称
        meeting.setName(meetingTitle.getValue());
        // 根据API文档，type=1实时类型，type=3表示录音类型
        meeting.setType(1);
        // 保存本地路径
        meeting.setAudioFilePath(recordingFilePath.getValue());
        
        repository.addMeeting(meeting).observeForever(result -> {
            if (result.isSuccess()) {
                MeetingDto createdMeeting = result.getData();
                meetingId.setValue(createdMeeting.getId());
                currentStage = ProcessStage.MEETING_CREATED;
                
                Timber.tag(TAG).d( "会议记录创建成功，ID: " + createdMeeting.getId());
                progressMessage.setValue("会议记录创建成功，正在启动语音识别...");
                
                // 步骤2: 调用soundRecordRecognition启动语音识别
                startVoiceRecognitionTask(fileUrl, language, createdMeeting.getId());
            } else {
                Timber.tag(TAG).e( "创建会议记录失败: " + result.getError());
                handleNetworkError("创建会议记录失败: " + result.getError());
            }
        });
    }
    
    // 启动语音识别任务
    private void startVoiceRecognitionTask(String fileUrl, String language, String meetingId) {
        Timber.tag(TAG).d( "启动语音识别任务");
        progressMessage.setValue("正在启动语音识别任务...");
        
        // 步骤2: 调用soundRecordRecognition提交识别任务
        // 使用16k_zh作为默认语言模型
        String engineModelType = "16k_zh_en";
        if (language != null) {
            engineModelType = language;
        }
        Constant.transcription = "";
        repository.submitAudioRecognitionTask(fileUrl, engineModelType, meetingId).observeForever(result -> {
            if (result.isSuccess()) {
                SoundRecordTaskResponseDto response = result.getData();
                if (response != null && response.getData() != null && response.getData().getTaskId() != null) {
                    Long taskId = response.getData().getTaskId();
                    currentTaskId = String.valueOf(taskId);
                    currentStage = ProcessStage.RECOGNITION_STARTED;
                    
                    Timber.tag(TAG).d( "语音识别任务启动成功，任务ID: " + taskId);
                    progressMessage.setValue("语音识别任务已启动，正在处理中...");
                    
                    // 步骤3: 调用soundRecordRecognitionTaskCheck轮询识别结果
                    startPollingRecognitionResult(String.valueOf(taskId), meetingId);
                } else {
                    handleNetworkError("语音识别任务响应无效");
                }
            } else {
                handleNetworkError(result.getError());
            }
        });
    }
    
    // 开始轮询查询识别结果
    private void startPollingRecognitionResult(String taskId, String meetingId) {
        Timber.tag(TAG).d( "开始轮询查询识别结果，任务ID: " + taskId);
        isPolling.set(true);
        
        final int[] pollCount = {0};
        
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPolling.get() || pollCount[0] >= MAX_POLLING_COUNT) {
                    if (pollCount[0] >= MAX_POLLING_COUNT) {
                        handleRecognitionError("语音识别超时");
                    }
                    return;
                }
                
                pollCount[0]++;
                progressMessage.setValue(String.format("正在识别中...(%d/%d)", pollCount[0], MAX_POLLING_COUNT));
                
                repository.queryAudioRecognitionResult(taskId, meetingId).observeForever(result -> {
                    if (result.isSuccess()) {
                        SoundRecordTaskResponseDto response = result.getData();
                        if (response != null && response.getData() != null) {
                            Integer status = response.getData().getStatus();
                            Timber.tag(TAG).d( "查询结果，状态: " + status);
                            
                            if (status != null && status == 2) {
                                // 识别成功
                                isPolling.set(false);
                                String transcription = response.getData().getResult();
                                Constant.transcription = transcription;
                                Timber.tag(TAG).d( "语音识别成功: " + transcription);
                                progressMessage.setValue("语音识别完成，正在更新会议记录...");
                                
                                // 步骤4: 调用updateMeetingRecord更新会议记录
                                updateMeetingWithTranscription(meetingId, transcription);
                            } else if (status != null && status == 3) {
                                // 识别失败
                                isPolling.set(false);
                                String errorMsg = response.getData().getErrorMsg();
                                handleNetworkError("语音识别失败: " + errorMsg);
                            } else {
                                // 继续轮询
                                handler.postDelayed(pollingRunnable, POLLING_INTERVAL);
                            }
                        } else {
                            handler.postDelayed(pollingRunnable, POLLING_INTERVAL);
                        }
                    } else {
                        // API调用失败，检查是否是网络问题
                        Timber.tag(TAG).e( "查询识别结果失败: " + result.getError());
                        if (!NetworkUtils.isNetworkAvailable(getApplication())) {
                            // 网络不可用，停止轮询，等待网络恢复
                            isPolling.set(false);
                            handleNetworkError("网络连接失败，查询识别结果中断");
                        } else {
                            // 网络可用但API失败，继续轮询
                            handler.postDelayed(pollingRunnable, POLLING_INTERVAL);
                        }
                    }
                });
            }
        };
        
        handler.post(pollingRunnable);
    }
    
    // 更新会议记录
    private void updateMeetingWithTranscription(String meetingId, String transcription) {
        Timber.tag(TAG).d( "更新会议记录，内容长度: " + (transcription != null ? transcription.length() : 0));
        
        MeetingDto meeting = new MeetingDto();
        meeting.setId(meetingId);
        meeting.setContent(transcription); // 设置识别出的文本内容
        
        repository.updateMeeting(meeting).observeForever(result -> {
            setLoading(false);
            if (result.isSuccess()) {
                Timber.tag(TAG).d( "会议记录更新成功");
                currentStage = ProcessStage.COMPLETED;
                progressMessage.setValue("会议记录已完成！");
                recognitionResult.setValue(new RecognitionResult(true, transcription, meetingId));
            } else {
                Timber.tag(TAG).e( "更新会议记录失败: " + result.getError());
                currentStage = ProcessStage.COMPLETED;
                // 即使更新失败，也返回成功结果，因为识别已经完成
                recognitionResult.setValue(new RecognitionResult(true, transcription, meetingId));
            }
        });
    }
    
    // 处理网络错误
    private void handleNetworkError(String errorMessage) {
        Timber.tag(TAG).e( "网络错误: " + errorMessage);
        hasNetworkError = true;
        isPolling.set(false);
        setLoading(false);
        progressMessage.setValue("网络连接中断");
        
        // 检查网络连接并决定是否显示重连对话框
        checkNetworkAndShowDialog();
    }
    
    // 检查网络连接状态
    private void checkNetworkAndShowDialog() {
        // 延迟检查网络，给网络恢复一些时间
        handler.postDelayed(() -> {
            if (NetworkUtils.isNetworkAvailable(getApplication())) {
                // 网络已恢复，显示重连对话框
                showNetworkReconnectDialog.setValue(true);
            } else {
                // 网络仍未恢复，继续等待
                handler.postDelayed(this::checkNetworkAndShowDialog, 2000);
            }
        }, 2000);
    }
    
    // 处理识别错误（非网络问题）
    private void handleRecognitionError(String errorMessage) {
        Timber.tag(TAG).e( "语音识别错误: " + errorMessage);
        isPolling.set(false);
        setLoading(false);
        setError(errorMessage);
        progressMessage.setValue("语音识别失败");
        recognitionResult.setValue(new RecognitionResult(false, errorMessage));
    }
    
    // 停止轮询
    public void stopPolling() {
        isPolling.set(false);
        if (pollingRunnable != null) {
            handler.removeCallbacks(pollingRunnable);
        }
    }

    public void cancelTask() {
        SessionUpload.cancelUpload();
        stopPolling();
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        cancelTask();
        handler.removeCallbacksAndMessages(null);
        if (audioRecorderManager != null) {
            audioRecorderManager.stopRecording();
        }
    }
    
    // Getters
    public LiveData<Boolean> getIsRecording() {
        return isRecording;
    }
    
    public LiveData<String> getRecordingFilePath() {
        return recordingFilePath;
    }
    
    public LiveData<String> getMeetingId() {
        return meetingId;
    }
    
    public LiveData<String> getSelectedLanguage() {
        return selectedLanguage;
    }
    
    public LiveData<String> getMeetingTitle() {
        return meetingTitle;
    }
    
    public LiveData<String> getProgressMessage() {
        return progressMessage;
    }
    
    public LiveData<RecognitionResult> getRecognitionResult() {
        return recognitionResult;
    }
    
    public LiveData<String> getErrorMessage() {
        return getError();
    }
    
    public LiveData<Boolean> getShowNetworkReconnectDialog() {
        return showNetworkReconnectDialog;
    }
    public LiveData<Boolean> getShowProgressDialog() {
        return showProgressDialog;
    }
    
    // 删除重复的getLoading()方法，使用父类的方法
    
    public void setSelectedLanguage(String language) {
        selectedLanguage.setValue(language);
    }

    public boolean isCreateNew() {
        return isCreateNew;
    }

    public void setCreateNew(boolean createNew) {
        isCreateNew = createNew;
    }

    // 识别结果类
    public static class RecognitionResult {
        private final boolean success;
        private final String message;
        private final String meetingId;
        
        public RecognitionResult(boolean success, String message) {
            this(success, message, null);
        }
        
        public RecognitionResult(boolean success, String message, String meetingId) {
            this.success = success;
            this.message = message;
            this.meetingId = meetingId;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public String getMeetingId() {
            return meetingId;
        }
    }
}