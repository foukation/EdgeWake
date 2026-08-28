package com.fxzs.lingxiagent.viewmodel.meeting;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.common.ObservableField;
import com.fxzs.lingxiagent.model.meeting.dto.MeetingDto;
import com.fxzs.lingxiagent.model.meeting.repository.MeetingRepository;
import com.fxzs.lingxiagent.model.meeting.repository.MeetingRepositoryImpl;
import com.fxzs.lingxiagent.model.meeting.repository.StreamProgressCallback;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;

import timber.log.Timber;

public class VMMeetingTopic extends BaseViewModel {
    private static final String TAG = "VMMeetingTopic";

    private final MeetingRepository repository;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // 话题内容
    private final ObservableField<String> topicsContent = new ObservableField<>("");
    private final MutableLiveData<String> topicsResult = new MutableLiveData<>();

    // 当前会议ID
    private String currentMeetingId;

    // 当前转写内容
    private String currentTranscriptionResult;

    // 刷新状态
    private final MutableLiveData<Boolean> isRefreshing = new MutableLiveData<>(false);

    public VMMeetingTopic(Application application) {
        super(application);
        this.repository = new MeetingRepositoryImpl();
    }
    
    public ObservableField<String> getTopicsContent() {
        return topicsContent;
    }

    public LiveData<String> getTopicsResult() {
        return topicsResult;
    }

    public LiveData<Boolean> getIsRefreshing() {
        return isRefreshing;
    }

    /**
     * 初始化话题内容
     * @param meetingId 会议ID
     * @param transcriptionResult 转写内容
     */
    public void initializeTopics(String meetingId, String transcriptionResult) {
        this.currentMeetingId = meetingId;
        this.currentTranscriptionResult = transcriptionResult;

        Timber.tag(TAG).d( "初始化话题内容 - meetingId: " + meetingId);

        // 首先检查会议详情中是否已有话题内容
        checkExistingTopics();
    }

    /**
     * 检查现有话题内容
     */
    private void checkExistingTopics() {
        if (currentMeetingId == null || currentMeetingId.trim().isEmpty()) {
            Timber.tag(TAG).w( "会议ID为空，无法检查现有话题");
            generateNewTopics(false);
            return;
        }

        setLoading(true);

        // 先检查本地缓存
        String cachedTopic = SharedPreferencesUtil.getMeetingTopic(currentMeetingId);
        if (!cachedTopic.isEmpty()) {
            Timber.tag(TAG).d( "找到本地缓存的话题内容，长度: " + cachedTopic.length());
            displayTopics(cachedTopic);
            setLoading(false);
            return;
        }

        // 从服务器获取会议详情
        repository.getMeetingDetail(currentMeetingId).observeForever(result -> {
            handler.post(() -> {
                setLoading(false);

                if (result != null && result.isSuccess() && result.getData() != null) {
                    MeetingDto meeting = result.getData();
                    String existingTopic = meeting.getTopic();

                    if (existingTopic != null && !existingTopic.trim().isEmpty()) {
                        Timber.tag(TAG).d( "服务器已有话题内容，长度: " + existingTopic.length());
                        // 保存到本地缓存
                        SharedPreferencesUtil.saveMeetingTopic(currentMeetingId, existingTopic);
                        displayTopics(existingTopic);
                    } else {
                        Timber.tag(TAG).d( "服务器无话题内容，开始生成");
                        generateNewTopics(false);
                    }
                } else {
                    Timber.tag(TAG).e( "获取会议详情失败: " + (result != null ? result.getError() : "未知错误"));
                    generateNewTopics(false);
                }
            });
        });
    }

    /**
     * 刷新话题内容（强制重新生成）
     */
    public void refreshTopics() {
        Timber.tag(TAG).d( "刷新话题内容");
        isRefreshing.setValue(true);

        // 清除本地缓存
        if (currentMeetingId != null) {
            SharedPreferencesUtil.clearMeetingTopic(currentMeetingId);
        }

        generateNewTopics(true);
    }

    /**
     * 显示话题内容
     */
    private void displayTopics(String topicContent) {
        topicsContent.set(topicContent);
        topicsResult.setValue(topicContent);
        Timber.tag(TAG).d( "显示话题内容，长度: " + topicContent.length());
    }

    /**
     * 生成新的话题内容
     */
    private void generateNewTopics(boolean isRefresh) {
        if (currentTranscriptionResult == null || currentTranscriptionResult.trim().isEmpty()) {
            String errorMsg = "转写内容为空，无法生成话题";
            Timber.tag(TAG).w( errorMsg);
            setError(errorMsg);
            if (isRefresh) {
                isRefreshing.setValue(false);
            }
            return;
        }

        Integer meetingIdInt = null;
        if (currentMeetingId != null && !currentMeetingId.trim().isEmpty()) {
            try {
                meetingIdInt = Integer.valueOf(currentMeetingId);
            } catch (NumberFormatException e) {
                Timber.tag(TAG).w( "会议ID格式错误: " + currentMeetingId);
            }
        }

        Timber.tag(TAG).d( "开始生成新话题 - meetingId: " + meetingIdInt + ", 内容长度: " + currentTranscriptionResult.length());

        if (!isRefresh) {
            setLoading(true);
        }
        topicsContent.set("正在生成会议话题...");

        generateTopicsInternal(currentTranscriptionResult, meetingIdInt, isRefresh);
    }

