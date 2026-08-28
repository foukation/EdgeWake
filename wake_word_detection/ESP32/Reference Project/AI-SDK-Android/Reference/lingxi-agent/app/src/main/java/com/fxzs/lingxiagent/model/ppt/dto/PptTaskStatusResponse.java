package com.fxzs.lingxiagent.model.ppt.dto;

import com.google.gson.annotations.SerializedName;

/**
 * PPT任务状态响应DTO
 */
public class PptTaskStatusResponse {
    
    @SerializedName("pptStatus")
    private String pptStatus;
    
    @SerializedName("pptUrl")
    private String pptUrl;
    
    @SerializedName("errMsg")
    private String errMsg;
    
    @SerializedName("totalPages")
    private int totalPages;
    
    @SerializedName("donePages")
    private int donePages;
    
    public PptTaskStatusResponse() {
    }
    
    // Getters and Setters
    public String getPptStatus() {
        return pptStatus;
    }
    
    public void setPptStatus(String pptStatus) {
        this.pptStatus = pptStatus;
    }
    
    public String getPptUrl() {
        return pptUrl;
    }
    
    public void setPptUrl(String pptUrl) {
        this.pptUrl = pptUrl;
    }
    
    public String getErrMsg() {
        return errMsg;
    }
    
    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }
    
    public int getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
    
    public int getDonePages() {
        return donePages;
    }
    
    public void setDonePages(int donePages) {
        this.donePages = donePages;
    }
    
    /**
     * 检查任务是否完成
     */
    public boolean isDone() {
        return "done".equals(pptStatus);
    }
    
    /**
     * 检查任务是否失败
     */
    public boolean isFailed() {
        return "build_failed".equals(pptStatus) || "failed".equals(pptStatus);
    }
    
    /**
     * 检查任务是否正在进行中
     */
    public boolean isInProgress() {
        return "building".equals(pptStatus) || "in_progress".equals(pptStatus);
    }
    
    @Override
    public String toString() {
        return "PptTaskStatusResponse{" +
                "pptStatus='" + pptStatus + '\'' +
                ", pptUrl='" + pptUrl + '\'' +
                ", errMsg='" + errMsg + '\'' +
                ", totalPages=" + totalPages +
                ", donePages=" + donePages +
                '}';
    }
}