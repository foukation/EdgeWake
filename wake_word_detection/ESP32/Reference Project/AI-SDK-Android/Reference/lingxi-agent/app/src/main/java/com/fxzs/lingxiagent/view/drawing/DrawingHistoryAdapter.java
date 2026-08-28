package com.fxzs.lingxiagent.view.drawing;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingImageDto;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 绘画历史记录适配器
 */
public class DrawingHistoryAdapter extends RecyclerView.Adapter<DrawingHistoryAdapter.HistoryViewHolder> {
    
    private List<DrawingImageDto> images = new ArrayList<>();
    private OnItemClickListener onItemClickListener;
    private OnDeleteClickListener onDeleteClickListener;
    
    public interface OnItemClickListener {
        void onItemClick(DrawingImageDto image);
    }
    
    public interface OnDeleteClickListener {
        void onDeleteClick(DrawingImageDto image);
    }
    
    public void setImages(List<DrawingImageDto> images) {
        this.images = images != null ? images : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
    
    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }
    
    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_drawing_history, parent, false);
        return new HistoryViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        DrawingImageDto image = images.get(position);
        
        // 加载图片
        Glide.with(holder.itemView.getContext())
                .load(image.getThumbnailUrl() != null ? image.getThumbnailUrl() : image.getImageUrl())
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .centerCrop()
                .into(holder.ivImage);
        
        // 显示提示词（截取前30个字符）
        String prompt = image.getPrompt();
        if (prompt != null && prompt.length() > 30) {
            prompt = prompt.substring(0, 30) + "...";
        }
        holder.tvPrompt.setText(prompt);
        
        // 显示风格
        holder.tvStyle.setText(image.getStyle());
        
        // 显示时间（格式化）
        String createTime = formatTime(image.getCreateTime());
        holder.tvTime.setText(createTime);
        
        // 点击事件
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(image);
            }
        });
        
        // 删除按钮
        holder.ivDelete.setOnClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(image);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return images.size();
    }
    
    /**
     * 格式化时间显示
     */
    private String formatTime(String timeStr) {
        if (timeStr == null) return "";
        
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
            Date date = inputFormat.parse(timeStr);
            
            // 判断是否是今天
            SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String today = dayFormat.format(new Date());
            String targetDay = dayFormat.format(date);
            
            if (today.equals(targetDay)) {
                return "今天 " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
            } else {
                return outputFormat.format(date);
            }
        } catch (ParseException e) {
            return timeStr;
        }
    }
    
    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView ivImage;
        TextView tvPrompt;
        TextView tvStyle;
        TextView tvTime;
        ImageView ivDelete;
        
        HistoryViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_view);
            ivImage = itemView.findViewById(R.id.iv_image);
            tvPrompt = itemView.findViewById(R.id.tv_prompt);
            tvStyle = itemView.findViewById(R.id.tv_style);
            tvTime = itemView.findViewById(R.id.tv_time);
            ivDelete = itemView.findViewById(R.id.iv_delete);
        }
    }
}