package com.fxzs.lingxiagent.view.chat.delegate;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;
import com.fxzs.lingxiagent.view.chat.ChatFileAdapter;

import timber.log.Timber;

/**
 * 文件消息委托
 * 负责处理用户发送的文件消息显示，包括：
 * - 文件适配器缓存管理
 * - 文件类型识别和图标显示
 * - 文件信息显示（名称、大小、类型）
 * - 上传进度显示
 */
public class FileMessageDelegate extends BaseViewTypeDelegate {
    
    private static final String TAG = "FileMessageDelegate";
    
    public FileMessageDelegate() {
        super(ChatAdapter.TYPE_USER_FILE, R.layout.item_chat_file_rv);
    }
    
    @Override
    protected RecyclerView.ViewHolder createViewHolder(View view) {
        return new ChatAdapter.ChatViewHolder(view);
    }
    
    @Override
    protected void onBindViewHolderInternal(RecyclerView.ViewHolder holder, ChatMessage message, 
                                           int position, ChatAdapterContext context) {
        ChatAdapter.ChatViewHolder fileHolder = (ChatAdapter.ChatViewHolder) holder;
        
        Timber.tag(TAG).d( "setUserFile: position=" + position + ", file count=" +
                (message.getChatFileBeanList() != null ? message.getChatFileBeanList().size() : 0));
        
        // 设置文件数据和类型
        setupFileData(fileHolder, message, context);
    }
    
    @Override
    protected Class<? extends RecyclerView.ViewHolder> getExpectedViewHolderClass() {
        return ChatAdapter.ChatViewHolder.class;
    }
    
    /**
     * 设置文件数据和适配器
     */
    private void setupFileData(ChatAdapter.ChatViewHolder holder, ChatMessage message, ChatAdapterContext context) {
        // 确保 RecyclerView 配置正确
        if (holder.rv_file != null) {
            holder.rv_file.setNestedScrollingEnabled(false);
        }
        
        // 确保缓存适配器已初始化
        ensureCachedFileAdapter(holder, context);
        
        // 使用缓存的适配器，只更新数据，保持与原有逻辑完全一致
        if (holder.cachedFileAdapter != null) {
            holder.cachedFileAdapter.setDataList(message.getChatFileBeanList());
            holder.cachedFileAdapter.setType(ChatFileAdapter.TYPE_FILE);
            holder.cachedFileAdapter.setShowClose(false);
            holder.cachedFileAdapter.notifyDataSetChanged();
            
            Timber.tag(TAG).d( "setupFileData: Updated file adapter with " + 
                    (message.getChatFileBeanList() != null ? message.getChatFileBeanList().size() : 0) + " files");
        } else {
            Timber.tag(TAG).w("setupFileData: cachedFileAdapter is null, file data not displayed");
        }
    }
    
    /**
     * 确保缓存的文件适配器已初始化
     */
    private void ensureCachedFileAdapter(ChatAdapter.ChatViewHolder holder, ChatAdapterContext context) {
        if (holder.cachedFileAdapter == null && holder.rv_file != null) {
            Timber.tag(TAG).d( "ensureCachedFileAdapter: Initializing cached file adapter");
            
            // 创建文件适配器
            holder.cachedFileAdapter = new ChatFileAdapter(
                context.getContext(), 
                new java.util.ArrayList<>(), 
                ChatFileAdapter.TYPE_FILE, 
                context.getOnFileItemClick()
            );
            holder.cachedFileAdapter.setShowClose(false);
            
            // 设置 RecyclerView 配置
            holder.rv_file.setAdapter(holder.cachedFileAdapter);
            
            // 设置 LayoutManager（文件类型使用垂直布局）
            androidx.recyclerview.widget.LinearLayoutManager layoutManager = 
                new androidx.recyclerview.widget.LinearLayoutManager(context.getContext());
            holder.rv_file.setLayoutManager(layoutManager);
            
            Timber.tag(TAG).d( "ensureCachedFileAdapter: File adapter initialized successfully");
        }
    }
}