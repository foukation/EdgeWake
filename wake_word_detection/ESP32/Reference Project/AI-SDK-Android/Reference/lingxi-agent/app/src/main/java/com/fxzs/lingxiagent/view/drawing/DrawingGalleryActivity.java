package com.fxzs.lingxiagent.view.drawing;

import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingGalleryItem;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.common.DataBindingUtils;
import com.fxzs.lingxiagent.view.user.UserProfileActivity;
import com.fxzs.lingxiagent.viewmodel.drawing.VMDrawingGallery;

import timber.log.Timber;

/**
 * AI绘画示例画廊页面
 */
public class DrawingGalleryActivity extends BaseActivity<VMDrawingGallery> {
    
    private RecyclerView rvGallery;
    private TextView tvPromptHint;
    private ImageView ivVoiceInput;
    
    // 底部导航栏
    private LinearLayout navDialog;
    private LinearLayout navAgent;
    private LinearLayout navDrawing;
    private LinearLayout navMeeting;
    private LinearLayout navProfile;
    
    private DrawingGalleryAdapter galleryAdapter;
    
    @Override
    protected int getLayoutResource() {
        return R.layout.activity_drawing_gallery;
    }
    
    @Override
    protected Class<VMDrawingGallery> getViewModelClass() {
        return VMDrawingGallery.class;
    }
    
    @Override
    protected void initializeViews() {
        // 初始化控件
        rvGallery = findViewById(R.id.rv_gallery);
        tvPromptHint = findViewById(R.id.tv_prompt_hint);
        ivVoiceInput = findViewById(R.id.iv_voice_input);
        
        // 底部导航栏
        navDialog = findViewById(R.id.nav_dialog);
        navAgent = findViewById(R.id.nav_agent);
        navDrawing = findViewById(R.id.nav_drawing);
        navMeeting = findViewById(R.id.nav_meeting);
        navProfile = findViewById(R.id.nav_profile);
        
        // 设置RecyclerView
        setupRecyclerView();
        
        // 设置底部导航栏
        setupBottomNavigation();
        
        // 设置语音输入点击事件
        ivVoiceInput.setOnClickListener(v -> navigateToDrawingActivity());
    }
    
    @Override
    protected void setupDataBinding() {
        // 绑定提示文本
        DataBindingUtils.bindTextView(tvPromptHint, viewModel.getPromptHint(), this);
    }
    
    @Override
    protected void setupObservers() {
        // 观察画廊列表
        viewModel.getGalleryItems().observe(this, items -> {
            if (items != null) {
                galleryAdapter.setItems(items);
            }
        });
        
        // 观察选中的图片
        viewModel.getSelectedItem().observe(this, item -> {
            if (item != null) {
                navigateToDrawingActivityWithPrompt(item);
            }
        });
    }
    
    private void setupRecyclerView() {
        // 使用网格布局，每行2列
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        rvGallery.setLayoutManager(layoutManager);
        
        // 设置适配器
        galleryAdapter = new DrawingGalleryAdapter();
        galleryAdapter.setOnItemClickListener(new DrawingGalleryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(DrawingGalleryItem item) {
                viewModel.selectItem(item);
            }
            
            @Override
            public void onActionClick(DrawingGalleryItem item) {
                // 点击"做同款"按钮，直接跳转到绘画页面
                navigateToDrawingActivityWithPrompt(item);
            }
        });
        rvGallery.setAdapter(galleryAdapter);
        
        // 添加间距装饰
        int spacing = getResources().getDimensionPixelSize(R.dimen.gallery_item_spacing);
        rvGallery.addItemDecoration(new GridSpacingItemDecoration(2, spacing, true));
    }
    
    private void setupBottomNavigation() {
        // 设置当前选中项
        navDrawing.setSelected(true);
        
        // 设置点击事件
        navDialog.setOnClickListener(v -> {
            // TODO: 导航到对话页面
            showToast("对话功能开发中");
        });
        
        navAgent.setOnClickListener(v -> {
            // TODO: 导航到智能体页面
            showToast("智能体功能开发中");
        });
        
        navDrawing.setOnClickListener(v -> {
            // 已经在绘画页面，不做操作
        });
        
        navMeeting.setOnClickListener(v -> {
            // TODO: 导航到会议页面
            showToast("会议功能开发中");
        });
        
        navProfile.setOnClickListener(v -> {
            // 导航到用户中心
            Intent intent = new Intent(this, UserProfileActivity.class);
            startActivity(intent);
            finish();
        });
    }
    
    private void navigateToDrawingActivity() {
        Intent intent = new Intent(this, DrawingActivity.class);
        startActivity(intent);
    }
    
    private void navigateToDrawingActivityWithPrompt(DrawingGalleryItem item) {
        Timber.tag("DrawingGallery").d( "navigateToDrawingActivityWithPrompt called");
        Timber.tag("DrawingGallery").d( "Item: " + item);
        Timber.tag("DrawingGallery").d( "Prompt: " + item.getPrompt());
        Timber.tag("DrawingGallery").d( "Style: " + item.getStyle());
        Timber.tag("DrawingGallery").d( "Image URL: " + item.getImageUrl());
        
        // 跳转到绘画页面，带上提示词、风格和参考图片
        Intent intent = new Intent(this, DrawingActivity.class);
        intent.putExtra("prompt", item.getPrompt());
        intent.putExtra("style", item.getStyle());
        // 传递示例图片风格ID（用于同款默认选中）
        if (item.getStyleId() != null) {
            intent.putExtra("styleId", item.getStyleId());
        }
        // 添加参考图片URL
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            intent.putExtra("reference_image_url", item.getImageUrl());
            Timber.tag("DrawingGallery").d( "Adding reference image URL to intent: " + item.getImageUrl());
        } else {
            Timber.tag("DrawingGallery").w( "Image URL is null or empty!");
        }
        
        Timber.tag("DrawingGallery").d( "Starting DrawingActivity with extras: " + intent.getExtras());
        startActivity(intent);
    }
    
    /**
     * 网格间距装饰器
     */
    public static class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private int spanCount;
        private int spacing;
        private boolean includeEdge;
        
        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }
        
        @Override
        public void getItemOffsets(android.graphics.Rect outRect, View view, 
                                   RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int column = position % spanCount;
            
            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount;
                outRect.right = (column + 1) * spacing / spanCount;
                
                if (position < spanCount) {
                    outRect.top = spacing;
                }
                outRect.bottom = spacing;
            } else {
                outRect.left = column * spacing / spanCount;
                outRect.right = spacing - (column + 1) * spacing / spanCount;
                if (position >= spanCount) {
                    outRect.top = spacing;
                }
            }
        }
    }
}