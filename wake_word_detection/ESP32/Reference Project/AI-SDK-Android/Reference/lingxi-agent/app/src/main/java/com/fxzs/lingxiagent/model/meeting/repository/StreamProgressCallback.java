package com.fxzs.lingxiagent.model.meeting.repository;

/**
 * SSE流式处理进度回调接口
 */
public interface StreamProgressCallback {
    /**
     * 当接收到新的内容片段时调用
     * @param chunk 新接收到的内容片段
     * @param accumulatedContent 累积的完整内容
     */
    void onChunkReceived(String chunk, String accumulatedContent);
    
    /**
     * 当流处理完成时调用
     * @param totalChunks 总共接收的片段数
     */
    void onStreamComplete(int totalChunks);
    
    /**
     * 当流处理出错时调用
     * @param error 错误信息
     */
    void onStreamError(String error);
}