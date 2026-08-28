package com.fxzs.lingxiagent.model.intention.dto;

/**
 * 表示银发助手任务请求的数据结构
 */
public class ImageAssistantRequest {
    /**
     * 任务类型，例如："click_coordinate", "text_input" 等
     */
    public String task_type;

    /**
     * 是否启用流式响应
     */
    public Boolean stream;

    /**
     * 用户查询/指令内容
     */
    public String query;

    /**
     * 可选的图像数据
     */
    public ImageData image;

    public static class ImageData {
        /**
         * 图像类型，例如："png", "jpeg" 等
         */
        public String type;

        /**
         * 图像宽度(像素)
         */
        public int width;

        /**
         * 图像高度(像素)
         */
        public int height;

        /**
         * Base64编码的图像数据
         */
        public String img_encoded;
    }
}