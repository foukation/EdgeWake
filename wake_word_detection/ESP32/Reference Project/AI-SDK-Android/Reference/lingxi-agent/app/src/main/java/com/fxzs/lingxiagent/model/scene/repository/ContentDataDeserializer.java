package com.fxzs.lingxiagent.model.scene.repository;


import com.fxzs.lingxiagent.model.scene.dto.DeltaContentBean;
import com.fxzs.lingxiagent.model.scene.dto.DeltaContentData;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class ContentDataDeserializer implements JsonDeserializer<DeltaContentData> {
    @Override
    public DeltaContentData deserialize(JsonElement json, Type typeOfT,
                                   JsonDeserializationContext context)
            throws JsonParseException {

        DeltaContentData contentData = new DeltaContentData();

        // 判断 data 字段是字符串还是对象
        if (json.isJsonPrimitive()) {
            // 处理字符串类型
            contentData.textValue = json.getAsString();
        } else if (json.isJsonObject()) {
            // 处理对象类型
            JsonObject jsonObject = json.getAsJsonObject();
            if (jsonObject.has("text")) {
                contentData.data = context.deserialize(json, DeltaContentBean.class);
            } else if (jsonObject.has("templateId")){
                contentData.content = jsonObject.get("content").getAsString();
                contentData.templateId = jsonObject.get("templateId").getAsString();
            } else if (jsonObject.has("url")){
                contentData.url = jsonObject.get("url").getAsString();
            }else {
                // 处理其他可能的对象结构
                // (根据实际API响应可能需要扩展)
            }
        }

        return contentData;
    }
}
