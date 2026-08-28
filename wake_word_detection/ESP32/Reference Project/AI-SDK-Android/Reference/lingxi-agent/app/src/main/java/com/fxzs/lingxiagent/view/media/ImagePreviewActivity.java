package com.fxzs.lingxiagent.view.media;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.media.ImagePreviewController;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.viewmodel.main.VMEmpty;
import com.github.chrisbanes.photoview.PhotoView;

import timber.log.Timber;

/**
 * 图片预览Activity
 * 支持图片缩放、手势操作和分享功能
 */
public class ImagePreviewActivity extends BaseActivity {
    
    public static final String EXTRA_IMAGE_URL = "image_url";
    public static final String EXTRA_IMAGE_NAME = "image_name";
    
    private PhotoView photoView;
    private ProgressBar progressBar;
    private TextView tvImageName;
    private ImageView btnClose;
    private ImageView btnShare;
    private LinearLayout errorLayout;
    private TextView tvErrorMessage;
    private View btnRetry;
    private LinearLayout topToolbar;
    
    private ImagePreviewController controller;
    private String imageUrl;
    private String imageName;
    
    /**
     * 启动图片预览Activity
     * @param context 上下文
     * @param imageUrl 图片URL
     * @param imageName 图片名称
     */
    public static void start(Context context, String imageUrl, String imageName) {
        Timber.tag("ImagePreviewActivity").d( "start called with imageUrl: " + imageUrl + ", imageName: " + imageName);
        Intent intent = new Intent(context, ImagePreviewActivity.class);
        intent.putExtra(EXTRA_IMAGE_URL, imageUrl);
        intent.putExtra(EXTRA_IMAGE_NAME, imageName);
        context.startActivity(intent);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置全屏模式
        setupFullScreenMode();

//        setContentView(R.layout.activity_image_preview);

    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_image_preview;
    }

    @Override
    protected Class getViewModelClass() {
        return VMEmpty.class;
    }

    @Override
    protected void setupDataBinding() {

    }

    @Override
    protected void initializeViews() {

        // 获取Intent参数
        getIntentExtras();

        // 初始化视图
        initViews();

        // 初始化控制器
        initController();

        // 设置事件监听
        setupEventListeners();

        // 加载图片
        loadImage();
    }

    @Override
    protected void setupObservers() {

    }

    /**
     * 设置全屏模式
     */
    private void setupFullScreenMode() {
        // 隐藏状态栏和导航栏
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        
        // 设置沉浸式模式
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }
    
    /**
     * 获取Intent传递的参数
     */
    private void getIntentExtras() {
        Intent intent = getIntent();
        if (intent != null) {
            imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL);
            imageName = intent.getStringExtra(EXTRA_IMAGE_NAME);
        }
        
