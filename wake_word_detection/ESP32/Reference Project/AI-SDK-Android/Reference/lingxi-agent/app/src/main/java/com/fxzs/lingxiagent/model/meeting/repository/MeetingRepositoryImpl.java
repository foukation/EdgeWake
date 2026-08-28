package com.fxzs.lingxiagent.model.meeting.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.model.common.BaseResponse;
import com.fxzs.lingxiagent.model.drawing.repository.DrawingRepository.Result;
import com.fxzs.lingxiagent.model.honor.repository.BenefitCode;
import com.fxzs.lingxiagent.model.meeting.api.MeetingApiService;
import com.fxzs.lingxiagent.model.meeting.dto.AudioUploadDto;
import com.fxzs.lingxiagent.model.meeting.dto.LanguageDto;
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
import com.fxzs.lingxiagent.model.network.RetrofitClient;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.util.StreamJsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class MeetingRepositoryImpl implements MeetingRepository {
    private final MeetingApiService apiService;
    private final MeetingApiService streamingApiService;

    public MeetingRepositoryImpl() {
        this.apiService = RetrofitClient.getInstance().createService(MeetingApiService.class);
        this.streamingApiService = RetrofitClient.getInstance().createStreamingService(MeetingApiService.class);
    }
    
    public MeetingRepositoryImpl(android.content.Context context) {
        this.apiService = RetrofitClient.getInstance().createService(MeetingApiService.class);
        this.streamingApiService = RetrofitClient.getInstance().createStreamingService(MeetingApiService.class);
    }

    @Override
    public LiveData<Result<TranscriptionDto>> startRealTimeTranscription(byte[] audioData) {
        MutableLiveData<Result<TranscriptionDto>> result = new MutableLiveData<>();
        
        // Timber.tag("MeetingRepository").d( "开始实时语音转文字请求");
        
        RequestBody requestBody = RequestBody.create(MediaType.parse("audio/pcm"), audioData);
        apiService.realTimeVoiceToText(requestBody).enqueue(new Callback<BaseResponse<TranscriptionDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<TranscriptionDto>> call, Response<BaseResponse<TranscriptionDto>> response) {
                // Timber.tag("MeetingRepository").d( "实时语音转文字API响应: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<TranscriptionDto> baseResponse = response.body();
                    if (baseResponse.getCode() == 0 || baseResponse.getCode() == 200) {
                        result.postValue(Result.success(baseResponse.getData()));
                    } else {
                        result.postValue(Result.error(baseResponse.getMsg() != null ? baseResponse.getMsg() : "实时语音转文字失败"));
                    }
                } else {
                    result.postValue(Result.error("实时语音转文字失败"));
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<TranscriptionDto>> call, Throwable t) {
                Timber.tag("MeetingRepository").e( "实时语音转文字网络请求失败: " + t.getMessage());
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }

    @Override
    public LiveData<Result<MeetingDto>> addMeeting(MeetingDto meeting) {
        MutableLiveData<Result<MeetingDto>> result = new MutableLiveData<>();
        
        // Timber.tag("MeetingRepository").d( "开始添加会议记录请求");
        // Timber.tag("MeetingRepository").d( "会议标题: " + meeting.getTitle());
        
        apiService.addMeetingRecord(meeting).enqueue(new Callback<BaseResponse<Integer>>() {
            @Override
            public void onResponse(Call<BaseResponse<Integer>> call, Response<BaseResponse<Integer>> response) {
                // Timber.tag("MeetingRepository").d( "添加会议记录API响应: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Integer> baseResponse = response.body();
                    if (baseResponse.getCode() == 0 || baseResponse.getCode() == 200) {
                        // API返回的是会议ID，需要创建一个新的MeetingDto对象
                        Integer meetingId = baseResponse.getData();
                        Timber.tag("MeetingRepository").i( "添加会议记录成功，ID: " + meetingId);
                        MeetingDto newMeeting = new MeetingDto();
                        newMeeting.setId(meetingId != null ? meetingId.toString() : null);
                        newMeeting.setTitle(meeting.getTitle());
                        newMeeting.setType(meeting.getType());
                        newMeeting.setStatus(meeting.getStatus());
                        newMeeting.setLanguage(meeting.getLanguage());
                        result.postValue(Result.success(newMeeting));
                    } else {
                        result.postValue(Result.error(baseResponse.getMsg() != null ? baseResponse.getMsg() : "添加会议记录失败"));
                    }
                } else {
                    result.postValue(Result.error("添加会议记录失败"));
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<Integer>> call, Throwable t) {
                Timber.tag("MeetingRepository").e( "添加会议记录网络请求失败: " + t.getMessage());
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }

    @Override
    public LiveData<Result<String>> updateMeeting(MeetingDto meeting) {
        MutableLiveData<Result<String>> result = new MutableLiveData<>();
        
        // Timber.tag("MeetingRepository").d( "开始更新会议记录请求");
        // Timber.tag("MeetingRepository").d( "会议ID: " + meeting.getId());
        
        // 将MeetingDto转换为Map参数
        java.util.HashMap<String, Object> params = new java.util.HashMap<>();
        if (meeting.getId() != null) params.put("id", Integer.valueOf(meeting.getId()));
        if (meeting.getName() != null) params.put("name", meeting.getName());
        if (meeting.getContent() != null) params.put("content", meeting.getContent());
        if (meeting.getFileUrl() != null) params.put("fileUrl", meeting.getFileUrl());
        if (meeting.getType() != null) params.put("type", meeting.getType());
        if (meeting.getAbstractText() != null) params.put("abstractText", meeting.getAbstractText());
        if (meeting.getMeetingText() != null) params.put("meetingText", meeting.getMeetingText());
        if (meeting.getAbstractChapterText() != null) params.put("abstractChapterText", meeting.getAbstractChapterText());
        if (meeting.getAbstractDetailText() != null) params.put("abstractDetailText", meeting.getAbstractDetailText());
        if (meeting.getAbstractOptimizeText() != null) params.put("abstractOptimizeText", meeting.getAbstractOptimizeText());
        if (meeting.getConversionId() != null) params.put("conversionId", Integer.valueOf(meeting.getConversionId()));
        if (meeting.getUserId() != null) params.put("userId", meeting.getUserId());
        
        apiService.updateMeetingRecord(params).enqueue(new Callback<BaseResponse<String>>() {
            @Override
            public void onResponse(Call<BaseResponse<String>> call, Response<BaseResponse<String>> response) {
                // Timber.tag("MeetingRepository").d( "更新会议记录API响应: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<String> baseResponse = response.body();
                    if (baseResponse.getCode() == 0 || baseResponse.getCode() == 200) {
                        result.postValue(Result.success(baseResponse.getData()));
                    } else {
                        result.postValue(Result.error(baseResponse.getMsg() != null ? baseResponse.getMsg() : "更新会议记录失败"));
                    }
                } else {
                    result.postValue(Result.error("更新会议记录失败"));
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<String>> call, Throwable t) {
                Timber.tag("MeetingRepository").e( "更新会议记录网络请求失败: " + t.getMessage());
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }

    @Override
    public LiveData<Result<Void>> deleteMeeting(String meetingId) {
        MutableLiveData<Result<Void>> result = new MutableLiveData<>();
        
        // Timber.tag("MeetingRepository").d( "开始删除会议记录请求，ID: " + meetingId);
        Map<String, Object> params = new HashMap<>();
        params.put("id",meetingId);
        apiService.deleteMeeting(params).enqueue(new Callback<BaseResponse<String>>() {
            @Override
            public void onResponse(Call<BaseResponse<String>> call, Response<BaseResponse<String>> response) {
                // Timber.tag("MeetingRepository").d( "删除会议记录API响应: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<String> baseResponse = response.body();
                    if (baseResponse.getCode() == 0 || baseResponse.getCode() == 200) {
                        result.postValue(Result.success(null));
                    } else {
                        result.postValue(Result.error(baseResponse.getMsg() != null ? baseResponse.getMsg() : "删除会议记录失败"));
                    }
                } else {
                    result.postValue(Result.error("删除会议记录失败"));
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<String>> call, Throwable t) {
                Timber.tag("MeetingRepository").e( "删除会议记录网络请求失败: " + t.getMessage());
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }

    @Override
    public LiveData<Result<MeetingDto>> getMeetingDetail(String meetingId) {
        MutableLiveData<Result<MeetingDto>> result = new MutableLiveData<>();
        
        // Timber.tag("MeetingRepository").d( "开始获取会议详情请求，ID: " + meetingId);
        
        // 创建请求参数
        Map<String, Object> params = new HashMap<>();
        params.put("id", meetingId);
        
        apiService.getMeetingDetail(params).enqueue(new Callback<BaseResponse<MeetingDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<MeetingDto>> call, Response<BaseResponse<MeetingDto>> response) {
                // Timber.tag("MeetingRepository").d( "获取会议详情API响应: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<MeetingDto> baseResponse = response.body();
                    if (baseResponse.getCode() == 0 || baseResponse.getCode() == 200) {
                        result.postValue(Result.success(baseResponse.getData()));
                    } else {
                        result.postValue(Result.error(baseResponse.getMsg() != null ? baseResponse.getMsg() : "获取会议详情失败"));
                    }
                } else {
                    result.postValue(Result.error("获取会议详情失败"));
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<MeetingDto>> call, Throwable t) {
                Timber.tag("MeetingRepository").e( "获取会议详情网络请求失败: " + t.getMessage());
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }

    @Override
    public LiveData<Result<MeetingListDto>> getMyMeetingList(int page, int size) {
        MutableLiveData<Result<MeetingListDto>> result = new MutableLiveData<>();
        
        // Timber.tag("MeetingRepository").d( "开始获取我的会议列表请求，页码: " + page + ", 大小: " + size);
        
        // 创建POST请求参数
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("page", page);
        params.put("size", size);
        
        apiService.getMyMeetingList(params).enqueue(new Callback<BaseResponse<MeetingListDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<MeetingListDto>> call, Response<BaseResponse<MeetingListDto>> response) {
                // Timber.tag("MeetingRepository").d( "获取我的会议列表API响应: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<MeetingListDto> baseResponse = response.body();
                    if (baseResponse.getCode() == 0 || baseResponse.getCode() == 200) {
                        result.postValue(Result.success(baseResponse.getData()));
                    } else if (baseResponse.getCode() == 500) {
                        // 处理后端500错误，返回空列表而不是错误
                        Timber.tag("MeetingRepository").w("后端返回500错误，使用空列表作为临时解决方案");
                        MeetingListDto emptyList = new MeetingListDto();
                        emptyList.setTotal(0);
                        emptyList.setPage(page);
                        emptyList.setSize(size);
                        emptyList.setList(new java.util.ArrayList<>());
                        result.postValue(Result.success(emptyList));
                    } else {
                        result.postValue(Result.error(baseResponse.getMsg() != null ? baseResponse.getMsg() : "获取会议列表失败"));
                    }
                } else {
                    result.postValue(Result.error("获取会议列表失败"));
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<MeetingListDto>> call, Throwable t) {
                Timber.tag("MeetingRepository").e( "获取会议列表网络请求失败: " + t.getMessage());
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }

    @Override
    public LiveData<Result<AudioUploadDto>> uploadAudioFile(File audioFile, String language) {
        MutableLiveData<Result<AudioUploadDto>> result = new MutableLiveData<>();
        
        // Timber.tag("MeetingRepository").d( "开始提交音频识别任务");
        // Timber.tag("MeetingRepository").d( "文件路径: " + audioFile.getAbsolutePath());
        // Timber.tag("MeetingRepository").d( "语言: " + language);
        
        // 创建请求对象 - 注意：这里假设文件已经上传到云端，传入的是文件URL
        SoundRecordRecognitionRequestDto request = new SoundRecordRecognitionRequestDto();
        request.setFileUrl(audioFile.getAbsolutePath()); // 这里应该是云端URL，暂时使用本地路径
        request.setEngineModelType(language != null ? language : "16k_zh_large");
        request.setMeetingId(1); // 默认会议ID
        
        apiService.submitAudioRecognitionTask(request).enqueue(new Callback<BaseResponse<SoundRecordTaskSubmitResponseDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<SoundRecordTaskSubmitResponseDto>> call, Response<BaseResponse<SoundRecordTaskSubmitResponseDto>> response) {
                // Timber.tag("MeetingRepository").d( "提交音频识别任务API响应: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<SoundRecordTaskSubmitResponseDto> baseResponse = response.body();
                    if (baseResponse.getCode() == 0 || baseResponse.getCode() == 200) {
                        // 转换响应为AudioUploadDto格式
                        SoundRecordTaskSubmitResponseDto taskResponse = baseResponse.getData();
                        AudioUploadDto audioUpload = new AudioUploadDto();
                        
                        // 从嵌套结构中获取taskId
                        if (taskResponse != null && taskResponse.getData() != null && taskResponse.getData().getTaskId() != null) {
                            audioUpload.setTaskId(String.valueOf(taskResponse.getData().getTaskId()));
                            audioUpload.setStatus("submitted");
                            audioUpload.setMessage("任务提交成功");
                            Timber.tag("MeetingRepository").i( "任务提交成功，taskId: " + taskResponse.getData().getTaskId());
                        } else {
                            audioUpload.setTaskId(null);
                            audioUpload.setStatus("error");
                            audioUpload.setMessage("未获取到taskId");
                            Timber.tag("MeetingRepository").e( "任务提交失败：未获取到taskId");
                        }
                        result.postValue(Result.success(audioUpload));
                    } else {
                        result.postValue(Result.error(baseResponse.getMsg() != null ? baseResponse.getMsg() : "提交音频识别任务失败"));
                    }
                } else {
                    result.postValue(Result.error("提交音频识别任务失败"));
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<SoundRecordTaskSubmitResponseDto>> call, Throwable t) {
                Timber.tag("MeetingRepository").e( "提交音频识别任务网络请求失败: " + t.getMessage());
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }

    @Override
    public LiveData<Result<TranscriptionDto>> queryRecognitionResult(String taskId) {
        MutableLiveData<Result<TranscriptionDto>> result = new MutableLiveData<>();
        
        // Timber.tag("MeetingRepository").d( "开始查询识别结果，taskId: " + taskId);
        
        // 创建请求对象
        SoundRecordTaskCheckRequestDto request = new SoundRecordTaskCheckRequestDto();
        try {
            request.setTaskId(Long.parseLong(taskId));
        } catch (NumberFormatException e) {
            Timber.tag("MeetingRepository").e( "taskId格式错误: " + taskId);
            result.postValue(Result.error("taskId格式错误"));
            return result;
        }
        request.setMeetingId(1); // 默认会议ID
        
        apiService.queryAudioRecognitionResult(request).enqueue(new Callback<BaseResponse<SoundRecordTaskResponseDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<SoundRecordTaskResponseDto>> call, Response<BaseResponse<SoundRecordTaskResponseDto>> response) {
                // Timber.tag("MeetingRepository").d( "查询识别结果API响应: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<SoundRecordTaskResponseDto> baseResponse = response.body();
                    if (baseResponse.getCode() == 0 || baseResponse.getCode() == 200) {
                        // 转换响应为TranscriptionDto格式
                        SoundRecordTaskResponseDto taskResponse = baseResponse.getData();
                        if (taskResponse != null) {
                            TranscriptionDto transcription = new TranscriptionDto();
                            
                            // 从嵌套结构中获取结果
                            if (taskResponse.getData() != null) {
                                transcription.setText(taskResponse.getData().getResult());
                                transcription.setIsFinal("success".equals(taskResponse.getData().getStatusStr()) || "completed".equals(taskResponse.getData().getStatusStr()));
                            } else {
                                // 兼容旧格式
                                transcription.setText(taskResponse.getResult());
                                transcription.setIsFinal("completed".equals(taskResponse.getStatus()));
                            }
                            
                            transcription.setTimestamp(System.currentTimeMillis());
                            result.postValue(Result.success(transcription));
                        } else {
                            result.postValue(Result.error("查询结果为空"));
                        }
                    } else {
                        result.postValue(Result.error(baseResponse.getMsg() != null ? baseResponse.getMsg() : "查询识别结果失败"));
                    }
                } else {
                    result.postValue(Result.error("查询识别结果失败"));
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<SoundRecordTaskResponseDto>> call, Throwable t) {
                Timber.tag("MeetingRepository").e( "查询识别结果网络请求失败: " + t.getMessage());
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }

    @Override
    public LiveData<Result<MeetingSummaryDto>> generateMeetingSummary(String content) {
        MutableLiveData<Result<MeetingSummaryDto>> result = new MutableLiveData<>();
        
        // Timber.tag("MeetingRepository").d( "开始生成会议摘要请求");
        // Timber.tag("MeetingRepository").d( "内容长度: " + (content != null ? content.length() : 0));
        
        RequestBody requestBody = RequestBody.create(MediaType.parse("text/plain"), content);
        // 使用更新后的API接口
        MeetingSummaryRequestDto request = new MeetingSummaryRequestDto("1-会议记录：bot-20250117141939-rvm5v", content, 1);
        apiService.generateMeetingSummary(request).enqueue(new Callback<BaseResponse<MeetingSummaryDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<MeetingSummaryDto>> call, Response<BaseResponse<MeetingSummaryDto>> response) {
                // Timber.tag("MeetingRepository").d( "生成会议摘要API响应: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<MeetingSummaryDto> baseResponse = response.body();
                    if (baseResponse.getCode() == 0 || baseResponse.getCode() == 200) {
                        result.postValue(Result.success(baseResponse.getData()));
                    } else {
                        result.postValue(Result.error(baseResponse.getMsg() != null ? baseResponse.getMsg() : "生成会议摘要失败"));
                    }
                } else {
                    result.postValue(Result.error("生成会议摘要失败"));
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<MeetingSummaryDto>> call, Throwable t) {
                Timber.tag("MeetingRepository").e( "生成会议摘要网络请求失败: " + t.getMessage());
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }
    
    @Override
    public LiveData<Result<MeetingSummaryDto>> generateMeetingSummaryForUI(String content, Integer meetingId) {
        MutableLiveData<Result<MeetingSummaryDto>> result = new MutableLiveData<>();
        
        // Timber.tag("MeetingRepository").d( "开始生成会议摘要请求 (ForUI)");
        // Timber.tag("MeetingRepository").d( "内容长度: " + (content != null ? content.length() : 0));
        // Timber.tag("MeetingRepository").d( "会议ID: " + meetingId);
        
        // 检查当前token
        String currentToken = SharedPreferencesUtil.getToken();
        // Timber.tag("MeetingRepository").d( "当前Token: " + currentToken);
        // Timber.tag("MeetingRepository").d( "Token长度: " + (currentToken != null ? currentToken.length() : 0));
        
        // botKey只需要横线前面的数字
        // 根据API文档，不同的数字对应不同的摘要类型：
        // 1 - 会议记录
        // 2 - 会议话题
        // 3 - 会议摘要详细
        // 4 - 会议摘要主题
        String botKey = "1"; // 使用会议记录类型
        // Timber.tag("MeetingRepository").d( "使用botKey: " + botKey);
        MeetingSummaryRequestDto request = new MeetingSummaryRequestDto(botKey, content, meetingId);
        // Timber.tag("MeetingRepository").d( "请求对象: botKey=" + request.getBotKey() + ", meetingId=" + request.getMeetingId());
        
        // 使用SSE接口（使用流式API服务）
        streamingApiService.generateMeetingSummaryStream(request).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                // Timber.tag("MeetingRepository").d( "SSE接口响应: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        // 解析SSE流
                        MeetingSummaryDto summaryDto = parseSSEResponse(response.body());
                        if (summaryDto != null) {
                            result.postValue(Result.success(summaryDto));
                        } else {
                            result.postValue(Result.error("系统异常"));
                        }
                    } catch (Exception e) {
                        Timber.tag("MeetingRepository").e( "SSE解析失败: " + e.getMessage());
                        // 检查是否是认证错误
                        if (e.getMessage() != null && e.getMessage().startsWith("AUTH_ERROR:")) {
                            String errorMsg = e.getMessage().substring("AUTH_ERROR:".length());
                            result.postValue(Result.error(errorMsg));
                        } else {
                            result.postValue(Result.error("系统异常"));
                        }
                    } finally {
                        response.body().close();
                    }
                } else {
                    // 处理HTTP错误（包括500错误）
                    Timber.tag("MeetingRepository").e( "HTTP错误码: " + response.code());
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
                        Timber.tag("MeetingRepository").e( "错误响应体: " + errorBody);
                        
                        // 尝试解析错误响应
                        try {
                            JSONObject errorJson = new JSONObject(errorBody);
                            int code = errorJson.optInt("code", -1);
                            String msg = errorJson.optString("msg", "系统异常");
                            
                            Timber.tag("MeetingRepository").e( "错误响应中的code: " + code);
                            Timber.tag("MeetingRepository").e( "错误响应中的msg: " + msg);
                            
                            if (code == 401) {
                                result.postValue(Result.error("账号未登录，请重新登录"));
                            } else if (response.code() == 500 && code == 401) {
                                // 特殊处理：HTTP 500但响应体是401的情况
                                Timber.tag("MeetingRepository").e( "检测到特殊情况：HTTP 500但响应体是401");
                                result.postValue(Result.error("账号未登录，请重新登录"));
                            } else {
                                result.postValue(Result.error(msg));
                            }
                        } catch (JSONException e) {
                            Timber.tag("MeetingRepository").e( "解析错误响应失败: " + e.getMessage());
                            result.postValue(Result.error("系统异常"));
                        }
                    } catch (IOException e) {
                        Timber.tag("MeetingRepository").e( "读取错误响应失败: " + e.getMessage());
                        result.postValue(Result.error("系统异常"));
                    }
                }
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                Timber.tag("MeetingRepository").e( "SSE接口失败: " + t.getMessage());
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }

    @Override
    public LiveData<Result<List<LanguageDto>>> getRealTimeLanguages() {
        MutableLiveData<Result<List<LanguageDto>>> result = new MutableLiveData<>();
        
        // Timber.tag("MeetingRepository").d( "开始请求实时语言列表API");
        
        apiService.getRealTimeLanguages().enqueue(new Callback<BaseResponse<Map<String, String>>>() {
            @Override
            public void onResponse(Call<BaseResponse<Map<String, String>>> call, Response<BaseResponse<Map<String, String>>> response) {
                Timber.tag("MeetingRepository").d( "实时语言列表API响应: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Map<String, String>> baseResponse = response.body();
                    // Timber.tag("MeetingRepository").d( "响应成功，响应码: " + baseResponse.getCode());
                    if (baseResponse.getCode() == 0 || baseResponse.getCode() == 200) {
                        Map<String, String> languageMap = baseResponse.getData();
                        List<LanguageDto> languages = convertMapToLanguageList(languageMap);
                        // Timber.tag("MeetingRepository").d( "获取到语言数量: " + (languages != null ? languages.size() : "null"));
                        result.postValue(Result.success(languages));
                    } else {
                        // Timber.tag("MeetingRepository").e( "API返回错误码: " + baseResponse.getCode());
                        result.postValue(Result.error(baseResponse.getMsg() != null ? baseResponse.getMsg() : "获取实时语言列表失败"));
                    }
                } else {
                    // Timber.tag("MeetingRepository").e( "响应失败或响应体为空");
                    result.postValue(Result.error("获取实时语言列表失败"));
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<Map<String, String>>> call, Throwable t) {
                Timber.tag("MeetingRepository").e( "实时语言列表API请求失败: " + t.getMessage());
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }

    @Override
    public LiveData<Result<List<LanguageDto>>> getAudioFileLanguages() {
        MutableLiveData<Result<List<LanguageDto>>> result = new MutableLiveData<>();
        
        // Timber.tag("MeetingRepository").d( "开始请求音频文件语言列表API");
        
        apiService.getAudioFileLanguages().enqueue(new Callback<BaseResponse<Map<String, String>>>() {
            @Override
            public void onResponse(Call<BaseResponse<Map<String, String>>> call, Response<BaseResponse<Map<String, String>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Map<String, String>> baseResponse = response.body();
                    Timber.tag("MeetingRepository").d( "响应成功，响应码: " + baseResponse.getCode());
                    if (baseResponse.getCode() == 0 || baseResponse.getCode() == 200) {
                        Map<String, String> languageMap = baseResponse.getData();
                        List<LanguageDto> languages = convertMapToLanguageList(languageMap);
                        // Timber.tag("MeetingRepository").d( "获取到语言数量: " + (languages != null ? languages.size() : "null"));
                        result.postValue(Result.success(languages));
                    } else {
                        // Timber.tag("MeetingRepository").e( "API返回错误码: " + baseResponse.getCode());
                        result.postValue(Result.error(baseResponse.getMsg() != null ? baseResponse.getMsg() : "获取音频文件语言列表失败"));
                    }
                } else {
                    // Timber.tag("MeetingRepository").e( "响应失败或响应体为空");
                    result.postValue(Result.error("获取音频文件语言列表失败"));
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<Map<String, String>>> call, Throwable t) {
                Timber.tag("MeetingRepository").e( "音频文件语言列表API请求失败: " + t.getMessage());
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }

    private List<LanguageDto> convertMapToLanguageList(Map<String, String> languageMap) {
        if (languageMap == null) {
            return null;
        }
        
        List<LanguageDto> languages = new ArrayList<>();
        for (Map.Entry<String, String> entry : languageMap.entrySet()) {
            LanguageDto language = new LanguageDto();
            language.setCode(entry.getKey());
            language.setName(entry.getValue());
            languages.add(language);
        }
        
        return languages;
    }
    
    @Override
    public LiveData<Result<MeetingSummaryDto>> generateMeetingSummaryWithProgress(String content, Integer meetingId, String botKey, StreamProgressCallback callback) {
        MutableLiveData<Result<MeetingSummaryDto>> result = new MutableLiveData<>();
        
        Timber.tag("MeetingRepository").i( "开始生成会议摘要 - 会议ID: " + meetingId + ", botKey: " + botKey + ", 内容长度: " + (content != null ? content.length() : 0));
        
        MeetingSummaryRequestDto request = new MeetingSummaryRequestDto(botKey, content, meetingId);
        
        // 使用SSE接口（使用流式API服务）
        streamingApiService.generateMeetingSummaryStream(request).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                Timber.tag("MeetingRepository").i( "SSE接口响应: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    // 在后台线程中解析SSE响应，避免阻塞主线程
                    new Thread(() -> {
                        try {
                            // 使用带进度回调的SSE解析
                            MeetingSummaryDto summaryDto = parseSSEResponseWithProgress(response.body(), callback);
                            if (summaryDto != null) {
                                result.postValue(Result.success(summaryDto));
                            } else {
                                if (callback != null) {
                                    callback.onStreamError("系统异常");
                                }
                                result.postValue(Result.error("系统异常"));
                            }
                        } catch (Exception e) {
                            Timber.tag("MeetingRepository").e( "SSE解析失败: " + e.getMessage());
                            if (callback != null) {
                                callback.onStreamError(e.getMessage());
                            }
                            // 检查是否是认证错误
                            if (e.getMessage() != null && e.getMessage().startsWith("AUTH_ERROR:")) {
                                String errorMsg = e.getMessage().substring("AUTH_ERROR:".length());
                                result.postValue(Result.error(errorMsg));
                            } else {
                                result.postValue(Result.error("系统异常"));
                            }
                        } finally {
                            response.body().close();
                        }
                    }).start();
                } else {
                    // 处理HTTP错误
                    String errorMsg = "系统异常";
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
                        JSONObject errorJson = new JSONObject(errorBody);
                        errorMsg = errorJson.optString("msg", "系统异常");
                    } catch (Exception e) {
                        // 忽略解析错误
                    }
                    if (callback != null) {
                        callback.onStreamError(errorMsg);
                    }
                    result.postValue(Result.error(errorMsg));
                }
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                Timber.tag("MeetingRepository").e( "SSE接口失败: " + t.getMessage());
                if (callback != null) {
                    callback.onStreamError("网络请求失败：" + t.getMessage());
                }
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }
    
    /**
     * 解析SSE响应流
     */
    private MeetingSummaryDto parseSSEResponse(okhttp3.ResponseBody responseBody) throws IOException {
        MeetingSummaryDto summaryDto = new MeetingSummaryDto();
        StreamJsonParser.MeetingSummaryParser parser = new StreamJsonParser.MeetingSummaryParser();
        
        // 读取完整响应
        String responseContent = responseBody.string();
        Timber.tag("MeetingRepository").i( "原始响应内容长度: " + responseContent.length());
        
        // 首先尝试解析为JSON错误响应（服务器可能返回JSON而不是SSE）
        try {
            // 使用StreamJsonParser修复可能不完整的JSON
            String fixedJson = StreamJsonParser.tryFixJSON(responseContent);
            JSONObject jsonResponse = new JSONObject(fixedJson);
            
            if (jsonResponse.has("code")) {
                int code = jsonResponse.getInt("code");
                if (code == 401) {
                    // 认证失败，抛出特定异常
                    throw new IOException("AUTH_ERROR:账号未登录，请重新登录");
                }
                if (code != 200 && code != 0) {
                    // 这是一个错误响应
                    String errorMsg = jsonResponse.optString("msg", "系统异常");
                    Timber.tag("MeetingRepository").e( "服务器返回错误码: " + code + ", 消息: " + errorMsg);
                    return null;
                }
                
                // 如果是成功的JSON响应
                if (jsonResponse.has("data") && !jsonResponse.isNull("data")) {
                    JSONObject dataObj = jsonResponse.getJSONObject("data");
                    // 解析JSON格式的摘要数据
                    summaryDto.setSummary(dataObj.optString("summary", ""));
                    
                    // 初始化所有列表为空列表，避免空指针
                    summaryDto.setKeyPoints(new ArrayList<>());
                    summaryDto.setTopics(new ArrayList<>());
                    summaryDto.setActionItems(new ArrayList<>());
                    summaryDto.setParticipants(new ArrayList<>());
                    
                    // 解析其他字段
                    if (dataObj.has("keyPoints")) {
                        List<String> keyPoints = new ArrayList<>();
                        JSONArray keyPointsArray = dataObj.getJSONArray("keyPoints");
                        for (int i = 0; i < keyPointsArray.length(); i++) {
                            keyPoints.add(keyPointsArray.getString(i));
                        }
                        summaryDto.setKeyPoints(keyPoints);
                    }
                    
                    if (dataObj.has("actionItems")) {
                        List<String> actionItems = new ArrayList<>();
                        JSONArray actionItemsArray = dataObj.getJSONArray("actionItems");
                        for (int i = 0; i < actionItemsArray.length(); i++) {
                            actionItems.add(actionItemsArray.getString(i));
                        }
                        summaryDto.setActionItems(actionItems);
                    }
                    
                    if (dataObj.has("topics")) {
                        List<String> topics = new ArrayList<>();
                        JSONArray topicsArray = dataObj.getJSONArray("topics");
                        for (int i = 0; i < topicsArray.length(); i++) {
                            topics.add(topicsArray.getString(i));
                        }
                        summaryDto.setTopics(topics);
                    }
                    
                    if (dataObj.has("participants")) {
                        List<String> participants = new ArrayList<>();
                        JSONArray participantsArray = dataObj.getJSONArray("participants");
                        for (int i = 0; i < participantsArray.length(); i++) {
                            participants.add(participantsArray.getString(i));
                        }
                        summaryDto.setParticipants(participants);
                    }
                    
                    return summaryDto;
                }
            }
        } catch (JSONException e) {
            // 不是JSON格式，尝试解析SSE流
            Timber.tag("MeetingRepository").i( "不是JSON格式，尝试解析SSE流");
            
            // 解析SSE流
            String[] lines = responseContent.split("\n");
            int lineCount = 0;
            int dataLineCount = 0;
            
            for (String line : lines) {
                lineCount++;
                // 尝试多种解析方式
                String parsedContent = StreamJsonParser.parseSSEDataLine(line);
                if (parsedContent != null && !parsedContent.isEmpty()) {
                    dataLineCount++;
                    parser.addContent(parsedContent);
                    // 只记录前5行和关键行
                    if (dataLineCount <= 5 || dataLineCount % 20 == 0) {
                        Timber.tag("MeetingRepository").i( "解析SSE数据行 " + dataLineCount + ": " + 
                            (parsedContent.length() > 50 ? parsedContent.substring(0, 50) + "..." : parsedContent));
                    }
                }
            }
            
            Timber.tag("MeetingRepository").i( "SSE解析完成 - 总行数: " + lineCount + ", 数据行数: " + dataLineCount);
            
            // 获取累积的内容
            String fullContent = parser.getFullContent();
            Timber.tag("MeetingRepository").i( "累积内容长度: " + fullContent.length());
            
            // 输出累积内容的关键预览
            if (fullContent.length() > 0) {
                Timber.tag("MeetingRepository").i( "摘要内容预览: " + 
                    (fullContent.length() > 200 ? fullContent.substring(0, 200) + "..." : fullContent));
            }
            
            if (!fullContent.isEmpty()) {
                // 使用解析器解析结构化内容
                StreamJsonParser.ParsedSummary parsed = parser.parseStructuredContent();
                
                // 转换为MeetingSummaryDto
                summaryDto.setSummary(parsed.summary);
                summaryDto.setKeyPoints(parsed.keyPoints);
                summaryDto.setTopics(parsed.topics);
                summaryDto.setActionItems(parsed.actionItems);
                summaryDto.setParticipants(parsed.participants);
                
                // 确保所有列表都不为null
                if (summaryDto.getKeyPoints() == null) summaryDto.setKeyPoints(new ArrayList<>());
                if (summaryDto.getTopics() == null) summaryDto.setTopics(new ArrayList<>());
                if (summaryDto.getActionItems() == null) summaryDto.setActionItems(new ArrayList<>());
                if (summaryDto.getParticipants() == null) summaryDto.setParticipants(new ArrayList<>());
                
                Timber.tag("MeetingRepository").i( "解析结果 - 摘要长度: " + summaryDto.getSummary().length() + 
                    ", 要点数: " + summaryDto.getKeyPoints().size() + 
                    ", 待办数: " + summaryDto.getActionItems().size());
                Timber.tag("MeetingRepository").i( "最终摘要内容: " + summaryDto.getSummary());
                
                return summaryDto;
            }
        }
        
        return null;
    }
    
    /**
     * 解析SSE响应流（带进度回调）
     */
    private MeetingSummaryDto parseSSEResponseWithProgress(okhttp3.ResponseBody responseBody, StreamProgressCallback callback) throws IOException {
        MeetingSummaryDto summaryDto = new MeetingSummaryDto();
        StreamJsonParser.MeetingSummaryParser parser = new StreamJsonParser.MeetingSummaryParser();
        
        // 使用BufferedReader逐行读取，实现真正的流式处理
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(responseBody.byteStream()))) {
            String line;
            int totalChunks = 0;
            boolean hasReceivedData = false;
            
            while ((line = reader.readLine()) != null) {
                // 跳过空行
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                // 第一次接收到数据时的特殊处理
                if (!hasReceivedData && line.startsWith("{") && !line.startsWith("data:")) {
                    // 可能是错误的JSON响应
                    StringBuilder jsonContent = new StringBuilder(line);
                    String nextLine;
                    // 读取最多10行来确定是否是完整的JSON
                    int lineCount = 1;
                    while ((nextLine = reader.readLine()) != null && lineCount < 10) {
                        jsonContent.append("\n").append(nextLine);
                        lineCount++;
                        if (nextLine.contains("}") && nextLine.trim().endsWith("}")) {
                            break;
                        }
                    }
                    
                    // 尝试解析为JSON错误响应
                    try {
                        String fixedJson = StreamJsonParser.tryFixJSON(jsonContent.toString());
                        JSONObject jsonResponse = new JSONObject(fixedJson);
                        
                        if (jsonResponse.has("code")) {
                            int code = jsonResponse.getInt("code");
                            if (code == 401) {
                                throw new IOException("AUTH_ERROR:账号未登录，请重新登录");
                            }
                            if (code != 200 && code != 0) {
                                String errorMsg = jsonResponse.optString("msg", "系统异常");
                                if (callback != null) {
                                    callback.onStreamError(errorMsg);
                                }
                                return null;
                            }
                        }
                    } catch (JSONException e) {
                        // 不是JSON错误响应，将内容作为普通SSE数据处理
                        for (String l : jsonContent.toString().split("\n")) {
                            Timber.tag("MeetingRepository").i( "processSSELine");
                            processSSELine(l, parser, callback, totalChunks);
                        }
                    }
                    hasReceivedData = true;
                    continue;
                }

                Timber.tag("MeetingRepository").i( "processSSELine1");
                // 解析SSE数据行
                hasReceivedData = true;
                totalChunks = processSSELine(line, parser, callback, totalChunks);
            }
            
            // 解析完成
            String fullContent = parser.getFullContent();
            Timber.tag("MeetingRepository").i( "流式处理完成，累积内容长度: " + fullContent.length());
            
            if (!fullContent.isEmpty()) {
                StreamJsonParser.ParsedSummary parsed = parser.parseStructuredContent();
                
                // 转换为MeetingSummaryDto
                summaryDto.setSummary(parsed.summary);
                summaryDto.setKeyPoints(parsed.keyPoints);
                summaryDto.setTopics(parsed.topics);
                summaryDto.setActionItems(parsed.actionItems);
                summaryDto.setParticipants(parsed.participants);
                
                // 确保所有列表都不为null
                if (summaryDto.getKeyPoints() == null) summaryDto.setKeyPoints(new ArrayList<>());
                if (summaryDto.getTopics() == null) summaryDto.setTopics(new ArrayList<>());
                if (summaryDto.getActionItems() == null) summaryDto.setActionItems(new ArrayList<>());
                if (summaryDto.getParticipants() == null) summaryDto.setParticipants(new ArrayList<>());
                
                // 通知流完成
                if (callback != null) {
                    callback.onStreamComplete(totalChunks);
                }
                
                Timber.tag("MeetingRepository").i( "流式处理完成 - 总片段数: " + totalChunks + 
                    ", 摘要长度: " + summaryDto.getSummary().length());
                
                return summaryDto;
            }
        } catch (IOException e) {
            Timber.tag("MeetingRepository").e( "流式读取失败: " + e.getMessage());
            if (callback != null) {
                callback.onStreamError("流式读取失败: " + e.getMessage());
            }
            throw e;
        }
        
        return null;
    }
    
    /**
     * 处理单行SSE数据
     */
    private int processSSELine(String line, StreamJsonParser.MeetingSummaryParser parser, 
                               StreamProgressCallback callback, int currentChunks) {
        String parsedContent = StreamJsonParser.parseSSEDataLine(line);
        if (parsedContent != null && !parsedContent.isEmpty()) {
            currentChunks++;
            parser.addContent(parsedContent);
            
            // 通知进度回调
            if (callback != null) {
                String fullContent = parser.getFullContent();
                Timber.tag("MeetingRepository").i( "回调前检查 - 片段长度: " + parsedContent.length() + 
                    ", 累积内容长度: " + fullContent.length());
                Timber.tag("MeetingRepository").i( "片段内容包含换行符: " + parsedContent.contains("\n") +
                    ", 包含\\n字符串: " + parsedContent.contains("\\n"));
                Timber.tag("MeetingRepository").i( "累积内容包含换行符: " + fullContent.contains("\n") +
                    ", 包含\\n字符串: " + fullContent.contains("\\n"));
                String previewContent = fullContent.length() > 100 ? fullContent.substring(0, 100) + "..." : fullContent;
                Timber.tag("MeetingRepository").i( "回调内容预览: " + previewContent.replace("\n", "\\n"));
                Timber.tag("MeetingRepository").i( "回调内容预览fullContent: " + fullContent.replace("\n", "\\n"));
                callback.onChunkReceived(parsedContent, fullContent);
            }
            
            // 只记录关键片段的日志，避免日志过多
            if (currentChunks <= 5 || currentChunks % 50 == 0) {
                Timber.tag("MeetingRepository").i( "流式接收片段 " + currentChunks + ": " + 
                    (parsedContent.length() > 50 ? parsedContent.substring(0, 50) + "..." : parsedContent));
            }
        }
        return currentChunks;
    }
    
    /**
     * 解析JSON格式的摘要响应
     */
    private MeetingSummaryDto parseJsonResponse(JSONObject dataObj) throws JSONException {
        MeetingSummaryDto summaryDto = new MeetingSummaryDto();
        
        summaryDto.setSummary(dataObj.optString("summary", ""));
        
        // 初始化所有列表为空列表
        summaryDto.setKeyPoints(new ArrayList<>());
        summaryDto.setTopics(new ArrayList<>());
        summaryDto.setActionItems(new ArrayList<>());
        summaryDto.setParticipants(new ArrayList<>());
        
        // 解析各个字段
        if (dataObj.has("keyPoints")) {
            List<String> keyPoints = new ArrayList<>();
            JSONArray keyPointsArray = dataObj.getJSONArray("keyPoints");
            for (int i = 0; i < keyPointsArray.length(); i++) {
                keyPoints.add(keyPointsArray.getString(i));
            }
            summaryDto.setKeyPoints(keyPoints);
        }
        
        if (dataObj.has("actionItems")) {
            List<String> actionItems = new ArrayList<>();
            JSONArray actionItemsArray = dataObj.getJSONArray("actionItems");
            for (int i = 0; i < actionItemsArray.length(); i++) {
                actionItems.add(actionItemsArray.getString(i));
            }
            summaryDto.setActionItems(actionItems);
        }
        
        if (dataObj.has("topics")) {
            List<String> topics = new ArrayList<>();
            JSONArray topicsArray = dataObj.getJSONArray("topics");
            for (int i = 0; i < topicsArray.length(); i++) {
                topics.add(topicsArray.getString(i));
            }
            summaryDto.setTopics(topics);
        }
        
        if (dataObj.has("participants")) {
            List<String> participants = new ArrayList<>();
            JSONArray participantsArray = dataObj.getJSONArray("participants");
            for (int i = 0; i < participantsArray.length(); i++) {
                participants.add(participantsArray.getString(i));
            }
            summaryDto.setParticipants(participants);
        }
        
        return summaryDto;
    }
    
    @Override
    public LiveData<Result<SoundRecordTaskResponseDto>> submitAudioRecognitionTask(String fileUrl, String language, String meetingId) {
        MutableLiveData<Result<SoundRecordTaskResponseDto>> result = new MutableLiveData<>();
        
        // Timber.tag("MeetingRepository").d( "提交语音识别任务: fileUrl=" + fileUrl + ", language=" + language + ", meetingId=" + meetingId);
        
        SoundRecordRecognitionRequestDto request = new SoundRecordRecognitionRequestDto();
        request.setFileUrl(fileUrl);
        request.setEngineModelType(language);
        try {
            request.setMeetingId(Integer.parseInt(meetingId));
        } catch (NumberFormatException e) {
            Timber.tag("MeetingRepository").e( "会议ID格式错误: " + meetingId);
            result.postValue(Result.error("会议ID格式错误"));
            return result;
        }
        
        apiService.submitAudioRecognitionTask(request).enqueue(new Callback<BaseResponse<SoundRecordTaskSubmitResponseDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<SoundRecordTaskSubmitResponseDto>> call, Response<BaseResponse<SoundRecordTaskSubmitResponseDto>> response) {
                // Timber.tag("MeetingRepository").d( "提交语音识别任务响应: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<SoundRecordTaskSubmitResponseDto> baseResponse = response.body();
                    if (baseResponse.getCode() == 0 || baseResponse.getCode() == 200) {
                        // 直接使用原始的DTO，因为VMRealtimeMeeting会处理提取taskId
                        SoundRecordTaskResponseDto taskResponse = new SoundRecordTaskResponseDto();
                        // 创建一个新的TaskData并复制taskId
                        SoundRecordTaskResponseDto.TaskData taskData = new SoundRecordTaskResponseDto.TaskData();
                        if (baseResponse.getData() != null && baseResponse.getData().getData() != null) {
                            taskData.setTaskId(baseResponse.getData().getData().getTaskId());
                        }
                        taskResponse.setData(taskData);
                        result.postValue(Result.success(taskResponse));
                    } else {
                        result.postValue(Result.error(baseResponse.getMsg() != null ? baseResponse.getMsg() : "提交语音识别任务失败"));
                    }
                } else {
                    result.postValue(Result.error("提交语音识别任务失败"));
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<SoundRecordTaskSubmitResponseDto>> call, Throwable t) {
                Timber.tag("MeetingRepository").e( "提交语音识别任务网络请求失败: " + t.getMessage());
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }
    
    @Override
    public LiveData<Result<SoundRecordTaskResponseDto>> queryAudioRecognitionResult(String taskId, String meetingId) {
        MutableLiveData<Result<SoundRecordTaskResponseDto>> result = new MutableLiveData<>();
        
        // Timber.tag("MeetingRepository").d( "查询语音识别结果: taskId=" + taskId + ", meetingId=" + meetingId);
        
        SoundRecordTaskCheckRequestDto request = new SoundRecordTaskCheckRequestDto();
        try {
            request.setTaskId(Long.parseLong(taskId));
            request.setMeetingId(Integer.parseInt(meetingId));
        } catch (NumberFormatException e) {
            Timber.tag("MeetingRepository").e( "任务ID或会议ID格式错误: taskId=" + taskId + ", meetingId=" + meetingId);
            result.postValue(Result.error("任务ID或会议ID格式错误"));
            return result;
        }
        
        apiService.queryAudioRecognitionResult(request).enqueue(new Callback<BaseResponse<SoundRecordTaskResponseDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<SoundRecordTaskResponseDto>> call, Response<BaseResponse<SoundRecordTaskResponseDto>> response) {
                // Timber.tag("MeetingRepository").d( "查询语音识别结果响应: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<SoundRecordTaskResponseDto> baseResponse = response.body();
                    if (baseResponse.getCode() == 0 || baseResponse.getCode() == 200) {
                        result.postValue(Result.success(baseResponse.getData()));
                    } else {
                        result.postValue(Result.error(baseResponse.getMsg() != null ? baseResponse.getMsg() : "查询语音识别结果失败"));
                    }
                } else {
                    result.postValue(Result.error("查询语音识别结果失败"));
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<SoundRecordTaskResponseDto>> call, Throwable t) {
                Timber.tag("MeetingRepository").e( "查询语音识别结果网络请求失败: " + t.getMessage());
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        
        return result;
    }
    
    @Override
    public LiveData<Result<MeetingDto>> getMeetingInfoById(Integer meetingId) {
        MutableLiveData<Result<MeetingDto>> result = new MutableLiveData<>();
        
        Timber.tag("MeetingRepository").d( "开始查询会议信息，ID: " + meetingId);
        
        // 构建请求参数
        java.util.HashMap<String, Object> params = new java.util.HashMap<>();
        params.put("id", meetingId);

        Call<BaseResponse<MeetingDto>> call = apiService.getMeetingDetail(params);
        call.enqueue(new Callback<BaseResponse<MeetingDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<MeetingDto>> call, Response<BaseResponse<MeetingDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<MeetingDto> baseResponse = response.body();
                    if (baseResponse.isSuccess()) {
                        Timber.tag("MeetingRepository").i( "成功获取会议信息");
                        MeetingDto meetingInfo = baseResponse.getData();
                        if (meetingInfo != null && meetingInfo.getAbstractText() != null) {
                            Timber.tag("MeetingRepository").i( "会议存在摘要，长度: " + meetingInfo.getAbstractText().length());
                        }
                        result.postValue(new Result<>(meetingInfo, null, true, ""));
                    } else {
                        String error = baseResponse.getMsg() != null ? baseResponse.getMsg() : "获取会议信息失败";
                        Timber.tag("MeetingRepository").e( "获取会议信息失败: " + error);
                        result.postValue(new Result<>(null, error, false, ""));
                    }
                } else {
                    String error = "服务器响应错误: " + response.code();
                    Timber.tag("MeetingRepository").e( error);
                    result.postValue(new Result<>(null, error,false, ""));
                }
            }
            
            @Override
            public void onFailure(Call<BaseResponse<MeetingDto>> call, Throwable t) {
                String error = "网络请求失败: " + t.getMessage();
                Timber.tag("MeetingRepository").e( error, t);
                result.postValue(new Result<>(null, error,false, ""));
            }
        });
        
        return result;
    }
    
    @Override
    public LiveData<Result<String>> updateMeetingRecord(Integer meetingId, String summaryType, String summaryContent) {
        MutableLiveData<Result<String>> result = new MutableLiveData<>();
        
        Timber.tag("MeetingRepository").i( "开始更新会议记录，ID: " + meetingId + ", 摘要类型: " + summaryType);
        
        // 构建请求参数
        java.util.HashMap<String, Object> params = new java.util.HashMap<>();
        params.put("id", meetingId);
        
        // 根据摘要类型设置对应的字段
        switch (summaryType) {
            case "5": // 按章节
                params.put("abstractChapterText", summaryContent);
                break;
            case "4": // 按主题
                params.put("abstractOptimizeText", summaryContent);
                break;
            case "3": // 详细
                params.put("abstractDetailText", summaryContent);
                break;
            default:
                params.put("abstractText", summaryContent);
                break;
        }
        
        // 不添加updateTime，让服务器自动设置
        
        Call<BaseResponse<String>> call = apiService.updateMeetingRecord(params);
        call.enqueue(new Callback<BaseResponse<String>>() {
            @Override
            public void onResponse(Call<BaseResponse<String>> call, Response<BaseResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<String> baseResponse = response.body();
                    if (baseResponse.isSuccess()) {
                        Timber.tag("MeetingRepository").i( "成功更新会议记录摘要");
                        result.postValue(new Result<>(baseResponse.getData(), null,true, ""));
                    } else {
                        String error = baseResponse.getMsg() != null ? baseResponse.getMsg() : "更新会议记录失败";
                        Timber.tag("MeetingRepository").e( "更新会议记录失败: " + error);
                        result.postValue(new Result<>(null, error,false, ""));
                    }
                } else {
                    String error = "服务器响应错误: " + response.code();
                    Timber.tag("MeetingRepository").e( error);
                    result.postValue(new Result<>(null, error,false, ""));
                }
            }
            
            @Override
            public void onFailure(Call<BaseResponse<String>> call, Throwable t) {
                String error = "网络请求失败: " + t.getMessage();
                Timber.tag("MeetingRepository").e( error, t);
                result.postValue(new Result<>(null, error,false, ""));
            }
        });
        
        return result;
    }

    @Override
    public LiveData<Result<java.util.Map<String, String>>> getOfflineEngineModelType() {
        MutableLiveData<Result<java.util.Map<String, String>>> result = new MutableLiveData<>();

        Timber.tag("MeetingRepository").d( "开始获取离线语音识别引擎模型类型列表");

//        apiService.getOfflineEngineModelType()
        apiService.getSttLanguage()
                .enqueue(new Callback<BaseResponse<java.util.Map<String, String>>>() {
            @Override
            public void onResponse(Call<BaseResponse<java.util.Map<String, String>>> call, Response<BaseResponse<java.util.Map<String, String>>> response) {
                Timber.tag("MeetingRepository").d( "获取语言模型类型响应: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<java.util.Map<String, String>> baseResponse = response.body();
                    if (baseResponse.getCode() == 0) {
                        Timber.tag("MeetingRepository").d( "获取语言模型类型成功，数量: " + (baseResponse.getData() != null ? baseResponse.getData().size() : 0));
                        result.postValue(Result.success(baseResponse.getData()));
                    } else {
                        String error = "获取语言模型类型失败: " + baseResponse.getMsg();
                        Timber.tag("MeetingRepository").e( error);
                        result.postValue(Result.error(error));
                    }
                } else {
                    String error = "获取语言模型类型请求失败，HTTP状态码: " + response.code();
                    Timber.tag("MeetingRepository").e( error);
                    result.postValue(Result.error(error));
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<java.util.Map<String, String>>> call, Throwable t) {
                String error = "获取语言模型类型网络请求失败: " + t.getMessage();
                Timber.tag("MeetingRepository").e( error, t);
                result.postValue(Result.error(error));
            }
        });

        return result;
    }

    @Override
    public LiveData<Result<MeetingHistoryListDto>> getMeetingHistoryList(java.util.Map<String, Object> params) {
        MutableLiveData<Result<MeetingHistoryListDto>> result = new MutableLiveData<>();

        Timber.tag("MeetingRepository").d( "开始获取会议历史记录列表");

        apiService.getMeetingHistoryList(params).enqueue(new Callback<BaseResponse<MeetingHistoryListDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<MeetingHistoryListDto>> call, Response<BaseResponse<MeetingHistoryListDto>> response) {
                Timber.tag("MeetingRepository").d( "获取会议历史记录响应: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<MeetingHistoryListDto> baseResponse = response.body();
                    if (baseResponse.getCode() == 0) {
                        Timber.tag("MeetingRepository").d( "获取会议历史记录成功，数量: " +
                            (baseResponse.getData() != null && baseResponse.getData().getList() != null ?
                             baseResponse.getData().getList().size() : 0));
                        result.postValue(Result.success(baseResponse.getData()));
                    } else {
                        String error = "获取会议历史记录失败: " + baseResponse.getMsg();
                        Timber.tag("MeetingRepository").e( error);
                        result.postValue(Result.error(error));
                    }
                } else {
                    String error = "获取会议历史记录请求失败，HTTP状态码: " + response.code();
                    Timber.tag("MeetingRepository").e( error);
                    result.postValue(Result.error(error));
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<MeetingHistoryListDto>> call, Throwable t) {
                String error = "获取会议历史记录网络请求失败: " + t.getMessage();
                Timber.tag("MeetingRepository").e( error, t);
                result.postValue(Result.error(error));
            }
        });

        return result;
    }

    @Override
    public LiveData<Result<String>> updateMeetingTopic(Integer meetingId, String topicContent) {
        MutableLiveData<Result<String>> result = new MutableLiveData<>();

        Timber.tag("MeetingRepository").i( "开始更新会议话题，ID: " + meetingId);

        // 构建请求参数
        java.util.HashMap<String, Object> params = new java.util.HashMap<>();
        params.put("id", meetingId);
        params.put("topic", topicContent);

        Call<BaseResponse<String>> call = apiService.updateMeetingRecord(params);
        call.enqueue(new Callback<BaseResponse<String>>() {
            @Override
            public void onResponse(Call<BaseResponse<String>> call, Response<BaseResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<String> baseResponse = response.body();
                    if (baseResponse.isSuccess()) {
                        Timber.tag("MeetingRepository").i( "成功更新会议话题");
                        result.postValue(new Result<>(baseResponse.getData(), null,true, ""));
                    } else {
                        String error = baseResponse.getMsg() != null ? baseResponse.getMsg() : "更新会议话题失败";
                        Timber.tag("MeetingRepository").e( "更新会议话题失败: " + error);
                        result.postValue(new Result<>(null, error,false, ""));
                    }
                } else {
                    Timber.tag("MeetingRepository").e( "更新会议话题请求失败");
                    result.postValue(new Result<>(null, "更新会议话题失败",false, ""));
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<String>> call, Throwable t) {
                Timber.tag("MeetingRepository").e( "更新会议话题网络请求失败: " + t.getMessage());
                result.postValue(new Result<>(null, "网络请求失败：" + t.getMessage(),false,""));
            }
        });

        return result;
    }

    @Override
    public LiveData<Result<String>> updateMeetingName(Integer meetingId, String name) {

        MutableLiveData<Result<String>> result = new MutableLiveData<>();


        // 构建请求参数
        java.util.HashMap<String, Object> params = new java.util.HashMap<>();
        params.put("id", meetingId);
        params.put("name", name);

        Call<BaseResponse<String>> call = apiService.updateMeetingRecord(params);
        call.enqueue(new Callback<BaseResponse<String>>() {
            @Override
            public void onResponse(Call<BaseResponse<String>> call, Response<BaseResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<String> baseResponse = response.body();
                    if (baseResponse.isSuccess()) {
                        result.postValue(new Result<>(baseResponse.getData(), null,true, ""));
                    } else {
                        String error = baseResponse.getMsg() != null ? baseResponse.getMsg() : "更新会议话题失败";
                        Timber.tag("MeetingRepository").e( "更新会议话题失败: " + error);
                        result.postValue(new Result<>(null, error,false, ""));
                    }
                } else {
                    Timber.tag("MeetingRepository").e( "更新会议话题请求失败");
                    result.postValue(new Result<>(null, "更新会议话题失败",false, ""));
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<String>> call, Throwable t) {
                Timber.tag("MeetingRepository").e( "更新会议话题网络请求失败: " + t.getMessage());
                result.postValue(new Result<>(null, "网络请求失败：" + t.getMessage(),false, ""));
            }
        });

        return result;
    }
    @Override
    public LiveData<Result<String>> bindMeetingAndConversationId(String meetingId, String conversionId) {

        MutableLiveData<Result<String>> result = new MutableLiveData<>();


        // 构建请求参数
        java.util.HashMap<String, Object> params = new java.util.HashMap<>();
        params.put("id", meetingId);
        params.put("conversionId", conversionId);

        Call<BaseResponse<String>> call = apiService.updateMeetingRecord(params);
        call.enqueue(new Callback<BaseResponse<String>>() {
            @Override
            public void onResponse(Call<BaseResponse<String>> call, Response<BaseResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<String> baseResponse = response.body();
                    if (baseResponse.isSuccess()) {
                        result.postValue(new Result<>(baseResponse.getData(), null,true, ""));
                    } else {
//                        String error = baseResponse.getMsg() != null ? baseResponse.getMsg() : "更新会议话题失败";
//                        Timber.tag("MeetingRepository").e( "更新会议话题失败: " + error);
//                        result.postValue(new Result<>(null, error, false));
                    }
                } else {
                    Timber.tag("MeetingRepository").e( "更新会议话题请求失败");
                    result.postValue(new Result<>(null, "更新会议话题失败",false, ""));
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<String>> call, Throwable t) {
                Timber.tag("MeetingRepository").e( "更新会议话题网络请求失败: " + t.getMessage());
                result.postValue(new Result<>(null, "网络请求失败：" + t.getMessage(),false, ""));
            }
        });

        return result;
    }

    public LiveData<Result<List<LanguageDto>>> getSttLanguage() {
        MutableLiveData<Result<List<LanguageDto>>> result = new MutableLiveData<>();
        apiService.getSttLanguage().enqueue(new Callback<BaseResponse<Map<String, String>>>() {
            @Override
            public void onResponse(Call<BaseResponse<Map<String, String>>> call, Response<BaseResponse<Map<String, String>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Map<String, String>> baseResponse = response.body();
                    if (baseResponse.getCode() == 0) {
                        Map<String, String> languageMap = baseResponse.getData(); // 语言名->模型名
                        List<LanguageDto> languages = new ArrayList<>();
                        if (languageMap != null) {
                            for (Map.Entry<String, String> entry : languageMap.entrySet()) {
                                LanguageDto dto = new LanguageDto();
                                dto.setName(entry.getKey()); // 如 "中文通用"
                                dto.setCode(entry.getValue()); // 如 "16k_zh"
                                languages.add(dto);
                            }
                        }
                        result.postValue(Result.success(languages));
                    } else if (BenefitCode.isBenefitError(String.valueOf(baseResponse.getCode()))) {
                        result.postValue(Result.billError(String.valueOf(baseResponse.getCode()), baseResponse.getMessage()));
                    } else {
                        result.postValue(Result.error(baseResponse.getMsg() != null ? baseResponse.getMsg() : "获取STT语言失败"));
                    }
                } else {
                    result.postValue(Result.error("获取STT语言失败"));
                }
            }
            @Override
            public void onFailure(Call<BaseResponse<Map<String, String>>> call, Throwable t) {
                result.postValue(Result.error("网络请求失败：" + t.getMessage()));
            }
        });
        return result;
    }
}
