package com.fxzs.lingxiagent.model.chat.api;

import com.fxzs.lingxiagent.model.chat.dto.ConversationDetailDto;
import com.fxzs.lingxiagent.model.chat.dto.ModelTypeResponse;
import com.fxzs.lingxiagent.model.chat.dto.ConversationDetailPageDto;
import com.fxzs.lingxiagent.model.chat.dto.ConversationHistoryListDto;
import com.fxzs.lingxiagent.model.chat.dto.ModelTypeResponse;
import com.fxzs.lingxiagent.model.common.BaseResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ChatApiService {
    
    /**
     * 获取大模型列表
     * @param modelType 模型类型（8 表示聊天模型）
     * @return 模型列表响应
     */
    @GET("app-api/lt/ai/chat/model/getModelTypeList")
    Call<BaseResponse<ModelTypeResponse>> getModelTypeList(@Query("modelType") int modelType);
    
    /**
     * 获取语音识别引擎模型类型列表
     * @return 语音识别语言列表响应（Map格式）
     */
    @GET("app-api/lt/ai/meeting/realTime/getEngineModelType")
    Call<BaseResponse<Map<String, String>>> getEngineModelType();
    
    /**
     * 获取对话历史记录列表
     * @param modelType 模型类型（1 表示智能体模型）
     * @param params 请求参数（pageNo, pageSize等）
     * @return 对话历史记录列表响应
     */
    @POST("app-api/lt/ai/chat/conversation/page-list")
    Call<BaseResponse<ConversationHistoryListDto>> getConversationHistoryList(@Query("modelType") int modelType, @Body Map<String, Object> params);

    /**
    *
     获得指定对话的消息列表
     * */
    @GET("app-api/lt/ai/chat/message/list-by-conversation-id")
    Call<BaseResponse<List<ConversationDetailDto>>> getListByConversationId(@Query("conversationId") long id);

    /**
     * 按对话ID分页获取消息列表
     */
    @GET("app-api/lt/ai/chat/message/page-by-conversation-id")
    Call<BaseResponse<ConversationDetailPageDto>> pageByConversationId(
            @Query("conversationId") long id,
            @Query("pageNo") int pageNo,
            @Query("pageSize") int pageSize
    );

    @DELETE("app-api/lt/ai/chat/conversation/delete-my")
    Call<BaseResponse<Boolean>> deleteConversation(@Query("id") long id);

    /**
     * 删除单条聊天消息
     * @param id 消息 ID
     * @return 删除结果
     */
    @DELETE("app-api/lt/ai/chat/message/delete")
    Call<BaseResponse<Boolean>> deleteChatMessage(@Query("id") long id);

    /**
     * 添加/批量添加历史数据
     * @param params 添加的参数（conversationId, messages等）
     */
    @POST("app-api/lt/ai/chat/message/batch-add-history")
    Call<BaseResponse<ArrayList<Integer>>> addConversationHistory(@Body Map<String, Object> params);
}