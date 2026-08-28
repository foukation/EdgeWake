package com.fxzs.lingxiagent.model.chat.dto;

import com.fxzs.lingxiagent.model.drawing.dto.AspectRatioDto;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingSessionDto;

import java.io.Serializable;

public class DrawingToChatBean implements Serializable {
   String prompt;
   String reference_image_url;//如果有参考图片URL，传递给新页面
    String style;
    String style_id;
    String ratio;

    //新模型直接传具体的width，height，不传ratio
    AspectRatioDto aspectRatioDto;

    /** START 从历史记录进来需要的参数*/
    DrawingSessionDto sessionDetail;
    long sessionId;
    /** END 从历史记录进来需要的参数*/



    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getReference_image_url() {
        return reference_image_url;
    }

    public void setReference_image_url(String reference_image_url) {
        this.reference_image_url = reference_image_url;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getStyle_id() {
        return style_id;
    }

    public void setStyle_id(String style_id) {
        this.style_id = style_id;
    }

    public String getRatio() {
        return ratio;
    }

    public void setRatio(String ratio) {
        this.ratio = ratio;
    }

    public DrawingSessionDto getSessionDetail() {
        return sessionDetail;
    }

    public void setSessionDetail(DrawingSessionDto sessionDetail) {
        this.sessionDetail = sessionDetail;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public long getSessionId() {
        return sessionId;
    }

    public AspectRatioDto getAspectRatioDto() {
        return aspectRatioDto;
    }

    public void setAspectRatioDto(AspectRatioDto aspectRatioDto) {
        this.aspectRatioDto = aspectRatioDto;
    }
}
