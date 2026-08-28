package com.fxzs.lingxiagent.model.scene.dto;

public class MessageData {
    private ThinkData reasoning_content;//模型思考文本输出
    private ContentData content;
    private RecommendPrompt recommendPrompts;//追问的文本

    public MessageData(ThinkData content, ContentData contentData) {
        this.reasoning_content = content;
        this.content = contentData;
    }

    // Getters
    public ThinkData getThinkData() { return reasoning_content; }
    public ContentData getContentData() { return content; }
    public RecommendPrompt getRecommendPrompt() { return recommendPrompts; }

    // Setters
    public void setContentType(ThinkData content) { this.reasoning_content = content; }
    public void setContentData(ContentData content) { this.content = content; }
    public void setRecommendPrompt(RecommendPrompt content) { this.recommendPrompts = content; }
}

