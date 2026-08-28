package com.fxzs.lingxiagent.model.intention.dto;

public class PromptRequest {
    /**
     * 用户输入的文本
     */
    public String query;
    /**
     * 系统提示，用于指导AI模型的行为
     * 可以包含变量占位符如 ${variable}，后续会被实际值替换
     */
    public String medicine_info;

    public static class PromptResponse {
        /**
         * 响应状态，如"success"或"error"
         */
        public String state;

        /**
         * 响应的实际内容文本
         */
        public String content;

        /**
         * 处理时间（以秒为单位）
         */
        public Double time;
    }
}