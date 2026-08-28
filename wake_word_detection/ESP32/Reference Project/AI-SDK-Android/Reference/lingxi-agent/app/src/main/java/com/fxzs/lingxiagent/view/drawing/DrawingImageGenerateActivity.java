package com.fxzs.lingxiagent.view.drawing;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.ZUtil.ImageUtil;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.viewmodel.drawing.VMDrawingTransform;

import timber.log.Timber;

/**
 * 图片生成页面 - 显示生成进度、成功/失败状态
 */
public class DrawingImageGenerateActivity extends BaseActivity<VMDrawingTransform> {
    
    public static final String EXTRA_ORIGINAL_IMAGE_URI = "original_image_uri";
    public static final String EXTRA_STYLE_ITEM = "style_item";
    public static final String EXTRA_STYLE_DESCRIPTION = "style_description";
    public static final String EXTRA_GENERATED_IMAGE_URL = "generated_image_url";
    public static final String EXTRA_SESSION_ID = "session_id";
    public static final String EXTRA_TASK_ID = "task_id";
    public static final String EXTRA_IS_FROM_HISTORY= "is_from_history";

    private static final int DELAY_SHOW_ORIGINAL = 3000; // 3秒后切换回生成后的图片
    
    private View layoutProgress;
    private View layoutError;
    private View layoutSuccess;
    private View layoutSuccessPic;

    private ProgressBar progressBar;
    private TextView tvProgress;
    private TextView tvProgressHint;
    
    private ImageView ivErrorIcon;
    private TextView tvErrorText;
    private LinearLayout btnRegenerate;
    
    private ImageView ivGeneratedImage;
    private ImageView iv_show_original;
    private LinearLayout btnShowFullImage;
    private LinearLayout btnShowOriginal;
    private LinearLayout btnDownload;
    private LinearLayout btnReEdit;
    private LinearLayout btnContinueCreate;

    private TextView tvStyleDescription;
    private ImageView iv_drawing;
    private ImageView ivStyleIcon;

    private Uri originalImageUri;
    private String generatedImageUrl;
    private DrawingTransformStyleItem styleItem;
    private String styleDescription;
    
    private static final String STATE_ORIGINAL_URI = "state_original_uri";
    private static final String STATE_GENERATED_URL = "state_generated_url";
    private static final String STATE_STYLE_ITEM = "state_style_item";
    private static final String STATE_STYLE_DESCRIPTION = "state_style_description";
    private static final String STATE_IS_SHOWING_ORIGINAL = "state_is_showing_original";
    private static final String STATE_IS_FROM_HISTORY = "state_is_from_history";
    private static final String STATE_PROGRESS_VALUE = "state_progress_value";
    private static final String STATE_CURRENT_STATE = "state_current_state";
    private static final String STATE_SESSION_ID = "state_session_id";
    private static final String STATE_TASK_ID = "state_task_id";

    private static final int UI_STATE_PROGRESS = 0;
    private static final int UI_STATE_ERROR = 1;
    private static final int UI_STATE_SUCCESS = 2;

    private Handler handler;
    private Runnable showOriginalRunnable;
    private boolean isShowingOriginal = false;
    private boolean isFromHistory = false;
    private int currentState = UI_STATE_PROGRESS;
    private int savedProgress = 0;
    private Bundle savedState;
    private Long savedSessionId;
    private Long savedTaskId;

