package com.fxzs.lingxiagent.model.chat.repository;

import com.fxzs.lingxiagent.model.chat.dto.ConversationDetailDto;
import com.fxzs.lingxiagent.model.chat.dto.ConversationHistoryListDto;
import com.fxzs.lingxiagent.model.chat.dto.ModelTypeResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface ChatRepository {
    interface Callback<T> {
        void onSuccess(T data);
        void onError(String error);
    }
    
    void getModelTypeList(int modelType, Callback<ModelTypeResponse> callback);
    
    void getEngineModelType(Callback<Map<String, String>> callback);
    
    /**
     * 获取对话历史记录列表
     * @param modelType 模型类型（1 表示智能体模型）
     * @param params 请求参数
     * @param callback 回调接口
     */
    void getConversationHistoryList(int modelType, Map<String, Object> params, Callback<ConversationHistoryListDto> callback);
    /**
     *
     *      获得指定对话的消息列表
     */
    void getListByConversationId(long id,Callback<List<ConversationDetailDto>> callback);
    /**
     * 分页获取指定对话的消息列表
     */
    void getPageByConversationId(long id, int pageNo, int pageSize, Callback<List<ConversationDetailDto>> callback);
    void deleteConversation(long id,Callback<Boolean> callback);
    /**
     * 添加/批量添加历史数据
     * @param conversationId 添加的参数（对话的conversationId, 可通过createMy接口创建）
     * @param messages 添加的参数（对话的信息）
     */
    void addConversationHistory(String conversationId, List<Map<String, Object>> messages, Callback<ArrayList<Integer>> callback);
    
    /**
     * 删除单条聊天消息
     * @param messageId 消息ID
     * @param callback 回调接口
     */
    void deleteChatMessage(long messageId, Callback<Boolean> callback);
}