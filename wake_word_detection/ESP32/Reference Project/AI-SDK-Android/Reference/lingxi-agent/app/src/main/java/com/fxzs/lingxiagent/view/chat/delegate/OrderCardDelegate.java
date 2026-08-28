package com.fxzs.lingxiagent.view.chat.delegate;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardOrderEntity;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;

import timber.log.Timber;

/**
 * 订票卡片委托
 * 负责处理订票查询卡片的显示，包括：
 * - 订票查询按钮的点击处理
 * - WebView页面跳转功能
 * - 订票深度链接的处理
 */
public class OrderCardDelegate extends CardMessageDelegate {
    
    private static final String TAG = "OrderCardDelegate";
    
    public OrderCardDelegate() {
        super(ChatAdapter.TYPE_ASSISTANT_ORDER_CARD, R.layout.lingxi_card_travel_order);
    }
    
    @Override
    protected RecyclerView.ViewHolder createViewHolder(View view) {
        return new ChatAdapter.ChatViewHolder(view);
    }
    
    @Override
    protected void onBindViewHolderInternal(RecyclerView.ViewHolder holder, ChatMessage message, 
                                           int position, ChatAdapterContext context) {
        ChatAdapter.ChatViewHolder orderHolder = (ChatAdapter.ChatViewHolder) holder;
        
        Timber.tag(TAG).d( "setAssistantOrderCard: position=" + position);
        
        // 设置订票查询按钮点击事件，完全保持与原有逻辑一致
        if (orderHolder.tvOrderQuery != null) {
            orderHolder.tvOrderQuery.setOnClickListener(view -> {
                ChatCardOrderEntity orderEntity = message.getOrderEntity();
                if (orderEntity != null && orderEntity.getDeepLinkUrl() != null) {
                    startWebViewActivity(context.getContext(), orderEntity.getDeepLinkUrl(), "灵犀-出行规划");
                    Timber.tag(TAG).d( "Order query clicked: " + orderEntity.getDeepLinkUrl());
                } else {
                    Timber.tag(TAG).w( "Order entity or deep link URL is null");
                }
            });
        } else {
            Timber.tag(TAG).w("tvOrderQuery is null");
        }
    }
    
    @Override
    protected Class<? extends RecyclerView.ViewHolder> getExpectedViewHolderClass() {
        return ChatAdapter.ChatViewHolder.class;
    }
}