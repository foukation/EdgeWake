package com.fxzs.lingxiagent.viewmodel.meeting;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.drawing.repository.DrawingRepository;
import com.fxzs.lingxiagent.model.meeting.dto.MeetingDto;
import com.fxzs.lingxiagent.model.meeting.repository.MeetingRepository;
import com.fxzs.lingxiagent.model.meeting.repository.MeetingRepositoryImpl;
import com.fxzs.lingxiagent.util.AudioRecognitionManager;

import java.util.Map;

import timber.log.Timber;

public class VMAudioTranscription extends BaseViewModel {
    private static final String TAG = "VMAudioTranscription";

    private final MeetingRepository repository;
    private AudioRecognitionManager audioRecognitionManager;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // UI 显示数据
    private final MutableLiveData<String> fileName = new MutableLiveData<>();
    private final MutableLiveData<String> transcriptionContent = new MutableLiveData<>();
    private final MutableLiveData<String> summaryContent = new MutableLiveData<>();
    private final MutableLiveData<String> topicContent = new MutableLiveData<>();
    private final MutableLiveData<String> qaContent = new MutableLiveData<>();

    // 转写流程状态
    private final MutableLiveData<String> selectedLanguage = new MutableLiveData<>("16k_zh");
    private final MutableLiveData<String> progressMessage = new MutableLiveData<>();
    private final MutableLiveData<TranscriptionResult> transcriptionResult = new MutableLiveData<>();
    private final MutableLiveData<Integer> currentMeetingId = new MutableLiveData<>();
    private boolean isCancel = false;


    public VMAudioTranscription(@NonNull Application application) {
        super(application);
        this.repository = new MeetingRepositoryImpl();
        this.audioRecognitionManager = new AudioRecognitionManager(application);
    }
    
    public void loadMockData() {
        // TODO: 临时代码，用于UI测试
        // 模拟文件名
        fileName.setValue("20241222音频文件.mp3");
        
        // 模拟转写内容
        String transcription = "淘宝，中国领先的电商平台，成立于2003年，阿里巴巴集团旗下的子公司。" +
                "作为全球最大的C2C和B2C市场，它连接了消费者、商家和中小企业，推动了中国乃至全球电子商务的发展。" +
                "通过不断创新，淘宝致力于打造安全、便捷的在线购物环境，倡导'让天下没有难做的生意'。";
        transcriptionContent.setValue(transcription);
        
        // 模拟摘要内容
        String summary = "本次会议主要讨论了淘宝的发展历程和企业文化。";
        summaryContent.setValue(summary);
        
        // 模拟话题内容
        String topics = "1. 公司发展历程\n2. 企业文化理念\n3. 商业模式创新";
        topicContent.setValue(topics);
        
        // 模拟问答内容
        String qa = "Q: 淘宝的核心价值观是什么？\nA: 让天下没有难做的生意。";
        qaContent.setValue(qa);
    }
    
    public MutableLiveData<String> getFileName() {
        return fileName;
    }
    
    public MutableLiveData<String> getTranscriptionContent() {
        return transcriptionContent;
    }
    
    public MutableLiveData<String> getSummaryContent() {
        return summaryContent;
    }
    
    public MutableLiveData<String> getTopicContent() {
        return topicContent;
    }
    
    public MutableLiveData<String> getQaContent() {
        return qaContent;
    }

    // Getter methods for transcription flow
    public LiveData<String> getSelectedLanguage() {
        return selectedLanguage;
    }

    public LiveData<String> getProgressMessage() {
        return progressMessage;
    }

    public LiveData<TranscriptionResult> getTranscriptionResult() {
        return transcriptionResult;
    }

    public LiveData<Integer> getCurrentMeetingId() {
        return currentMeetingId;
    }



    public void setSelectedLanguage(String language) {
        selectedLanguage.setValue(language);
    }

    public void setCancel(boolean cancel) {
        isCancel = cancel;
        if(audioRecognitionManager != null){
            audioRecognitionManager.setCancel(cancel);
        }

    }

    /**
     * 获取离线引擎模型类型
     */
    public LiveData<DrawingRepository.Result<Map<String, String>>> getOfflineEngineModelType() {
//        return repository.getSttLanguage();
        return repository.getOfflineEngineModelType();
    }

