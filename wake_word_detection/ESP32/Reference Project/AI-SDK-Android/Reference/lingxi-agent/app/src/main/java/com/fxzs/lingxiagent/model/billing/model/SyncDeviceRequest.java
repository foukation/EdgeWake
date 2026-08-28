package com.fxzs.lingxiagent.model.billing.model;

public class SyncDeviceRequest {

    private final String deviceModelId;
    private final String deviceNo;
    private final String deviceName;
    private final long bindTime;

    public SyncDeviceRequest(String deviceModelId,
                             String deviceNo,
                             String deviceName,
                             long bindTime) {

        this.deviceModelId = deviceModelId;
        this.deviceNo = deviceNo;
        this.deviceName = deviceName;
        this.bindTime = bindTime;
    }

    public String getDeviceModelId() {
        return deviceModelId;
    }

    public String getDeviceNo() {
        return deviceNo;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public long getBindTime() {
        return bindTime;
    }
}