        // 设置默认值
        if (imageName == null || imageName.isEmpty()) {
            imageName = "图片";
        }
    }
    
    /**
     * 初始化视图组件
     */
    private void initViews() {
        photoView = findViewById(R.id.photo_view);
        progressBar = findViewById(R.id.progress_bar);
        tvImageName = findViewById(R.id.tv_image_name);
        btnClose = findViewById(R.id.btn_close);
        btnShare = findViewById(R.id.btn_share);
        errorLayout = findViewById(R.id.error_layout);
        tvErrorMessage = findViewById(R.id.tv_error_message);
        btnRetry = findViewById(R.id.btn_retry);
        topToolbar = findViewById(R.id.top_toolbar);
        
        // 设置图片名称
        tvImageName.setText(imageName);
    }
    
    /**
     * 初始化图片预览控制器
     */
    private void initController() {
        controller = new ImagePreviewController(photoView, progressBar);
    }
    
    /**
     * 设置事件监听器
     */
    private void setupEventListeners() {
        // 关闭按钮
        btnClose.setOnClickListener(v -> finish());
        
        // 分享按钮
        btnShare.setOnClickListener(v -> shareImage());
        
        // 重试按钮
        btnRetry.setOnClickListener(v -> {
            hideErrorState();
            loadImage();
        });
        
        // PhotoView点击事件 - 切换工具栏显示/隐藏
        photoView.setOnPhotoTapListener((view, x, y) -> toggleToolbarVisibility());
        
        // PhotoView缩放监听
        photoView.setOnScaleChangeListener((scaleFactor, focusX, focusY) -> {
            // 可以在这里添加缩放相关的逻辑
        });
    }
    
    /**
     * 加载图片
     */
    private void loadImage() {
        if (imageUrl == null || imageUrl.isEmpty()) {
            showErrorState("图片地址为空");
            return;
        }
        
        // 检查内存状态
        if (controller != null) {
            controller.checkMemoryAndOptimize();
        }
        
        // 判断是否为大图片并选择合适的加载方式
        if (isLargeImageUrl(imageUrl)) {
            controller.loadLargeImage(imageUrl, new ImagePreviewController.LoadCallback() {
                @Override
                public void onLoadSuccess() {
                    runOnUiThread(() -> hideErrorState());
                }
                
                @Override
                public void onLoadFailed(String errorMessage) {
                    runOnUiThread(() -> showErrorState(errorMessage));
                }
            });
        } else {
            controller.loadImage(imageUrl, new ImagePreviewController.LoadCallback() {
                @Override
                public void onLoadSuccess() {
                    runOnUiThread(() -> hideErrorState());
                }
                
                @Override
                public void onLoadFailed(String errorMessage) {
                    runOnUiThread(() -> showErrorState(errorMessage));
                }
            });
        }
    }
    
    /**
     * 判断是否为大图片URL
     * @param imageUrl 图片URL
     * @return true如果可能是大图片
     */
    private boolean isLargeImageUrl(String imageUrl) {
        if (imageUrl == null) {
            return false;
        }
        
        String lowerUrl = imageUrl.toLowerCase();
        
        // 检查是否包含高分辨率标识
        return lowerUrl.contains("hd") || lowerUrl.contains("high") || 
               lowerUrl.contains("large") || lowerUrl.contains("original") ||
               lowerUrl.contains("full") || lowerUrl.endsWith(".tiff") || 
               lowerUrl.endsWith(".tif") || lowerUrl.endsWith(".bmp") || 
               lowerUrl.endsWith(".raw");
    }
    
    /**
     * 分享图片
     */
    private void shareImage() {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            controller.shareImage(imageUrl, imageName);
        }
    }
    
    /**
     * 切换工具栏显示/隐藏
     */
    private void toggleToolbarVisibility() {
        if (topToolbar.getVisibility() == View.VISIBLE) {
            hideToolbar();
        } else {
            showToolbar();
        }
    }
    
    /**
     * 显示工具栏
     */
    private void showToolbar() {
        topToolbar.setVisibility(View.VISIBLE);
        topToolbar.animate()
                .alpha(1.0f)
                .setDuration(200)
                .start();
    }
    
    /**
     * 隐藏工具栏
     */
    private void hideToolbar() {
        topToolbar.animate()
                .alpha(0.0f)
                .setDuration(200)
                .withEndAction(() -> topToolbar.setVisibility(View.GONE))
                .start();
    }
    
    /**
     * 显示错误状态
     * @param errorMessage 错误消息
     */
    private void showErrorState(String errorMessage) {
        errorLayout.setVisibility(View.VISIBLE);
        photoView.setVisibility(View.GONE);
        
        if (errorMessage != null && !errorMessage.isEmpty()) {
            tvErrorMessage.setText(errorMessage);
        } else {
            tvErrorMessage.setText("图片加载失败");
        }
    }
    
    /**
     * 隐藏错误状态
     */
    private void hideErrorState() {
        errorLayout.setVisibility(View.GONE);
        photoView.setVisibility(View.VISIBLE);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (controller != null) {
            controller.pauseImageLoading();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (controller != null) {
            controller.resumeImageLoading();
        }
    }
    
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (controller != null) {
            controller.checkMemoryAndOptimize();
        }
    }
    
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (controller != null && level >= TRIM_MEMORY_RUNNING_MODERATE) {
            controller.checkMemoryAndOptimize();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (controller != null) {
            controller.cleanup();
        }
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}