    /**
     * 开始音视频文件转写流程
     * @param audioFilePath 音频文件路径
     * @param fileName 文件名
     */
    public void startAudioTranscription(String audioFilePath, String fileName) {
        Timber.tag(TAG).d( "开始音视频转写流程: " + audioFilePath + ", fileName: " + fileName);

        setLoading(true);
        this.fileName.setValue(fileName);
        progressMessage.setValue("正在创建会议记录...");

        setCancel(false);

        // 第一步：创建会议记录
        createMeetingRecord(fileName, audioFilePath);
    }

    /**
     * 创建会议记录
     */
    private void createMeetingRecord(String fileName, String audioFilePath) {
        Timber.tag(TAG).d( "创建会议记录 - fileName: " + fileName);

        // 创建会议记录
        MeetingDto meeting = new MeetingDto();
        meeting.setName(fileName); // 使用文件名作为会议名称
        meeting.setTitle(fileName); // 同时设置title字段
        meeting.setType(3); // 1表示上传的音频文件
        meeting.setStatus(0); // 0表示处理中
        meeting.setLanguage(selectedLanguage.getValue()); // 选择的语言

        // 调用Repository添加会议记录
        repository.addMeeting(meeting).observeForever(result -> {
            if (result != null && result.isSuccess()) {
                MeetingDto createdMeeting = result.getData();
                Integer meetingId = Integer.valueOf(createdMeeting.getId());
                currentMeetingId.setValue(meetingId);
                Timber.tag(TAG).d( "会议记录创建成功，ID: " + meetingId);

                handler.post(() -> {
                    progressMessage.setValue("会议记录创建成功，开始上传文件...");
                    // 第二步：使用真实的meetingId开始音频识别流程
                    startAudioRecognitionWithMeetingId(audioFilePath, fileName, meetingId);
                });
            } else {
                Timber.tag(TAG).e( "会议记录创建失败: " + (result != null ? result.getError() : "未知错误"));
                handler.post(() -> {
                    setLoading(false);
                    setError("创建会议记录失败: " + (result != null ? result.getError() : "未知错误"));
                    transcriptionResult.setValue(new TranscriptionResult(false, null, null, result != null ? result.getError() : "未知错误"));
                });
            }
        });
    }

    /**
     * 使用真实的meetingId开始音频识别流程
     */
    private void startAudioRecognitionWithMeetingId(String audioFilePath, String fileName, Integer meetingId) {
        Timber.tag(TAG).d( "使用meetingId开始识别: " + meetingId + ", fileName: " + fileName);

        audioRecognitionManager.setSelectedLanguage(selectedLanguage.getValue());
        // 开始音频识别流程，使用AudioRecognitionManager和真实的meetingId
        audioRecognitionManager.startAudioRecognition(audioFilePath, meetingId, new AudioRecognitionManager.AudioRecognitionCallback() {
            @Override
            public void onUploadProgress(int progress) {
                handler.post(() -> {
                    progressMessage.setValue("正在上传文件... " + progress + "%");
                });
            }

            @Override
            public void onUploadSuccess(String fileUrl) {
                handler.post(() -> {
                    progressMessage.setValue("文件上传成功，正在更新会议记录...");
                    progressMessage.setValue("UPLOAD_SUCCESS");
                    // 立即更新会议记录的fileUrl
                    updateMeetingFileUrl(meetingId, fileUrl);
                });
            }

            @Override
            public void onTaskSubmitted(Long taskId) {

            }

            @Override
            public void onRecognitionProgress(int progress) {
                handler.post(() -> {
                    progressMessage.setValue("正在识别中...");
                    // 可以实时更新部分识别结果
//                    transcriptionContent.setValue(partialResult);
                });
            }


            @Override
            public void onRecognitionCompleted(String result, Integer meetingId) {
                handler.post(() -> {
                    Timber.tag(TAG).d( "识别成功: " + result + ", meetingId: " + meetingId);
                    // 第三步：更新会议记录
                    updateMeetingWithTranscription(meetingId, result, fileName);
                });
            }

            @Override
            public void onError(String error) {
                handler.post(() -> {
                    Timber.tag(TAG).e( "识别失败: " + error);
                    setLoading(false);
//                    setError("识别失败1: " + error);
                    transcriptionResult.setValue(new TranscriptionResult(false, null, null, error));
                });
            }
        });
    }

