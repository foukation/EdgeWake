package com.fxzs.lingxiagent.view.user;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.fxzs.lingxiagent.R;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class FeedbackImageAdapter extends RecyclerView.Adapter<FeedbackImageAdapter.ImageViewHolder> {
    
    private final Context context;
    private final List<String> imageUrls;
    private OnImageDeleteListener deleteListener;
    
    public interface OnImageDeleteListener {
        void onImageDelete(int position);
    }
    
    public FeedbackImageAdapter(Context context) {
        this.context = context;
        this.imageUrls = new ArrayList<>();
    }
    
    public void setOnImageDeleteListener(OnImageDeleteListener listener) {
        this.deleteListener = listener;
    }
    
    public void setImages(List<String> urls) {
        imageUrls.clear();
        if (urls != null) {
            imageUrls.addAll(urls);
        }
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_feedback_image, parent, false);
        return new ImageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUrl = imageUrls.get(position);
        
        Timber.tag("FeedbackImageAdapter").d( "Loading image at position " + position + ": " + imageUrl);
        
        // 使用Glide加载图片，改进URI处理
        Object imageSource;
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            // 网络图片
            imageSource = imageUrl;
            Timber.tag("FeedbackImageAdapter").d( "Loading as network image");
        } else if (imageUrl.startsWith("file://")) {
            // 文件URI
            imageSource = Uri.parse(imageUrl);
            Timber.tag("FeedbackImageAdapter").d( "Loading as file URI");
        } else {
            // 本地文件路径
            java.io.File file = new java.io.File(imageUrl);
            imageSource = file;
            Timber.tag("FeedbackImageAdapter").d( "Loading as local file, exists: " + file.exists() + ", path: " + file.getAbsolutePath());
        }
        
        Glide.with(context)
            .load(imageSource)
            .transform(new CenterCrop(), new RoundedCorners(16))
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_image_error)
            .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                @Override
                public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                    Timber.tag("FeedbackImageAdaptericInput").e( "Failed to load image: " + imageUrl +  e);
                    return false;
                }
                
                @Override
                public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                    Timber.tag("FeedbackImageAdapter").d( "Successfully loaded image: " + imageUrl);
                    return false;
                }
            })
            .into(holder.ivImage);
        
        // 点击图片可以预览
        holder.ivImage.setOnClickListener(v -> {
            if (onImageClickListener != null) {
                onImageClickListener.onImageClick(position, imageUrl);
            }
        });
        
        holder.ivDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onImageDelete(position);
            }
        });
    }
    
    public interface OnImageClickListener {
        void onImageClick(int position, String imageUrl);
    }
    
    private OnImageClickListener onImageClickListener;
    
    public void setOnImageClickListener(OnImageClickListener listener) {
        this.onImageClickListener = listener;
    }
    
    @Override
    public int getItemCount() {
        return imageUrls.size();
    }
    
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        ImageView ivDelete;
        
        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_image);
            ivDelete = itemView.findViewById(R.id.iv_delete);
        }
    }
}