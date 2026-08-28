package com.fxzs.lingxiagent.view.chat.delegate;

import android.content.Intent;
import android.provider.Settings;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;

import timber.log.Timber;

/**
 * 无障碍权限卡片委托
 * 负责处理无障碍权限请求卡片的显示，包括：
 * - 权限请求按钮的点击处理
 * - 系统无障碍设置页面跳转功能
 * - 无障碍服务权限管理界面的启动
 */
public class AccessibilityPermissionCardDelegate extends CardMessageDelegate {
    
    private static final String TAG = "AccessibilityPermissionCardDelegate";
    
    public AccessibilityPermissionCardDelegate() {
        super(ChatAdapter.TYPE_ASSISTANT_ACC_PERM_CARD, R.layout.lingxi_card_permission_accessibility);
    }
    
    @Override
    protected RecyclerView.ViewHolder createViewHolder(View view) {
        return new ChatAdapter.ChatViewHolder(view);
    }
    
    @Override
    protected void onBindViewHolderInternal(RecyclerView.ViewHolder holder, ChatMessage message, 
                                           int position, ChatAdapterContext context) {
        ChatAdapter.ChatViewHolder permHolder = (ChatAdapter.ChatViewHolder) holder;
        
        Timber.tag(TAG).d( "setAssistantAccPermCard: position=" + position);
        
        // 设置无障碍权限按钮点击事件，完全保持与原有逻辑一致
        if (permHolder.goOpenAccBtn != null) {
            permHolder.goOpenAccBtn.setOnClickListener(view -> {
                try {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    context.getContext().startActivity(intent);
                    Timber.tag(TAG).d( "Opened accessibility permission settings");
                } catch (Exception e) {
                    Timber.tag(TAG).e( "Failed to open accessibility permission settings", e);
                }
            });
        } else {
            Timber.tag(TAG).w( "goOpenAccBtn is null");
        }
    }
    
    @Override
    protected Class<? extends RecyclerView.ViewHolder> getExpectedViewHolderClass() {
        return ChatAdapter.ChatViewHolder.class;
    }
}