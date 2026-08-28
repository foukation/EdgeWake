package com.fxzs.lingxiagent.model.ppt.dto;

/**
 * PPT标题优化请求DTO
 * 用于调用getPptTitle接口获取优化后的标题
 */
public class PptTitleRequest {
    
    /**
     * 用户输入的原始主题内容
     */
    private String input;
    
    public PptTitleRequest() {
    }
    
    public PptTitleRequest(String input) {
        this.input = input;
    }
    
    public String getInput() {
        return input;
    }
    
    public void setInput(String input) {
        this.input = input;
    }
    
    @Override
    public String toString() {
        return "PptTitleRequest{" +
                "input='" + input + '\'' +
                '}';
    }
}