package com.fxzs.lingxiagent.model.scene.repository;

/**
 * 场景入驻平台请求接口
 */
public interface SceneRepository {

    void sendStreamRequest(String inputString, SceneStreamHandler handler);
}