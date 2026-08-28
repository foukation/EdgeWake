package com.fxzs.lingxiagent.view.aiwork;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.aiwork.AiWorkFilterBean;

import java.util.List;

public class AiWorkFilterAdapter extends RecyclerView.Adapter<AiWorkFilterAdapter.OptionViewHolder> {

    private List<AiWorkFilterBean> options;
    private int selectedPosition = 0; // 跟踪选中项
    private OnOptionSelectedListener listener;

    Context context;
    public interface OnOptionSelectedListener {
        void onOptionSelected(AiWorkFilterBean option);
    }

    public AiWorkFilterAdapter(Context context, List<AiWorkFilterBean> options, OnOptionSelectedListener listener) {
        this.context = context;
        this.options = options;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_option_ai_meeting, parent, false);
        return new OptionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OptionViewHolder holder, @SuppressLint("RecyclerView") int position) {
        AiWorkFilterBean option = options.get(position);
        holder.textView.setText(option.getName());

        holder.iv_select.setVisibility(position == selectedPosition ? View.VISIBLE : View.INVISIBLE); // 选中项高亮

        holder.itemView.setOnClickListener(v -> {
            selectedPosition = position;
            notifyDataSetChanged(); // 刷新列表以更新高亮
            listener.onOptionSelected(option);

        });
    }

    @Override
    public int getItemCount() {
        return options.size();
    }

    static class OptionViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ImageView iv_select;

        OptionViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.tv);
            iv_select = itemView.findViewById(R.id.iv_select);
        }
    }

    public void setSelectedPosition(int selectedPosition) {
        this.selectedPosition = selectedPosition;
        notifyDataSetChanged();
    }
}
