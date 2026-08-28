package com.fxzs.lingxiagent.model.honor.repository;

/**
 * 荣耀出行、聚餐请求接口
 */
public interface HonorRepository {

    void sendStreamRequest(String inputString, StreamHandler handler);
}