    @Override
    protected int getLayoutResource() {
        return R.layout.act_drawing_image_generate;
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
        handler = new Handler(Looper.getMainLooper());

        if (savedState != null) {
            restoreState(savedState);
        } else {
            // 获取传递的数据
            Intent intent = getIntent();
            String originalUriString = intent.getStringExtra(EXTRA_ORIGINAL_IMAGE_URI);
            Long sessionId = intent.getLongExtra(EXTRA_SESSION_ID, 0);
            Long taskId = intent.getLongExtra(EXTRA_TASK_ID, 0);
            if (originalUriString != null) {
                originalImageUri = Uri.parse(originalUriString);
            }
            if (originalUriString != null) {
                viewModel.setSessionId(sessionId);
            }
            styleItem = (DrawingTransformStyleItem) intent.getSerializableExtra(EXTRA_STYLE_ITEM);
            styleDescription = intent.getStringExtra(EXTRA_STYLE_DESCRIPTION);
            generatedImageUrl = intent.getStringExtra(EXTRA_GENERATED_IMAGE_URL);
            isFromHistory = intent.getBooleanExtra(EXTRA_IS_FROM_HISTORY, false);
            savedSessionId = sessionId;
            savedTaskId = taskId;
        }

        initViews();
        setupClickListeners();
        adjustLayoutHeightsForTablet();

        // 根据是否有生成结果决定显示哪个状态
        if (currentState == UI_STATE_SUCCESS) {
            showSuccessState();
        } else if (currentState == UI_STATE_ERROR) {
            showErrorState();
        } else {
            if (generatedImageUrl != null && !generatedImageUrl.isEmpty()) {
                showSuccessState();
            } else {
                if (isFromHistory && savedTaskId != null && savedTaskId != 0) {
                    showProgressState();
                    viewModel.setCurrentTaskId(savedTaskId);
                    viewModel.startPollingTaskStatus();
                } else if (savedState == null) {
                    // 调用API生成图片
                    showProgressState();
                    startGenerateImage();
                } else {
                    showProgressState();
                }
            }
        }

        if (isTablet()) {
            findViewById(R.id.ll_space).setVisibility(View.VISIBLE);
            applyTabletStyleTweaks();
        }
        applyRestoredImageState();
    }
    
    private void initViews() {
        layoutProgress = findViewById(R.id.layout_progress);
        layoutError = findViewById(R.id.layout_error);
        layoutSuccess = findViewById(R.id.layout_success);
        layoutSuccessPic = findViewById(R.id.layout_success_pic);

        progressBar = findViewById(R.id.progress_bar);
        tvProgress = findViewById(R.id.tv_progress);
        tvProgressHint = findViewById(R.id.tv_progress_hint);
        
        ivErrorIcon = findViewById(R.id.iv_error_icon);
        tvErrorText = findViewById(R.id.tv_error_text);
        btnRegenerate = findViewById(R.id.btn_regenerate);
        
        ivGeneratedImage = findViewById(R.id.iv_generated_image);
        iv_show_original = findViewById(R.id.iv_show_original);
        btnShowFullImage = findViewById(R.id.btn_show_full_image);
        btnShowOriginal = findViewById(R.id.btn_show_original);
        btnDownload = findViewById(R.id.btn_download);
        btnReEdit = findViewById(R.id.btn_re_edit);
        btnContinueCreate = findViewById(R.id.btn_continue_create);

        tvStyleDescription = findViewById(R.id.tv_style_description);
        iv_drawing = findViewById(R.id.iv_drawing);
        ivStyleIcon = findViewById(R.id.iv_style_icon);

        // 设置风格信息
        if (styleItem != null && ivStyleIcon != null) {
            // 加载风格图标
            Glide.with(this).load(styleItem.getIconUrl()).into(ivStyleIcon);
        }
        if (styleDescription != null && !styleDescription.isEmpty()) {
            tvStyleDescription.setText(styleDescription);
        } else {
//            tvStyleDescription.setText("转化风格的时候,所有景物保持一致,不要增加或删除任何景物");
        }
        
        // 设置返回按钮
        findViewById(R.id.back).setOnClickListener(v -> finish());
    }

    private void adjustLayoutHeightsForTablet() {
        if (!isTablet()) {
            return;
        }
        int height = getResources().getDisplayMetrics().heightPixels * 4 / 9;
        updateLayoutHeight(layoutProgress, height);
        updateLayoutHeight(layoutSuccessPic, height);
        updateLayoutHeight(layoutError, height);
    }

