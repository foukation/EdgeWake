package com.fxzs.lingxiagent.view.chat.delegate;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;

import timber.log.Timber;

/**
 * 会议头部消息委托
 * 负责处理会议智能问答头部消息的显示，包括：
 * - 会议头部提示文本的设置
 * - AI智能体头像的显示
 * - 会议相关的特殊交互处理
 */
public class MeetingHeadDelegate extends BaseViewTypeDelegate {
    
    private static final String TAG = "MeetingHeadDelegate";
    
    public MeetingHeadDelegate() {
        super(ChatAdapter.TYPE_USER_HEAD_MEETING, R.layout.item_meeting_head_message);
    }
    
    @Override
    protected RecyclerView.ViewHolder createViewHolder(View view) {
        return new ChatAdapter.ChatViewHolder(view);
    }
    
    @Override
    protected void onBindViewHolderInternal(RecyclerView.ViewHolder holder, ChatMessage message, 
                                           int position, ChatAdapterContext context) {
        ChatAdapter.ChatViewHolder meetingHolder = (ChatAdapter.ChatViewHolder) holder;
        
        Timber.tag(TAG).d( "setMeetingHeadMessage: position=" + position);
        
        // 设置会议头部提示文本，完全保持与原有逻辑一致
        if (meetingHolder.tv_agent_hint != null && message.getMessage() != null) {
            meetingHolder.tv_agent_hint.setText(message.getMessage());
            Timber.tag(TAG).d( "Set meeting head hint text: " + message.getMessage());
        } else {
            Timber.tag(TAG).w("tv_agent_hint is null or message text is null");
        }
        
        // 注意：原代码中的头像设置被注释掉了，这里保持一致
        // if (meetingHolder.iv_agent != null && message.getAvatarRes() != 0) {
        //     ImageUtil.load(context, message.getAvatarRes(), meetingHolder.iv_agent);
        // }
    }
    
    @Override
    protected Class<? extends RecyclerView.ViewHolder> getExpectedViewHolderClass() {
        return ChatAdapter.ChatViewHolder.class;
    }
}