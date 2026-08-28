package com.fxzs.lingxiagent.view.chat.delegate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;

import java.util.List;

/**
 * ViewType 委托基类
 * 提供委托类的通用功能实现，减少重复代码
 * 
 * 子类只需要实现特定的业务逻辑，通用功能由基类提供
 */
public abstract class BaseViewTypeDelegate implements ViewTypeDelegate {
    
    protected final int viewType;
    protected final int layoutResId;
    
    /**
     * 构造函数
     * @param viewType 视图类型常量
     * @param layoutResId 布局资源 ID
     */
    protected BaseViewTypeDelegate(int viewType, int layoutResId) {
        this.viewType = viewType;
        this.layoutResId = layoutResId;
    }
    
    @Override
    public int getViewType() {
        return viewType;
    }
    
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, LayoutInflater inflater) {
        View view = inflater.inflate(layoutResId, parent, false);
        return createViewHolder(view);
    }
    
    /**
     * 创建具体的 ViewHolder 实例
     * 子类需要实现此方法来创建对应的 ViewHolder
     * @param view 已填充的视图
     * @return ViewHolder 实例
     */
    protected abstract RecyclerView.ViewHolder createViewHolder(View view);
    
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, ChatMessage message, 
                                int position, ChatAdapterContext context) {
        // 类型安全检查
        if (!isValidViewHolder(holder)) {
            android.util.Log.e(getClass().getSimpleName(), 
                "Invalid ViewHolder type for delegate: expected " + getExpectedViewHolderClass().getSimpleName() + 
                ", got " + holder.getClass().getSimpleName());
            return;
        }
        
        // 调用具体的绑定实现
        onBindViewHolderInternal(holder, message, position, context);
    }
    
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, ChatMessage message, int position, 
                                List<Object> payloads, ChatAdapterContext context) {
        // 默认实现：检查是否有特殊的 payload 处理
        if (payloads != null && !payloads.isEmpty() && handlePayloads(holder, message, position, payloads, context)) {
            return; // payload 已处理
        }
        
        // 回退到标准绑定
        onBindViewHolder(holder, message, position, context);
    }
    
    /**
     * 处理 payload 更新
     * 子类可以重写此方法来处理特定的 payload 更新
     * @return true 如果 payload 已被处理，false 则回退到标准绑定
     */
    protected boolean handlePayloads(RecyclerView.ViewHolder holder, ChatMessage message, int position, 
                                   List<Object> payloads, ChatAdapterContext context) {
        return false; // 默认不处理 payloads
    }
    
    /**
     * 具体的数据绑定实现
     * 子类需要实现此方法来处理数据绑定逻辑
     */
    protected abstract void onBindViewHolderInternal(RecyclerView.ViewHolder holder, ChatMessage message, 
                                                    int position, ChatAdapterContext context);
    
    /**
     * 检查 ViewHolder 类型是否正确
     * 子类可以重写此方法来进行类型检查
     */
    protected boolean isValidViewHolder(RecyclerView.ViewHolder holder) {
        return getExpectedViewHolderClass().isInstance(holder);
    }
    
    /**
     * 获取期望的 ViewHolder 类型
     * 子类需要实现此方法来指定期望的 ViewHolder 类型
     */
    protected abstract Class<? extends RecyclerView.ViewHolder> getExpectedViewHolderClass();
    
    // 通用工具方法
    
    /**
     * 设置视图可见性
     * @param view 目标视图
     * @param visible 是否可见
     */
    protected void setVisibility(View view, boolean visible) {
        if (view != null) {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
    
    /**
     * 设置 TextView 文本
     * @param textView 目标 TextView
     * @param text 要设置的文本
     */
    protected void setText(TextView textView, String text) {
        if (textView != null) {
            textView.setText(text != null ? text : "");
        }
    }
    
    /**
     * 安全地设置点击监听器
     * @param view 目标视图
     * @param listener 点击监听器
     */
    protected void setOnClickListener(View view, View.OnClickListener listener) {
        if (view != null) {
            view.setOnClickListener(listener);
        }
    }
    
    /**
     * 安全地设置长按监听器
     * @param view 目标视图
     * @param listener 长按监听器
     */
    protected void setOnLongClickListener(View view, View.OnLongClickListener listener) {
        if (view != null) {
            view.setOnLongClickListener(listener);
        }
    }
    
    /**
     * 检查消息是否被选中
     * @param message 聊天消息
     * @return true 如果消息被选中
     */
    protected boolean isMessageSelected(ChatMessage message) {
        return message != null && message.getIsSelected();
    }
    
    /**
     * 获取消息内容，处理 null 情况
     * @param message 聊天消息
     * @return 消息内容，null 时返回空字符串
     */
    protected String getMessageContent(ChatMessage message) {
        return message != null && message.getMessage() != null ? message.getMessage() : "";
    }
    
    /**
     * ViewHolder 回收时的清理逻辑
     * 子类可以重写此方法来添加资源清理逻辑
     * @param holder 要回收的 ViewHolder
     * @param context ChatAdapter 上下文
     */
    public void onViewRecycled(RecyclerView.ViewHolder holder, ChatAdapterContext context) {
        // 基类默认不进行任何清理
        // 子类可以重写此方法来实现特定的清理逻辑
    }
}