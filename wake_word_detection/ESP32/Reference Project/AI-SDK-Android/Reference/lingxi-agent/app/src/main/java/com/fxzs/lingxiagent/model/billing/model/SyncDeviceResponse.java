package com.fxzs.lingxiagent.model.billing.model;

import java.util.List;

public class SyncDeviceResponse {

    private int code;
    private String msg;
    private boolean success;
    private List<SyncResult> data;

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public boolean isSuccess() {
        return success;
    }

    public List<SyncResult> getData() {
        return data;
    }

    public static class SyncResult {

        private String deviceModelId;
        private String deviceNo;
        private boolean success;

        public String getDeviceModelId() {
            return deviceModelId;
        }

        public String getDeviceNo() {
            return deviceNo;
        }

        public boolean isSuccess() {
            return success;
        }
    }
}