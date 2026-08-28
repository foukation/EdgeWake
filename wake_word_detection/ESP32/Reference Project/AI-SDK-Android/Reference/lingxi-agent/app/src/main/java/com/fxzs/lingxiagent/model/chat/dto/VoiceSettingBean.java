package com.fxzs.lingxiagent.model.chat.dto;

public class VoiceSettingBean {
    /**
     * 声音名称
     */
    String name;
    /**
     * 声音描述
     */
    String des;
    /**
     * 声音头像
     */
    String avatar;
    /**
     * 发音人
     */
    int per;
    /**
     * 语速
     */
    int spd = 5;
    /**
     * 音调
     */
    int pit = 5;
    /**
     * 音量
     */
    int vol = 5;
    /**
     * 是否选中
     */
    boolean isSelect;

    public VoiceSettingBean(String name, String des, String avatar, boolean isSelect) {
        this.name = name;
        this.des = des;
        this.avatar = avatar;
        this.isSelect = isSelect;
    }

    public VoiceSettingBean(String name, String des, int per, boolean isSelect) {
        this.name = name;
        this.des = des;
        this.per = per;
        this.isSelect = isSelect;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDes() {
        return des;
    }

    public void setDes(String des) {
        this.des = des;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public boolean isSelect() {
        return isSelect;
    }

    public void setSelect(boolean select) {
        isSelect = select;
    }

    public void setPer(int per) {
        this.per = per;
    }

    public void setSpd(int spd) {
        this.spd = spd;
    }

    public void setPit(int pit) {
        this.pit = pit;
    }

    public void setVol(int vol) {
        this.vol = vol;
    }

    public int getPer() {
        return per;
    }

    public int getSpd() {
        return spd;
    }

    public int getPit() {
        return pit;
    }

    public int getVol() {
        return vol;
    }

}
