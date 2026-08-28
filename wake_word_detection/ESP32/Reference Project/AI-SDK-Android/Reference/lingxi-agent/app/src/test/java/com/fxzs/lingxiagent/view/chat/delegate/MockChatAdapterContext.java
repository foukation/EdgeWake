package com.fxzs.lingxiagent.view.chat.delegate;

import android.content.Context;
import android.content.Intent;
import com.fxzs.lingxiagent.model.chat.callback.MsgActionCallback;
import com.fxzs.lingxiagent.model.chat.callback.OnFileItemClick;
import com.fxzs.lingxiagent.util.audio.TTSManager;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;

import static org.mockito.Mockito.mock;

/**
 * 测试用的 ChatAdapterContext 模拟实现
 * 提供可控制的测试环境
 */
public class MockChatAdapterContext implements ChatAdapterContext {
    
    private Context context;
    private MsgActionCallback msgActionCallback;
    private OnFileItemClick onFileItemClick;
    private boolean isSelectable = false;
    private TTSManager ttsManager;
    private ChatAdapter.OnMessageActionCallback messageActionCallback;
    private int chatType = 0;
    private Object modelType;
    
    // 用于验证方法调用的标志
    public boolean notifyItemChangedCalled = false;
    public boolean notifyItemRemovedCalled = false;
    public boolean notifyItemRangeChangedCalled = false;
    public boolean showToastCalled = false;
    public boolean startActivityCalled = false;
    
    public int lastNotifiedPosition = -1;
    public int lastNotifiedPositionStart = -1;
    public int lastNotifiedItemCount = -1;
    public String lastToastMessage = null;
    public Intent lastStartedIntent = null;
    
    public MockChatAdapterContext() {
        this.context = mock(Context.class);
        this.msgActionCallback = mock(MsgActionCallback.class);
        this.onFileItemClick = mock(OnFileItemClick.class);
        this.ttsManager = mock(TTSManager.class);
        this.messageActionCallback = mock(ChatAdapter.OnMessageActionCallback.class);
    }
    
    @Override
    public Context getContext() {
        return context;
    }
    
    @Override
    public MsgActionCallback getMsgActionCallback() {
        return msgActionCallback;
    }
    
    @Override
    public OnFileItemClick getOnFileItemClick() {
        return onFileItemClick;
    }
    
    @Override
    public boolean isSelectable() {
        return isSelectable;
    }
    
    @Override
    public TTSManager getTTSManager() {
        return ttsManager;
    }
    
    @Override
    public void notifyItemChanged(int position) {
        notifyItemChangedCalled = true;
        lastNotifiedPosition = position;
    }
    
    @Override
    public void notifyItemRemoved(int position) {
        notifyItemRemovedCalled = true;
        lastNotifiedPosition = position;
    }
    
    @Override
    public void notifyItemRangeChanged(int positionStart, int itemCount) {
        notifyItemRangeChangedCalled = true;
        lastNotifiedPositionStart = positionStart;
        lastNotifiedItemCount = itemCount;
    }
    
    @Override
    public void showToast(String message) {
        showToastCalled = true;
        lastToastMessage = message;
    }
    
    @Override
    public void startActivity(Intent intent) {
        startActivityCalled = true;
        lastStartedIntent = intent;
    }
    
    @Override
    public ChatAdapter.OnMessageActionCallback getMessageActionCallback() {
        return messageActionCallback;
    }
    
    @Override
    public int getChatType() {
        return chatType;
    }
    
    @Override
    public Object getModelType() {
        return modelType;
    }
    
    // 设置方法用于测试配置
    
    public void setContext(Context context) {
        this.context = context;
    }
    
    public void setMsgActionCallback(MsgActionCallback msgActionCallback) {
        this.msgActionCallback = msgActionCallback;
    }
    
    public void setOnFileItemClick(OnFileItemClick onFileItemClick) {
        this.onFileItemClick = onFileItemClick;
    }
    
    public void setSelectable(boolean selectable) {
        isSelectable = selectable;
    }
    
    public void setTtsManager(TTSManager ttsManager) {
        this.ttsManager = ttsManager;
    }
    
    public void setMessageActionCallback(ChatAdapter.OnMessageActionCallback messageActionCallback) {
        this.messageActionCallback = messageActionCallback;
    }
    
    public void setChatType(int chatType) {
        this.chatType = chatType;
    }
    
    public void setModelType(Object modelType) {
        this.modelType = modelType;
    }
    
    // 重置方法用于测试清理
    
    public void reset() {
        notifyItemChangedCalled = false;
        notifyItemRemovedCalled = false;
        notifyItemRangeChangedCalled = false;
        showToastCalled = false;
        startActivityCalled = false;
        
        lastNotifiedPosition = -1;
        lastNotifiedPositionStart = -1;
        lastNotifiedItemCount = -1;
        lastToastMessage = null;
        lastStartedIntent = null;
    }
}