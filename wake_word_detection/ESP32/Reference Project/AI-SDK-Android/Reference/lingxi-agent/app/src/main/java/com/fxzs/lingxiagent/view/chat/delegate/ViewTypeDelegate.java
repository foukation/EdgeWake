package com.fxzs.lingxiagent.view.chat.delegate;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;

import java.util.List;

/**
 * ViewType 委托接口
 * 定义了每种视图类型处理器的标准契约
 * 
 * 每个具体的委托类负责处理一种特定的消息类型，包括：
 * - ViewHolder 的创建
 * - 数据绑定
 * - 生命周期管理
 */
public interface ViewTypeDelegate {
    
    /**
     * 获取该委托支持的视图类型
     * @return 视图类型常量（如 ChatAdapter.TYPE_USER）
     */
    int getViewType();
    
    /**
     * 创建 ViewHolder
     * @param parent 父容器
     * @param inflater 布局填充器
     * @return 创建的 ViewHolder 实例
     */
    RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, LayoutInflater inflater);
    
    /**
     * 绑定数据到 ViewHolder
     * @param holder ViewHolder 实例
     * @param message 聊天消息数据
     * @param position 在列表中的位置
     * @param context 适配器上下文，提供回调和工具方法
     */
    void onBindViewHolder(RecyclerView.ViewHolder holder, ChatMessage message, int position, ChatAdapterContext context);
    
    /**
     * 处理 payload 更新（可选实现）
     * 用于增量更新，如流式消息更新
     * @param holder ViewHolder 实例
     * @param message 聊天消息数据
     * @param position 在列表中的位置
     * @param payloads 更新载荷列表
     * @param context 适配器上下文
     */
    default void onBindViewHolder(RecyclerView.ViewHolder holder, ChatMessage message, int position, 
                                 List<Object> payloads, ChatAdapterContext context) {
        // 默认实现：忽略 payloads，调用标准绑定方法
        onBindViewHolder(holder, message, position, context);
    }
    
    /**
     * ViewHolder 回收时的清理工作
     * 用于释放资源，如停止动画、清理监听器等
     * @param holder 被回收的 ViewHolder
     */
    default void onViewRecycled(RecyclerView.ViewHolder holder) {
        // 默认实现：无操作
    }
    
    /**
     * ViewHolder 附加到窗口时的处理
     * @param holder ViewHolder 实例
     */
    default void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        // 默认实现：无操作
    }
    
    /**
     * ViewHolder 从窗口分离时的处理
     * @param holder ViewHolder 实例
     */
    default void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
        // 默认实现：无操作
    }
}