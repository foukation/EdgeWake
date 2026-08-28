package com.fxzs.lingxiagent.viewmodel.meeting;

import android.app.Application;
import android.text.TextUtils;

import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.common.ObservableField;

public class VMMeetingQA extends BaseViewModel {
    
    // 问题输入
    private final ObservableField<String> questionInput = new ObservableField<>("");
    
    // 问答内容
    private final ObservableField<String> qaContent = new ObservableField<>("");
    
    // 会议内容
    private final ObservableField<String> meetingContent = new ObservableField<>("");
    
    // 最后一个用户消息（用于ChatMessageView）
    private String lastUserMessage = "";
    
    public VMMeetingQA(Application application) {
        super(application);
    }
    
    public ObservableField<String> getQuestionInput() {
        return questionInput;
    }
    
    public ObservableField<String> getQaContent() {
        return qaContent;
    }
    
    public ObservableField<String> getMeetingContent() {
        return meetingContent;
    }
    
    public String getLastUserMessage() {
        return lastUserMessage;
    }
    
    public void setLastUserMessage(String message) {
        this.lastUserMessage = message;
    }
    
    /**
     * 提交问题并获取答案
     */
    public void askQuestion(String question, String meetingTranscription) {
        if (TextUtils.isEmpty(question) || TextUtils.isEmpty(meetingTranscription)) {
            setError("问题或会议内容为空");
            return;
        }
        
        setLoading(true);
        setError(null);
        
        // 构建问答对话内容
        String currentContent = qaContent.get();
        StringBuilder newContent = new StringBuilder();
        
        if (currentContent != null && !currentContent.trim().isEmpty()) {
            newContent.append(currentContent).append("\n\n");
        }
        
        // 添加用户问题
        newContent.append("🙋 问题: ").append(question).append("\n\n");
        
        // TODO: 这里应该调用AI API来生成答案
        // 目前使用模拟回答
        String answer = generateMockAnswer(question, meetingTranscription);
        newContent.append("🤖 回答: ").append(answer);
        
        qaContent.set(newContent.toString());
        
        setLoading(false);
        setSuccess("问答完成");
    }
    
    /**
     * 生成模拟答案（后续替换为AI API调用）
     */
    private String generateMockAnswer(String question, String meetingTranscription) {
        // 简单的关键词匹配逻辑
        String lowerQuestion = question.toLowerCase();
        
        if (lowerQuestion.contains("主要") || lowerQuestion.contains("重点") || lowerQuestion.contains("核心")) {
            return "根据会议内容分析，主要讨论了以下几个重点：\n• 会议的核心议题\n• 重要决策事项\n• 后续行动计划";
        } else if (lowerQuestion.contains("谁") || lowerQuestion.contains("负责")) {
            return "根据会议记录，相关负责人员的分工安排将在会议纪要中详细说明。";
        } else if (lowerQuestion.contains("时间") || lowerQuestion.contains("何时")) {
            return "具体的时间安排请参考会议中提到的时间节点和里程碑计划。";
        } else {
            return "基于本次会议内容，您的问题涉及的相关信息需要进一步分析。建议查看完整的会议转写内容获取更详细的答案。\n\n💡 提示：您可以尝试更具体的问题，比如询问特定的决议、时间安排或负责人信息。";
        }
    }
    
    /**
     * 清空问答内容
     */
    public void clearQAContent() {
        qaContent.set("");
        questionInput.set("");
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
    }
}