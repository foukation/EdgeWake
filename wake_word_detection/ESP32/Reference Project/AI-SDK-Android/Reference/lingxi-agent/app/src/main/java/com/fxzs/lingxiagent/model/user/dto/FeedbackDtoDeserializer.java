package com.fxzs.lingxiagent.model.user.dto;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class FeedbackDtoDeserializer implements JsonDeserializer<FeedbackDto> {
    @Override
    public FeedbackDto deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        FeedbackDto feedbackDto = new FeedbackDto();
        
        // 处理基本字段
        if (jsonObject.has("id") && !jsonObject.get("id").isJsonNull()) {
            feedbackDto.setId(jsonObject.get("id").getAsLong());
        }
        
        if (jsonObject.has("title") && !jsonObject.get("title").isJsonNull()) {
            feedbackDto.setTitle(jsonObject.get("title").getAsString());
        }
        
        if (jsonObject.has("content") && !jsonObject.get("content").isJsonNull()) {
            feedbackDto.setContent(jsonObject.get("content").getAsString());
        }
        
        // API返回的是contact，但DTO中是contactInfo
        if (jsonObject.has("contact") && !jsonObject.get("contact").isJsonNull()) {
            feedbackDto.setContactInfo(jsonObject.get("contact").getAsString());
        }
        
        if (jsonObject.has("reply") && !jsonObject.get("reply").isJsonNull()) {
            feedbackDto.setReply(jsonObject.get("reply").getAsString());
        }
        
        // 回复时间字段处理
        if (jsonObject.has("replyTime") && !jsonObject.get("replyTime").isJsonNull()) {
            long replyTime = jsonObject.get("replyTime").getAsLong();
            feedbackDto.setReplyTime(replyTime);
        }
        
        if (jsonObject.has("status") && !jsonObject.get("status").isJsonNull()) {
            feedbackDto.setStatus(jsonObject.get("status").getAsInt());
        }
        
        // 创建时间字段处理
        if (jsonObject.has("createTime") && !jsonObject.get("createTime").isJsonNull()) {
            long createTime = jsonObject.get("createTime").getAsLong();
            feedbackDto.setCreateTime(createTime);
        }

        // 更新时间字段处理
        if (jsonObject.has("updateTime") && !jsonObject.get("updateTime").isJsonNull()) {
            long updateTime = jsonObject.get("updateTime").getAsLong();
            feedbackDto.setUpdateTime(updateTime);
        }
        
        // 特殊处理 images 字段
        if (jsonObject.has("images") && !jsonObject.get("images").isJsonNull()) {
            JsonElement imagesElement = jsonObject.get("images");
            List<String> imagesList = new ArrayList<>();
            
            if (imagesElement.isJsonArray()) {
                // 如果是数组，遍历添加
                JsonArray imagesArray = imagesElement.getAsJsonArray();
                for (JsonElement element : imagesArray) {
                    if (!element.isJsonNull()) {
                        imagesList.add(element.getAsString());
                    }
                }
            } else if (imagesElement.isJsonPrimitive()) {
                // 如果是字符串，按逗号分割
                String imageString = imagesElement.getAsString();
                if (imageString != null && !imageString.isEmpty()) {
                    String[] imageArray = imageString.split(",");
                    for (String image : imageArray) {
                        String trimmedImage = image.trim();
                        if (!trimmedImage.isEmpty()) {
                            imagesList.add(trimmedImage);
                        }
                    }
                }
            }
            
            feedbackDto.setImages(imagesList);
        } else {
            feedbackDto.setImages(new ArrayList<>());
        }
        
        return feedbackDto;
    }
}