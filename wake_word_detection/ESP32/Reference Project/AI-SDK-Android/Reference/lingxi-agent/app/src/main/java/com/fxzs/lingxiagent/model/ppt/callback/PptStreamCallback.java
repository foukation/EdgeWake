package com.fxzs.lingxiagent.model.ppt.callback;

/**
 * PPT流式请求回调接口
 * 用于处理PPT相关的SSE流式响应
 */
public interface PptStreamCallback {
    
    /**
     * 接收到流式数据
     * @param data 接收到的数据片段
     */
    void onReceive(String data);
    
    /**
     * 流式请求完成
     */
    void onComplete();
    
    /**
     * 流式请求出错
     * @param error 错误信息
     */
    void onError(String error);
    
    /**
     * 流式请求开始
     */
    default void onStart() {
        // 默认空实现
    }
}
