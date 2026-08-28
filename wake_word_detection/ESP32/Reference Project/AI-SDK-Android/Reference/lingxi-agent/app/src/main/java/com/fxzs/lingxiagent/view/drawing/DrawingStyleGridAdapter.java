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

import java.util.ArrayList;
import java.util.List;

/**
 * 风格网格布局适配器（用于选择风格页面）- 单选模式
 */
public class DrawingStyleGridAdapter extends RecyclerView.Adapter<DrawingStyleGridAdapter.VH> {

    public interface OnStyleClickListener {
        void onStyleClick(int position, DrawingTransformStyleItem item);
    }

    private final Context context;
    private List<DrawingTransformStyleItem> data;
    private int selectedPosition = -1;
    private OnStyleClickListener onStyleClickListener;

    public DrawingStyleGridAdapter(Context context, List<DrawingTransformStyleItem> data, DrawingTransformStyleItem selectedItem) {
        this.context = context;
        this.data = data != null ? data : new ArrayList<>();
        // 找到选中项的位置
        if (selectedItem != null && data != null) {
            for (int i = 0; i < data.size(); i++) {
                if (data.get(i).getId() == selectedItem.getId()) {
                    this.selectedPosition = i;
                    break;
                }
            }
        }
    }

    public void setOnStyleClickListener(OnStyleClickListener listener) {
        this.onStyleClickListener = listener;
    }

    public void updateData(List<DrawingTransformStyleItem> newData) {
        this.data = newData != null ? newData : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        if (position >= 0 && position < getItemCount()) {
            int oldPosition = selectedPosition;
            selectedPosition = position;
            if (oldPosition >= 0 && oldPosition < getItemCount()) {
                notifyItemChanged(oldPosition);
            }
            notifyItemChanged(selectedPosition);
        }
    }

    public DrawingTransformStyleItem getSelectedItem() {
        if (selectedPosition >= 0 && selectedPosition < getItemCount()) {
            return data.get(selectedPosition);
        }
        return null;
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    private boolean isSelected(int position) {
        return position == selectedPosition;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_drawing_style_grid, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        DrawingTransformStyleItem item = data.get(position);
        holder.tvName.setText(item.getName());
        Glide.with(context).load(item.getIconUrl()).into(holder.ivCover);

        boolean selected = isSelected(position);
        holder.ivCheck.setVisibility(selected ? View.VISIBLE : View.GONE);
        holder.itemView.setSelected(selected);

        holder.itemView.setOnClickListener(v -> {
            if (selectedPosition != position) {
                setSelectedPosition(position);
                if (onStyleClickListener != null) {
                    onStyleClickListener.onStyleClick(position, item);
                }
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
        TextView tvName;

        VH(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.iv_style_image);
            ivCheck = itemView.findViewById(R.id.iv_style_selected_check);
            tvName = itemView.findViewById(R.id.tv_style_name);
        }
    }
}

