package com.fxzs.lingxiagent.lingxi.translate.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.translate.model.TranslateResult;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 翻译结果适配器 - 聆听模式使用
 */
public class TranslateResultAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private static final int TYPE_RESULT = 0;
    private static final int TYPE_MID_RESULT = 1;
    
    private List<TranslateResult> results;
    private String currentMidResult = "";
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public TranslateResultAdapter(List<TranslateResult> results) {
        this.results = results;
    }

    public void updateResults(List<TranslateResult> results) {
        this.results = results;
        notifyDataSetChanged();
    }
    
    public void updateMidResult(String midResult) {
        this.currentMidResult = midResult;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == results.size() && !TextUtils.isEmpty(currentMidResult)) {
            return TYPE_MID_RESULT;
        }
        return TYPE_RESULT;
    }

    @Override
    public int getItemCount() {
        int count = results.size();
        if (!TextUtils.isEmpty(currentMidResult)) {
            count += 1; // 添加正在识别的项
        }
        return count;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_MID_RESULT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_translate_mid_result, parent, false);
            return new MidResultViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_translate_result, parent, false);
            return new ResultViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof MidResultViewHolder) {
            ((MidResultViewHolder) holder).bind(currentMidResult);
        } else if (holder instanceof ResultViewHolder) {
            TranslateResult result = results.get(position);
            ((ResultViewHolder) holder).bind(result, timeFormat);
        }
    }

    /**
     * 翻译结果ViewHolder
     */
    static class ResultViewHolder extends RecyclerView.ViewHolder {
        
        private TextView tvOriginalText;
        private TextView tvTranslatedText;
        private TextView tvTime;

        public ResultViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOriginalText = itemView.findViewById(R.id.tv_original_text);
            tvTranslatedText = itemView.findViewById(R.id.tv_translated_text);
            tvTime = itemView.findViewById(R.id.tv_time);
        }

        public void bind(TranslateResult result, SimpleDateFormat timeFormat) {
            tvOriginalText.setText(result.getOriginalText());
            tvTranslatedText.setText(result.getTranslatedText());
            tvTime.setText(timeFormat.format(new Date(result.getTimestamp())));
        }
    }

    /**
     * 识别中结果ViewHolder
     */
    static class MidResultViewHolder extends RecyclerView.ViewHolder {
        
        private TextView tvMidText;

        public MidResultViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMidText = itemView.findViewById(R.id.tv_mid_text);
        }

        public void bind(String midResult) {
            tvMidText.setText(midResult);
        }
    }
}