package com.fxzs.lingxiagent.model.scene.repository;

import com.fxzs.lingxiagent.model.scene.dto.SceneResponse;

// 流式处理器接口
public interface SceneStreamHandler {
    void onDataChunk(SceneResponse resp);
    void onStreamComplete();
    void onError(String errMsg);
    void onStreamStop();
}