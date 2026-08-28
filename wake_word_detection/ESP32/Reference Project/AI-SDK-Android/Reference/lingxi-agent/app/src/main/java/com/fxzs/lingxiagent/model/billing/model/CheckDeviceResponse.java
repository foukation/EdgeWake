package com.fxzs.lingxiagent.model.billing.model;

public class CheckDeviceResponse {

    private String deviceNo;
    private String deviceModelId;
    private boolean exists;

    public String getDeviceNo() {
        return deviceNo;
    }

    public String getDeviceModelId() {
        return deviceModelId;
    }

    public boolean isExists() {
        return exists;
    }
}