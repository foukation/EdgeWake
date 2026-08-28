package com.fxzs.lingxiagent.view.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fxzs.lingxiagent.R;

import timber.log.Timber;

public class ChatMessageView extends LinearLayout {
    private static final String TAG = "ChatMessageView";
    
    private UserInputDisplayView userInputView;
    private AIResponseView aiResponseView;
    
    public ChatMessageView(@NonNull Context context) {
        super(context);
        init(context);
    }
    
    public ChatMessageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public ChatMessageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        Timber.tag(TAG).d( "ChatMessageView.init() 开始");
        LayoutInflater.from(context).inflate(R.layout.view_chat_message, this, true);
        
        userInputView = findViewById(R.id.user_input_view);
        aiResponseView = findViewById(R.id.ai_response_view);
        
        Timber.tag(TAG).d( "视图绑定结果:");
        Timber.tag(TAG).d( "  - userInputView: " + (userInputView != null ? "找到" : "未找到"));
        Timber.tag(TAG).d( "  - aiResponseView: " + (aiResponseView != null ? "找到" : "未找到"));
        Timber.tag(TAG).d( "ChatMessageView.init() 完成");
    }
    
    public void setUserInput(String input) {
        if (input != null && !input.isEmpty()) {
            userInputView.setText(input);
            userInputView.setVisibility(View.VISIBLE);
        } else {
            userInputView.setVisibility(View.GONE);
        }
    }
    
    public void setAIResponse(String response) {
        aiResponseView.setContent(response);
        aiResponseView.showCopyButton(true);
        aiResponseView.showSpeakButton(true);
    }
    
    public void setAIResponseWithThinking(String response, int thinkingTime) {
        aiResponseView.setAsThinkingResponse(response, thinkingTime);
    }
    
    public void setAIResponseWithThinking(String response, int thinkingTime, String thinkingText) {
        Timber.tag(TAG).d( "ChatMessageView.setAIResponseWithThinking() 被调用");
        Timber.tag(TAG).d( "  - response: " + response.substring(0, Math.min(50, response.length())) + "...");
        Timber.tag(TAG).d( "  - thinkingTime: " + thinkingTime);
        Timber.tag(TAG).d( "  - thinkingText: " + (thinkingText != null ? thinkingText.substring(0, Math.min(50, thinkingText.length())) + "..." : "null"));
        aiResponseView.setAsThinkingResponse(response, thinkingTime, thinkingText);
    }
    
    public void setAIResponseWithTitle(String title, String response) {
        aiResponseView.setAsTopicResponse(title, response);
    }
    
    public void setOnRefreshClickListener(AIResponseView.OnRefreshClickListener listener) {
        aiResponseView.setOnRefreshClickListener(listener);
    }
    
    public void showUserInput(boolean show) {
        userInputView.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    
    public void release() {
        aiResponseView.release();
    }
    
    public AIResponseView getAIResponseView() {
        return aiResponseView;
    }
    
    public UserInputDisplayView getUserInputView() {
        return userInputView;
    }
    
    /**
     * 设置带有思维链的AI响应
     * @param userInput 用户输入
     * @param aiResponse AI响应内容
     * @param thinkingTime 思考时间（秒）
     * @param thinkingProcess 思维过程文本
     */
    public void setMessageWithThinking(String userInput, String aiResponse, int thinkingTime, String thinkingProcess) {
        Timber.tag(TAG).d( "设置带思维链的消息，思考时间: " + thinkingTime + "秒");
        
        // 设置用户输入
        setUserInput(userInput);
        
        // 设置AI响应和思维链
        if (thinkingProcess != null && !thinkingProcess.isEmpty()) {
            aiResponseView.setAsThinkingResponse(aiResponse, thinkingTime, thinkingProcess);
        } else {
            aiResponseView.setAsThinkingResponse(aiResponse, thinkingTime);
        }
    }
    
    /**
     * 设置简单的对话消息（无思维链）
     * @param userInput 用户输入
     * @param aiResponse AI响应内容
     */
    public void setSimpleMessage(String userInput, String aiResponse) {
        Timber.tag(TAG).d( "设置简单消息");
        setUserInput(userInput);
        setAIResponse(aiResponse);
    }
    
    /**
     * 仅显示AI消息（无用户输入）
     * @param aiResponse AI响应内容
     * @param thinkingTime 思考时间（秒），如果为0则不显示思维链
     * @param thinkingProcess 思维过程文本
     */
    public void setAIMessageOnly(String aiResponse, int thinkingTime, String thinkingProcess) {
        showUserInput(false);
        
        if (thinkingTime > 0) {
            aiResponseView.setAsThinkingResponse(aiResponse, thinkingTime, thinkingProcess);
        } else {
            setAIResponse(aiResponse);
        }
    }
}