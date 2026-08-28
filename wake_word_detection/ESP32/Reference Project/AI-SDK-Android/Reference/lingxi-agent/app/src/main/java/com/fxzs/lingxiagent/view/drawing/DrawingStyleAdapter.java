package com.fxzs.lingxiagent.view.drawing;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingStyleDto;
import com.fxzs.lingxiagent.util.ZUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 绘画风格适配器
 */
public class DrawingStyleAdapter extends RecyclerView.Adapter<DrawingStyleAdapter.StyleViewHolder> {
    
    private List<DrawingStyleDto> styles = new ArrayList<>();
    private int selectedPosition = 0;
    private OnStyleClickListener onStyleClickListener;
    
    public interface OnStyleClickListener {
        void onStyleClick(DrawingStyleDto style, int position);
    }
    
    public void setStyles(List<DrawingStyleDto> styles) {
        this.styles = styles != null ? styles : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public void setSelectedPosition(int position) {
        ZUtils.print("void setSelectedPosition: " + position);
        int oldPosition = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(oldPosition);
        notifyItemChanged(selectedPosition);
    }
    
    public void setOnStyleClickListener(OnStyleClickListener listener) {
        this.onStyleClickListener = listener;
    }
    
    @NonNull
    @Override
    public StyleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_drawing_style, parent, false);
        return new StyleViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull StyleViewHolder holder, int position) {
        DrawingStyleDto style = styles.get(position);
        holder.tvStyleName.setText(style.getName());
        
        // 加载风格图标
        if (style.getIconUrl() != null && !style.getIconUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(style.getIconUrl())
                    .transform(new RoundedCorners(8)) // 12dp圆角
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .into(holder.ivStyleImage);
        } else {
            holder.ivStyleImage.setImageResource(R.drawable.ic_image_placeholder);
        }
        
        // 设置选中状态
        boolean isSelected = position == selectedPosition;
        
        // 设置容器的选中状态（这会改变背景颜色）
        if (holder.llStyleContainer != null) {
            holder.llStyleContainer.setSelected(isSelected);
        }
        
        // 设置选中标记的可见性
        if (holder.flCheckMark != null) {
            holder.flCheckMark.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        }
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (onStyleClickListener != null && position != selectedPosition) {
                onStyleClickListener.onStyleClick(style, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return styles.size();
    }
    
    static class StyleViewHolder extends RecyclerView.ViewHolder {
        ImageView ivStyleImage;
        TextView tvStyleName;
        FrameLayout flCheckMark;
        View llStyleContainer;
        
        StyleViewHolder(View itemView) {
            super(itemView);
            ivStyleImage = itemView.findViewById(R.id.iv_style_image);
            tvStyleName = itemView.findViewById(R.id.tv_style_name);
            flCheckMark = itemView.findViewById(R.id.fl_check_mark);
            llStyleContainer = itemView.findViewById(R.id.ll_style_container);
        }
    }
}

