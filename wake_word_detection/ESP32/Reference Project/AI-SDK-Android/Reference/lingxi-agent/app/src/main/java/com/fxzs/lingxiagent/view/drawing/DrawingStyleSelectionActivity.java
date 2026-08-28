package com.fxzs.lingxiagent.view.drawing;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.viewmodel.drawing.VMDrawingTransform;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 选择风格页面 - 网格布局展示所有风格
 */
public class DrawingStyleSelectionActivity extends BaseActivity<VMDrawingTransform> {
    
    public static final String EXTRA_SELECTED_STYLE = "selected_style";
    public static final String EXTRA_ALL_STYLES = "all_styles";
    
    private RecyclerView rvStyleGrid;
    private DrawingStyleGridAdapter gridAdapter;
    private List<DrawingTransformStyleItem> allStyles;
    private DrawingTransformStyleItem selectedStyle;
    private TextView tvConfirm;
    
    @Override
    protected int getLayoutResource() {
        return R.layout.act_drawing_style_selection;
    }
    
    @Override
    protected Class<VMDrawingTransform> getViewModelClass() {
        return VMDrawingTransform.class;
    }
    
    @Override
    protected void setupDataBinding() {
        // no-op
    }
    
    @Override
    protected void initializeViews() {
        // 获取传递过来的数据
        Intent intent = getIntent();
        if (intent != null) {
            Serializable allStylesSerializable = intent.getSerializableExtra(EXTRA_ALL_STYLES);
            if (allStylesSerializable instanceof List) {
                allStyles = (List<DrawingTransformStyleItem>) allStylesSerializable;
            }
            Serializable preSelectedSerializable = intent.getSerializableExtra(EXTRA_SELECTED_STYLE);
            if (preSelectedSerializable instanceof DrawingTransformStyleItem) {
                selectedStyle = (DrawingTransformStyleItem) preSelectedSerializable;
            }
        }
        
        // 如果没有传递数据，从 ViewModel 获取
        if (allStyles == null || allStyles.isEmpty()) {
            List<DrawingTransformStyleItem> styles = viewModel.getStyleItems().getValue();
            if (styles != null) {
                allStyles = new ArrayList<>(styles);
            } else {
                allStyles = new ArrayList<>();
            }
        }
        
        setupRecyclerView();
        setupConfirmButton();
        setupBackButton();
    }
    
    private void setupRecyclerView() {
        rvStyleGrid = findViewById(R.id.rv_style_grid);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 4);
        rvStyleGrid.setLayoutManager(layoutManager);
        
        gridAdapter = new DrawingStyleGridAdapter(this, allStyles, selectedStyle);
        gridAdapter.setOnStyleClickListener((position, item) -> {
            // 单选模式：直接更新选中的风格
            selectedStyle = item;
        });
        rvStyleGrid.setAdapter(gridAdapter);
    }
    
    private void setupConfirmButton() {
        tvConfirm = findViewById(R.id.tv_confirm);
        tvConfirm.setOnClickListener(v -> {
            // 返回选中的风格（单选）
            Intent resultIntent = new Intent();
            DrawingTransformStyleItem selected = gridAdapter.getSelectedItem();
            if (selected != null) {
                resultIntent.putExtra(EXTRA_SELECTED_STYLE, selected);
            }
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }
    
    private void setupBackButton() {
        findViewById(R.id.back).setOnClickListener(v -> finish());
    }
    
    @Override
    protected void setupObservers() {
        // 监听风格列表数据（如果从 ViewModel 加载）
        viewModel.getStyleItems().observe(this, styleItems -> {
            if (styleItems != null && !styleItems.isEmpty() && (allStyles == null || allStyles.isEmpty())) {
                allStyles = new ArrayList<>(styleItems);
                if (gridAdapter != null) {
                    gridAdapter.updateData(allStyles);
                }
            }
        });
    }
}

