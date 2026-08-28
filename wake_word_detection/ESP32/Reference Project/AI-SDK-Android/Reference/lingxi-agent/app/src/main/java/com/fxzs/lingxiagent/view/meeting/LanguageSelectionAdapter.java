package com.fxzs.lingxiagent.view.meeting;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.meeting.dto.LanguageDto;

import java.util.List;

/**
 * 语言选择适配器
 */
public class LanguageSelectionAdapter extends RecyclerView.Adapter<LanguageSelectionAdapter.LanguageViewHolder> {
    
    private List<LanguageDto> languages;
    private String selectedLanguageCode;
    private OnLanguageSelectedListener listener;
    
    public interface OnLanguageSelectedListener {
        void onLanguageSelected(LanguageDto language);
    }
    
    public LanguageSelectionAdapter(List<LanguageDto> languages, String selectedLanguageCode, OnLanguageSelectedListener listener) {
        this.languages = languages;
        this.selectedLanguageCode = selectedLanguageCode;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public LanguageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_language_selection, parent, false);
        return new LanguageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull LanguageViewHolder holder, int position) {
        LanguageDto language = languages.get(position);
        holder.bind(language);
    }
    
    @Override
    public int getItemCount() {
        return languages != null ? languages.size() : 0;
    }
    
    public void updateSelectedLanguage(String languageCode) {
        this.selectedLanguageCode = languageCode;
        notifyDataSetChanged();
    }
    
    class LanguageViewHolder extends RecyclerView.ViewHolder {
        private TextView tvLanguageName;
        private ImageView ivSelected;
        
        public LanguageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLanguageName = itemView.findViewById(R.id.tv_language_name);
            ivSelected = itemView.findViewById(R.id.iv_selected);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    LanguageDto language = languages.get(position);
                    listener.onLanguageSelected(language);
                }
            });
        }
        
        public void bind(LanguageDto language) {
            tvLanguageName.setText(language.getName());
            
            // 显示选中状态
            boolean isSelected = language.getName().equals(selectedLanguageCode);
            ivSelected.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            
            // 设置选中状态的文字颜色
            tvLanguageName.setTextColor(isSelected ? 
                itemView.getContext().getColor(R.color.primary_blue) : 
                itemView.getContext().getColor(R.color.text_primary));
        }
    }
}