    private void applyTabletStyleTweaks() {
        if (!isTablet()) {
            return;
        }
        // 风格描述在 Pad 上略小一点，避免过于抢眼
        if (tvStyleDescription != null) {
            tvStyleDescription.setTextSize(13);
        }
        // 风格类型图在 Pad 上放大一点
        if (ivStyleIcon != null) {
            ViewGroup.LayoutParams params = ivStyleIcon.getLayoutParams();
            if (params != null) {
                params.width = dpToPx(96);
                params.height = dpToPx(126);
                ivStyleIcon.setLayoutParams(params);
            }
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void updateLayoutHeight(View target, int height) {
        if (target == null) {
            return;
        }
        ViewGroup.LayoutParams params = target.getLayoutParams();
        if (params != null) {
            params.height = height;
            target.setLayoutParams(params);
        }
    }
    
    private void setupClickListeners() {
        btnRegenerate.setOnClickListener(v -> {
            // 重新生成
            showProgressState();
            startGenerateImage();
        });
        
        btnShowFullImage.setOnClickListener(v -> {
            // 显示大图
//            if (generatedImageUrl != null) {
//                Intent intent = new Intent(this, DrawingImageViewerActivity.class);
//                intent.putExtra("image_url", generatedImageUrl);
//                intent.putExtra("hide_bottom_bar", true);
//                startActivity(intent);
//            }
            downloadImage();
        });
        
        btnShowOriginal.setOnClickListener(v -> {
            // 显示原图
            showOriginalImage();
        });
        
        btnDownload.setOnClickListener(v -> {
            // 下载图片
            downloadImage();
        });
        
        btnReEdit.setOnClickListener(v -> {
            // 重新编辑 - 返回风格转绘页面

            if(isFromHistory){

                Intent intent = new Intent(this, DrawingTransformActivity.class);
                intent.putExtra(DrawingTransformActivity.EXTRA_URL,generatedImageUrl);
                intent.putExtra(DrawingTransformActivity.EXTRA_SESSION_ID,viewModel.getSessionId());
                intent.putExtra(DrawingTransformActivity.EXTRA_DES,styleDescription);
//                intent.putExtra(EXTRA_SESSION_ID,viewModel.getSessionId());
                startActivity(intent);
                finish();
            }else {
                Intent intent = new Intent();
                intent.putExtra(DrawingTransformActivity.EXTRA_TYPE,1);
                intent.putExtra(DrawingTransformActivity.EXTRA_URL,originalImageUri.toString());
                intent.putExtra(DrawingTransformActivity.EXTRA_SESSION_ID,viewModel.getSessionId());
                setResult(RESULT_OK,intent);
                finish();
            }
        });
        
        btnContinueCreate.setOnClickListener(v -> {
            // 继续创作

            if(isFromHistory){

                Intent intent = new Intent(this, DrawingTransformActivity.class);
//                intent.putExtra(DrawingTransformActivity.EXTRA_URL,generatedImageUrl);
//                intent.putExtra(DrawingTransformActivity.EXTRA_SESSION_ID,viewModel.getSessionId());
//                intent.putExtra(DrawingTransformActivity.EXTRA_DES,styleDescription);
                startActivity(intent);
                finish();
            }else {
                Intent intent = new Intent();
                intent.putExtra(DrawingTransformActivity.EXTRA_TYPE,2);
//                intent.putExtra(DrawingTransformActivity.EXTRA_URL,generatedImageUrl);
//                intent.putExtra(DrawingTransformActivity.EXTRA_SESSION_ID,viewModel.getSessionId());
                setResult(RESULT_OK,intent);
                finish();
            }
//            // 调用API生成图片
//            showProgressState();
//            startGenerateImage();
        });
    }
    
    private void showProgressState() {
        currentState = UI_STATE_PROGRESS;
        layoutProgress.setVisibility(View.VISIBLE);
        layoutError.setVisibility(View.GONE);
        if (layoutSuccess != null) {
            layoutSuccess.setVisibility(View.GONE);
        }
        if (layoutSuccessPic != null) {
            layoutSuccessPic.setVisibility(View.GONE);
        }

        ImageUtil.loadGif(DrawingImageGenerateActivity.this, R.drawable.bg_imagine_loading_small, iv_drawing);

        // 隐藏底部按钮
        View bottomBar = findViewById(R.id.bottom_bar);
        if (bottomBar != null) {
            bottomBar.setVisibility(View.GONE);
        }
        
        // 重置进度
        if (progressBar != null) {
            progressBar.setProgress(0);
        }
        if (tvProgress != null) {
            tvProgress.setText("0%");
        }
    }
    
    private void showErrorState() {
        currentState = UI_STATE_ERROR;
        layoutProgress.setVisibility(View.GONE);
        layoutError.setVisibility(View.VISIBLE);
        if (layoutSuccess != null) {
            layoutSuccess.setVisibility(View.GONE);
        }
        if (layoutSuccessPic != null) {
            layoutSuccessPic.setVisibility(View.GONE);
        }

        // 显示底部重新生成按钮
        View bottomBar = findViewById(R.id.bottom_bar);
        if (bottomBar != null) {
            bottomBar.setVisibility(View.VISIBLE);
        }
    }
    
    private void showSuccessState() {
        currentState = UI_STATE_SUCCESS;
        layoutProgress.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        if (layoutSuccess != null) {
            layoutSuccess.setVisibility(View.VISIBLE);
        }
        if (layoutSuccessPic != null) {
            layoutSuccessPic.setVisibility(View.VISIBLE);
        }

        // 隐藏底部重新生成按钮
        View bottomBar = findViewById(R.id.bottom_bar);
        if (bottomBar != null) {
            bottomBar.setVisibility(View.GONE);
        }
        
        // 加载生成的图片
        if (generatedImageUrl != null && !generatedImageUrl.isEmpty() && ivGeneratedImage != null) {
            Glide.with(this)
                    .load(generatedImageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .into(ivGeneratedImage);
        }
    }
    
    /**
     * 开始生成图片
     */
    private void startGenerateImage() {
        if (originalImageUri == null) {
            showErrorState();
            return;
        }
        
        // 调用 ViewModel 生成图片
        viewModel.generateTransformImage(originalImageUri, styleItem, styleDescription);
    }
    
    /**
     * 显示原图
     */
    private void showOriginalImage() {
        if (originalImageUri == null) {
            return;
        }
        if(!isShowingOriginal){
//            ai_draw_transform_compare_before
            isShowingOriginal = true;
            Glide.with(this)
                    .load(originalImageUri)
                    .into(ivGeneratedImage);
            Glide.with(this)
                    .load(R.drawable.ai_draw_transform_compare_after)
                    .into(iv_show_original);
        } else{
            isShowingOriginal = false;
                Glide.with(this)
                        .load(generatedImageUrl)
                        .into(ivGeneratedImage);
            Glide.with(this)
                    .load(R.drawable.ai_draw_transform_compare_before)
                    .into(iv_show_original);
        }
        
        // 3秒后自动切换回生成后的图片
//        if (showOriginalRunnable != null) {
//            handler.removeCallbacks(showOriginalRunnable);
//        }
//        showOriginalRunnable = () -> {
//            if (isShowingOriginal && generatedImageUrl != null) {
//                Glide.with(this)
//                        .load(generatedImageUrl)
//                        .into(ivGeneratedImage);
//                isShowingOriginal = false;
//            }
//        };
//        handler.postDelayed(showOriginalRunnable, DELAY_SHOW_ORIGINAL);
    }
    
    /**
     * 下载图片
     */
    private void downloadImage() {
        String previewImageUrl = generatedImageUrl;
        if (isShowingOriginal && originalImageUri != null) {
            previewImageUrl = originalImageUri.toString();
        }

        if (previewImageUrl == null || previewImageUrl.isEmpty()) {
            return;
        }

        // 跳转到图片查看页面进行下载
        Intent intent = new Intent(this, DrawingImageViewerActivity.class);
        intent.putExtra("image_url", previewImageUrl);
        startActivity(intent);
    }
    
    @Override
    protected void setupObservers() {
        // 监听生成进度
        viewModel.getGenerateProgress().observe(this, progress -> {
            if (progress != null && progressBar != null && tvProgress != null) {
                savedProgress = progress;
                progressBar.setProgress(progress);
                tvProgress.setText(progress + "%");
            }
        });
        
        // 监听生成结果
        viewModel.getGeneratedImageUrl().observe(this, imageUrl -> {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                generatedImageUrl = imageUrl;
                showSuccessState();
            }
        });
        
        // 监听生成错误
        viewModel.getGenerateError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                showErrorState();
            }
        });
        
