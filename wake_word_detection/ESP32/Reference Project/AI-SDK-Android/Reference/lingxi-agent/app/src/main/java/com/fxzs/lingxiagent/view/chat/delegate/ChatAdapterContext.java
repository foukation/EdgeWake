package com.fxzs.lingxiagent.view.chat.delegate;

import android.content.Context;
import android.content.Intent;

import com.fxzs.lingxiagent.model.chat.callback.MsgActionCallback;
import com.fxzs.lingxiagent.model.chat.callback.OnFileItemClick;
import com.fxzs.lingxiagent.util.audio.TTSManager;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;

/**
 * ChatAdapter 上下文接口
 * 为委托类提供访问适配器功能和回调的统一接口
 * 
 * 这个接口封装了委托类可能需要的所有适配器级别的功能，
 * 避免委托类直接依赖 ChatAdapter 的具体实现
 */
public interface ChatAdapterContext {
    
    /**
     * 获取 Android Context
     * @return Context 实例
     */
    Context getContext();
    
    /**
     * 获取消息操作回调
     * @return 消息操作回调接口
     */
    MsgActionCallback getMsgActionCallback();
    
    /**
     * 获取文件项点击回调
     * @return 文件点击回调接口
     */
    OnFileItemClick getOnFileItemClick();
    
    /**
     * 获取当前是否处于选择模式
     * @return true 如果当前可以选择消息
     */
    boolean isSelectable();
    
    /**
     * 获取 TTS 管理器
     * @return TTS 管理器实例
     */
    TTSManager getTTSManager();
    
    /**
     * 通知指定位置的项目已更改
     * @param position 更改的位置
     */
    void notifyItemChanged(int position);
    
    /**
     * 通知指定位置的项目已移除
     * @param position 移除的位置
     */
    void notifyItemRemoved(int position);
    
    /**
     * 通知指定范围的项目已更改
     * @param positionStart 起始位置
     * @param itemCount 项目数量
     */
    void notifyItemRangeChanged(int positionStart, int itemCount);
    
    /**
     * 显示 Toast 消息
     * @param message 要显示的消息
     */
    void showToast(String message);
    
    /**
     * 启动 Activity
     * @param intent Intent 对象
     */
    void startActivity(Intent intent);
    
    /**
     * 获取消息操作回调（用于删除等操作）
     * @return 消息操作回调接口，可能为 null
     */
    ChatAdapter.OnMessageActionCallback getMessageActionCallback();
    
    /**
     * 获取当前聊天类型
     * @return 聊天类型（如会议、普通聊天等）
     */
    int getChatType();
    
    /**
     * 获取模型类型
     * @return 模型类型枚举
     */
    Object getModelType(); // 使用 Object 避免循环依赖，实际类型为 HomeModelEntity.ModelType
    
    /**
     * 获取聊天消息总数
     * @return 消息总数
     */
    int getItemCount();
    
    /**
     * 获取长按监听器
     * @return 长按监听器
     */
    android.view.View.OnLongClickListener getOnLongClickListener();
    
    /**
     * 处理分享点击事件
     * @param position 消息位置
     */
    void onShareClick(ChatAdapter.ChatViewHolder holder, int position);
    
    /**
     * 获取指定位置的消息
     * @param position 消息位置
     * @return 指定位置的消息，如果位置无效则返回 null
     */
    com.fxzs.lingxiagent.model.chat.dto.ChatMessage getMessage(int position);
}