package com.fxzs.lingxiagent.view.drawing;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.drawing.dto.AspectRatioDto;

import java.util.ArrayList;
import java.util.List;

public class AspectRatioAdapter extends RecyclerView.Adapter<AspectRatioAdapter.RatioViewHolder> {
    
    private List<AspectRatioDto> ratios = new ArrayList<>();
    private int selectedPosition = 0;
    private OnRatioClickListener onRatioClickListener;
    
    public interface OnRatioClickListener {
        void onRatioClick(AspectRatioDto ratio, int position);
    }
    
    public void setRatios(List<AspectRatioDto> ratios) {
        this.ratios = ratios != null ? ratios : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(oldPosition);
        notifyItemChanged(selectedPosition);
    }
    
    public void setOnRatioClickListener(OnRatioClickListener listener) {
        this.onRatioClickListener = listener;
    }
    
    @NonNull
    @Override
    public RatioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_aspect_ratio, parent, false);
        return new RatioViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull RatioViewHolder holder, int position) {
        AspectRatioDto ratio = ratios.get(position);
        holder.tvRatio.setText(ratio.getRatio());
        holder.tvRatioDesc.setText(ratio.getDisplayName());
        
        boolean isSelected = position == selectedPosition;
        holder.itemView.setSelected(isSelected);
        
        holder.itemView.setOnClickListener(v -> {
            if (onRatioClickListener != null && position != selectedPosition) {
                onRatioClickListener.onRatioClick(ratio, position);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return ratios.size();
    }
    
    static class RatioViewHolder extends RecyclerView.ViewHolder {
        TextView tvRatio;
        TextView tvRatioDesc;
        
        RatioViewHolder(View itemView) {
            super(itemView);
            tvRatio = itemView.findViewById(R.id.tv_ratio);
            tvRatioDesc = itemView.findViewById(R.id.tv_ratio_desc);
        }
    }
}