        // 监听加载状态
        viewModel.getLoading().observe(this, isLoading -> {
            // 可以根据加载状态更新UI
        });


        viewModel.getStyleItems().observe(this, styleItems -> {
            if (styleItems != null) {
                if(isFromHistory){
                    for (int i = 0; i < styleItems.size(); i++) {
                        if (styleItems.get(i).getId() == styleItem.getId()) {
                            // 设置风格信息
                            if (styleItem != null && ivStyleIcon != null) {
                                // 加载风格图标
                                Glide.with(this).load(styleItems.get(i).getIconUrl()).into(ivStyleIcon);
                            }
                        }
                    }
                }
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (showOriginalRunnable != null) {
            handler.removeCallbacks(showOriginalRunnable);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (originalImageUri != null) {
            outState.putString(STATE_ORIGINAL_URI, originalImageUri.toString());
        }
        outState.putString(STATE_GENERATED_URL, generatedImageUrl);
        outState.putSerializable(STATE_STYLE_ITEM, styleItem);
        outState.putString(STATE_STYLE_DESCRIPTION, styleDescription);
        outState.putBoolean(STATE_IS_SHOWING_ORIGINAL, isShowingOriginal);
        outState.putBoolean(STATE_IS_FROM_HISTORY, isFromHistory);
        outState.putInt(STATE_PROGRESS_VALUE, savedProgress);
        outState.putInt(STATE_CURRENT_STATE, currentState);
        if (savedSessionId != null) {
            outState.putLong(STATE_SESSION_ID, savedSessionId);
        }
        if (savedTaskId != null) {
            outState.putLong(STATE_TASK_ID, savedTaskId);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            savedState = savedInstanceState;
        }
        super.onCreate(savedInstanceState);
    }

    private void restoreState(Bundle state) {
        String originalUriString = state.getString(STATE_ORIGINAL_URI);
        if (originalUriString != null) {
            originalImageUri = Uri.parse(originalUriString);
        }
        generatedImageUrl = state.getString(STATE_GENERATED_URL);
        styleItem = (DrawingTransformStyleItem) state.getSerializable(STATE_STYLE_ITEM);
        styleDescription = state.getString(STATE_STYLE_DESCRIPTION);
        isShowingOriginal = state.getBoolean(STATE_IS_SHOWING_ORIGINAL, false);
        isFromHistory = state.getBoolean(STATE_IS_FROM_HISTORY, false);
        savedProgress = state.getInt(STATE_PROGRESS_VALUE, 0);
        currentState = state.getInt(STATE_CURRENT_STATE, UI_STATE_PROGRESS);
        if (state.containsKey(STATE_SESSION_ID)) {
            savedSessionId = state.getLong(STATE_SESSION_ID, 0);
            viewModel.setSessionId(savedSessionId);
        }
        if (state.containsKey(STATE_TASK_ID)) {
            savedTaskId = state.getLong(STATE_TASK_ID, 0);
        }
    }

    private void applyRestoredImageState() {
        if (currentState == UI_STATE_SUCCESS && ivGeneratedImage != null) {
            if (isShowingOriginal && originalImageUri != null) {
                Glide.with(this)
                        .load(originalImageUri)
                        .into(ivGeneratedImage);
                Glide.with(this)
                        .load(R.drawable.ai_draw_transform_compare_after)
                        .into(iv_show_original);
            } else if (generatedImageUrl != null && !generatedImageUrl.isEmpty()) {
                Glide.with(this)
                        .load(generatedImageUrl)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .into(ivGeneratedImage);
                Glide.with(this)
                        .load(R.drawable.ai_draw_transform_compare_before)
                        .into(iv_show_original);
            }
        }
        if (currentState == UI_STATE_PROGRESS && progressBar != null && tvProgress != null) {
            progressBar.setProgress(savedProgress);
            tvProgress.setText(savedProgress + "%");
        }
    }
}

