package com.fxzs.lingxiagent.model.honor.repository;

import com.fxzs.lingxiagent.model.honor.dto.MessageData;
import com.fxzs.lingxiagent.model.honor.dto.TripHonorRes;

// 流式处理器接口
public interface StreamHandler {
    void onDataChunk(TripHonorRes resp);
    void onDataChunk(MessageData resp);
    void onStreamComplete();
    void onError(String errMsg);
    void onStreamStop();
}