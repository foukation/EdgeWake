package com.fxzs.lingxiagent.view.meeting;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.meeting.dto.LanguageDto;

import java.util.List;

/**
 * 语言选择弹窗
 */
public class LanguageSelectionDialog {
    
    private Dialog dialog;
    private LanguageSelectionAdapter adapter;
    private OnLanguageSelectedListener listener;
    LanguageDto currenLanguageDto;

    public interface OnLanguageSelectedListener {
        void onLanguageSelected(LanguageDto language);
    }
    
//    public LanguageSelectionDialog(Context context, List<LanguageDto> languages, String selectedLanguageCode, OnLanguageSelectedListener listener) {
//        this.listener = listener;
//        createDialog(context, languages, selectedLanguageCode);
//    }
    public LanguageSelectionDialog(Context context, List<LanguageDto> languages, LanguageDto currenLanguageDto , OnLanguageSelectedListener listener) {
        this.listener = listener;
        createDialog(context, languages, currenLanguageDto);
    }
    
    private void createDialog(Context context, List<LanguageDto> languages, LanguageDto currenLanguageDto) {
        dialog = new Dialog(context, R.style.CustomDialog);
        
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_language_selection, null);
        dialog.setContentView(dialogView);
        
        // 设置弹窗属性
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
            
            // 设置弹窗动画
            window.setWindowAnimations(R.style.PopupMenuAnimation);
            
            // 设置背景透明
            window.setDimAmount(0.5f);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        
        // 初始化视图
        initViews(dialogView, languages, currenLanguageDto);
    }
    
    private void initViews(View dialogView, List<LanguageDto> languages, LanguageDto currenLanguageDto) {
        // 关闭按钮
        ImageView ivClose = dialogView.findViewById(R.id.iv_close);
        ivClose.setOnClickListener(v -> dismiss());
        
        // 语言列表
        RecyclerView rvLanguages = dialogView.findViewById(R.id.rv_languages);
        rvLanguages.setLayoutManager(new LinearLayoutManager(dialogView.getContext()));
        
        adapter = new LanguageSelectionAdapter(languages, currenLanguageDto.getName(), language -> {
            if (listener != null) {
                listener.onLanguageSelected(language);
            }
            dismiss();
        });
        
        rvLanguages.setAdapter(adapter);
        
        // 点击外部区域关闭弹窗
        dialog.setCanceledOnTouchOutside(true);
    }
    
    public void show() {
        if (dialog != null && !dialog.isShowing()) {
            dialog.show();
        }
    }
    
    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
    
    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }
}
