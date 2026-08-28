package com.fxzs.lingxiagent.view.user;

import android.graphics.Color;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.viewmodel.user.VMLanguageSettings;

import java.util.HashMap;
import java.util.Map;

public class LanguageSettingsActivity extends BaseActivity<VMLanguageSettings> {
    
    private ImageView ivBack;
    private LinearLayout containerLanguages;
    
    // 存储语言选项的视图引用
    private Map<String, View> languageViews = new HashMap<>();
    private Map<String, ImageView> checkViews = new HashMap<>();
    
    @Override
    protected int getLayoutResource() {
        return R.layout.activity_language_settings;
    }
    
    @Override
    protected Class<VMLanguageSettings> getViewModelClass() {
        return VMLanguageSettings.class;
    }
    
    @Override
    protected void initializeViews() {
        // 初始化控件
        ivBack = findViewById(R.id.iv_back);
        containerLanguages = findViewById(R.id.container_languages);
        
        // 设置点击事件
        ivBack.setOnClickListener(v -> finish());
    }
    
    @Override
    protected void setupDataBinding() {
        // 语言设置不需要数据绑定
    }
    
    @Override
    protected void setupObservers() {
        // 观察语言列表加载
        viewModel.getLanguageList().observe(this, languageMap -> {
            if (languageMap != null && !languageMap.isEmpty()) {
                // 创建语言选项
                createLanguageOptions(languageMap);
                // 更新UI显示当前语言
                updateLanguageSelection();
            }
        });
        
        // 观察语言变化
        viewModel.getLanguageChanged().observe(this, changed -> {
            if (changed != null && changed) {
                // 更新UI显示
                updateLanguageSelection();
                // 设置结果并返回
                setResult(RESULT_OK);
                finish();
            }
        });
        
        // 观察加载状态
        viewModel.getLoading().observe(this, isLoading -> {
            if (isLoading != null && isLoading) {
                // 显示加载中
                containerLanguages.setVisibility(View.GONE);
            } else {
                // 隐藏加载中
                containerLanguages.setVisibility(View.VISIBLE);
            }
        });
    }
    
    private void createLanguageOptions(Map<String, String> languageMap) {
        // 清空容器
        containerLanguages.removeAllViews();
        languageViews.clear();
        checkViews.clear();
        
        // 创建语言选项
        for (Map.Entry<String, String> entry : languageMap.entrySet()) {
            String languageCode = entry.getKey();
            String languageName = entry.getValue();
            
            // 创建语言选项视图
            View itemView = createLanguageItemView(languageCode, languageName);
            containerLanguages.addView(itemView);
        }
    }
    
    private View createLanguageItemView(String languageCode, String languageName) {
        // 获取布局填充器
        LayoutInflater inflater = LayoutInflater.from(this);
        
        // 创建卡片视图
        CardView cardView = (CardView) inflater.inflate(R.layout.item_language_option, containerLanguages, false);
        
        // 获取卡片内的视图
        RelativeLayout layoutItem = cardView.findViewById(R.id.layout_language_item);
        TextView tvLanguageName = cardView.findViewById(R.id.tv_language_name);
        TextView tvLanguageDesc = cardView.findViewById(R.id.tv_language_desc);
        ImageView ivCheck = cardView.findViewById(R.id.iv_check);
        
        // 设置语言名称和描述
        tvLanguageName.setText(languageName);
        
        // 根据语言代码设置描述
        String description = viewModel.getLanguageDescription(languageCode, languageName);
        tvLanguageDesc.setText(description);
        
        // 设置点击事件
        layoutItem.setOnClickListener(v -> selectLanguage(languageCode));
        
        // 保存视图引用
        languageViews.put(languageCode, layoutItem);
        checkViews.put(languageCode, ivCheck);
        
        return cardView;
    }
    
    private void selectLanguage(String languageCode) {
        // 先更新UI
        updateSelectionUI(languageCode);
        // 然后通知ViewModel
        viewModel.selectLanguage(languageCode);
    }
    
    private void updateLanguageSelection() {
        String currentLanguage = viewModel.getCurrentLanguage();
        updateSelectionUI(currentLanguage);
    }
    
    private void updateSelectionUI(String languageCode) {
        // 重置所有选中状态
        for (ImageView checkView : checkViews.values()) {
            checkView.setVisibility(View.GONE);
        }
        
        // 显示当前选中的语言
        ImageView selectedCheckView = checkViews.get(languageCode);
        if (selectedCheckView != null) {
            selectedCheckView.setVisibility(View.VISIBLE);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 设置状态栏颜色为白色，与背景一致，并保证内容不被遮挡
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(Color.parseColor("#FFFFFF"));
            getWindow().getDecorView().postDelayed(() -> {
                getWindow().setStatusBarColor(Color.parseColor("#FFFFFF"));
            }, 100);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }
}