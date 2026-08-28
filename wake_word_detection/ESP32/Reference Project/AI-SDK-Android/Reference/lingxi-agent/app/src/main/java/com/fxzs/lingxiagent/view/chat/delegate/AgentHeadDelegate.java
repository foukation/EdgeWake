package com.fxzs.lingxiagent.view.chat.delegate;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.callback.MsgActionCallback;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.util.ZUtil.ImageUtil;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;
import com.fxzs.lingxiagent.view.common.NoMultiClickListener;

import java.util.List;

import timber.log.Timber;

/**
 * 智能体头部消息委托
 * 负责处理智能体头部消息的显示，包括：
 * - 智能体头像的网络加载和圆角处理
 * - 智能体提示文本的设置
 * - 智能体相关的特殊交互处
 */
public class AgentHeadDelegate extends BaseViewTypeDelegate {
    
    private static final String TAG = "AgentHeadDelegate";
    
    public AgentHeadDelegate() {
        super(ChatAdapter.TYPE_USER_HEAD_AGENT, R.layout.item_meeting_head_message); // 使用相同的布局
    }
    
    @Override
    protected RecyclerView.ViewHolder createViewHolder(View view) {
        return new ChatAdapter.ChatViewHolder(view);
    }
    
    @Override
    protected void onBindViewHolderInternal(RecyclerView.ViewHolder holder, ChatMessage message, 
                                           int position, ChatAdapterContext context) {
        ChatAdapter.ChatViewHolder agentHolder = (ChatAdapter.ChatViewHolder) holder;
        ChatAdapter chatAdapter = (ChatAdapter) context;

        agentHolder.llNexusPilot.removeAllViews();
        List<String> nexusPilotList = message.getNexusPilotList();
        if (nexusPilotList != null){
            if (chatAdapter.getItemCount() > 1){
                agentHolder.llNexusPilot.setVisibility(View.GONE);
            }else {
                agentHolder.llNexusPilot.setVisibility(View.VISIBLE);
                LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());
                for (int i = 0; i < nexusPilotList.size(); i++) {
                    View subView = inflater.inflate(R.layout.item_neuxs_message, agentHolder.llNexusPilot, false);
                    TextView tvSub = subView.findViewById(R.id.tv_nexus_thinking);
                    tvSub.setText(nexusPilotList.get(i));
                    final int subIndex = i;
                    subView.setOnClickListener(new NoMultiClickListener() {
                        @Override
                        public void onNoMultiClick(View v) {
                            MsgActionCallback callback = context.getMsgActionCallback();
                            if (callback != null) {
                                callback.sendMsg(nexusPilotList.get(subIndex) );
                            }
                        }
                    });

                    agentHolder.llNexusPilot.addView(subView);
                }
            }

        }

        // 设置智能体提示文本，完全保持与原有逻辑一致
        if (agentHolder.tv_agent_hint != null && message.getMessage() != null) {
            agentHolder.tv_agent_hint.setText(message.getMessage());
            Timber.tag(TAG).d( "Set agent head hint text: " + message.getMessage());
        } else {
            Timber.tag(TAG).w("tv_agent_hint is null or message text is null");
        }

        // 设置智能体头像，完全保持与原有逻辑一致
        if (agentHolder.iv_agent != null && message.getAvatar() != null && !message.getAvatar().isEmpty()) {
            // 清除默认背景，避免与网络头像重叠
            agentHolder.iv_agent.setBackground(null);
            ImageUtil.netRadius(context.getContext(), message.getAvatar(), agentHolder.iv_agent);
            Timber.tag(TAG).d( "Set agent head avatar: " + message.getAvatar());
        } else {
            Timber.tag(TAG).w("iv_agent is null or avatar URL is empty");
        }
    }


    @Override
    protected Class<? extends RecyclerView.ViewHolder> getExpectedViewHolderClass() {
        return ChatAdapter.ChatViewHolder.class;
    }
}