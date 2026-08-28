package com.fxzs.lingxiagent.model.meeting.repository;

import androidx.lifecycle.LiveData;

import com.fxzs.lingxiagent.model.drawing.repository.DrawingRepository.Result;
import com.fxzs.lingxiagent.model.meeting.dto.AudioUploadDto;
import com.fxzs.lingxiagent.model.meeting.dto.LanguageDto;
import com.fxzs.lingxiagent.model.meeting.dto.MeetingDto;
import com.fxzs.lingxiagent.model.meeting.dto.MeetingHistoryListDto;
import com.fxzs.lingxiagent.model.meeting.dto.MeetingListDto;
import com.fxzs.lingxiagent.model.meeting.dto.MeetingSummaryDto;
import com.fxzs.lingxiagent.model.meeting.dto.SoundRecordTaskResponseDto;
import com.fxzs.lingxiagent.model.meeting.dto.TranscriptionDto;

import java.io.File;
import java.util.List;

public interface MeetingRepository {
    // Real-time voice to text
    LiveData<Result<TranscriptionDto>> startRealTimeTranscription(byte[] audioData);
    
    // Meeting management
    LiveData<Result<MeetingDto>> addMeeting(MeetingDto meeting);
    LiveData<Result<String>> updateMeeting(MeetingDto meeting);
    LiveData<Result<Void>> deleteMeeting(String meetingId);
    LiveData<Result<MeetingDto>> getMeetingDetail(String meetingId);
    LiveData<Result<MeetingListDto>> getMyMeetingList(int page, int size);
    
    // Audio file upload and recognition
    LiveData<Result<AudioUploadDto>> uploadAudioFile(File audioFile, String language);
    LiveData<Result<TranscriptionDto>> queryRecognitionResult(String taskId);
    
    // Meeting summary
    LiveData<Result<MeetingSummaryDto>> generateMeetingSummary(String content);
    
    // Meeting summary for UI
    LiveData<Result<MeetingSummaryDto>> generateMeetingSummaryForUI(String content, Integer meetingId);
    
    // Meeting summary with stream progress callback
    LiveData<Result<MeetingSummaryDto>> generateMeetingSummaryWithProgress(String content, Integer meetingId, String botKey, StreamProgressCallback callback);
    
    // Get meeting info by ID
    LiveData<Result<MeetingDto>> getMeetingInfoById(Integer meetingId);
    
    // Update meeting record
    LiveData<Result<String>> updateMeetingRecord(Integer meetingId, String summaryType, String summaryContent);

    // Update meeting topic
    LiveData<Result<String>> updateMeetingTopic(Integer meetingId, String topicContent);
    LiveData<Result<String>> updateMeetingName(Integer meetingId, String name);
    LiveData<Result<String>> bindMeetingAndConversationId(String meetingId, String conversionId);

    // Language options
    LiveData<Result<List<LanguageDto>>> getRealTimeLanguages();
    LiveData<Result<List<LanguageDto>>> getAudioFileLanguages();
    /** 获取会议离线STT语言列表（新版接口） */
    LiveData<Result<List<LanguageDto>>> getSttLanguage();

    // Get offline engine model types
    LiveData<Result<java.util.Map<String, String>>> getOfflineEngineModelType();

    // Get meeting history list
    LiveData<Result<MeetingHistoryListDto>> getMeetingHistoryList(java.util.Map<String, Object> params);

    // Audio recognition tasks
    LiveData<Result<SoundRecordTaskResponseDto>> submitAudioRecognitionTask(String fileUrl, String language, String meetingId);
    LiveData<Result<SoundRecordTaskResponseDto>> queryAudioRecognitionResult(String taskId, String meetingId);
}