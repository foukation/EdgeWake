package com.fxzs.lingxiagent.view.chat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.dto.OptionMeetingLan;
import com.fxzs.lingxiagent.util.ZUtils;

import java.util.List;

public class OptionMeetingLanAdapter extends RecyclerView.Adapter<OptionMeetingLanAdapter.OptionViewHolder> {

    private List<OptionMeetingLan> options;
    private int selectedPosition = -1; // 跟踪选中项
    private OnOptionSelectedListener listener;

    Context context;
    public interface OnOptionSelectedListener {
        void onOptionSelected(OptionMeetingLan option);
    }

    public OptionMeetingLanAdapter(Context context, List<OptionMeetingLan> options, OnOptionSelectedListener listener) {
        this.context = context;
        this.options = options;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_option_lan, parent, false);
        return new OptionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OptionViewHolder holder, @SuppressLint("RecyclerView") int position) {
        OptionMeetingLan option = options.get(position);
        holder.textView.setText(option.getTitle());
        holder.tv_sub.setText(option.getSubTitle());
        holder.iv.setVisibility(position == selectedPosition ? View.VISIBLE : View.INVISIBLE); // 选中项高亮

        ZUtils.setViewBg(context,
                holder.root_view,
                position == selectedPosition? R.drawable.bg_lan_select : R.drawable.bg_white_stroke_r16);

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

    /**
     * 更新数据并刷新列表
     */
    public void updateData(List<OptionMeetingLan> newOptions) {
        this.options = newOptions;

        // 查找默认选中项
        selectedPosition = -1;
        for (int i = 0; i < newOptions.size(); i++) {
            if (newOptions.get(i).isSelect()) {
                selectedPosition = i;
                break;
            }
        }

        notifyDataSetChanged();
    }

    static class OptionViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        TextView tv_sub;
        ImageView iv;
        RelativeLayout root_view;

        OptionViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.tv);
            tv_sub = itemView.findViewById(R.id.tv_sub);
            iv = itemView.findViewById(R.id.iv);
            root_view = itemView.findViewById(R.id.root_view);
        }
    }
}
