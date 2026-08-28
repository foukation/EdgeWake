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
import com.fxzs.lingxiagent.model.chat.dto.ModelTypeResponse;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.viewmodel.user.VMModelSelection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelSelectionActivity extends BaseActivity<VMModelSelection> {
    
    private ImageView ivBack;
    private LinearLayout containerModels;
    
    // 存储模型选项的视图引用
    private Map<String, View> modelViews = new HashMap<>();
    private Map<String, ImageView> checkViews = new HashMap<>();
    
    @Override
    protected int getLayoutResource() {
        return R.layout.activity_model_selection;
    }
    
    @Override
    protected Class<VMModelSelection> getViewModelClass() {
        return VMModelSelection.class;
    }
    
    @Override
    protected void initializeViews() {
        // 初始化控件
        ivBack = findViewById(R.id.iv_back);
        containerModels = findViewById(R.id.container_models);
        
        // 设置点击事件
        ivBack.setOnClickListener(v -> finish());
    }
    
    @Override
    protected void setupDataBinding() {
        // 动态创建模型选项，不需要数据绑定
    }
    
    @Override
    protected void setupObservers() {
        // 观察模型列表加载
        viewModel.getModelList().observe(this, modelList -> {
            if (modelList != null && !modelList.isEmpty()) {
                // 创建模型选项
                createModelOptions(modelList);
            }
        });
        
        // 观察选择成功事件
        viewModel.getSelectionSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                finish();
            }
        });
        
        // 观察加载状态
        viewModel.getLoading().observe(this, isLoading -> {
            if (isLoading != null && isLoading) {
                // 显示加载中
                containerModels.setVisibility(View.GONE);
            } else {
                // 隐藏加载中
                containerModels.setVisibility(View.VISIBLE);
            }
        });
    }
    
    private void createModelOptions(List<ModelTypeResponse.ModelItem> modelList) {
        // 清空容器
        containerModels.removeAllViews();
        modelViews.clear();
        checkViews.clear();
        
        // 创建模型选项
        for (ModelTypeResponse.ModelItem model : modelList) {
            // 创建模型选项视图
            View itemView = createModelItemView(model);
            containerModels.addView(itemView);
        }
    }
    
    private View createModelItemView(ModelTypeResponse.ModelItem model) {
        // 获取布局填充器
        LayoutInflater inflater = LayoutInflater.from(this);
        
        // 创建卡片视图
        CardView cardView = (CardView) inflater.inflate(R.layout.item_model_option, containerModels, false);
        
        // 获取卡片内的视图
        RelativeLayout layoutItem = cardView.findViewById(R.id.layout_model_item);
        TextView tvModelName = cardView.findViewById(R.id.tv_model_name);
        TextView tvModelDesc = cardView.findViewById(R.id.tv_model_desc);
        ImageView ivCheck = cardView.findViewById(R.id.iv_check);
        
        // 设置模型名称和描述
        tvModelName.setText(model.getName());
        tvModelDesc.setText(model.getModelDesc());
        
        // 设置选中状态
        if (viewModel.isModelSelected(model.getModel())) {
            ivCheck.setVisibility(View.VISIBLE);
        } else {
            ivCheck.setVisibility(View.GONE);
        }
        
        // 设置点击事件
        layoutItem.setOnClickListener(v -> {
            // 选择模型
            viewModel.selectModel(model.getModel(), model.getName());
            
            // 更新UI
            updateSelectionUI(model.getModel());
        });
        
        // 保存视图引用
        modelViews.put(model.getModel(), layoutItem);
        checkViews.put(model.getModel(), ivCheck);
        
        return cardView;
    }
    
    private void updateSelectionUI(String modelCode) {
        // 重置所有选中状态
        for (ImageView checkView : checkViews.values()) {
            checkView.setVisibility(View.GONE);
        }
        
        // 显示当前选中的模型
        ImageView selectedCheckView = checkViews.get(modelCode);
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