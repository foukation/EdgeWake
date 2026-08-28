package com.fxzs.lingxiagent.lingxi.translate;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.translate.model.LanguageOption;
import com.fxzs.lingxiagent.lingxi.translate.util.LanguageUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 语言选择弹窗
 */
public class LanguageSelectionDialog extends Dialog {

    private RecyclerView rvLanguages;
    private LanguageAdapter adapter;
    private OnLanguageSelectedListener listener;
    int selectPosition = 0;
    boolean isShowZh;
    String currentCode;
    String OtherSideCode;//对方的code，决定是否显示相同的语音
    private boolean isSource;

    public interface OnLanguageSelectedListener {
        void onLanguageSelected(String language);
    }

    public LanguageSelectionDialog(@NonNull Context context,String code,String otherSideCode,Boolean isSource) {
        super(context, R.style.DialogStyle);
        this.currentCode = code;
        this.OtherSideCode = otherSideCode;
        this.isSource = isSource;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_translate_language_selection);
        
        setCancelable(true);
        setCanceledOnTouchOutside(true);
        
        initViews();
        setupData();
    }

    private void initViews() {
        rvLanguages = findViewById(R.id.rv_languages);
        rvLanguages.setLayoutManager(new LinearLayoutManager(getContext()));
        
        adapter = new LanguageAdapter();
        adapter.setOnLanguageClickListener(language -> {
            if (listener != null) {
                listener.onLanguageSelected(language);
            }
            dismiss();
        });
        rvLanguages.setAdapter(adapter);

    }

    private void setupData() {
        List<LanguageOption> languages = new ArrayList<>();

        // 使用 LanguageUtils 获取所有支持的语言
        List<String> allLanguageNames;
        List<String> allLanguageCodes;

        if(isSource){
            allLanguageNames = LanguageUtils.getInstance().getSourceLanguagesNames();
            allLanguageCodes = LanguageUtils.getInstance().getSourceLanguageCodes();
        }else{
            allLanguageNames = LanguageUtils.getInstance().getTargetLanguagesNames();
            allLanguageCodes = LanguageUtils.getInstance().getTargetLanguageCodes();
        }
        for (int i = 0; i < allLanguageCodes.size(); i++) {
            String code = allLanguageCodes.get(i);
            String name = allLanguageNames.get(i);
            
            // 过滤掉对方已选择的语言（避免源语言和目标语言相同）
            if (!code.equals(OtherSideCode)) {
                languages.add(new LanguageOption(name, code, code.equals(currentCode)));
            }
        }
        
        adapter.setLanguages(languages);
    }

    public void setOnLanguageSelectedListener(OnLanguageSelectedListener listener) {
        this.listener = listener;
    }

    /**
     * 语言列表适配器
     */
    private static class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.ViewHolder> {
        
        private List<LanguageOption> languages = new ArrayList<>();
        private OnLanguageClickListener listener;
        
        public interface OnLanguageClickListener {
            void onLanguageClick(String language);
        }
        
        public void setLanguages(List<LanguageOption> languages) {
            this.languages = languages;
            notifyDataSetChanged();
        }
        
        public void setOnLanguageClickListener(OnLanguageClickListener listener) {
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_translate_dialog_lan, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LanguageOption language = languages.get(position);
            holder.bind(language, listener);
        }

        @Override
        public int getItemCount() {
            return languages.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            
            private TextView tvLanguageName;
//            private TextView tvLanguageDesc;
            private View iv_select;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvLanguageName = itemView.findViewById(R.id.tv_language_name);
//                tvLanguageDesc = itemView.findViewById(R.id.tv_language_desc);
                iv_select = itemView.findViewById(R.id.iv_select);
            }

            public void bind(LanguageOption language, OnLanguageClickListener listener) {
                tvLanguageName.setText(language.getName());
//                tvLanguageDesc.setText(language.getDescription());
                iv_select.setVisibility(language.isSelected()?View.VISIBLE:View.GONE);
                
                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onLanguageClick(language.getName());
                    }
                });
            }
        }
    }
}