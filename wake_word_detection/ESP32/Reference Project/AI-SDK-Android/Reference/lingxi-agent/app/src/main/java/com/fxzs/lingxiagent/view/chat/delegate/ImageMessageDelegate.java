package com.fxzs.lingxiagent.view.chat.delegate;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;
import com.fxzs.lingxiagent.view.chat.ChatFileAdapter;

import timber.log.Timber;

/**
 * 图片消息委托
 * 负责处理用户发送的图片消息显示，包括：
 * - 图片适配器缓存管理
 * - 图片加载（URI和网络路径）
 * - 上传进度显示
 * - 图片预览和点击处理
 * - 滚动优化设置
 */
public class ImageMessageDelegate extends BaseViewTypeDelegate {
    
    private static final String TAG = "ImageMessageDelegate";
    
    public ImageMessageDelegate() {
        super(ChatAdapter.TYPE_USER_FILE_IMAGE, R.layout.item_chat_file_rv);
    }
    
    @Override
    protected RecyclerView.ViewHolder createViewHolder(View view) {
        return new ChatAdapter.ChatViewHolder(view);
    }
    
    @Override
    protected void onBindViewHolderInternal(RecyclerView.ViewHolder holder, ChatMessage message, 
                                           int position, ChatAdapterContext context) {
        ChatAdapter.ChatViewHolder imageHolder = (ChatAdapter.ChatViewHolder) holder;
        
        Timber.tag(TAG).d( "onBindViewHolderInternal: position=" + position + ", image count=" +
                (message.getChatFileBeanList() != null ? message.getChatFileBeanList().size() : 0));
        Timber.tag(TAG).d( "onBindViewHolderInternal: cachedFileAdapter is " + 
                (imageHolder.cachedFileAdapter != null ? "not null" : "null"));
        
        // 设置滚动优化
        setupScrollOptimization(imageHolder);
        
        // 设置图片数据和类型
        setupImageData(imageHolder, message, context);
        
        // 输出调试信息
        ZUtils.print("item.getChatFileBeanList() == " + 
                (message.getChatFileBeanList() != null ? message.getChatFileBeanList().size() : 0));
    }
    
    @Override
    protected Class<? extends RecyclerView.ViewHolder> getExpectedViewHolderClass() {
        return ChatAdapter.ChatViewHolder.class;
    }
    
    /**
     * 设置滚动优化
     */
    private void setupScrollOptimization(ChatAdapter.ChatViewHolder holder) {
        // 禁用嵌套滚动以优化性能，保持与原有逻辑完全一致
        if (holder.rv_file != null) {
            holder.rv_file.setNestedScrollingEnabled(false);
        }
    }
    
    /**
     * 设置图片数据和适配器
     */
    private void setupImageData(ChatAdapter.ChatViewHolder holder, ChatMessage message, ChatAdapterContext context) {
        // 使用缓存的适配器，只更新数据，保持与原有逻辑完全一致
        if (holder.cachedFileAdapter != null) {
            holder.cachedFileAdapter.setDataList(message.getChatFileBeanList());
            holder.cachedFileAdapter.setType(ChatFileAdapter.TYPE_IMAGE);
            // ChatFileAdapter的点击监听器在构造函数中设置，这里不需要再设置（保持原有注释和逻辑）
            holder.cachedFileAdapter.notifyDataSetChanged();
            
            Timber.tag(TAG).d( "setupImageData: Updated image adapter with " + 
                    (message.getChatFileBeanList() != null ? message.getChatFileBeanList().size() : 0) + " images");
        } else {
            Timber.tag(TAG).w( "setupImageData: cachedFileAdapter is null, image data not displayed");
        }
    }
}