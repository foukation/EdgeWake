package com.fxzs.lingxiagent.view.drawing;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingGalleryItem;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingSampleDto;
import com.fxzs.lingxiagent.view.common.BaseFragment;
import com.fxzs.lingxiagent.viewmodel.drawing.VMDrawingGallerySub;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class DrawingSubFragment extends BaseFragment<VMDrawingGallerySub> {
    
    private List<DrawingSampleDto> currentSamples = new ArrayList<>();
    private int type;
    private SwipeRefreshLayout swipeRefresh;

    public DrawingSubFragment() {
        // Required empty public constructor
    }

    public DrawingSubFragment(int type) {

        Bundle args = new Bundle();
        args.putInt("type", type);
        setArguments(args);
    }

    // 画廊
    private RecyclerView rvGallery;
    private DrawingGalleryAdapter galleryAdapter;
    
    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_drawing_sub;
    }
    
    @Override
    protected Class<VMDrawingGallerySub> getViewModelClass() {
        return VMDrawingGallerySub.class;
    }
    
    @Override
    protected void initializeViews(View view) {

        // 从 arguments 获取 type 参数
        if (getArguments() != null) {
            type = getArguments().getInt("type", 0);
        }
        // 初始化RecyclerView
        rvGallery = findViewById(R.id.rv_gallery);
        swipeRefresh = findViewById(R.id.swipe_refresh);

        
        // 设置RecyclerView
        setupRecyclerView();

        viewModel.loadSamplesByCategoryID(type);
        // 设置下拉刷新
        swipeRefresh.setOnRefreshListener(() ->     viewModel.loadSamplesByCategoryID(type));
    }
    
    @Override
    protected void setupDataBinding() {
        // 暂时不需要数据绑定
    }
    
    @Override
    protected void setupObservers() {
        // 观察画廊列表
        viewModel.getGalleryItems().observe(getViewLifecycleOwner(), items -> {
            if (items != null) {
                galleryAdapter.setItems(items);
                    swipeRefresh.setRefreshing(false);
            }
        });
        // 观察samples数据
        viewModel.getSamples().observe(getViewLifecycleOwner(), samples -> {
            if (samples != null) {
                currentSamples = samples;
            }
        });

        
        // 观察选中的图片
        viewModel.getSelectedItem().observe(getViewLifecycleOwner(), item -> {
            if (item != null) {
                navigateToDrawingActivityWithPrompt(item);
            }
        });
    }
    
    private void setupRecyclerView() {
        // 使用StaggeredGridLayoutManager实现瀑布流
        androidx.recyclerview.widget.StaggeredGridLayoutManager layoutManager = 
            new androidx.recyclerview.widget.StaggeredGridLayoutManager(2, 
                androidx.recyclerview.widget.StaggeredGridLayoutManager.VERTICAL);
        layoutManager.setGapStrategy(androidx.recyclerview.widget.StaggeredGridLayoutManager.GAP_HANDLING_NONE); // 防止重新排列
        rvGallery.setLayoutManager(layoutManager);
        
        // 优化RecyclerView性能
        rvGallery.setHasFixedSize(true);
        rvGallery.setItemViewCacheSize(20); // 增加缓存大小
        rvGallery.setDrawingCacheEnabled(true);
        rvGallery.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        
        // 禁用item动画，防止闪烁
        rvGallery.setItemAnimator(null);
        
        galleryAdapter = new DrawingGalleryAdapter();
        galleryAdapter.setOnItemClickListener(new DrawingGalleryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(DrawingGalleryItem item) {
                // 根据图片URL找到对应的sample
                DrawingSampleDto matchedSample = null;
                for (DrawingSampleDto sample : currentSamples) {
                    if (sample.getImageUrl() != null && sample.getImageUrl().equals(item.getImageUrl())) {
                        matchedSample = sample;
                        break;
                    }
                }
                
                if (matchedSample != null) {
                    // 跳转到详情页
                    Timber.tag("DrawingFragment").d( "=== 点击画廊项目 ===");
                    Timber.tag("DrawingFragment").d( "Sample ID: " + matchedSample.getId() + " <-- 将传递给详情页");
                    Timber.tag("DrawingFragment").d( "Image URL: " + matchedSample.getImageUrl());
                    Timber.tag("DrawingFragment").d( "Prompt: " + matchedSample.getPrompt());
                    
                    Intent intent = new Intent(getActivity(), DrawingDetailActivity.class);
                    intent.putExtra("sample_id", String.valueOf(matchedSample.getId()));
                    intent.putExtra("image_url", matchedSample.getImageUrl());
                    intent.putExtra("prompt", matchedSample.getPrompt());
                    intent.putExtra("style", matchedSample.getStyle());
                    // 传递示例图片风格ID，便于“做同款”时默认选中
                    if (matchedSample.getStyleId() != null) {
                        intent.putExtra("styleId", String.valueOf(matchedSample.getStyleId()));
                    }
                    intent.putExtra("ratio", item.getRatio());
                    intent.putExtra("width", matchedSample.getWidth());
                    intent.putExtra("height", matchedSample.getHeight());

                    startActivity(intent);
                } else {
                    Timber.tag("DrawingFragment").e( "未找到匹配的sample!");
                }
            }
            
            @Override
            public void onActionClick(DrawingGalleryItem item) {
                // 做同款功能 - 跳转到选择比例页面
                DrawingSampleDto matchedSample = null;
                for (DrawingSampleDto sample : currentSamples) {
                    if (sample.getImageUrl() != null && sample.getImageUrl().equals(item.getImageUrl())) {
                        matchedSample = sample;
                        break;
                    }
                }
                
                if (matchedSample != null) {
                    // 跳转到DrawingActivity并传递提示词和参考图片
                    Intent intent = new Intent(getActivity(), DrawingActivity.class);
                    intent.putExtra("prompt", matchedSample.getPrompt());
                    intent.putExtra("style", matchedSample.getStyle());
                    // 添加参考图片URL
                    if (matchedSample.getImageUrl() != null && !matchedSample.getImageUrl().isEmpty()) {
                        intent.putExtra("reference_image_url", matchedSample.getImageUrl());
                    }
                    intent.putExtra("ratio", item.getRatio());
                    intent.putExtra("styleId", item.getStyleId());
                    intent.putExtra("width", matchedSample.getWidth());
                    intent.putExtra("height", matchedSample.getHeight());
                    startActivity(intent);
                }
            }
        });
        rvGallery.setAdapter(galleryAdapter);
        
        int spacing = getResources().getDimensionPixelSize(R.dimen.gallery_item_spacing);
        rvGallery.addItemDecoration(new DrawingGalleryActivity.GridSpacingItemDecoration(2, spacing, true));
    }
    

    
    private void navigateToDrawingActivityWithPrompt(DrawingGalleryItem item) {
        Intent intent = new Intent(getActivity(), DrawingActivity.class);
        intent.putExtra("prompt", item.getPrompt());
        intent.putExtra("style", item.getStyle());
        intent.putExtra("ratio", item.getRatio());
        intent.putExtra("styleId", item.getStyleId());
        intent.putExtra("width", item.getWidth());
        intent.putExtra("height", item.getHeight());
        // 添加参考图片URL
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            intent.putExtra("reference_image_url", item.getImageUrl());
        }
        startActivity(intent);
    }
}