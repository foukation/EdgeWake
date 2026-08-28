package com.fxzs.lingxiagent.model.scene.dto;

import androidx.annotation.Nullable;

public class ContentData {
    @Nullable
    private String type;//0. 思考步骤;1. 文本消息;2. H5卡片;3. 模板卡片(见4章节);4. 系统级交互
    public DeltaContentData data;

    public ContentData() {
    }

    public ContentData(@Nullable String type, DeltaContentData data) {
        this.type = type;
        this.data = data;
    }

    // Getters
    @Nullable
    public String getType() { return type; }

    public DeltaContentData getDeltaContentData() { return data; }

    // Setters
    public void setType(String type) { this.type = type; }
    public void setDeltaContentData(DeltaContentData data) { this.data = data; }
}
