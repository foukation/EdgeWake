package com.fxzs.lingxiagent.model.meeting.api;

import com.fxzs.lingxiagent.model.common.BaseResponse;
import com.fxzs.lingxiagent.model.meeting.dto.MeetingDto;
import com.fxzs.lingxiagent.model.meeting.dto.MeetingHistoryListDto;
import com.fxzs.lingxiagent.model.meeting.dto.MeetingListDto;
import com.fxzs.lingxiagent.model.meeting.dto.MeetingSummaryDto;
import com.fxzs.lingxiagent.model.meeting.dto.MeetingSummaryRequestDto;
import com.fxzs.lingxiagent.model.meeting.dto.SoundRecordRecognitionRequestDto;
import com.fxzs.lingxiagent.model.meeting.dto.SoundRecordTaskCheckRequestDto;
import com.fxzs.lingxiagent.model.meeting.dto.SoundRecordTaskResponseDto;
import com.fxzs.lingxiagent.model.meeting.dto.SoundRecordTaskSubmitResponseDto;
import com.fxzs.lingxiagent.model.meeting.dto.TranscriptionDto;

import java.util.Map;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Streaming;

public interface MeetingApiService {

    // 实时语音转文字 (WebSocket接口，此处仅为占位)
    @POST("ai-api/aiMeeting/realTimeVoiceToText")
    Call<BaseResponse<TranscriptionDto>> realTimeVoiceToText(@Body RequestBody audioData);

    // 更新会议记录信息
    @POST("app-api/lt/ai/meeting/updateMeetingRecord")
    Call<BaseResponse<String>> updateMeetingRecord(@Body Map<String, Object> params);

    // 录音文件识别任务提交
    @POST("app-api/lt/ai/meeting/soundRecordRecognition")
    Call<BaseResponse<SoundRecordTaskSubmitResponseDto>> submitAudioRecognitionTask(@Body SoundRecordRecognitionRequestDto request);

    // 录音文件识别任务结果查询
    @POST("app-api/lt/ai/meeting/soundRecordRecognitionTaskCheck")
    Call<BaseResponse<SoundRecordTaskResponseDto>> queryAudioRecognitionResult(@Body SoundRecordTaskCheckRequestDto request);

    // 我的会议记录列表
    @POST("app-api/lt/ai/meeting/myList")
    Call<BaseResponse<MeetingListDto>> getMyMeetingList(@Body Map<String, Object> params);

    // AI会议摘要能力
    @POST("app-api/lt/ai/meeting/meetingSummary")
    Call<BaseResponse<MeetingSummaryDto>> generateMeetingSummary(@Body MeetingSummaryRequestDto request);
    
    // AI会议摘要能力（SSE流式响应）
    @POST("app-api/lt/ai/meeting/meetingSummary")
    @Headers("Accept: text/event-stream")
    @Streaming
    Call<okhttp3.ResponseBody> generateMeetingSummaryStream(@Body MeetingSummaryRequestDto request);

    // 根据ID获取会议记录详情
    @POST("app-api/lt/ai/meeting/getMeetingInfoById")
    Call<BaseResponse<MeetingDto>> getMeetingDetail(@Body Map<String, Object> params);

    // 获取离线语音识别引擎模型类型列表
    @GET("app-api/lt/ai/meeting/offline/getEngineModelType")
    Call<BaseResponse<Map<String, String>>> getOfflineEngineModelType();

    // 获取我的会议历史记录列表
    @POST("app-api/lt/ai/meeting/myList")
    Call<BaseResponse<MeetingHistoryListDto>> getMeetingHistoryList(@Body Map<String, Object> params);

    // 根据ID删除会议记录
    @POST("app-api/lt/ai/meeting/deleteById")
    Call<BaseResponse<String>> deleteMeeting(@Body Map<String, Object> params/*@Query("id") String id*/);

    // 添加会议记录
    @POST("app-api/lt/ai/meeting/addAiMeetingRecord")
    Call<BaseResponse<Integer>> addMeetingRecord(@Body MeetingDto meeting);

    // 获取实时语音语言枚举
    @GET("app-api/lt/ai/meeting/realTime/getEngineModelType")
    Call<BaseResponse<Map<String, String>>> getRealTimeLanguages();

    // 获取录音文件语言枚举
    @GET("app-api/lt/ai/meeting/offline/getEngineModelType")
    Call<BaseResponse<Map<String, String>>> getAudioFileLanguages();
    @GET("app-api/lt/ai/meeting/offline/getSttLanguage/v2")
    Call<BaseResponse<Map<String, String>>> getSttLanguage();
}