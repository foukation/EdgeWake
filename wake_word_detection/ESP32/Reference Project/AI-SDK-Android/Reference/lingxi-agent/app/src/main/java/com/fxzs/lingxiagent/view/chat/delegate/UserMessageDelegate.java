package com.fxzs.lingxiagent.view.chat.delegate;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;
import com.fxzs.lingxiagent.view.dialog.ChatTextActionDialog;
import com.fxzs.lingxiagent.view.dialog.TextSelectorView;

import timber.log.Timber;

/**
 * 用户消息委托
 * 负责处理用户发送的普通文本消息的显示和交互
 * 
 * 主要功能：
 * - 显示用户消息内容
 * - 处理消息选择状态（多选模式）
 * - 处理长按操作（复制、分享等）
 */
public class UserMessageDelegate extends BaseViewTypeDelegate {
    
    public UserMessageDelegate() {
        super(0, R.layout.item_user_message); // TYPE_USER = 0
    }
    
    @Override
    protected RecyclerView.ViewHolder createViewHolder(View view) {
        return new ChatAdapter.ChatViewHolder(view);
    }
    
    @Override
    protected void onBindViewHolderInternal(RecyclerView.ViewHolder holder, ChatMessage message, 
                                           int position, ChatAdapterContext context) {
        ChatAdapter.ChatViewHolder userHolder = (ChatAdapter.ChatViewHolder) holder;
        
        // 设置用户消息内容
        setText(userHolder.messageText, message.getMessage());
        
        // 设置选择状态
        setupSelectionState(userHolder, message, context);
        
        // 设置长按事件
        setupLongClickListener(userHolder, message, position, context);
    }
    
    @Override
    protected Class<? extends RecyclerView.ViewHolder> getExpectedViewHolderClass() {
        return ChatAdapter.ChatViewHolder.class;
    }
    
    /**
     * 设置消息选择状态
     */
    private void setupSelectionState(ChatAdapter.ChatViewHolder holder, ChatMessage message, 
                                    ChatAdapterContext context) {
        if (context.isSelectable()) {
            // 多选模式：显示选择按钮
            setVisibility(holder.radio, true);
            setVisibility(holder.radioSelected, message.getIsSelected());
        } else {
            // 普通模式：隐藏选择按钮
            setVisibility(holder.radio, false);
            setVisibility(holder.radioSelected, false);
        }
    }
    
    /**
     * 设置长按事件监听器
     */
    private void setupLongClickListener(ChatAdapter.ChatViewHolder holder, ChatMessage message, 
                                       int position, ChatAdapterContext context) {
//        holder.messageText.setOnLongClickListener(view -> {
//            // 显示文本操作对话框
//            showTextActionDialog(holder, message, position, context);
//            return true;
//        });

        holder.messageText.setOnLongClickListener(context.getOnLongClickListener());
    }
    
    /**
     * 显示文本操作对话框（复制、分享、删除等）
     */
    private void showTextActionDialog(ChatAdapter.ChatViewHolder holder, ChatMessage message, 
                                     int position, ChatAdapterContext context) {
        ChatTextActionDialog dialog = new ChatTextActionDialog(context.getContext());
        dialog.showAtLocation(0, 0);
        
        // 设置操作回调
        dialog.setOnActionClickListener(new ChatTextActionDialog.OnActionClickListener() {
            @Override
            public void onLikeClick() {
                // 点赞功能暂未实现
            }
            
            @Override
            public void onDislikeClick() {
                // 点踩功能暂未实现
            }
            
            @Override
            public void onCopyClick() {
                copyMessageText(message, context);
            }
            
            @Override
            public void onSelectClick() {
                enableTextSelection(holder);
            }
            
            @Override
            public void onReadClick() {
                readMessageText(message, context);
            }
            
            @Override
            public void onShareClick() {
                shareMessage(message, position, context);
            }
            
            @Override
            public void onCollectClick() {
                // 收藏功能暂未实现
            }
            
            @Override
            public void onMoreClick() {
                // 更多功能暂未实现
            }
            
            @Override
            public void onDeleteClick() {
                deleteMessage(message, position, context);
            }
        });
    }
    
    /**
     * 复制消息文本
     */
    private void copyMessageText(ChatMessage message, ChatAdapterContext context) {
        String text = message.getMessage();
        if (text != null && !text.isEmpty()) {
            // 使用系统剪贴板复制文本
            android.content.ClipboardManager clipboard = 
                (android.content.ClipboardManager) context.getContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("用户消息", text);
            clipboard.setPrimaryClip(clip);
            
            context.showToast("已复制到剪贴板");
        }
    }
    
    /**
     * 启用文本选择模式
     */
    private void enableTextSelection(ChatAdapter.ChatViewHolder holder) {
        if (holder.messageText instanceof TextSelectorView) {
            TextSelectorView selectorView = (TextSelectorView) holder.messageText;
            selectorView.setSelectionEnabled(true);
        }
    }
    
    /**
     * 朗读消息文本
     */
    private void readMessageText(ChatMessage message, ChatAdapterContext context) {
        String text = message.getMessage();
        if (text != null && !text.isEmpty()) {
            // 使用 TTS 朗读文本
            if (context.getTTSManager() != null) {
                context.getTTSManager().textForceToAudio(text);
            }
        }
    }
    
    /**
     * 分享消息
     */
    private void shareMessage(ChatMessage message, int position, ChatAdapterContext context) {
        // 选中当前消息
        message.setIsSelected(true);
        
        // 通知回调处理分享
        if (context.getMsgActionCallback() != null) {
            // 这里可以添加分享相关的回调
            context.showToast("分享功能开发中");
        }
    }
    
    /**
     * 删除消息
     */
    private void deleteMessage(ChatMessage message, int position, ChatAdapterContext context) {
        Timber.tag("UserMessageDelegate").d( "点击删除，position: " + position);
        Timber.tag("UserMessageDelegate").d( "准备删除消息，ID: " + (message != null ? message.getId() : "null"));
        Timber.tag("UserMessageDelegate").d( "消息内容: " + (message != null ? message.getMessage() : "null"));
        Timber.tag("UserMessageDelegate").d( "消息类型: " + (message != null ? message.getMsgType() : "null"));
        
        if (message != null && message.getId() != null) {
            // 使用回调通知ViewModel删除消息
            if (context.getMessageActionCallback() != null) {
                context.getMessageActionCallback().onDeleteMessage(message.getId().longValue(),true);
            } else {
                // 如果没有设置回调，显示错误信息
                Timber.tag("UserMessageDelegate").w("MessageActionCallback is null, cannot delete message");
                context.showToast("删除功能未正确初始化");
            }
        } else {
            Timber.tag("UserMessageDelegate").w( "无法删除消息：消息ID为空");
            context.showToast("该消息无法删除（本地消息）");
        }
    }
    
}