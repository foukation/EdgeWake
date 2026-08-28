package com.fxzs.lingxiagent.view.chat;

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
import com.fxzs.lingxiagent.model.chat.dto.AiWritingTypeBean;

import java.util.List;

public class OptionAiMeetingAdapter extends RecyclerView.Adapter<OptionAiMeetingAdapter.OptionViewHolder> {

    private List<AiWritingTypeBean> options;
    private int selectedPosition = -1; // 跟踪选中项
    private OnOptionSelectedListener listener;

    Context context;
    public interface OnOptionSelectedListener {
        void onOptionSelected(AiWritingTypeBean option);
    }

    public OptionAiMeetingAdapter(Context context, List<AiWritingTypeBean> options, OnOptionSelectedListener listener) {
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
        AiWritingTypeBean option = options.get(position);
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
