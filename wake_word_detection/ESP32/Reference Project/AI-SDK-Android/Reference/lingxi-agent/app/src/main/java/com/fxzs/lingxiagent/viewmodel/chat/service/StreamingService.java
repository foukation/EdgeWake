package com.fxzs.lingxiagent.viewmodel.chat.service;

import androidx.annotation.Nullable;

import com.fxzs.lingxiagent.model.chat.callback.SSECallback;
import com.fxzs.lingxiagent.model.chat.dto.ChatFileBean;
import com.fxzs.lingxiagent.network.ZNet.ApiResponse;
import com.fxzs.lingxiagent.network.ZNet.HttpRequest;
import com.fxzs.lingxiagent.network.ZNet.bean.SSEBean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import io.reactivex.disposables.Disposable;

/**
 * 流式对话服务：封装标准 SSE 流（非灵犀分支）的发送与解析。
 */
public class StreamingService {

    public interface Callback {
        /**
         * @param content 片段内容
         * @param isReason 是否为 assistant-reason（思考内容）
         * @param sendId 用户发送消息的ID（如果有）
         * @param receiveId AI回复消息的ID（如果有）
         */
        void onReceive(String content, boolean isReason, Integer sendId, Integer receiveId);
        void onEnd();
    }

    private final HttpRequest request;

    public StreamingService(HttpRequest request) {
        this.request = request;
    }

    public void startStandardStream(long conversationId,
                                    long modelId,
                                    String title,
                                    @Nullable List<ChatFileBean> files,
                                    boolean thinkingEnabled,
                                    Callback callback) {
        request.sendStreams(conversationId,
                modelId,
                title,
                false,
                files,
                thinkingEnabled ? "enabled" : "disabled",
                new SSECallback() {
                    @Override
                    public void receive(String responseBodyString) {
                        Gson gson = new GsonBuilder().setLenient().create();
                        Type type = new TypeToken<ApiResponse<SSEBean>>(){}.getType();
                        ApiResponse<SSEBean> res = gson.fromJson(responseBodyString, type);
                        if (res != null && res.getCode() == 0 && res.getData() != null && res.getData().getReceive() != null) {
                            String recvType = res.getData().getReceive().getType();
                            String content = res.getData().getReceive().getContent();
                            boolean isReason = "assistant-reason".equals(recvType);
                            
                            // 提取消息ID
                            Integer sendId = null;
                            Integer receiveId = null;
                            
                            if (res.getData().getSend() != null) {
                                sendId = res.getData().getSend().getId();
                            }
                            
                            if (res.getData().getReceive() != null) {
                                receiveId = res.getData().getReceive().getId();
                            }
                            
                            if (callback != null) {
                                callback.onReceive(content, isReason, sendId, receiveId);
                            }
                        }
                    }

                    @Override
                    public void end() {
                        if (callback != null) callback.onEnd();
                    }
                },
                null);
    }

    public Disposable getCurrentDisposable() {
        return request.getSseDisposable();
    }
}

