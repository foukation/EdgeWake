package com.fxzs.lingxiagent.view.user;

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
import com.fxzs.lingxiagent.model.chat.dto.VoiceSettingBean;
import com.fxzs.lingxiagent.util.ZUtils;

import java.util.List;

public class VoiceSettingAdapter extends RecyclerView.Adapter<VoiceSettingAdapter.OptionViewHolder> {

    private List<VoiceSettingBean> options;
    private int selectedPosition = 0; // 跟踪选中项
    private OnOptionSelectedListener listener;

    Context context;
    public interface OnOptionSelectedListener {
        void onOptionSelected(VoiceSettingBean option);
    }

    public VoiceSettingAdapter(Context context, List<VoiceSettingBean> options, OnOptionSelectedListener listener) {
        this.context = context;
        this.options = options;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_setting_voice, parent, false);
        return new OptionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OptionViewHolder holder, @SuppressLint("RecyclerView") int position) {
        VoiceSettingBean option = options.get(position);
        holder.tv_name.setText(option.getName());
        holder.tv_des.setText(option.getDes());

        ZUtils.setIvBg(context,holder.iv_select,position == selectedPosition ? R.mipmap.ic_choose:R.drawable.setting_voice_add);
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
        TextView tv_name;
        TextView tv_des;
        ImageView iv_select;

        OptionViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_name = itemView.findViewById(R.id.tv_name);
            tv_des = itemView.findViewById(R.id.tv_des);
            iv_select = itemView.findViewById(R.id.iv_select);
        }
    }

    public void setSelectedPosition(int selectedPosition) {
        this.selectedPosition = selectedPosition;
        notifyDataSetChanged();
    }
}
