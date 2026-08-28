package com.fxzs.lingxiagent.view.drawing;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fxzs.lingxiagent.R;

import java.util.List;

/**
 * 风格转绘专用 Adapter（不要复用/修改文生图链路的 DrawingStyleAdapter）
 */
public class DrawingTransformStyleAdapter extends RecyclerView.Adapter<DrawingTransformStyleAdapter.VH> {

    public interface OnStyleClickListener {
        void onStyleClick(int position, DrawingTransformStyleItem item);
    }

    private final Context context;
    private List<DrawingTransformStyleItem> data;
    private int selectedPosition = -1;
    private OnStyleClickListener onStyleClickListener;

    public DrawingTransformStyleAdapter(Context context, List<DrawingTransformStyleItem> data) {
        this.context = context;
        this.data = data;
    }
    
    /**
     * 更新数据列表
     */
    public void updateData(List<DrawingTransformStyleItem> newData) {
        this.data = newData;
        notifyDataSetChanged();
    }

    public void setOnStyleClickListener(OnStyleClickListener listener) {
        this.onStyleClickListener = listener;
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }
    
    public void setSelectedPosition(int position) {
        if (/*position >= 0 &&*/ position < getItemCount()) {
            int old = selectedPosition;
            selectedPosition = position;
            if (old >= 0 && old < getItemCount()) {
                notifyItemChanged(old);
            }
            notifyItemChanged(selectedPosition);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_drawing_transform_style, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        DrawingTransformStyleItem item = data.get(position);
//        holder.tvName.setText(item.getName());
        Glide.with(context).load(item.getIconUrl()).into(holder.ivCover);

        holder.ivCheck.setVisibility(position == selectedPosition ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (selectedPosition != position) {
                int old = selectedPosition;
                selectedPosition = position;
                notifyItemChanged(old);
                notifyItemChanged(selectedPosition);
            }
            if (onStyleClickListener != null) {
                onStyleClickListener.onStyleClick(position, item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivCover;
        View ivCheck;
//        TextView tvName;

        VH(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.iv_style_image);
            ivCheck = itemView.findViewById(R.id.iv_style_selected_check);
//            tvName = itemView.findViewById(R.id.tv_style_name);
        }
    }
}