    /**
     * 生成会议话题
     * @param transcriptionResult 转写内容
     * @param meetingId 会议ID
     */
    public void generateTopics(String transcriptionResult, Integer meetingId) {
        // 兼容旧版本调用，直接生成话题
        this.currentMeetingId = meetingId != null ? meetingId.toString() : null;
        this.currentTranscriptionResult = transcriptionResult;
        generateNewTopics(false);
    }

    /**
     * 内部话题生成方法
     */
    private void generateTopicsInternal(String transcriptionResult, Integer meetingId, boolean isRefresh) {
        if (transcriptionResult == null || transcriptionResult.trim().isEmpty()) {
            setError("转写内容为空，无法提取话题");
            if (isRefresh) {
                isRefreshing.setValue(false);
            }
            return;
        }

        Timber.tag(TAG).d( "开始生成会议话题 - meetingId: " + meetingId + ", 内容长度: " + transcriptionResult.length());

        // 调用Repository生成会议话题，使用botKey=2
        repository.generateMeetingSummaryWithProgress(transcriptionResult, meetingId, "2", new StreamProgressCallback() {
            @Override
            public void onChunkReceived(String chunk, String accumulatedContent) {
                // 切换到主线程更新UI
                handler.post(() -> {
                    Timber.tag(TAG).d( "收到流式数据 - chunk长度: " + (chunk != null ? chunk.length() : 0) +
                        ", 累积内容长度: " + (accumulatedContent != null ? accumulatedContent.length() : 0));

                    // 打印部分内容用于调试
                    if (accumulatedContent != null && accumulatedContent.length() > 0) {
                        String preview = accumulatedContent.substring(0, Math.min(200, accumulatedContent.length()));
                        Timber.tag(TAG).d( "累积内容预览: " + preview + "...");
                    }

                    // 实时更新内容到LiveData，让Fragment能够观察到
                    if (accumulatedContent != null && !accumulatedContent.trim().isEmpty()) {
                        topicsContent.set(accumulatedContent);
                        // 同时更新LiveData以便Fragment实时观察
                        topicsResult.setValue(accumulatedContent);
                        Timber.tag(TAG).d( "已更新topicsResult，内容长度: " + accumulatedContent.length());
                    }
                });
            }

            @Override
            public void onStreamComplete(int totalChunks) {
                handler.post(() -> {
                    Timber.tag(TAG).d( "话题生成流式完成，总片段数: " + totalChunks);
                });
            }

            @Override
            public void onStreamError(String error) {
                handler.post(() -> {
                    Timber.tag(TAG).e( "话题生成流式错误: " + error);
                    setLoading(false);
                    setError("话题生成失败: " + error);
                });
            }
        }).observeForever(result -> {
            handler.post(() -> {
                setLoading(false);
                if (isRefresh) {
                    isRefreshing.setValue(false);
                }

                if (result != null && result.isSuccess()) {
                    Timber.tag(TAG).d( "会议话题生成成功");

                    // 直接使用流式返回的最终内容，不进行额外格式化
                    String currentContent = topicsContent.get();
                    if (currentContent != null && !currentContent.trim().isEmpty() &&
                        !currentContent.equals("正在生成会议话题...")) {

                        Timber.tag(TAG).d( "使用流式返回的最终内容，长度: " + currentContent.length());

                        // 保存到本地缓存
                        if (currentMeetingId != null) {
                            SharedPreferencesUtil.saveMeetingTopic(currentMeetingId, currentContent);
                            Timber.tag(TAG).d( "已保存话题到本地缓存");
                        }

                        // 更新服务器
                        if (meetingId != null) {
                            updateMeetingTopicOnServer(meetingId, currentContent);
                        }

                        // 最终确认更新（通常流式过程中已经更新了）
                        topicsResult.setValue(currentContent);
                    }

                } else {
                    String errorMsg = result != null ? result.getError() : "未知错误";
                    Timber.tag(TAG).e( "会议话题生成失败: " + errorMsg);
                    setError("话题生成失败: " + errorMsg);

                    // 设置默认内容
                    topicsContent.set("当前未检测到有效会议内容，暂时无法提取会议话题");
                }
            });
        });
    }



    /**
     * 更新服务器上的会议话题
     */
    public void updateMeetingTopicOnServer(Integer meetingId, String topicContent) {
        Timber.tag(TAG).d( "开始更新服务器会议话题 - meetingId: " + meetingId);

        repository.updateMeetingTopic(meetingId, topicContent).observeForever(result -> {
            handler.post(() -> {
                if (result != null && result.isSuccess()) {
                    Timber.tag(TAG).d( "成功更新服务器会议话题");
                } else {
                    String errorMsg = result != null ? result.getError() : "未知错误";
                    Timber.tag(TAG).e( "更新服务器会议话题失败: " + errorMsg);
                    // 不显示错误给用户，因为本地已经有内容了
                }
            });
        });
    }

    /**
     * 生成会议话题（兼容旧版本，不带meetingId）
     */
    public void generateTopics(String transcriptionResult) {
        generateTopics(transcriptionResult, null);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
    }
}