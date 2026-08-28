package com.fxzs.lingxiagent.lingxi.translate;

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
import com.fxzs.lingxiagent.lingxi.translate.model.TranslateResult;
import com.fxzs.lingxiagent.lingxi.translate.util.LanguageUtils;
import com.fxzs.lingxiagent.util.ZUtils;

import java.util.List;

public class TranslateDetailAdapter extends RecyclerView.Adapter<TranslateDetailAdapter.OptionViewHolder> {

    private List<TranslateResult> options;
    private int selectedPosition = -1; // 跟踪选中项
    private OnOptionSelectedListener listener;

    Context context;
    public interface OnOptionSelectedListener {
        void onOptionSelected(TranslateResult option);
    }

    public TranslateDetailAdapter(Context context, List<TranslateResult> options, OnOptionSelectedListener listener) {
        this.context = context;
        this.options = options;
        this.listener = listener;
    }

   public void setData( List<TranslateResult> options){
        this.options = options;
        notifyDataSetChanged();
    }
    @NonNull
    @Override
    public OptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_translate_detail_listen, parent, false);
        return new OptionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OptionViewHolder holder, @SuppressLint("RecyclerView") int position) {
        TranslateResult bean = options.get(position);
        holder.tv_source.setText(bean.getSourceText());
        holder.tv_source_language.setText(LanguageUtils.getInstance().getSourceLanguageName(bean.getSource()));
        holder.tv_target.setText(bean.getTargetText());
        holder.tv_target_language.setText(LanguageUtils.getInstance().getTargetLanguageName(bean.getTarget()));

        holder.itemView.setOnClickListener(v -> {
            selectedPosition = position;
            notifyDataSetChanged(); // 刷新列表以更新高亮
            listener.onOptionSelected(bean);
        });
        holder.tv_source_copy.setOnClickListener(v -> {
            ZUtils.copy(context,bean.getSourceText());

        });
        holder.tv_target_copy.setOnClickListener(v -> {
            ZUtils.copy(context,bean.getTargetText());

        });
    }

    @Override
    public int getItemCount() {
        return options.size();
    }

    static class OptionViewHolder extends RecyclerView.ViewHolder {
        TextView tv_source_language;
        TextView tv_source;
        ImageView tv_source_copy;
        TextView tv_target_language;
        TextView tv_target;
        ImageView tv_target_copy;

        OptionViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_source_language = itemView.findViewById(R.id.tv_source_language);
            tv_source = itemView.findViewById(R.id.tv_source);
            tv_source_copy = itemView.findViewById(R.id.tv_source_copy);
            tv_target_language = itemView.findViewById(R.id.tv_target_language);
            tv_target = itemView.findViewById(R.id.tv_target);
            tv_target_copy = itemView.findViewById(R.id.tv_target_copy);
        }
    }
}
