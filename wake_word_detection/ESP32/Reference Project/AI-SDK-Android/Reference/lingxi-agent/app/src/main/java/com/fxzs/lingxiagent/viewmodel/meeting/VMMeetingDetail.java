package com.fxzs.lingxiagent.viewmodel.meeting;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.common.ObservableField;
import com.fxzs.lingxiagent.model.meeting.dto.MeetingDto;
import com.fxzs.lingxiagent.model.meeting.dto.MeetingSummaryDto;
import com.fxzs.lingxiagent.model.meeting.repository.MeetingRepository;
import com.fxzs.lingxiagent.model.meeting.repository.MeetingRepositoryImpl;

public class VMMeetingDetail extends BaseViewModel {
    
    private final MeetingRepository repository;
    
    // Meeting details
    private final MutableLiveData<MeetingDto> meetingDetail = new MutableLiveData<>();
    private final MutableLiveData<MeetingSummaryDto> meetingSummary = new MutableLiveData<>();
    
    // Observable fields for UI
    private final ObservableField<String> meetingTitle = new ObservableField<>("");
    private final ObservableField<String> meetingContent = new ObservableField<>("");
    private final ObservableField<String> meetingSummaryText = new ObservableField<>("");
    private final ObservableField<String> meetingDate = new ObservableField<>("");
    private final ObservableField<String> meetingDuration = new ObservableField<>("");
    private final ObservableField<Boolean> hasSummary = new ObservableField<>(false);
    
    // Current tab selection
    private final ObservableField<Integer> selectedTab = new ObservableField<>(0);
    
    private String currentMeetingId;
    
    public VMMeetingDetail(@NonNull Application application) {
        super(application);
        repository = new MeetingRepositoryImpl();
    }
    
    // Load meeting details
    public void loadMeetingDetail(String meetingId) {
        if (meetingId == null || meetingId.isEmpty()) {
            setError("会议ID不能为空");
            return;
        }
        
        currentMeetingId = meetingId;
        setLoading(true);
        
        repository.getMeetingDetail(meetingId).observeForever(result -> {
            setLoading(false);
            if (result != null && result.isSuccess()) {
                MeetingDto meeting = result.getData();
                if (meeting != null) {
                    meetingDetail.setValue(meeting);
                    updateUI(meeting);
                    
                    // Generate summary if not exists
                    if (meeting.getSummary() == null || meeting.getSummary().isEmpty()) {
                        generateSummary(meeting.getContent());
                    }
                } else {
                    setError("加载会议详情失败");
                }
            } else {
                setError(result != null ? result.getError() : "加载会议详情失败");
            }
        });
    }
    
    // Update UI fields
    private void updateUI(MeetingDto meeting) {
        meetingTitle.set(meeting.getTitle() != null ? meeting.getTitle() : "");
        meetingContent.set(meeting.getContent() != null ? meeting.getContent() : "");
        meetingSummaryText.set(meeting.getSummary() != null ? meeting.getSummary() : "");
        meetingDate.set(meeting.getCreateTime() != null ? meeting.getCreateTime() : "");
        meetingDuration.set(meeting.getDuration() != null ? meeting.getDuration() : "");
        hasSummary.set(meeting.getSummary() != null && !meeting.getSummary().isEmpty());
    }
    
    // Generate meeting summary
    public void generateSummary(String content) {
        if (content == null || content.isEmpty()) {
            setError("会议内容为空，无法生成摘要");
            return;
        }
        
        setLoading(true);
        repository.generateMeetingSummary(content).observeForever(result -> {
            setLoading(false);
            if (result != null && result.isSuccess()) {
                MeetingSummaryDto summary = result.getData();
                if (summary != null) {
                    meetingSummary.setValue(summary);
                    if (summary.getSummary() != null) {
                        meetingSummaryText.set(summary.getSummary());
                        hasSummary.set(true);
                        
                        // Update meeting with summary
                        MeetingDto meeting = meetingDetail.getValue();
                        if (meeting != null) {
                            meeting.setSummary(summary.getSummary());
                            repository.updateMeeting(meeting);
                        }
                    }
                } else {
                    setError("生成摘要失败");
                }
            } else {
                setError(result != null ? result.getError() : "生成摘要失败");
            }
        });
    }
    
    // Regenerate summary
    public void regenerateSummary() {
        MeetingDto meeting = meetingDetail.getValue();
        if (meeting != null && meeting.getContent() != null) {
            generateSummary(meeting.getContent());
        }
    }
    
    // Export meeting content
    public void exportMeeting() {
        // TODO: Implement export functionality
        setSuccess("导出功能正在开发中");
    }
    
    // Share meeting
    public void shareMeeting() {
        // TODO: Implement share functionality
        setSuccess("分享功能正在开发中");
    }
    
    // Delete meeting
    public void deleteMeeting() {
        if (currentMeetingId == null) {
            return;
        }
        
        setLoading(true);
        repository.deleteMeeting(currentMeetingId).observeForever(result -> {
            setLoading(false);
            if (result != null && result.isSuccess()) {
                setSuccess("删除成功");
                // Navigation back will be handled by Activity
            } else {
                setError(result != null ? result.getError() : "删除失败");
            }
        });
    }
    
    // Getters
    public LiveData<MeetingDto> getMeetingDetail() {
        return meetingDetail;
    }
    
    public LiveData<MeetingSummaryDto> getMeetingSummary() {
        return meetingSummary;
    }
    
    public ObservableField<String> getMeetingTitle() {
        return meetingTitle;
    }
    
    public ObservableField<String> getMeetingContent() {
        return meetingContent;
    }
    
    public ObservableField<String> getMeetingSummaryText() {
        return meetingSummaryText;
    }
    
    public ObservableField<String> getMeetingDate() {
        return meetingDate;
    }
    
    public ObservableField<String> getMeetingDuration() {
        return meetingDuration;
    }
    
    public ObservableField<Boolean> getHasSummary() {
        return hasSummary;
    }
    
    public ObservableField<Integer> getSelectedTab() {
        return selectedTab;
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
    }
}