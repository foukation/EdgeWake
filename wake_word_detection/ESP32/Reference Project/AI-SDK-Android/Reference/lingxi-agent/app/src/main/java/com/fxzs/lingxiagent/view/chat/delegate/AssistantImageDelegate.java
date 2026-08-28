package com.fxzs.lingxiagent.view.chat.delegate;

import android.content.Context;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.util.ZUtil.DrawingActionUtils;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;
import com.fxzs.lingxiagent.view.chat.ImageGroupPreviewActivity;

import timber.log.Timber;

/**
 * 助手图片集委托
 * 负责处理助手生成的图片集显示，包括：
 * - 水平滚动的图片列表布局
 * - ImageItemHolder适配器的创建和设置
 * - 图片预览和操作功能
 * - 内嵌的ImageItemHolder适配器类
 */
public class AssistantImageDelegate extends BaseViewTypeDelegate {
    
    private static final String TAG = "AssistantImageDelegate";
    
    public AssistantImageDelegate() {
        super(ChatAdapter.TYPE_ASSISTANT_IMG, R.layout.lingxi_chat_pictures);
    }
    
    @Override
    protected RecyclerView.ViewHolder createViewHolder(View view) {
        return new ChatAdapter.ChatViewHolder(view);
    }
    
    @Override
    protected void onBindViewHolderInternal(RecyclerView.ViewHolder holder, ChatMessage message, 
                                           int position, ChatAdapterContext context) {
        ChatAdapter.ChatViewHolder imageHolder = (ChatAdapter.ChatViewHolder) holder;
        
        Timber.tag(TAG).d( "setAssistantImage: position=" + position + ", image count=" +
                (message.getImageList() != null ? message.getImageList().size() : 0));
        
        // 设置图片列表布局和适配器
        setupImageList(imageHolder, message, context);
    }
    
    @Override
    protected Class<? extends RecyclerView.ViewHolder> getExpectedViewHolderClass() {
        return ChatAdapter.ChatViewHolder.class;
    }
    
    /**
     * 设置图片列表的布局管理器和适配器
     */
    private void setupImageList(ChatAdapter.ChatViewHolder holder, ChatMessage message, ChatAdapterContext context) {
        if (holder.imageListView != null) {
            // 设置水平滚动的布局管理器，完全保持与原有逻辑一致
            LinearLayoutManager layoutManager = new LinearLayoutManager(context.getContext(), LinearLayoutManager.HORIZONTAL, false);
            holder.imageListView.setLayoutManager(layoutManager);
            
            // 创建并设置ImageItemHolder适配器
            if (message.getImageList() != null) {
                ImageItemHolder adapter = new ImageItemHolder(context.getContext(), message.getImageList());
                holder.imageListView.setAdapter(adapter);
                
                Timber.tag(TAG).d( "setupImageList: Set up image list with " + message.getImageList().size() + " images");
            } else {
                Timber.tag(TAG).w( "setupImageList: Image list is null");
            }
        } else {
            Timber.tag(TAG).w( "setupImageList: imageListView is null");
        }
    }
    
    /**
     * 内嵌的图片项适配器类，完全保持与原有ChatAdapter中ImageItemHolder的逻辑一致
     * 这个类处理图片的加载、点击预览和操作按钮功能
     */
    public static class ImageItemHolder extends RecyclerView.Adapter<ImageItemHolder.ViewHolder> {
        private final java.util.ArrayList<String> imagesPath;
        private final Context context;

        public ImageItemHolder(Context ctx, java.util.ArrayList<String> imagesPath) {
            this.context = ctx;
            this.imagesPath = imagesPath;
        }

        @androidx.annotation.NonNull
        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext()).inflate(R.layout.lingxi_chat_pictures_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            String imageUrl = imagesPath.get(position);
            
            // 使用Glide加载图片，应用圆角变换
            com.bumptech.glide.Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_image_loading)
                    .transform(new com.bumptech.glide.load.resource.bitmap.CenterCrop(), 
                              new com.bumptech.glide.load.resource.bitmap.RoundedCorners(
                                      com.fxzs.lingxiagent.lingxi.main.utils.ScreenUtils.INSTANCE.dpToPx(8, context)))
                    .into(holder.imageView);
            
            // 设置图片点击预览
            holder.imageView.setOnClickListener(v -> showImageOptions(holder.itemView.getContext(), position));
            
            // 设置继续生成按钮
            holder.continueView.setOnClickListener(v -> {
                com.bumptech.glide.Glide.with(context)
                        .asBitmap()
                        .load(imageUrl)
                        .into(new com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                            @Override
                            public void onResourceReady(@androidx.annotation.NonNull android.graphics.Bitmap resource, 
                                                       @androidx.annotation.Nullable com.bumptech.glide.request.transition.Transition<? super android.graphics.Bitmap> transition) {
                                // 调用图片预览Activity的分享方法
                                ImageGroupPreviewActivity.shareBitmap(context, resource);
                            }

                            @Override
                            public void onLoadCleared(@androidx.annotation.Nullable android.graphics.drawable.Drawable placeholder) {
                                // 资源清除时的处理
                            }
                        });
            });
            
            // 设置下载按钮
            holder.downloadView.setOnClickListener(v -> DrawingActionUtils.performDownload(context, imageUrl));
        }

        @Override
        public int getItemCount() {
            return imagesPath != null ? imagesPath.size() : 0;
        }

        /**
         * 显示图片预览选项
         */
        private void showImageOptions(Context context, int position) {
            android.content.Intent intent = new android.content.Intent(context, ImageGroupPreviewActivity.class);
            intent.putExtra("imagesPath", imagesPath);
            intent.putExtra("position", position);
            context.startActivity(intent);
        }

        /**
         * ViewHolder类，持有图片项的各个视图引用
         */
        static class ViewHolder extends RecyclerView.ViewHolder {
            private final View continueView, downloadView;
            android.widget.ImageView imageView;

            public ViewHolder(@androidx.annotation.NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.ivImage);
                downloadView = itemView.findViewById(R.id.iv_chat_draw_download);
                continueView = itemView.findViewById(R.id.iv_chat_draw_continue);
            }
        }
    }
}