package com.fxzs.lingxiagent.lingxi.translate.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.translate.model.DialogResult;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 对话翻译结果适配器 - 对话模式使用
 */
public class DialogTranslateAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private static final int TYPE_RESULT_A_TO_B = 0; // A语言到B语言
    private static final int TYPE_RESULT_B_TO_A = 1; // B语言到A语言
    
    private List<DialogResult> results;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public DialogTranslateAdapter(List<DialogResult> results) {
        this.results = results;
    }

    public void updateResults(List<DialogResult> results) {
        this.results = results;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        DialogResult result = results.get(position);
        return result.isFromLanguageA() ? TYPE_RESULT_A_TO_B : TYPE_RESULT_B_TO_A;
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_RESULT_A_TO_B) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_dialog_result_a_to_b, parent, false);
            return new DialogResultViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_dialog_result_b_to_a, parent, false);
            return new DialogResultViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DialogResult result = results.get(position);
        ((DialogResultViewHolder) holder).bind(result, timeFormat);
    }

    /**
     * 对话结果ViewHolder
     */
    static class DialogResultViewHolder extends RecyclerView.ViewHolder {
        
        private TextView tvOriginalText;
        private TextView tvTranslatedText;
        private TextView tvTime;

        public DialogResultViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOriginalText = itemView.findViewById(R.id.tv_original_text);
            tvTranslatedText = itemView.findViewById(R.id.tv_translated_text);
            tvTime = itemView.findViewById(R.id.tv_time);
        }

        public void bind(DialogResult result, SimpleDateFormat timeFormat) {
            tvOriginalText.setText(result.getOriginalText());
            tvTranslatedText.setText(result.getTranslatedText());
            tvTime.setText(timeFormat.format(new Date(result.getTimestamp())));
        }
    }
}