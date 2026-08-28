package com.fxzs.lingxiagent.network.ZNet.bean;

public class SmsLoginBean {
//     "userId": 1330,
//             "accessToken": "29cc0de9434b46bfaed4268aadc9c304",
//             "refreshToken": "47fb389cb98d4055a0b4bd100c292618",
//             "expiresTime": 1754230297702,
//             "openid": null,
//             "newRegister": false
    private int userId;
    private String accessToken;
    private String refreshToken;
    private long expiresTime;
    private String openid;
    private boolean newRegister;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public long getExpiresTime() {
        return expiresTime;
    }

    public void setExpiresTime(long expiresTime) {
        this.expiresTime = expiresTime;
    }

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public boolean isNewRegister() {
        return newRegister;
    }

    public void setNewRegister(boolean newRegister) {
        this.newRegister = newRegister;
    }
}
