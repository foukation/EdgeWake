package com.fxzs.lingxiagent.network.ZNet.bean;

import java.util.List;

public class SSEFileAnalyseBean {
//    {"RequestId":"fa95e726-04de-461d-abb8-0736d8cfeb4e","TaskId":"fa95e726-04de-461d-abb8-0736d8cfeb4e","ResponseType":"PROGRESS","Progress":"80","ProgressMessage":"文件解析中(1/1)，成功：1，失败：0","DocumentRecognizeResultUrl":"","FailedPages":[],"StatusCode":"","success_page_num":0,"fail_page_num":0}

    String RequestId;
    String TaskId;
    String ResponseType;
    String Progress;
    String ProgressMessage;
    String DocumentRecognizeResultUrl;
    List<String> FailedPages;
    String StatusCode;
    int success_page_num;
    int fail_page_num;

    public String getRequestId() {
        return RequestId;
    }

    public void setRequestId(String requestId) {
        RequestId = requestId;
    }

    public String getTaskId() {
        return TaskId;
    }

    public void setTaskId(String taskId) {
        TaskId = taskId;
    }

    public String getResponseType() {
        return ResponseType;
    }

    public void setResponseType(String responseType) {
        ResponseType = responseType;
    }

    public String getProgress() {
        return Progress;
    }

    public void setProgress(String progress) {
        Progress = progress;
    }

    public String getProgressMessage() {
        return ProgressMessage;
    }

    public void setProgressMessage(String progressMessage) {
        ProgressMessage = progressMessage;
    }

    public String getDocumentRecognizeResultUrl() {
        return DocumentRecognizeResultUrl;
    }

    public void setDocumentRecognizeResultUrl(String documentRecognizeResultUrl) {
        DocumentRecognizeResultUrl = documentRecognizeResultUrl;
    }

    public List<String> getFailedPages() {
        return FailedPages;
    }

    public void setFailedPages(List<String> failedPages) {
        FailedPages = failedPages;
    }

    public String getStatusCode() {
        return StatusCode;
    }

    public void setStatusCode(String statusCode) {
        StatusCode = statusCode;
    }

    public int getSuccess_page_num() {
        return success_page_num;
    }

    public void setSuccess_page_num(int success_page_num) {
        this.success_page_num = success_page_num;
    }

    public int getFail_page_num() {
        return fail_page_num;
    }

    public void setFail_page_num(int fail_page_num) {
        this.fail_page_num = fail_page_num;
    }
}
