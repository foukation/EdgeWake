package com.fxzs.lingxiagent.view.ppt;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.ppt.dto.PptTemplate;
import com.fxzs.lingxiagent.util.ZDpUtils;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.viewmodel.ppt.VMPptTemplateSelection;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class PptTemplateSelectionActivity extends BaseActivity<VMPptTemplateSelection> {
    
    private ImageView ivBack;
    private ImageView ivClose;
    private View colorSelector;
    private View colorIndicator;
    private View color_indicator_random;
    private View styleSelector;
    private TextView styleSelectorText;
    private LinearLayout styleDropdown;
    private RecyclerView templateRecyclerView;
    private View refreshTemplatesButton;
    private Button generatePptButton;
//    private View progressContainer;
    private ProgressBar progressBar;
    private TextView progressText;
//    private Button stopGenerationButton;
    
    private TemplateAdapter templateAdapter;
    private String selectedTopic;
    private String pptId;

    // 生成进度弹窗相关
    private View progressDialog;
    private View progressBarInDialog;
    private TextView progressStatusText;
    private TextView progressPercentage;
    private View ll_progress_percentage;
    private View ll_empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            selectedTopic = getIntent().getStringExtra("topic");
            pptId = getIntent().getStringExtra("ppt_id");

            Timber.tag("PptTemplateSelectionActivity").d( "onCreate: topic=" + selectedTopic + ", pptId=" + pptId);

            setupRecyclerView();
        } catch (Exception e) {
            Timber.tag("PptTemplateSelectionActivity").e( "onCreate失败"+ e);
            finish();
        }
        ZUtils.startService(this);
    }
    
    @Override
    protected int getLayoutResource() {
        return R.layout.activity_ppt_template_selection;
    }
    
    @Override
    protected Class<VMPptTemplateSelection> getViewModelClass() {
        return VMPptTemplateSelection.class;
    }
    
    @Override
    protected void initializeViews() {
        ivBack = findViewById(R.id.iv_back);
        ivClose = findViewById(R.id.iv_close);
        colorSelector = findViewById(R.id.color_filter);
        colorIndicator = findViewById(R.id.color_indicator);
        color_indicator_random = findViewById(R.id.color_indicator_random);
        styleSelector = findViewById(R.id.style_filter);
        styleSelectorText = findViewById(R.id.style_filter_text);
        styleDropdown = findViewById(R.id.style_dropdown);
        templateRecyclerView = findViewById(R.id.template_recycler_view);
        refreshTemplatesButton = findViewById(R.id.refresh_templates_button);
        generatePptButton = findViewById(R.id.generate_ppt_button);
//        progressContainer = findViewById(R.id.progress_container);
        progressBar = findViewById(R.id.progress_bar);
        progressText = findViewById(R.id.progress_text);
        ll_empty = findViewById(R.id.ll_empty);
//        stopGenerationButton = findViewById(R.id.stop_generation_button);

        ivBack.setOnClickListener(v -> finish());
        ivClose.setOnClickListener(v -> finish());

        colorSelector.setOnClickListener(v -> showColorPicker());
        styleSelector.setOnClickListener(v -> toggleStyleDropdown());

        // 设置风格选项点击事件
        setupStyleDropdownListeners();

        refreshTemplatesButton.setOnClickListener(v -> viewModel.refreshTemplates());
        generatePptButton.setOnClickListener(v -> generatePpt());
//        stopGenerationButton.setOnClickListener(v -> viewModel.stopGeneration());
    }
    
    @Override
    protected void setupDataBinding() {
        viewModel.getSelectedColor().observeForever(color -> {
            // 更新颜色指示器
            updateColorIndicator(color);
        });

        viewModel.getSelectedStyle().observeForever(style ->
            styleSelectorText.setText(style)
        );

        viewModel.getGenerateButtonEnabled().observeForever(enabled -> {
            generatePptButton.setEnabled(enabled);
            generatePptButton.setAlpha(enabled ? 1.0f : 0.5f);
        });
        
        viewModel.getIsGenerating().observeForever(isGenerating -> {
//            progressContainer.setVisibility(isGenerating ? View.VISIBLE : View.GONE);
            generatePptButton.setVisibility(isGenerating ? View.GONE : View.VISIBLE);
            refreshTemplatesButton.setEnabled(!isGenerating);
        });
        
        viewModel.getGenerationProgress().observeForever(progress -> {
            if (progress != null && progressBar !=null) {
                progressBar.setProgress(progress);
            }
        });
        
        viewModel.getGenerationStatus().observeForever(status -> {
            if (status != null && progressText != null) {
                progressText.setText(status);
            }
        });
    }
    
    @Override
    protected void setupObservers() {
        try {
            if (viewModel == null) {
                Timber.tag("PptTemplateSelectionActivity").e( "viewModel为空，无法设置观察者");
                return;
            }

            viewModel.getTemplateList().observeForever(templates -> {
                try {
                    if (templateAdapter != null && templates != null) {
                        Timber.tag("PptTemplateSelectionActivity").d( "更新模板列表，数量: " + templates.size());
                        templateAdapter.setTemplates(templates);
                        templateRecyclerView.setVisibility(View.VISIBLE);
                        ll_empty.setVisibility(View.GONE);
                    }else{
                        templateRecyclerView.setVisibility(View.GONE);
                        ll_empty.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {
                    Timber.tag("PptTemplateSelectionActivity").e( "更新模板列表失败"+ e);
                }
            });

            viewModel.getSelectedTemplateId().observeForever(id -> {
                try {
                    if (templateAdapter != null) {
                        templateAdapter.setSelectedTemplateId(id);
                    }
                } catch (Exception e) {
                    Timber.tag("PptTemplateSelectionActivity").e( "更新选中模板失败"+ e);
                }
            });

            viewModel.getErrorMessage().observeForever(error -> {
                try {
                    if (error != null && !error.isEmpty()) {
                        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Timber.tag("PptTemplateSelectionActivity").e( "显示错误消息失败"+ e);
                }
            });

            viewModel.getSuccessMessage().observeForever(success -> {
                try {
                    if (success != null && !success.isEmpty()) {
                        Toast.makeText(this, success, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Timber.tag("PptTemplateSelectionActivity").e( "显示成功消息失败"+ e);
                }
            });

            viewModel.getNoMoreTemplatesEvent().observe(this, flag -> {
                if (flag != null && flag) {
                    com.fxzs.lingxiagent.view.common.GlobalToast.show(
                        this, "没有更多的模版数据",
                        com.fxzs.lingxiagent.view.common.GlobalToast.Type.NORMAL
                    );
                    templateRecyclerView.setVisibility(View.GONE);
                    ll_empty.setVisibility(View.VISIBLE);
                }
            });
        } catch (Exception e) {
            Timber.tag("PptTemplateSelectionActivity").e( "setupObservers失败"+ e);
        }
    }
    
    private void setupRecyclerView() {
        try {
            if (templateRecyclerView == null) {
                Timber.tag("PptTemplateSelectionActivity").e( "templateRecyclerView为空");
                return;
            }

            GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
            templateRecyclerView.setLayoutManager(layoutManager);

            // 添加间距装饰器：图片间距15dp
            templateRecyclerView.addItemDecoration(new GridSpacingItemDecoration(2, 15, false));

            templateAdapter = new TemplateAdapter();
            templateAdapter.setOnTemplateClickListener(template -> {
                if (viewModel != null) {
                    viewModel.selectTemplate(template.getId());
                }
            });
            templateRecyclerView.setAdapter(templateAdapter);

            if (viewModel != null) {
                viewModel.loadTemplates();
            }
        } catch (Exception e) {
            Timber.tag("PptTemplateSelectionActivity").e( "setupRecyclerView失败"+ e);
        }
    }
    
    private void showColorPicker() {

        styleDropdown.setVisibility(View.GONE);
        // 创建弹窗布局
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_color_selection, null);

        // 创建PopupWindow
        PopupWindow popupWindow = new PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        );

        // 设置弹窗背景：使用无内边距的圆角描边背景，避免底部视觉留白
        popupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_dropdown_panel));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);

        // 设置颜色选择事件
        setupColorSelectionListeners(popupView, popupWindow);

        // 显示弹窗（在颜色选择器下方）
        popupWindow.showAsDropDown(colorSelector, 0, 8);
    }

    private void setupColorSelectionListeners(View popupView, PopupWindow popupWindow) {
        // 全部（彩虹色）
        popupView.findViewById(R.id.color_all).setOnClickListener(v -> {
            selectColor("全部", popupWindow);
        });

        // 红色
        popupView.findViewById(R.id.color_red).setOnClickListener(v -> {
            selectColor("红色", popupWindow);
        });

        // 橙色
        popupView.findViewById(R.id.color_orange).setOnClickListener(v -> {
            selectColor("橙色", popupWindow);
        });

        // 绿色
        popupView.findViewById(R.id.color_green).setOnClickListener(v -> {
            selectColor("绿色", popupWindow);
        });

        // 橙红色
//        popupView.findViewById(R.id.color_orange_red).setOnClickListener(v -> {
//            selectColor("橙红色", popupWindow);
//        });

        // 蓝色
        popupView.findViewById(R.id.color_blue).setOnClickListener(v -> {
            selectColor("蓝色", popupWindow);
        });

        // 紫色
        popupView.findViewById(R.id.color_purple).setOnClickListener(v -> {
            selectColor("紫色", popupWindow);
        });

//        // 青色
//        popupView.findViewById(R.id.color_cyan).setOnClickListener(v -> {
//            selectColor("青色", popupWindow);
//        });

        // 粉色
        popupView.findViewById(R.id.color_pink).setOnClickListener(v -> {
            selectColor("粉色", popupWindow);
        });
    }

    private void selectColor(String color, PopupWindow popupWindow) {
        viewModel.setSelectedColor(color);
        updateColorIndicator(color);
        popupWindow.dismiss();
    }
    
    private void toggleStyleDropdown() {
        if (styleDropdown.getVisibility() == View.VISIBLE) {
            styleDropdown.setVisibility(View.GONE);
        } else {
            styleDropdown.setVisibility(View.VISIBLE);
        }
    }

    private void setupStyleDropdownListeners() {
        findViewById(R.id.style_recommended).setOnClickListener(v -> selectStyle("推荐"));
        findViewById(R.id.style_business).setOnClickListener(v -> selectStyle("简约商务"));
        findViewById(R.id.style_cartoon).setOnClickListener(v -> selectStyle("卡通插画"));
//        findViewById(R.id.style_tech).setOnClickListener(v -> selectStyle("炫酷科技"));
        findViewById(R.id.style_chinese).setOnClickListener(v -> selectStyle("中国风"));
        findViewById(R.id.style_fresh).setOnClickListener(v -> selectStyle("水彩清新"));
//        findViewById(R.id.style_government).setOnClickListener(v -> selectStyle("党务政务"));
//        findViewById(R.id.style_other).setOnClickListener(v -> selectStyle("其他"));
    }

    private void selectStyle(String style) {
        viewModel.setSelectedStyle(style);
        styleDropdown.setVisibility(View.GONE);
    }

    private void updateColorIndicator(String color) {
        // 根据颜色名称更新颜色指示器的背景
        if ("全部".equals(color)) {
            color_indicator_random.setVisibility(View.VISIBLE);
            colorIndicator.setVisibility(View.GONE);
            // 显示彩虹渐变
            colorIndicator.setBackground(getResources().getDrawable(R.drawable.bg_color_rainbow));
            return;
        }

        int colorRes;
        switch (color) {
            case "红色":
                colorRes = android.graphics.Color.parseColor("#FF4444");
                break;
            case "橙色":
                colorRes = android.graphics.Color.parseColor("#FF8800");
                break;
            case "绿色":
                colorRes = android.graphics.Color.parseColor("#44AA44");
                break;
            case "橙红色":
                colorRes = android.graphics.Color.parseColor("#FF6600");
                break;
            case "蓝色":
                colorRes = android.graphics.Color.parseColor("#4488FF");
                break;
            case "紫色":
                colorRes = android.graphics.Color.parseColor("#8844FF");
                break;
            case "青色":
                colorRes = android.graphics.Color.parseColor("#44AAAA");
                break;
            case "粉色":
                colorRes = android.graphics.Color.parseColor("#FF44AA");
                break;
            default:
                // 默认显示彩虹渐变
                colorIndicator.setBackground(getResources().getDrawable(R.drawable.bg_color_rainbow));
                return;
        }
        color_indicator_random.setVisibility(View.GONE);
        colorIndicator.setVisibility(View.VISIBLE);
        colorIndicator.setBackground(getResources().getDrawable(R.drawable.bg_color_circle));
        ViewCompat.setBackgroundTintList(colorIndicator, ColorStateList.valueOf(colorRes));

    }
    
    private void generatePpt() {
        if (selectedTopic == null || selectedTopic.isEmpty()) {
            Toast.makeText(this, "主题信息丢失", Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示生成进度弹窗
        showProgressDialog();

        viewModel.generatePpt(selectedTopic).observeForever(result -> {
            if (result != null) {
                // 隐藏弹窗
                hideProgressDialog();

                if (result.isSuccess()) {
                    // PPT生成成功，跳转到预览界面
                    Intent intent = new Intent(this, PptPreviewActivity.class);
                    intent.putExtra(com.fxzs.lingxiagent.util.PptStateManager.EXTRA_PPT_URL, result.getPptUrl());
                    intent.putExtra(com.fxzs.lingxiagent.util.PptStateManager.EXTRA_TOPIC, selectedTopic);
                    startActivity(intent);
//                    finish();
                } else {
                    // PPT生成失败
                    String errorMsg = result.getErrorMessage();
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = "PPT生成失败，请重试";
                    }
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showProgressDialog() {
        if (progressDialog != null) {
            return; // 弹窗已经显示
        }

        // 创建弹窗布局
        progressDialog = LayoutInflater.from(this).inflate(R.layout.dialog_ppt_generation, null);

        // 获取弹窗内的控件
        progressBarInDialog = progressDialog.findViewById(R.id.progress_bar);
        progressStatusText = progressDialog.findViewById(R.id.progress_status_text);
        progressPercentage = progressDialog.findViewById(R.id.progress_percentage);
        ll_progress_percentage = progressDialog.findViewById(R.id.ll_progress_percentage);

        // 设置关闭按钮点击事件
        progressDialog.findViewById(R.id.close_dialog).setOnClickListener(v -> {
            hideProgressDialog();
            viewModel.stopGeneration(); // 停止生成
        });

        // 将弹窗添加到根布局
        ViewGroup rootView = findViewById(android.R.id.content);
        rootView.addView(progressDialog);
        startLoadingAnimation();

        // 监听生成进度
        observeGenerationProgress();
    }

    private void startLoadingAnimation() {
        View iv_progress = progressDialog.findViewById(R.id.iv_progress);
        if (iv_progress != null) {
            try {
                Animation rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.ppt_loading_rotation);
                iv_progress.startAnimation(rotateAnimation);
            } catch (Exception e) {
                // 如果动画加载失败，忽略异常
            }
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null) {
            ViewGroup rootView = findViewById(android.R.id.content);
            rootView.removeView(progressDialog);
            progressDialog = null;
        }
    }

    private void observeGenerationProgress() {
        viewModel.getGenerationProgress().observeForever(progress -> {
            if (progressDialog != null && progress != null) {
                // 更新进度条宽度
                int progressWidth = (int) (progress * 2.32f); // 232dp是进度条容器的宽度
                ViewGroup.LayoutParams params = progressBarInDialog.getLayoutParams();
                params.width = (int) (progressWidth * getResources().getDisplayMetrics().density);
                progressBarInDialog.setLayoutParams(params);

                // 更新百分比文本
                progressPercentage.setText((int) (progress) + "%");

                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) ll_progress_percentage.getLayoutParams();

                ZUtils.print("progress = "+progress);
                ZUtils.print("(progress/100f) = "+(progress/100f));
                float marginLeft = -19+(280-48)*(progress/100f);
                ZUtils.print("marginLeft = "+marginLeft);
                layoutParams.setMargins(ZDpUtils.dpToPx(this,marginLeft),0,0,0);

                // 更新状态文本
                if (progress < 0.3f) {
                    progressStatusText.setText("正在分析主题");
                } else if (progress < 0.6f) {
                    progressStatusText.setText("正在生成内容");
                } else if (progress < 0.9f) {
                    progressStatusText.setText("正在优化排版");
                } else {
                    progressStatusText.setText("即将完成");
                }
            }
        });
    }

    private static class TemplateAdapter extends RecyclerView.Adapter<TemplateAdapter.ViewHolder> {
        private List<PptTemplate> templates = new ArrayList<>();
        private String selectedTemplateId;
        private OnTemplateClickListener listener;
        
        interface OnTemplateClickListener {
            void onTemplateClick(PptTemplate template);
        }
        
        void setOnTemplateClickListener(OnTemplateClickListener listener) {
            this.listener = listener;
        }
        
        void setTemplates(List<PptTemplate> templates) {
            this.templates = templates;
            notifyDataSetChanged();
        }
        
        void setSelectedTemplateId(String id) {
            String oldId = selectedTemplateId;
            selectedTemplateId = id;
            
            int oldPos = findPositionById(oldId);
            int newPos = findPositionById(id);
            
            if (oldPos >= 0) notifyItemChanged(oldPos);
            if (newPos >= 0) notifyItemChanged(newPos);
        }
        
        private int findPositionById(String id) {
            if (id == null) return -1;
            for (int i = 0; i < templates.size(); i++) {
                if (id.equals(templates.get(i).getId())) {
                    return i;
                }
            }
            return -1;
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ppt_template, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PptTemplate template = templates.get(position);
            holder.bind(template, template.getId().equals(selectedTemplateId));
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTemplateClick(template);
                }
            });
        }
        
        @Override
        public int getItemCount() {
            return templates.size();
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView templateImage;
            View selectionBorder;
            ImageView checkMark;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                templateImage = itemView.findViewById(R.id.template_image);
                selectionBorder = itemView.findViewById(R.id.selection_border);
                checkMark = itemView.findViewById(R.id.check_mark);
            }

            void bind(PptTemplate template, boolean isSelected) {
                // 加载模板缩略图
                if (template.getThumbnailUrl() != null && !template.getThumbnailUrl().isEmpty()) {
                    Glide.with(templateImage.getContext())
                        .load(template.getThumbnailUrl())
                        .placeholder(R.drawable.template_placeholder)
                        .error(R.drawable.template_error)
                        .into(templateImage);
                } else {
                    templateImage.setImageResource(R.drawable.template_placeholder);
                }

                // 显示选中状态
                selectionBorder.setVisibility(isSelected ? View.VISIBLE : View.GONE);
                checkMark.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 确保状态栏样式在Resume时也正确
        setupImmersiveStatusBar();
    }

    /**
     * 设置沉浸式状态栏 - 参考会议页面样式
     */
    private void setupImmersiveStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(Color.parseColor("#FFFFFF"));

            // 延迟设置确保生效
            getWindow().getDecorView().postDelayed(() -> {
                getWindow().setStatusBarColor(Color.parseColor("#FFFFFF"));
            }, 100);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // 设置状态栏文字为深色
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }



    // 网格间距装饰器
    public static class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private int spanCount;
        private int spacing;
        private boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = dpToPx(spacing);
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(android.graphics.Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int column = position % spanCount; // 0 = 左列, 1 = 右列

            // 水平间距：左列右边距7.5dp，右列左边距7.5dp，这样中间总共15dp
            if (column == 0) {
                // 左列：无左边距，右边距7.5dp
                outRect.left = 0;
                outRect.right = spacing / 2;
            } else {
                // 右列：左边距7.5dp，无右边距
                outRect.left = spacing / 2;
                outRect.right = 0;
            }

            // 垂直间距：除第一行外，其他行顶部15dp间距
            if (position >= spanCount) {
                outRect.top = spacing;
            } else {
                outRect.top = 0;
            }
            outRect.bottom = 0;
        }

        private int dpToPx(int dp) {
            android.content.res.Resources r = android.content.res.Resources.getSystem();
            return Math.round(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
        }
    }

    // PptTemplate类已移至com.fxzs.lingxiagent.model.ppt.dto.PptTemplate

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ZUtils.stopService(this);
    }
}