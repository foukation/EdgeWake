package com.fxzs.lingxiagent.model.intention.dto;

/**
 * 银发助手API响应的基础数据结构
 */
public class ImageAssistantResponse {
    /**
     * 响应状态："success" 或 "error"
     */
    public String state;

    /**
     * 响应内容，是一个JSON字符串，需要根据具体业务场景自行解析
     */
    public String content;

    /**
     * 处理时间(秒)
     */
    public Double time;
}