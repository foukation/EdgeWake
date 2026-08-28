package com.fxzs.lingxiagent.viewmodel.meeting;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import androidx.annotation.NonNull;

import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.common.ObservableField;
import com.fxzs.lingxiagent.model.honor.repository.BenefitCode;
import com.fxzs.lingxiagent.model.meeting.dto.LanguageDto;
import com.fxzs.lingxiagent.model.meeting.dto.MeetingDto;
import com.fxzs.lingxiagent.model.meeting.dto.MeetingListDto;
import com.fxzs.lingxiagent.model.meeting.dto.MeetingSummaryDto;
import com.fxzs.lingxiagent.model.meeting.dto.TranscriptionDto;
import com.fxzs.lingxiagent.model.meeting.repository.MeetingRepository;
import com.fxzs.lingxiagent.model.meeting.repository.MeetingRepositoryImpl;
import com.fxzs.lingxiagent.model.meeting.repository.StreamProgressCallback;
import com.fxzs.lingxiagent.util.BillDialogHelper;
import com.fxzs.lingxiagent.view.meeting.MeetingTabFragment.MeetingContentDto;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class VMMeeting extends BaseViewModel {
    
    // Repository
    private final MeetingRepository repository;
    
    // Observable fields for UI binding
    private final ObservableField<String> meetingTitle = new ObservableField<>("");
    private final ObservableField<String> transcriptionText = new ObservableField<>("");
    private final ObservableField<Boolean> isRecording = new ObservableField<>(false);
    private final ObservableField<LanguageDto> selectedLanguageDto = new ObservableField<>();
    private final ObservableField<String> selectedLanguage = new ObservableField<>("zh-CN");
    private final ObservableField<Boolean> startButtonEnabled = new ObservableField<>(true);
    
    // Meeting list
    private final ObservableField<List<MeetingDto>> meetingList = new ObservableField<>(new ArrayList<>());
    private final ObservableField<Boolean> hasMoreMeetings = new ObservableField<>(true);
    private int currentPage = 1;
    private static final int PAGE_SIZE = 20;
    
    // Current meeting
    private final ObservableField<MeetingDto> currentMeeting = new ObservableField<>();
    
    // Language options
    private final ObservableField<List<LanguageDto>> realTimeLanguages = new ObservableField<>();
    private final ObservableField<List<LanguageDto>> audioFileLanguages = new ObservableField<>();
    
    // Meeting summary
    private final ObservableField<MeetingSummaryDto> meetingSummary = new ObservableField<>();
    
    // Meeting summary result for UI
    private final ObservableField<MeetingSummaryDto> meetingSummaryResult = new ObservableField<>();
    
    // Real-time transcription
    private final ObservableField<TranscriptionDto> latestTranscription = new ObservableField<>();
    private final StringBuilder fullTranscription = new StringBuilder();
    
    // Flag to track initial load
    private boolean isInitialLoad = true;
    
    // Meeting content for display
    private final ObservableField<MeetingContentDto> meetingContent = new ObservableField<>();
    
    // 录音文件路径
    private String currentRecordingPath;
    private String savedRecordingPath;
    
    // 转写结果
    private String transcriptionResult;
    private String audioFilePath;
    private Integer meetingId;

    private final MutableLiveData<String> showBillDialogEvent =
            new MutableLiveData<>();

    public LiveData<String> getShowBillDialogEvent() {
        return showBillDialogEvent;
    }

    public VMMeeting(@NonNull Application application) {
        super(application);
        repository = new MeetingRepositoryImpl();
        initializeData();
        setupValidation();
    }
    
    private void initializeData() {
        loadLanguages();
        loadMeetingList(true);
    }
    
    private void setupValidation() {
        // Enable/disable start button based on title
        meetingTitle.observeForever(title -> {
            boolean isValid = title != null && !title.trim().isEmpty();
            startButtonEnabled.set(isValid && !isRecording.get());
        });
        
        // Update button state when recording status changes
        isRecording.observeForever(recording -> {
            boolean titleValid = meetingTitle.get() != null && !meetingTitle.get().trim().isEmpty();
            startButtonEnabled.set(titleValid && !recording);
        });
    }
    
    // Start/stop recording
    public void toggleRecording() {
        if (isRecording.get()) {
            stopRecording();
        } else {
            startRecording();
        }
    }
    
    private void startRecording() {
        String title = meetingTitle.get();
        if (title == null || title.trim().isEmpty()) {
            setError("请输入会议名称");
            return;
        }
        
        // Create new meeting and save to backend
        MeetingDto newMeeting = new MeetingDto();
        newMeeting.setTitle(title.trim());
        newMeeting.setLanguage(selectedLanguage.get());
        newMeeting.setType(0); // Real-time
        newMeeting.setStatus(0); // Processing
        
        setLoading(true);
        repository.addMeeting(newMeeting).observeForever(result -> {
            setLoading(false);
            if (result != null && result.isSuccess()) {
                currentMeeting.postValue(result.getData());
                isRecording.set(true);
                fullTranscription.setLength(0);
                transcriptionText.set("");
            } else {
                setError(result != null ? result.getError() : "创建会议失败");
            }
        });
    }
    
    private void stopRecording() {
        isRecording.set(false);
        MeetingDto meeting = currentMeeting.get();
        if (meeting != null) {
            meeting.setContent(fullTranscription.toString());
            meeting.setStatus(1); // Completed
            
            setLoading(true);
            repository.updateMeeting(meeting).observeForever(result -> {
                setLoading(false);
                if (result != null && result.isSuccess()) {
                    // Generate summary
                    generateSummary(fullTranscription.toString());
                    // Refresh meeting list
                    loadMeetingList(true);
                } else {
                    setError(result != null ? result.getError() : "保存会议失败");
                }
            });
        }
    }
    
    // Process audio data for transcription
    public void processAudioData(byte[] audioData) {
        if (!isRecording.get() || audioData == null) {
            return;
        }
        
        repository.startRealTimeTranscription(audioData).observeForever(result -> {
            if (result != null && result.isSuccess()) {
                TranscriptionDto transcription = result.getData();
                if (transcription != null && transcription.getText() != null) {
                    latestTranscription.postValue(transcription);
                    
                    // Append to full transcription
                    if (transcription.getIsFinal() != null && transcription.getIsFinal()) {
                        fullTranscription.append(transcription.getText()).append(" ");
                        transcriptionText.set(fullTranscription.toString());
                    }
                }
            }
        });
    }
    
    // Generate meeting summary
    private void generateSummary(String content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        
        repository.generateMeetingSummary(content).observeForever(result -> {
            if (result != null && result.isSuccess()) {
                MeetingSummaryDto summary = result.getData();
                if (summary != null) {
                    meetingSummary.postValue(summary);
                    
                    // Update current meeting with summary
                    MeetingDto meeting = currentMeeting.get();
                    if (meeting != null && summary.getSummary() != null) {
                        meeting.setSummary(summary.getSummary());
                        repository.updateMeeting(meeting);
                    }
                }
            }
        });
    }
    
    // Generate meeting summary for UI
    public void generateMeetingSummary(String content, Integer meetingId) {
        if (content == null || content.isEmpty()) {
            setError("会议内容为空，无法生成摘要");
            return;
        }
        
        if (meetingId == null) {
            setError("会议ID无效");
            return;
        }
        
        // 测试token有效性
        Timber.tag("VMMeeting").i( "开始生成会议摘要 - 会议ID: " + meetingId + ", 内容长度: " + content.length());
        repository.getMyMeetingList(1, 1).observeForever(testResult -> {
            if (testResult != null && testResult.isSuccess()) {
                Timber.tag("VMMeeting").i( "Token验证通过，开始摘要生成");
            } else {
                Timber.tag("VMMeeting").d( "Token验证失败: " + (testResult != null ? testResult.getError() : "未知错误"));
            }
        });
        
        setLoading(true);
        
        // 初始化一个空的摘要结果，用于实时更新
        MeetingSummaryDto realtimeSummary = new MeetingSummaryDto();
        realtimeSummary.setSummary("");
        // 不设置空列表，让它们保持null，这样UI才能识别为流式传输状态
        // 使用ObservableField的set方法确保主线程更新
        Timber.tag("VMMeeting").d( "初始化空摘要，准备set");
        meetingSummaryResult.set(realtimeSummary);
        Timber.tag("VMMeeting").d("初始摘要已设置");
        
        // 使用带进度回调的方法，默认使用会议记录类型
        repository.generateMeetingSummaryWithProgress(content, meetingId, "1", new StreamProgressCallback() {
            @Override
            public void onChunkReceived(String chunk, String fullContent) {
                // 实时更新UI显示 - 每次创建新对象避免对象重用问题
                MeetingSummaryDto currentSummary = new MeetingSummaryDto();
                currentSummary.setSummary(fullContent);
                // 保持其他字段为null，表示流式传输中
                
                // 参考StreamActivity的成功实现，确保在主线程更新ObservableField
                boolean isMainThread = android.os.Looper.myLooper() == android.os.Looper.getMainLooper();
                Timber.tag("VMMeeting").d( "当前线程: " + Thread.currentThread().getName() + 
                    ", 是否主线程: " + isMainThread);
                
                if (isMainThread) {
                    // 主线程直接更新ObservableField
                    meetingSummaryResult.set(currentSummary);
                    Timber.tag("VMMeeting").i( "使用set更新ObservableField（主线程）");
                } else {
                    // 非主线程，切换到主线程更新ObservableField
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        meetingSummaryResult.set(currentSummary);
                        Timber.tag("VMMeeting").i( "通过Handler.post更新ObservableField（非主线程）");
                    });
                }
                
                // 输出调试信息，检查数据是否正确
                Timber.tag("VMMeeting").i( "摘要实时更新 - 已生成: " + fullContent.length() + " 字符");
                Timber.tag("VMMeeting").i( "摘要内容预览: " + 
                    (fullContent.length() > 200 ? fullContent.substring(0, 200) + "..." : fullContent));
                Timber.tag("VMMeeting").i( "chunk内容: " + (chunk != null ? chunk : "null"));
                Timber.tag("VMMeeting").i( "fullContent是否为空: " + (fullContent == null || fullContent.trim().isEmpty()));
                Timber.tag("VMMeeting").i( "currentSummary内容长度: " + 
                    (currentSummary.getSummary() != null ? currentSummary.getSummary().length() : 0));
            }
            
            @Override
            public void onStreamComplete(int totalChunks) {
                setLoading(false);

                // 确保最终内容得到更新
                MeetingSummaryDto finalSummary = meetingSummaryResult.get();
                Timber.tag("VMMeeting").i( "流完成 - 最终摘要: " + 
                    (finalSummary != null ? "存在" : "null"));
                if (finalSummary != null) {
                    Timber.tag("VMMeeting").i( "发送最终摘要更新");
                    // 使用相同的主线程更新机制
                    boolean isMainThread = android.os.Looper.myLooper() == android.os.Looper.getMainLooper();
                    if (isMainThread) {
                        meetingSummaryResult.set(finalSummary);
                    } else {
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            meetingSummaryResult.set(finalSummary);
                        });
                    }
                }
                
                // 输出摘要生成完成的关键信息
                Timber.tag("VMMeeting").i( "摘要生成完成 - 总片段数: " + totalChunks);
                if (finalSummary != null && finalSummary.getSummary() != null) {
                    Timber.tag("VMMeeting").i( "最终摘要长度: " + finalSummary.getSummary().length() + " 字符");
                    Timber.tag("VMMeeting").i( "最终摘要内容: " + finalSummary.getSummary());
                }
            }
            
            @Override
            public void onStreamError(String error) {
                setLoading(false);
                setError(error);
            }
        }).observeForever(result -> {
            if (result != null && result.isSuccess()) {
                MeetingSummaryDto summary = result.getData();
                if (summary != null) {
                    // 流式传输已经在onChunkReceived中实时更新了UI
                    // 这里输出最终结果的关键信息
                    Timber.tag("VMMeeting").i( "流式处理最终结果 - 摘要长度: " + 
                        (summary.getSummary() != null ? summary.getSummary().length() : 0));
                    // 不覆盖流式更新的内容
                }
            }
        });
    }
    
    // Load meeting list
    public void loadMeetingList(boolean refresh) {
        if (refresh) {
            currentPage = 1;
            isInitialLoad = false; // 用户主动刷新不算初始加载
        }
        
        setLoading(true);
        repository.getMyMeetingList(currentPage, PAGE_SIZE).observeForever(result -> {
            setLoading(false);
            if (result != null && result.isSuccess()) {
                MeetingListDto listDto = result.getData();
                if (listDto != null && listDto.getList() != null) {
                    List<MeetingDto> currentList = meetingList.get();
                    if (currentList == null) {
                        currentList = new ArrayList<>();
                    }
                    
                    if (refresh) {
                        currentList.clear();
                    }
                    currentList.addAll(listDto.getList());
                    meetingList.postValue(currentList);
                    
                    // Check if there are more pages
                    hasMoreMeetings.postValue(listDto.getList().size() >= PAGE_SIZE);
                    currentPage++;
                    isInitialLoad = false; // 成功加载后标记为非初始加载
                } else {
                    // 只在非初始加载时显示错误toast
                    if (!isInitialLoad) {
                        setError("加载会议列表失败");
                    } else {
                        // 初始加载失败只记录日志
                        Timber.tag("VMMeeting").e( "初始加载会议列表失败: 数据为空");
                    }
                }
            } else {
                // 只在非初始加载时显示错误toast
                if (!isInitialLoad) {
                    setError(result != null ? result.getError() : "加载会议列表失败");
                } else {
                    // 初始加载失败只记录日志
                    Timber.tag("VMMeeting").e( "初始加载会议列表失败: " + (result != null ? result.getError() : "未知错误"));
                }
            }
        });
    }
    
    // Load more meetings
    public void loadMoreMeetings() {
        if (Boolean.TRUE.equals(hasMoreMeetings.get()) && !Boolean.TRUE.equals(getLoading().getValue())) {
            loadMeetingList(false);
        }
    }
    
    // Delete meeting
    public void deleteMeeting(String meetingId) {
        setLoading(true);
        repository.deleteMeeting(meetingId).observeForever(result -> {
            setLoading(false);
            if (result != null && result.isSuccess()) {
                setSuccess("删除成功");
                loadMeetingList(true);
            } else {
                setError(result != null ? result.getError() : "删除失败");
            }
        });
    }
    
    // Load languages
    public void loadLanguages() {
        Timber.tag("VMMeeting").i( "开始加载语言列表");
        
        // 从API获取实时会议支持的语言列表
        repository.getSttLanguage().observeForever(result -> {

            if (result != null && result.isSuccess()) {
                List<LanguageDto> languages = result.getData();
                if (languages != null && !languages.isEmpty()) {
                    Timber.tag("VMMeeting").i( "实时语言列表加载成功，数量: " + languages.size());
                    for (LanguageDto lang : languages) {
                        Timber.tag("VMMeeting").i( "实时语言: " + lang.getName() + " (" + lang.getCode() + ")");
                    }
                    realTimeLanguages.postValue(languages);
                    selectedLanguageDto.postValue(languages.get(0));
                    setError(null); // 清除错误状态
                } else {
                    Timber.tag("VMMeeting").e( "实时语言列表为空");
                    realTimeLanguages.postValue(new ArrayList<>());
                }
            } else {
                Timber.tag("VMMeeting").e( "实时语言列表加载失败: " + (result != null ? result.getCode() : "未知错误"));
                realTimeLanguages.postValue(new ArrayList<>());
                if (BenefitCode.isBenefitError(result.getCode())) {
                    showBillDialogEvent.postValue(result.getError());
                } else {
                    setError("网络连接失败，无法获取语音识别语言列表");
                }
            }
        });
        
        // 从API获取音频文件转写支持的语言列表
        repository.getSttLanguage().observeForever(result -> {

            String json = new Gson().toJson(result);
            if (result != null && result.isSuccess()) {
                List<LanguageDto> languages = result.getData();
                if (languages != null && !languages.isEmpty()) {
                    Timber.tag("VMMeeting").i( "音频文件语言列表加载成功，数量: " + languages.size());
                    for (LanguageDto lang : languages) {
                        Timber.tag("VMMeeting").i( "音频文件语言: " + lang.getName() + " (" + lang.getCode() + ")");
                    }
                    audioFileLanguages.postValue(languages);
                } else {
                    Timber.tag("VMMeeting").e( "音频文件语言列表为空");
                    audioFileLanguages.postValue(new ArrayList<>());
                }
            } else {
                Timber.tag("VMMeeting").e( "音频文件语言列表加载失败: " + (result != null ? result.getCode() : "未知错误"));
                audioFileLanguages.postValue(new ArrayList<>());
            }
        });
    }
    
    // Getters for observable fields
    public ObservableField<String> getMeetingTitle() {
        return meetingTitle;
    }
    
    public ObservableField<String> getTranscriptionText() {
        return transcriptionText;
    }
    
    public ObservableField<Boolean> getIsRecording() {
        return isRecording;
    }
    
    public ObservableField<String> getSelectedLanguage() {
        return selectedLanguage;
    }

    public ObservableField<LanguageDto> getSelectedLanguageDto() {
        return selectedLanguageDto;
    }

    public ObservableField<Boolean> getStartButtonEnabled() {
        return startButtonEnabled;
    }
    
    public ObservableField<List<MeetingDto>> getMeetingList() {
        return meetingList;
    }
    
    public ObservableField<Boolean> getHasMoreMeetings() {
        return hasMoreMeetings;
    }
    
    public ObservableField<MeetingDto> getCurrentMeeting() {
        return currentMeeting;
    }
    
    public ObservableField<List<LanguageDto>> getRealTimeLanguages() {
        return realTimeLanguages;
    }
    
    public ObservableField<MeetingSummaryDto> getMeetingSummary() {
        return meetingSummary;
    }
    
    public ObservableField<MeetingSummaryDto> getMeetingSummaryResult() {
        return meetingSummaryResult;
    }
    
    public ObservableField<TranscriptionDto> getLatestTranscription() {
        return latestTranscription;
    }
    
    public ObservableField<MeetingContentDto> getMeetingContent() {
        return meetingContent;
    }
    
    // Load meeting content for display
    public void loadMeetingContent(String meetingId) {
        if (meetingId == null) {
            setError("会议ID无效");
            return;
        }
        
        // 不加载假数据，只有当真实的语音识别结果通过 setTranscriptionResult 设置时才会有内容
        // 实际的会议内容将通过语音识别结果填充
    }
    
    // Refresh meeting content
    public void refreshMeetingContent() {
        MeetingDto meeting = currentMeeting.get();
        if (meeting != null && meeting.getId() != null) {
            loadMeetingContent(meeting.getId());
        } else {
            setError("没有可刷新的内容");
        }
    }
    
    // 设置当前录音文件路径
    public void setCurrentRecordingPath(String path) {
        this.currentRecordingPath = path;
    }
    
    // 设置保存的录音文件路径
    public void setRecordingFilePath(String path) {
        this.savedRecordingPath = path;
        
        // 如果有当前会议，更新会议信息
        MeetingDto meeting = currentMeeting.get();
        if (meeting != null) {
            meeting.setAudioFilePath(path);
            currentMeeting.postValue(meeting);
        }
    }
    
    // 获取录音文件路径
    public String getRecordingFilePath() {
        return savedRecordingPath;
    }
    
    // 设置转写结果
    public void setTranscriptionResult(String result) {
        this.transcriptionResult = result;
        transcriptionText.set(result);
        
        // 只更新转写内容，不设置其他假数据
        MeetingContentDto content = meetingContent.get();
        if (content == null) {
            content = new MeetingContentDto();
        }
        content.setTranscription(result);
        // 不设置假的摘要、话题、问答数据
        meetingContent.postValue(content);
    }
    
    // 获取转写结果
    public String getTranscriptionResult() {
        return transcriptionResult;
    }
    
    // 设置音频文件路径
    public void setAudioFilePath(String path) {
        this.audioFilePath = path;
    }
    
    // 获取音频文件路径
    public String getAudioFilePath() {
        return audioFilePath;
    }

    // 设置会议ID
    public void setMeetingId(Integer meetingId) {
        this.meetingId = meetingId;
    }

    // 获取会议ID
    public Integer getMeetingId() {
        return meetingId;
    }
    
    // 处理模拟语音识别结果
    public void processMockTranscription(String mockResult) {
        if (mockResult == null || mockResult.isEmpty()) {
            Timber.tag("VMMeeting").w("语音识别结果为空");
            return;
        }
        
        Timber.tag("VMMeeting").i( "处理模拟语音识别结果: " + mockResult);
        
        // 将识别结果追加到完整转写文本中
        fullTranscription.append(mockResult).append("\n");
        String fullText = fullTranscription.toString();
        
        // 更新转写文本显示
        transcriptionText.postValue(fullText);
        
        // 更新会议内容
        setTranscriptionResult(fullText);
        
        // 如果有当前会议，更新会议的内容
        MeetingDto meeting = currentMeeting.get();
        if (meeting != null) {
            meeting.setContent(fullText);
            currentMeeting.postValue(meeting);
        }
        
        Timber.tag("VMMeeting").i( "语音识别结果已处理，当前总长度: " + fullText.length());
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        meetingTitle.removeObserver(title -> {});
        isRecording.removeObserver(recording -> {});
    }
}