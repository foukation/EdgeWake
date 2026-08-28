package com.fxzs.lingxiagent.model.deepresearch.repository;

import com.fxzs.lingxiagent.model.deepresearch.dto.TripDeepResearchRes;
import com.fxzs.lingxiagent.model.honor.dto.MessageData;

// 流式处理器接口
public interface DeepResearchStreamHandler {
    void onDataChunk(TripDeepResearchRes resp);
    void onDataChunk(MessageData resp);
    void onStreamComplete();
    void onError(String errMsg);
    void onStreamStop();
}