package com.fxzs.lingxiagent.view.user;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fxzs.lingxiagent.R;

import java.util.ArrayList;
import java.util.List;

public class FeedbackDetailImageAdapter extends RecyclerView.Adapter<FeedbackDetailImageAdapter.ImageViewHolder> {
    
    private Context context;
    private List<String> imageUrls = new ArrayList<>();
    private OnImageClickListener onImageClickListener;
    
    public interface OnImageClickListener {
        void onImageClick(String imageUrl, int position);
    }
    
    public FeedbackDetailImageAdapter(Context context) {
        this.context = context;
    }
    
    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public void setOnImageClickListener(OnImageClickListener listener) {
        this.onImageClickListener = listener;
    }
    
    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_feedback_detail_image, parent, false);
        return new ImageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUrl = imageUrls.get(position);
        
        Glide.with(context)
            .load(imageUrl)
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_image_placeholder)
            .centerCrop()
            .into(holder.ivImage);
        
        holder.itemView.setOnClickListener(v -> {
            if (onImageClickListener != null) {
                onImageClickListener.onImageClick(imageUrl, position);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return imageUrls.size();
    }
    
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        
        ImageViewHolder(View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_image);
        }
    }
}