package com.fxzs.lingxiagent.view.user;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.user.dto.FeedbackDto;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FeedbackHistoryAdapter extends RecyclerView.Adapter<FeedbackHistoryAdapter.ViewHolder> {
    
    private Context context;
    private List<FeedbackDto> feedbackList = new ArrayList<>();
    private OnItemClickListener onItemClickListener;
    
    public interface OnItemClickListener {
        void onItemClick(FeedbackDto feedback);
    }
    
    public FeedbackHistoryAdapter(Context context) {
        this.context = context;
    }
    
    public void setFeedbackList(List<FeedbackDto> feedbackList) {
        this.feedbackList = feedbackList != null ? feedbackList : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_feedback_history, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FeedbackDto feedback = feedbackList.get(position);
        
        // 设置反馈内容
        holder.tvFeedbackContent.setText(feedback.getContent());
        
        // 设置时间 - 格式化为 yyyy-M-d
        Long createTime = feedback.getCreateTime();
        if (createTime != null) {
            try {
                Date date = new Date(createTime);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-M-d", Locale.getDefault());
                holder.tvTime.setText(sdf.format(date));
            } catch (Exception e) {
                holder.tvTime.setText("");
            }
        } else {
            holder.tvTime.setText("");
        }
        
        // 设置图片（如果有）
        if (feedback.getImages() != null && !feedback.getImages().isEmpty()) {
            holder.layoutImages.setVisibility(View.VISIBLE);
            
            // 显示所有图片（API可能会返回images字段而不是imageUrls）
            List<String> imageUrls = feedback.getImages() != null ? feedback.getImages() : feedback.getImageUrls();
            if (imageUrls == null) {
                imageUrls = new ArrayList<>();
            }
            
            ImageView[] imageViews = {holder.ivImage1, holder.ivImage2, holder.ivImage3, holder.ivImage4};
            
            for (int i = 0; i < imageViews.length; i++) {
                if (i < imageUrls.size()) {
                    imageViews[i].setVisibility(View.VISIBLE);
                    // 使用Glide加载图片
                    Glide.with(context)
                        .load(imageUrls.get(i))
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .centerCrop()
                        .into(imageViews[i]);
                } else {
                    imageViews[i].setVisibility(View.GONE);
                }
            }
        } else if (feedback.getImageUrls() != null && !feedback.getImageUrls().isEmpty()) {
            // 兼容旧的imageUrls字段
            holder.layoutImages.setVisibility(View.VISIBLE);
            
            List<String> imageUrls = feedback.getImageUrls();
            ImageView[] imageViews = {holder.ivImage1, holder.ivImage2, holder.ivImage3, holder.ivImage4};
            
            for (int i = 0; i < imageViews.length; i++) {
                if (i < imageUrls.size()) {
                    imageViews[i].setVisibility(View.VISIBLE);
                    // 使用Glide加载图片
                    Glide.with(context)
                        .load(imageUrls.get(i))
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .centerCrop()
                        .into(imageViews[i]);
                } else {
                    imageViews[i].setVisibility(View.GONE);
                }
            }
        } else {
            holder.layoutImages.setVisibility(View.GONE);
        }
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(feedback);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return feedbackList.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFeedbackContent;
        HorizontalScrollView layoutImages;
        ImageView ivImage1, ivImage2, ivImage3, ivImage4;
        TextView tvTime;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvFeedbackContent = itemView.findViewById(R.id.tv_feedback_content);
            layoutImages = itemView.findViewById(R.id.layout_images);
            ivImage1 = itemView.findViewById(R.id.iv_image1);
            ivImage2 = itemView.findViewById(R.id.iv_image2);
            ivImage3 = itemView.findViewById(R.id.iv_image3);
            ivImage4 = itemView.findViewById(R.id.iv_image4);
            tvTime = itemView.findViewById(R.id.tv_time);
        }
    }
}