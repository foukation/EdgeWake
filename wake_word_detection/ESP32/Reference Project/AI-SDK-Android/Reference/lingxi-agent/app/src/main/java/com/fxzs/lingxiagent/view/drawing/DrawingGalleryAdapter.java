package com.fxzs.lingxiagent.view.drawing;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingGalleryItem;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * 绘画画廊适配器
 */
public class DrawingGalleryAdapter extends RecyclerView.Adapter<DrawingGalleryAdapter.ViewHolder> {
    
    private List<DrawingGalleryItem> items = new ArrayList<>();
    private OnItemClickListener onItemClickListener;
    
    public DrawingGalleryAdapter() {
        // 设置稳定的ID，防止瀑布流重新排列
        setHasStableIds(true);
    }
    
    public interface OnItemClickListener {
        void onItemClick(DrawingGalleryItem item);
        void onActionClick(DrawingGalleryItem item);
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
    
    public void setItems(List<DrawingGalleryItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_drawing_gallery, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DrawingGalleryItem item = items.get(position);
        holder.bind(item);
    }
    
    @Override
    public int getItemCount() {
        return items.size();
    }
    
    @Override
    public long getItemId(int position) {
        // 使用position作为稳定的ID
        return position;
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivGalleryImage;
        private final TextView tvAction;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivGalleryImage = itemView.findViewById(R.id.iv_gallery_image);
            tvAction = itemView.findViewById(R.id.tv_action);
        }
        
        void bind(DrawingGalleryItem item) {
            Timber.tag("DrawingGalleryAdapter").d( "Binding item - URL: " + item.getImageUrl() + ", Prompt: " + item.getPrompt());
            
            // 加载图片
            RequestOptions options = new RequestOptions()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL) // 缓存原始图片和转换后的图片
                    .skipMemoryCache(false) // 启用内存缓存
                    .dontAnimate() // 禁用动画，防止闪烁
                    .override(400, 600); // 设置图片大小，减少内存占用
            
            // 使用实际的图片URL
            String imageUrl = item.getImageUrl();
            
            // 先清除之前的加载任务
            Glide.with(itemView.getContext()).clear(ivGalleryImage);
            
            Glide.with(itemView.getContext())
                    .load(imageUrl)
                    .apply(options)
                    .thumbnail(0.1f) // 加载缩略图
                    .into(ivGalleryImage);
            
            // 设置操作文本
            tvAction.setText(item.getActionText());
            
            // 设置点击事件
            itemView.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(item);
                }
            });
            
            tvAction.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    onItemClickListener.onActionClick(item);
                }
            });
        }
    }
}