    /**
     * 更新会议记录的识别结果
     */
    private void updateMeetingWithTranscription(Integer meetingId, String transcriptionResult, String fileName) {
        Timber.tag(TAG).d( "更新会议记录 - meetingId: " + meetingId + ", 内容长度: " +
            (transcriptionResult != null ? transcriptionResult.length() : 0));

        if(isCancel){
           return;
        }
        if (meetingId == null) {
            Timber.tag(TAG).e( "meetingId为空，无法更新会议记录");
            // 直接返回成功结果，不更新记录
            handleTranscriptionSuccess(transcriptionResult, fileName, meetingId);
            return;
        }

        // 显示更新进度
        progressMessage.setValue("正在保存识别结果...");

        // 创建MeetingDto对象来更新会议记录
        MeetingDto meeting = new MeetingDto();
        meeting.setId(meetingId.toString());
        meeting.setContent(transcriptionResult); // 设置识别结果
        meeting.setStatus(1); // 设置状态为已完成
        // 注意：这里不需要再次设置fileUrl，因为在上传成功时已经更新过了

        // 调用Repository更新会议记录
        repository.updateMeeting(meeting).observeForever(result -> {
            if (result != null && result.isSuccess()) {
                Timber.tag(TAG).d( "会议记录更新成功");
                handler.post(() -> {
                    handleTranscriptionSuccess(transcriptionResult, fileName, meetingId);
                });
            } else {
                Timber.tag(TAG).e( "会议记录更新失败: " + (result != null ? result.getError() : "未知错误"));
                handler.post(() -> {
                    // 即使更新失败，也返回识别结果，但给出警告
                    setError("识别完成，但保存失败: " + (result != null ? result.getError() : "未知错误"));
                    handleTranscriptionSuccess(transcriptionResult, fileName, meetingId);
                });
            }
        });
    }

    /**
     * 更新会议记录的文件URL
     */
    private void updateMeetingFileUrl(Integer meetingId, String fileUrl) {
        Timber.tag(TAG).d( "更新会议记录文件URL - meetingId: " + meetingId + ", fileUrl: " + fileUrl);

        if (meetingId == null || fileUrl == null) {
            Timber.tag(TAG).w( "meetingId或fileUrl为空，跳过文件URL更新");
            progressMessage.setValue("开始语音识别...");
            return;
        }

        // 创建MeetingDto对象来更新会议记录
        MeetingDto meeting = new MeetingDto();
        meeting.setId(meetingId.toString());
        meeting.setFileUrl(fileUrl); // 设置文件URL

        // 调用Repository更新会议记录
        repository.updateMeeting(meeting).observeForever(result -> {
            if (result != null && result.isSuccess()) {
                Timber.tag(TAG).d( "会议记录文件URL更新成功");
                handler.post(() -> {
                    progressMessage.setValue("开始语音识别...");
                });
            } else {
                Timber.tag(TAG).w("会议记录文件URL更新失败: " + (result != null ? result.getError() : "未知错误"));
                handler.post(() -> {
                    // 即使更新失败，也继续识别流程
                    progressMessage.setValue("开始语音识别...");
                });
            }
        });
    }

    /**
     * 处理转写成功
     */
    private void handleTranscriptionSuccess(String result, String fileName, Integer meetingId) {
        setLoading(false);
        progressMessage.setValue("识别完成！");

        // 更新UI显示的内容
        transcriptionContent.setValue(result);
        this.fileName.setValue(fileName);

        // 返回成功结果
        transcriptionResult.setValue(new TranscriptionResult(true, result, meetingId, null));
    }

    /**
     * 转写结果数据类
     */
    public static class TranscriptionResult {
        private final boolean success;
        private final String transcriptionText;
        private final Integer meetingId;
        private final String errorMessage;

        public TranscriptionResult(boolean success, String transcriptionText, Integer meetingId, String errorMessage) {
            this.success = success;
            this.transcriptionText = transcriptionText;
            this.meetingId = meetingId;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getTranscriptionText() {
            return transcriptionText;
        }

        public Integer getMeetingId() {
            return meetingId;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}