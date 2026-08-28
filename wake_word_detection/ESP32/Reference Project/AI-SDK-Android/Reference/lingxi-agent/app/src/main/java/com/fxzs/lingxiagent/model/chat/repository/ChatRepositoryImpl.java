package com.fxzs.lingxiagent.model.chat.repository;

import com.fxzs.lingxiagent.model.chat.api.ChatApiService;
import com.fxzs.lingxiagent.model.chat.dto.ConversationDetailDto;
import com.fxzs.lingxiagent.model.chat.dto.ConversationHistoryListDto;
import com.fxzs.lingxiagent.model.chat.dto.ModelTypeResponse;
import com.fxzs.lingxiagent.model.common.BaseResponse;
import com.fxzs.lingxiagent.model.chat.dto.ConversationDetailPageDto;
import com.fxzs.lingxiagent.model.network.RetrofitClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public class ChatRepositoryImpl implements ChatRepository {
    private final ChatApiService apiService;
    
    public ChatRepositoryImpl() {
        // 使用带有AuthInterceptor的RetrofitClient
        this.apiService = RetrofitClient.getInstance().createService(ChatApiService.class);
        Timber.tag("ChatRepositoryImpl").d( "使用带AuthInterceptor的RetrofitClient初始化ChatApiService");
    }
    
    @Override
    public void getModelTypeList(int modelType, Callback<ModelTypeResponse> callback) {
        apiService.getModelTypeList(modelType).enqueue(new retrofit2.Callback<BaseResponse<ModelTypeResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<ModelTypeResponse>> call, Response<BaseResponse<ModelTypeResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<ModelTypeResponse> baseResponse = response.body();
                    if (baseResponse.getCode() == 0 && baseResponse.getData() != null) {
                        callback.onSuccess(baseResponse.getData());
                    } else {
                        callback.onError(baseResponse.getMsg() != null ? baseResponse.getMsg() : "获取模型列表失败");
                    }
                } else {
                    callback.onError("网络请求失败");
                }
            }
            
            @Override
            public void onFailure(Call<BaseResponse<ModelTypeResponse>> call, Throwable t) {
                callback.onError("网络错误: " + t.getMessage());
            }
        });
    }
    
    @Override
    public void getEngineModelType(Callback<Map<String, String>> callback) {
        apiService.getEngineModelType().enqueue(new retrofit2.Callback<BaseResponse<Map<String, String>>>() {
            @Override
            public void onResponse(Call<BaseResponse<Map<String, String>>> call, Response<BaseResponse<Map<String, String>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Map<String, String>> baseResponse = response.body();
                    if (baseResponse.getCode() == 0 && baseResponse.getData() != null) {
                        callback.onSuccess(baseResponse.getData());
                    } else {
                        callback.onError(baseResponse.getMsg() != null ? baseResponse.getMsg() : "获取引擎模型类型失败");
                    }
                } else {
                    callback.onError("网络请求失败");
                }
            }
            
            @Override
            public void onFailure(Call<BaseResponse<Map<String, String>>> call, Throwable t) {
                callback.onError("网络错误: " + t.getMessage());
            }
        });
    }
    
    @Override
    public void getConversationHistoryList(int modelType, Map<String, Object> params, Callback<ConversationHistoryListDto> callback) {
        apiService.getConversationHistoryList(modelType, params).enqueue(new retrofit2.Callback<BaseResponse<ConversationHistoryListDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<ConversationHistoryListDto>> call, Response<BaseResponse<ConversationHistoryListDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<ConversationHistoryListDto> baseResponse = response.body();
                    if (baseResponse.getCode() == 0 && baseResponse.getData() != null) {
                        callback.onSuccess(baseResponse.getData());
                    } else {
                        callback.onError(baseResponse.getMsg() != null ? baseResponse.getMsg() : "获取对话历史记录失败");
                    }
                } else {
                    callback.onError("网络请求失败");
                }
            }
            
            @Override
            public void onFailure(Call<BaseResponse<ConversationHistoryListDto>> call, Throwable t) {
                callback.onError("网络错误: " + t.getMessage());
            }
        });
    }

    @Override
    public void getListByConversationId(long id, Callback<List<ConversationDetailDto>> callback) {
        apiService.getListByConversationId(id).enqueue(new retrofit2.Callback<BaseResponse<List<ConversationDetailDto>>>() {
            @Override
            public void onResponse(Call<BaseResponse<List<ConversationDetailDto>>> call, Response<BaseResponse<List<ConversationDetailDto>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<List<ConversationDetailDto>> baseResponse = response.body();
                    if (baseResponse.getCode() == 0 && baseResponse.getData() != null) {
                        callback.onSuccess(baseResponse.getData());
                    } else {
                        callback.onError(baseResponse.getMsg() != null ? baseResponse.getMsg() : "获取对话历史记录失败");
                    }
                } else {
                    callback.onError("网络请求失败");
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<List<ConversationDetailDto>>> call, Throwable t) {
                callback.onError("网络错误: " + t.getMessage());
            }
        });
    }

    @Override
    public void getPageByConversationId(long id, int pageNo, int pageSize, Callback<List<ConversationDetailDto>> callback) {
        apiService.pageByConversationId(id, pageNo, pageSize).enqueue(new retrofit2.Callback<BaseResponse<ConversationDetailPageDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<ConversationDetailPageDto>> call, Response<BaseResponse<ConversationDetailPageDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<ConversationDetailPageDto> baseResponse = response.body();
                    if (baseResponse.getCode() == 0 && baseResponse.getData() != null) {
                        List<ConversationDetailDto> list = baseResponse.getData().getList();
                        if (list == null) list = new java.util.ArrayList<>();
                        callback.onSuccess(list);
                    } else {
                        callback.onError(baseResponse.getMsg() != null ? baseResponse.getMsg() : "获取对话消息失败");
                    }
                } else {
                    callback.onError("网络请求失败");
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<ConversationDetailPageDto>> call, Throwable t) {
                callback.onError("网络错误: " + t.getMessage());
            }
        });
    }

    @Override
    public void deleteConversation(long id, Callback<Boolean> callback) {

        apiService.deleteConversation(id).enqueue(new retrofit2.Callback<BaseResponse<Boolean>>() {
            @Override
            public void onResponse(Call<BaseResponse<Boolean>> call, Response<BaseResponse<Boolean>> response) {

                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Boolean> baseResponse = response.body();
                    if (baseResponse.getCode() == 0 && baseResponse.getData() != null) {
                        callback.onSuccess(baseResponse.getData());
                    } else {
                        callback.onError(baseResponse.getMsg() != null ? baseResponse.getMsg() : "获取对话历史记录失败");
                    }
                } else {
                    callback.onError("网络请求失败");
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<Boolean>> call, Throwable t) {
                callback.onError("网络错误: " + t.getMessage());
            }
        });
    }

    /**
     * 删除单条聊天消息
     */
    public void deleteChatMessage(long messageId, Callback<Boolean> callback) {
        Timber.tag("ChatRepositoryImpl").d( "开始删除消息，ID: " + messageId);
        Timber.tag("ChatRepositoryImpl").d( "API接口: DELETE /app-api/lt/ai/chat/message/delete?id=" + messageId);
        
        apiService.deleteChatMessage(messageId).enqueue(new retrofit2.Callback<BaseResponse<Boolean>>() {
            @Override
            public void onResponse(Call<BaseResponse<Boolean>> call, Response<BaseResponse<Boolean>> response) {
                Timber.tag("ChatRepositoryImpl").d( "删除API响应码: " + response.code());
                Timber.tag("ChatRepositoryImpl").d( "请求URL: " + call.request().url().toString());
                Timber.tag("ChatRepositoryImpl").d( "请求Headers: " + call.request().headers().toString());
                
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Boolean> baseResponse = response.body();
                    Timber.tag("ChatRepositoryImpl").d( "响应体code: " + baseResponse.getCode() + ", data: " + baseResponse.getData() + ", msg: " + baseResponse.getMsg());
                    
                    if (baseResponse.getCode() == 0 && baseResponse.getData() != null) {
                        Timber.tag("ChatRepositoryImpl").d( "删除成功，结果: " + baseResponse.getData());
                        callback.onSuccess(baseResponse.getData());
                    } else {
                        String errorMsg = baseResponse.getMsg() != null ? baseResponse.getMsg() : "删除消息失败";
                        Timber.tag("ChatRepositoryImpl").e( "删除失败，服务器返回错误: " + errorMsg);
                        callback.onError(errorMsg);
                    }
                } else {
                    String errorMsg = "网络请求失败，响应码: " + response.code();
                    Timber.tag("ChatRepositoryImpl").e( errorMsg);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<Boolean>> call, Throwable t) {
                String errorMsg = "网络错误: " + t.getMessage();
                Timber.tag("ChatRepositoryImpl").e( "删除请求失败: " + errorMsg);
                Timber.tag("ChatRepositoryImpl").e( "请求URL: " + call.request().url().toString());
                callback.onError(errorMsg);
            }
        });
    }

    @Override
    public void addConversationHistory(String conversationId, List<Map<String, Object>> messages, Callback<ArrayList<Integer>> callback) {
        Map<String, Object> params = createConversationHistoryParams(conversationId, messages);
        apiService.addConversationHistory(params).enqueue(new retrofit2.Callback<BaseResponse<ArrayList<Integer>>>() {
            @Override
            public void onResponse(Call<BaseResponse<ArrayList<Integer>>> call, Response<BaseResponse<ArrayList<Integer>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<ArrayList<Integer>> baseResponse = response.body();
                    if (baseResponse.getCode() == 0 && baseResponse.getData() != null) {
                        callback.onSuccess(baseResponse.getData());
                        Timber.tag("ChatRepositoryImpl").e( "baseResponse.getData(): " + baseResponse.getData());
                    } else {
                        callback.onError(baseResponse.getMsg() != null ? baseResponse.getMsg() : "添加历史记录失败");
                    }
                } else {
                    callback.onError("网络请求失败");
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<ArrayList<Integer>>> call, Throwable  t) {
                callback.onError("网络错误: " + t.getMessage());
            }

//            @Override
//            public void onFailure(Call<BaseResponse<Integer>> call, Throwable t) {
//                callback.onError("网络错误: " + t.getMessage());
//            }
        });
    }

    /**
     * 创建添加单条历史记录请求参数
     */
    private Map<String, Object> createConversationHistoryParams(String conversationId, List<Map<String, Object>> messages) {
        // 创建最外层的 conversation 数据结构
        Map<String, Object> conversation = new HashMap<>();
        conversation.put("conversationId", conversationId);
        // 将 messages 列表添加到 conversation
        conversation.put("messages", messages);

        return conversation;
    }
}