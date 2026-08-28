package com.fxzs.lingxiagent.view.chat.delegate;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;

import timber.log.Timber;

/**
 * 错误消息委托
 * 用于处理未注册的视图类型，显示错误信息而不是崩溃
 * 
 * 这个委托作为安全网，确保应用在遇到未知视图类型时不会崩溃，
 * 而是显示一个友好的错误消息，便于调试和用户体验
 */
public class ErrorMessageDelegate extends BaseViewTypeDelegate {
    
    private static final int ERROR_VIEW_TYPE = -1;
    
    public ErrorMessageDelegate() {
        super(ERROR_VIEW_TYPE, R.layout.item_error_message);
    }
    
    @Override
    protected RecyclerView.ViewHolder createViewHolder(View view) {
        return new ErrorMessageViewHolder(view);
    }
    
    @Override
    protected void onBindViewHolderInternal(RecyclerView.ViewHolder holder, ChatMessage message, 
                                           int position, ChatAdapterContext context) {
        ErrorMessageViewHolder errorHolder = (ErrorMessageViewHolder) holder;
        
        // 显示错误信息
        String errorMessage = String.format(
            "未知消息类型: %d\n消息内容: %s\n位置: %d", 
            message != null ? message.getMsgType() : -1,
            getMessageContent(message),
            position
        );
        
        setText(errorHolder.errorText, errorMessage);
        
        // 设置点击事件，显示详细错误信息
        setOnClickListener(errorHolder.itemView, v -> {
            if (context != null) {
                context.showToast("检测到未知消息类型，请联系开发人员");
            }
        });
        
        // 记录错误日志
        Timber.tag("ErrorMessageDelegate").e(
            String.format("Unknown message type: %d at position %d", 
                message != null ? message.getMsgType() : -1, position));
    }
    
    @Override
    protected Class<? extends RecyclerView.ViewHolder> getExpectedViewHolderClass() {
        return ErrorMessageViewHolder.class;
    }
    
    /**
     * 错误消息 ViewHolder
     */
    public static class ErrorMessageViewHolder extends RecyclerView.ViewHolder {
        public final TextView errorText;
        
        public ErrorMessageViewHolder(View itemView) {
            super(itemView);
            errorText = itemView.findViewById(R.id.error_text);
            
            // 如果找不到错误文本视图，创建一个简单的 TextView
            if (errorText == null) {
                TextView fallbackText = new TextView(itemView.getContext());
                fallbackText.setId(R.id.error_text);
                fallbackText.setPadding(16, 16, 16, 16);
                fallbackText.setTextColor(0xFFFF0000); // 红色文本
                fallbackText.setBackgroundColor(0xFFFFE6E6); // 浅红色背景
                
                if (itemView instanceof ViewGroup) {
                    ((ViewGroup) itemView).addView(fallbackText);
                }
            }
        }
    }
}