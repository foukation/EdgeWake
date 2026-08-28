package com.fxzs.lingxiagent.model.meeting.dto;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class LanguageDto implements Serializable {
    @SerializedName("code")
    private String code;

    @SerializedName("name")
    private String name;

    @SerializedName("nameEn")
    private String nameEn;

    @SerializedName("supported")
    private Boolean supported;

    // Getters and setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameEn() {
        return nameEn;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public Boolean getSupported() {
        return supported;
    }

    public void setSupported(Boolean supported) {
        this.supported = supported;
    }
}