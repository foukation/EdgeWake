package com.fxzs.lingxiagent.model.wps;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class WpsTaskResponse {

    @SerializedName("code")
    public int code;

    @SerializedName("message")
    public String message;

    @SerializedName("data")
    public Data data;

    public static class Data {
        // For task creation
        @SerializedName("task_id")
        public String taskId;

        // For task status - can be String ("success") or Integer (1)
        @SerializedName("status")
        public Object status;

        @SerializedName("progress")
        public double progress;

        @SerializedName("message")
        public String message;

        @SerializedName("result")
        public Result result;

        @SerializedName("task")
        public TaskInfo task;

        // For pdf-to-docs status
        @SerializedName("download_url")
        public String downloadUrl;
    }

    public static class Result {
        @SerializedName("images")
        public List<UrlItem> images;

        @SerializedName("pdfs")
        public List<UrlItem> pdfs;

        @SerializedName("txts")
        public List<UrlItem> txts;
    }

    public static class UrlItem {
        @SerializedName("url")
        public String url;

        @SerializedName("size")
        public int size;
    }

    public static class TaskInfo {
        @SerializedName("elapsed")
        public int elapsed;

        @SerializedName("resource_size")
        public int resourceSize;
    }
}
