package com.fxzs.lingxiagent.view.ppt;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Rect;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.ppt.dto.OutlineItem;
import com.fxzs.lingxiagent.util.PptLifecycleManager;
import com.fxzs.lingxiagent.util.PptNavigationHelper;
import com.fxzs.lingxiagent.util.PptStateManager;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.common.CommonDialog;
import com.fxzs.lingxiagent.view.common.ConfirmDialog;
import com.fxzs.lingxiagent.viewmodel.ppt.VMPptOutlineEdit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class PptOutlineEditActivity extends BaseActivity<VMPptOutlineEdit> {

    private ImageView backButton;
    private LinearLayout outlineLinearContainer;
    private LinearLayout changeOutlineButton;
    private LinearLayout selectTemplateButton;
    private LinearLayout stopButtonContainer;
    private LinearLayout thinkingProcessContainer;
    private TextView thinkingProcessText;
    private TextView thinkingStatusText;
    private ScrollView mainScrollView;

    // 计时相关
    private long thinkingStartTime;

    // 大纲管理相关
    private List<OutlineItem> currentOutlineItems = new ArrayList<>();
    private Map<String, View> outlineViewMap = new HashMap<>(); // 用于快速查找View

    // 自动滚动相关
    private boolean isUserScrolling = false; // 用户是否正在手动滚动
    private boolean allowAutoScroll = true; // 是否允许自动滚动

    // 刷新节流/去重，避免高频全量重绘导致ANR
    private static final long OUTLINE_UPDATE_MIN_INTERVAL_MS = 250;
    private long lastOutlineUpdateTimeMs = 0L;
    private String lastOutlineSignature = "";

    // 键盘相关
    private boolean isKeyboardVisible = false; // 键盘是否可见
    private String pptId;
    private String currentTopic;
    private boolean isGeneratingFromIntent;
    private PptStateManager stateManager;
    private PptLifecycleManager lifecycleManager;
    private View iv_arrow;
    private boolean isShowThinking = true;
    private TextView tv_title;

    private ConfirmDialog confirmExitDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化管理器
        stateManager = PptStateManager.getInstance(this);
        lifecycleManager = new PptLifecycleManager(this);

        // 从Intent或状态管理器获取数据
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            stateManager.restoreFromBundle(extras);
        }

        // 恢复Activity状态
        PptNavigationHelper.restoreActivityState(this, savedInstanceState);

        // 获取项目数据
        com.fxzs.lingxiagent.model.ppt.dto.PptProject currentProject = stateManager.getCurrentProject();
        if (currentProject != null) {
            pptId = currentProject.getId();
            currentTopic = currentProject.getTopic();
            isGeneratingFromIntent = currentProject.isGenerating();
        } else {
            // 兼容旧的Intent方式
            pptId = getIntent().getStringExtra(com.fxzs.lingxiagent.util.PptStateManager.EXTRA_PPT_ID);
            currentTopic = getIntent().getStringExtra(com.fxzs.lingxiagent.util.PptStateManager.EXTRA_TOPIC);
            isGeneratingFromIntent = getIntent().getBooleanExtra(com.fxzs.lingxiagent.util.PptStateManager.EXTRA_IS_GENERATING, false);
        }

        tv_title.setText(currentTopic);
        Timber.tag("PptOutlineEditActivity").d( "onCreate - pptId: " + pptId + ", currentTopic: " + currentTopic + ", isGenerating: " + isGeneratingFromIntent);

        // 测试数据 - 如果没有数据则创建一些测试数据
        if (!isGeneratingFromIntent && (pptId == null || pptId.isEmpty())) {
            createTestData();
        }

        // 如果是生成模式，启用自动滚动
        if (isGeneratingFromIntent) {
            allowAutoScroll = true;
            isUserScrolling = false;
            Timber.tag("PptOutlineEditActivity").d( "生成模式，启用自动滚动");
        }
        ZUtils.startService(this);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_ppt_outline_edit;
    }

    @Override
    protected Class<VMPptOutlineEdit> getViewModelClass() {
        return VMPptOutlineEdit.class;
    }

    @Override
    protected void initializeViews() {
        backButton = findViewById(R.id.back_button);
        outlineLinearContainer = findViewById(R.id.outline_linear_container);
        changeOutlineButton = findViewById(R.id.change_outline_button);
        selectTemplateButton = findViewById(R.id.select_template_button);
        stopButtonContainer = findViewById(R.id.stop_button_container);
        thinkingStatusText = findViewById(R.id.thinking_status_text);
        mainScrollView = findViewById(R.id.main_scroll_view);
        thinkingProcessContainer = findViewById(R.id.thinking_process_container);
        thinkingProcessText = findViewById(R.id.thinking_process_text);
        iv_arrow = findViewById(R.id.iv_arrow);
        tv_title = findViewById(R.id.tv_title);

        backButton.setOnClickListener(v -> {
            Timber.tag("PptOutlineEditActivity").d( "返回按钮被点击");
//            if (outlineLinearContainer != null && outlineLinearContainer.getChildCount() > 0 ){
//                //生成完成直接退出
//                finish();
//            }else{
                showConfirmExitDialog();
//            }
        });
        iv_arrow.setOnClickListener(v -> {
            isShowThinking = !isShowThinking;
            thinkingProcessText.setVisibility(isShowThinking?View.VISIBLE:View.GONE);
            if (isShowThinking) {
                ZUtils.setViewBg(PptOutlineEditActivity.this,iv_arrow, R.mipmap.home_up_arrow);
            } else {
                ZUtils.setViewBg(PptOutlineEditActivity.this,iv_arrow, R.mipmap.home_down_arrow);
            }
        });

        // 设置ScrollView滚动监听
        setupScrollListener();

        // 设置键盘监听
        setupKeyboardListener();
        changeOutlineButton.setOnClickListener(v -> showRegenerateConfirmDialog());
        selectTemplateButton.setOnClickListener(v -> navigateToTemplateSelection());
        stopButtonContainer.setOnClickListener(v -> {
            Timber.tag("PptOutlineEditActivity").d( "停止生成按钮被点击");
            stopGeneration();
        });

        // 初始化思考过程为展开状态
        thinkingProcessContainer.setVisibility(View.VISIBLE);

        // Set current topic if available
        if (currentTopic != null) {
            viewModel.setCurrentTopic(currentTopic);
        }
    }

    @Override
    protected void setupDataBinding() {
        viewModel.getIsGenerating().observe(this, isGenerating -> {
            if (isGenerating != null && isGenerating) {
                // 开始计时
                thinkingStartTime = System.currentTimeMillis();
                thinkingStatusText.setText("思考中...");
                stopButtonContainer.setVisibility(View.VISIBLE);
                // 生成过程中隐藏底部按钮
                findViewById(R.id.bottom_bar).setVisibility(View.GONE);
                Timber.tag("PptOutlineEditActivity").d( "开始生成，启动计时");
            } else {
                // 生成完成，显示最终计时
                updateThinkingStatusToCompleted();
                stopButtonContainer.setVisibility(View.GONE);
                // 生成完成后显示底部按钮
                findViewById(R.id.bottom_bar).setVisibility(View.VISIBLE);

                // 生成完成后重新更新大纲视图，确保"添加内容"模块正确显示
                List<OutlineItem> currentItems = viewModel.getOutlineItems().getValue();
                Timber.tag("PptOutlineEditActivity").d( "开始更新大纲视图，setupDataBinding 数据: ");
//                printData(currentItems);
                if (currentItems != null) {
                    Timber.tag("PptOutlineEditActivity").d( "开始更新大纲视图，setupDataBinding: ");
                    updateOutlineViews(currentItems);
                }

                Timber.tag("PptOutlineEditActivity").d( "生成完成");
            }
        });
    }

    @Override
    protected void setupObservers() {
        Timber.tag("PptOutlineEditActivity").d( "setupObservers called");

        // 观察大纲数据变化
        viewModel.getOutlineItems().observe(this, items -> {
            try {
                Timber.tag("PptOutlineEditActivity").d( "大纲数据更新，项目数: " + (items != null ? items.size() : 0));
//                printData(items);

                // 在主线程中安全更新LinearLayout
                runOnUiThread(() -> {
                    try {
//                        Timber.tag("PptOutlineEditActivity").d( "开始更新大纲视图，setupObservers: ");
                        updateOutlineViews(items);
//                        Timber.tag("PptOutlineEditActivity").d( "LinearLayout已更新大纲数据");
//                        printData(items);
                        // 如果是流式生成过程中，使用智能滚动
                        if (isGeneratingFromIntent) {
                            mainScrollView.postDelayed(() -> smartScrollToLatest(), 150);
                        }
                    } catch (Exception e) {
                        Timber.tag("PptOutlineEditActivity").e( "更新LinearLayout数据时出错"+ e);
                    }
                });
            } catch (Exception e) {
                Timber.tag("PptOutlineEditActivity").e( "处理大纲数据更新时出错"+ e);
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getSuccessMessage().observe(this, success -> {
//            if (success != null && !success.isEmpty()) {
//                Toast.makeText(this, success, Toast.LENGTH_SHORT).show();
//            }
        });

        // 观察思考过程（始终保持显示）
        viewModel.getThinkingProcess().observe(this, thinkingText -> {
            if (thinkingText != null && !thinkingText.isEmpty()) {
                thinkingProcessText.setText(thinkingText);

                // 自动滚动到底部
                thinkingProcessText.post(() -> {
                    if (mainScrollView != null) {
                        mainScrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });

                Timber.tag("PptOutlineEditActivity").d( "思考过程更新: " + thinkingText.substring(0, Math.min(50, thinkingText.length())) + "...");
            }
        });

        // 观察流式内容 - 现在直接更新思考过程文本
        viewModel.getStreamingContent().observe(this, streamingText -> {
            if (streamingText != null && !streamingText.isEmpty()) {
                thinkingProcessText.setText(streamingText);
            }
        });

        // 在观察者设置完成后，检查是否需要启动流式生成
        checkAndStartStreamGeneration();
    }

    private void printData(List<OutlineItem> items) {
        if(items != null && items.size() >0){
            // 检查数据完整性：验证每个item是否有唯一的引用
            java.util.Set<String> titles = new java.util.HashSet<>();
            boolean hasDuplicateTitles = false;

            for (int i = 0; i < items.size(); i++) {
                OutlineItem item = items.get(i);
                if (item == null) {
                    Timber.tag("PptOutlineEditActivity").e( "项目[" + i + "]为null");
                    continue;
                }

                // 在循环中先获取item和subItems的引用，避免引用问题
                String title = item.getTitle();
                List<OutlineItem> subItems = item.getSubItems();

                // 检查item对象的hashCode，验证是否为不同对象
                int itemHashCode = System.identityHashCode(item);

                // 检查是否有重复的标题
                if (title != null && !title.trim().isEmpty()) {
                    if (titles.contains(title)) {
                        Timber.tag("PptOutlineEditActivity").w( "警告：发现重复的主标题: " + title + "，位置: " + i);
                        hasDuplicateTitles = true;
                    }
                    titles.add(title);
                }

                // 构建subItems的详细信息字符串
                String subItemsInfo = "null";
                if (subItems != null) {
                    if (subItems.isEmpty()) {
                        subItemsInfo = "[]";
                    } else {
                        StringBuilder sb = new StringBuilder("[");
                        for (int j = 0; j < subItems.size(); j++) {
                            if (j > 0) sb.append(", ");
                            OutlineItem subItem = subItems.get(j);
                            if (subItem != null) {
                                sb.append(subItem.getTitle());
                            } else {
                                sb.append("null");
                            }
                        }
                        sb.append("]");
                        subItemsInfo = sb.toString();
                    }
                }

                Timber.tag("PptOutlineEditActivity").d( "大纲数据更新，项目[" + i + "] === : " + title + ". subItems: " + subItemsInfo + " (hashCode: " + itemHashCode + ")");
            }

            // 如果发现所有标题都相同，记录警告
            if (hasDuplicateTitles && titles.size() == 1 && items.size() > 1) {
                Timber.tag("PptOutlineEditActivity").e( "严重警告：所有主标题都相同！项目数: " + items.size() + ", 唯一标题: " + (titles.isEmpty() ? "无" : titles.iterator().next()));
            }
        }
    }

    /**
     * 检查并启动流式生成
     */
    private void checkAndStartStreamGeneration() {
        Timber.tag("PptOutlineEditActivity").d( "checkAndStartStreamGeneration called - isGenerating: " + isGeneratingFromIntent + ", currentTopic: " + currentTopic);
        Timber.tag("PptOutlineEditActivity").d( "重新检查Intent: topic=" + getIntent().getStringExtra(com.fxzs.lingxiagent.util.PptStateManager.EXTRA_TOPIC) + ", isGenerating=" + getIntent().getBooleanExtra(com.fxzs.lingxiagent.util.PptStateManager.EXTRA_IS_GENERATING, false));

        // 1. 先检查 ViewModel 当前是否已经在生成或已经有数据
        //    若已经有状态，说明可能是旋转屏幕等配置变更导致的重建，不应再次触发生成/加载
        Boolean vmIsGenerating = viewModel.getIsGenerating().getValue();
        List<OutlineItem> vmItems = viewModel.getOutlineItems().getValue();
        if (Boolean.TRUE.equals(vmIsGenerating)) {
            Timber.tag("PptOutlineEditActivity").d( "ViewModel 已在生成中，避免因屏幕旋转重复触发生成");
            return;
        }
        if (vmItems != null && !vmItems.isEmpty()) {
            Timber.tag("PptOutlineEditActivity").d( "ViewModel 已有大纲数据（可能是生成中或已生成），避免重复加载/生成");
            return;
        }

        // 直接从Intent重新获取值，避免字段被重置的问题
        String intentTopic = getIntent().getStringExtra(com.fxzs.lingxiagent.util.PptStateManager.EXTRA_TOPIC);
        boolean intentIsGenerating = getIntent().getBooleanExtra(com.fxzs.lingxiagent.util.PptStateManager.EXTRA_IS_GENERATING, false);
        String intentPptId = getIntent().getStringExtra(com.fxzs.lingxiagent.util.PptStateManager.EXTRA_PPT_ID);

        Timber.tag("PptOutlineEditActivity").d( "从Intent重新获取: topic=" + intentTopic + ", isGenerating=" + intentIsGenerating + ", pptId=" + intentPptId);

        if (intentIsGenerating && intentTopic != null) {
            Timber.tag("PptOutlineEditActivity").d( "观察者设置完成，开始流式生成大纲，主题: " + intentTopic);
            viewModel.startReceivingStreamData(intentTopic, this);
        } else if (intentPptId != null) {
            Timber.tag("PptOutlineEditActivity").d( "观察者设置完成，加载已有大纲数据，pptId: " + intentPptId);
            viewModel.loadOutline(intentPptId);
        } else {
            Timber.tag("PptOutlineEditActivity").d( "没有pptId和topic，isGenerating: " + intentIsGenerating + ", currentTopic: " + intentTopic);
        }
    }

    /**
     * 更新大纲视图 - 替代RecyclerView的数据绑定
     */
    private void updateOutlineViews(List<OutlineItem> inItems) {
        try {
            List<OutlineItem> items = new ArrayList<>();
            if (inItems != null) {
                items.addAll(inItems);
            }

            // 高频流式更新时做节流 + 去重，避免主线程反复全量重建
            long now = System.currentTimeMillis();
            String newSignature = buildOutlineSignature(items);
            boolean isSameData = newSignature.equals(lastOutlineSignature);
            boolean isTooFrequent = (now - lastOutlineUpdateTimeMs) < OUTLINE_UPDATE_MIN_INTERVAL_MS;
            if (isSameData || isTooFrequent) {
                return;
            }
            lastOutlineSignature = newSignature;
            lastOutlineUpdateTimeMs = now;

            // 清空现有视图
            int oldChildCount = outlineLinearContainer.getChildCount();
            outlineLinearContainer.removeAllViews();
            Timber.tag("PptOutlineEditActivity").d( "清空视图，旧视图数量: " + oldChildCount + ", 新数据数量: " + items.size());
            outlineViewMap.clear();
            currentOutlineItems.clear();
            currentOutlineItems.addAll(items);
            Timber.tag("PptOutlineEditActivity").d( "开始更新大纲视图，currentOutlineItems 项目数: " + currentOutlineItems.size());

            // 为每个大纲项创建视图
            for (int i = 0; i < items.size(); i++) {
                OutlineItem item = items.get(i);
                if (item != null) {
                    // 创建 final 副本变量，供 lambda 表达式使用
                    final int finalPosition = i;
                    final OutlineItem finalItem = item;
                    final String finalItemTitle = item.getTitle();
                    
                    String itemTitle = item.getTitle();
                    int itemHashCode = System.identityHashCode(item);
                    List<OutlineItem> itemSubItems = item.getSubItems();
                    int subItemsCount = itemSubItems != null ? itemSubItems.size() : 0;
//                    Timber.tag("PptOutlineEditActivity").d("updateOutlineViews: 创建主项[" + i + "], title=" + itemTitle + ", hashCode=" + itemHashCode + ", subItemsCount=" + subItemsCount);
                    
                    View itemView = createOutlineItemView(item, i);
                    if (itemView != null) {
                        outlineLinearContainer.addView(itemView);
                        outlineViewMap.put(item.getId(), itemView);
//                        Timber.tag("PptOutlineEditActivity").d("updateOutlineViews: 主项[" + i + "]视图已添加到容器，当前容器子视图数: " + outlineLinearContainer.getChildCount());

                        if(isTablet()){
                            // 验证视图中的文本是否正确设置
                            itemView.post(() -> {
                                try {
                                    // itemView 是一个 LinearLayout 容器，第一个子视图是 titleArea
                                    if (itemView instanceof LinearLayout) {
                                        LinearLayout container = (LinearLayout) itemView;
                                        if (container.getChildCount() > 0) {
                                            View titleArea = container.getChildAt(0);
                                            if (titleArea != null) {
                                                TextView titleTextView = titleArea.findViewById(R.id.tv_name);
                                                EditText ed_name = titleArea.findViewById(R.id.ed_name);
                                                if (titleTextView != null) {
                                                    String displayedText = titleTextView.getText().toString();
                                                    String expectedText = finalItemTitle;
                                                    String expectedDisplayText = (expectedText != null && !expectedText.trim().isEmpty()) ? expectedText : "请输入标题";
                                                    if (!displayedText.equals(expectedDisplayText)) {
//                                                        Timber.tag("PptOutlineEditActivity").w("updateOutlineViews: 主项[" + finalPosition + "]文本不匹配！期望: " + expectedDisplayText + ", 实际: " + displayedText);
                                                        // 强制重新设置文本
                                                        titleTextView.setText(expectedDisplayText);
                                                        if ("请输入标题".equals(expectedDisplayText)) {
                                                            titleTextView.setTextColor(getColor(R.color.figma_text_secondary));
                                                        } else {
                                                            titleTextView.setTextColor(getColor(R.color.figma_text_primary));
                                                        }
                                                        titleTextView.invalidate();
                                                        titleTextView.requestLayout();
                                                    } else {
//                                                        Timber.tag("PptOutlineEditActivity").d("updateOutlineViews: 主项[" + finalPosition + "]文本验证通过: " + displayedText);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    Timber.tag("PptOutlineEditActivity").e("updateOutlineViews: 验证主项[" + finalPosition + "]文本时出错: " + e.getMessage());
                                }
                            });
                        }
                    } else {
                        Timber.tag("PptOutlineEditActivity").e("updateOutlineViews: itemView[" + i + "]为null，无法添加");
                    }
                } else {
                    Timber.tag("PptOutlineEditActivity").e("updateOutlineViews: item[" + i + "]为null，跳过");
                }
            }

            // 只在生成完成后显示"添加内容"模块，避免生成过程中的闪烁
            Boolean isGenerating = viewModel.getIsGenerating().getValue();
            if (!Boolean.TRUE.equals(isGenerating)) {
                // 生成完成后才显示添加内容模块
                View addContainer = LayoutInflater.from(this).inflate(R.layout.item_ppt_main_add,null);
                addContainer.setOnClickListener(v -> addNewOutlineItem(items.size()-1));
                outlineLinearContainer.addView(addContainer);
                Timber.tag("PptOutlineEditActivity").d( "生成完成，显示添加内容模块");
            } else {
                Timber.tag("PptOutlineEditActivity").d( "生成中，隐藏添加内容模块");
            }

            int finalChildCount = outlineLinearContainer.getChildCount();
            Timber.tag("PptOutlineEditActivity").d( "大纲视图更新完成，最终容器子视图数: " + finalChildCount + ", 数据项数: " + items.size());
            
            // 验证视图是否正确添加
            if (finalChildCount == 0 && items.size() > 0) {
                Timber.tag("PptOutlineEditActivity").e( "警告：数据有 " + items.size() + " 项，但容器子视图数为 0！");
            }

            // 智能滚动到最新内容（类似思考过程）
            smartScrollToLatest();
        } catch (Exception e) {
            Timber.tag("PptOutlineEditActivity").e( "更新大纲视图时出错"+ e);
            Toast.makeText(this, "更新大纲视图失败", Toast.LENGTH_SHORT).show();
        }
    }

    private String buildOutlineSignature(List<OutlineItem> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (OutlineItem item : items) {
            if (item == null) {
                sb.append("|null");
                continue;
            }
            sb.append('|').append(item.getId() == null ? "" : item.getId())
                    .append(':').append(item.getTitle() == null ? "" : item.getTitle());
            List<OutlineItem> subs = item.getSubItems();
            if (subs != null) {
                for (OutlineItem sub : subs) {
                    if (sub == null) {
                        sb.append("#null");
                    } else {
                        sb.append('#').append(sub.getId() == null ? "" : sub.getId())
                                .append(':').append(sub.getTitle() == null ? "" : sub.getTitle());
                    }
                }
            }
        }
        return sb.toString();
    }

    /**
     * 创建单个大纲项的视图
     */
    private View createOutlineItemView(OutlineItem item, int position) {
        // 创建主容器
        LinearLayout containerLayout = new LinearLayout(this);
        containerLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        containerParams.setMargins(0, 0, 0, dpToPx(20));
        containerLayout.setLayoutParams(containerParams);
        containerLayout.setPadding(0, 0, 0, 0); // 大纲容器padding为0
        containerLayout.setBackgroundColor(Color.TRANSPARENT);

        // 创建主标题区域
        View titleArea = createTitleArea(item, position);
        containerLayout.addView(titleArea);

        // 创建子项目容器
        LinearLayout subItemsContainer = null;
        if (item.getSubItems() != null && !item.getSubItems().isEmpty()) {
            subItemsContainer = createSubItemsContainer(item.getSubItems(), position);
            containerLayout.addView(subItemsContainer);
        }

        // 主标题左侧三角形：展开/收起子项
        ImageView expandToggle = titleArea.findViewById(R.id.iv_expand_toggle);
        if (expandToggle != null) {
            if (subItemsContainer == null) {
                expandToggle.setVisibility(View.INVISIBLE);
            } else {
                expandToggle.setVisibility(View.VISIBLE);
                expandToggle.setRotation(90f); // 默认展开

                // 放大点击区域，避免小图标难点
                titleArea.post(() -> {
                    Rect rect = new Rect();
                    expandToggle.getHitRect(rect);
                    int extraPadding = dpToPx(8);
                    rect.left -= extraPadding;
                    rect.top -= extraPadding;
                    rect.right += extraPadding;
                    rect.bottom += extraPadding;
                    titleArea.setTouchDelegate(new TouchDelegate(rect, expandToggle));
                });

                LinearLayout finalSubItemsContainer = subItemsContainer;
                expandToggle.setOnClickListener(v -> {
                    boolean isExpanded = finalSubItemsContainer.getVisibility() == View.VISIBLE;
                    finalSubItemsContainer.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
                    expandToggle.animate().rotation(isExpanded ? 0f : 90f).setDuration(160).start();
                });
            }
        }

        return containerLayout;
    }

    /**
     * 创建主标题区域
     */
    private View createTitleArea(OutlineItem item, int position) {
//        RelativeLayout titleArea = new RelativeLayout(this);
//        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
//            LinearLayout.LayoutParams.MATCH_PARENT,
//            dpToPx(22) // 设置固定高度为22dp
//        );
//        titleParams.setMargins(dpToPx(16), 0, dpToPx(16), dpToPx(4)); // 距离容器边缘16dp
//        titleArea.setLayoutParams(titleParams);
//        titleArea.setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2)); // 添加2dp内边距让边框包裹输入框
//
//        // 设置背景为透明，选中时显示灰色边框
//        titleArea.setBackgroundColor(android.graphics.Color.TRANSPARENT);
//        titleArea.setClickable(true);
//        titleArea.setFocusable(true);
//
//        // 创建三角形图标
//        View triangleIcon = new View(this);
//        RelativeLayout.LayoutParams triangleParams = new RelativeLayout.LayoutParams(dpToPx(6), dpToPx(8));
//        triangleParams.addRule(RelativeLayout.ALIGN_PARENT_START);
//        triangleParams.addRule(RelativeLayout.CENTER_VERTICAL);
//        triangleParams.setMargins(dpToPx(2), 0, dpToPx(8), 0); // 左边距2dp适应padding，右边距8dp
//        triangleIcon.setLayoutParams(triangleParams);
//        triangleIcon.setBackgroundColor(android.graphics.Color.parseColor("#666666"));
//        triangleIcon.setId(View.generateViewId());
//        titleArea.addView(triangleIcon);
//
//        // 创建标题TextView/EditText容器
//        RelativeLayout titleContainer = new RelativeLayout(this);
//        RelativeLayout.LayoutParams titleContainerParams = new RelativeLayout.LayoutParams(
//            RelativeLayout.LayoutParams.MATCH_PARENT,
//            RelativeLayout.LayoutParams.MATCH_PARENT
//        );
//        titleContainerParams.addRule(RelativeLayout.END_OF, triangleIcon.getId());
//        titleContainerParams.addRule(RelativeLayout.CENTER_VERTICAL);
//        titleContainerParams.setMargins(0, 0, dpToPx(62), 0); // 为按钮留出62dp空间（包含padding）
//        titleContainer.setLayoutParams(titleContainerParams);
//        titleContainer.setId(View.generateViewId());
//        titleArea.addView(titleContainer);
//
//        // 创建标题TextView（显示模式）
//        TextView titleTextView = new TextView(this);
//        RelativeLayout.LayoutParams titleTextParams = new RelativeLayout.LayoutParams(
//            RelativeLayout.LayoutParams.MATCH_PARENT,
//            RelativeLayout.LayoutParams.MATCH_PARENT
//        );
//        titleTextView.setLayoutParams(titleTextParams);
//        String titleText = item.getTitle() != null ? item.getTitle() : "标题" + position;
//        titleTextView.setText(titleText);
//        titleTextView.setTextSize(16); // 字体大小16sp
//        titleTextView.setTextColor(android.graphics.Color.parseColor("#1E1E1E"));
//        android.util.Log.d("PptOutlineEdit", "设置标题文本: " + titleText);
//        titleTextView.setPadding(dpToPx(2), 0, dpToPx(2), 0); // 减少padding适应边框
//        titleTextView.setGravity(android.view.Gravity.CENTER_VERTICAL); // 垂直居中
//        titleTextView.setLineSpacing(0, 1.0f); // 行高等于块高度
//        titleTextView.setId(View.generateViewId());
//        titleContainer.addView(titleTextView);
//
//        // 创建标题EditText（编辑模式）
//        EditText titleEditText = new EditText(this);
//        RelativeLayout.LayoutParams titleEditParams = new RelativeLayout.LayoutParams(
//            RelativeLayout.LayoutParams.MATCH_PARENT,
//            RelativeLayout.LayoutParams.MATCH_PARENT
//        );
//        titleEditText.setLayoutParams(titleEditParams);
//        titleEditText.setText(item.getTitle() != null ? item.getTitle() : "");
//        titleEditText.setTextSize(16); // 字体大小16sp
//        titleEditText.setTextColor(android.graphics.Color.parseColor("#1E1E1E"));
//        titleEditText.setBackgroundColor(android.graphics.Color.WHITE);
//        titleEditText.setPadding(dpToPx(2), 0, dpToPx(2), 0); // 减少padding适应边框
//        titleEditText.setGravity(android.view.Gravity.CENTER_VERTICAL); // 垂直居中
//        titleEditText.setLineSpacing(0, 1.0f); // 行高等于块高度
//        titleEditText.setVisibility(View.GONE); // 默认隐藏
//        titleEditText.setId(View.generateViewId());
//        titleContainer.addView(titleEditText);
//
//        // 创建按钮容器
//        LinearLayout buttonContainer = createButtonContainer(item, position, titleTextView, titleEditText);
//        RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(
//            RelativeLayout.LayoutParams.WRAP_CONTENT,
//            RelativeLayout.LayoutParams.WRAP_CONTENT
//        );
//        buttonParams.addRule(RelativeLayout.ALIGN_PARENT_END);
//        buttonParams.addRule(RelativeLayout.CENTER_VERTICAL);
//        buttonContainer.setLayoutParams(buttonParams);
//        titleArea.addView(buttonContainer);


        View titleArea = LayoutInflater.from(this).inflate(R.layout.item_ppt_main_title,null);
        TextView titleTextView = titleArea.findViewById(R.id.tv_name);
        EditText titleEditText = (EditText)titleArea.findViewById(R.id.ed_name);
        // 创建按钮容器
        LinearLayout buttonContainer = titleArea.findViewById(R.id.ll_actions);
        // 设置按钮点击事件
       View addButton = buttonContainer.findViewById(R.id.iv_main_add);
       View deleteButton = buttonContainer.findViewById(R.id.iv_main_delete);
//        addButton.setOnClickListener(v -> addNewOutlineItem(position));

            int subPosition = item.getSubItems() != null?item.getSubItems().size()-1:-1;
            addButton.setOnClickListener(v -> addNewSubItem(position, subPosition));

        deleteButton.setOnClickListener(v -> deleteOutlineItem(position));

        String itemTitle = item.getTitle();
        int itemHashCode = System.identityHashCode(item);

//        String titleText = item.getTitle() != null ? item.getTitle() : "标题" + position;
        String titleText = itemTitle != null ? itemTitle : "请输入标题";
        
        if (titleTextView != null) {
            titleTextView.setText(titleText);
            if("请输入标题".equals(titleText)){
                titleTextView.setTextColor(getColor(R.color.figma_text_secondary));
            }else {
                titleTextView.setTextColor(getColor(R.color.figma_text_primary));
            }
            // 强制刷新视图，确保文本正确显示
            titleTextView.invalidate();
            titleTextView.requestLayout();
        } else {
        }

        if (titleEditText != null) {
            // 先移除所有TextWatcher，避免在设置文本时触发
            titleEditText.clearFocus();
            // 移除之前的TextWatcher（如果有）
            titleEditText.removeTextChangedListener(null);
            // 设置文本
            titleEditText.setText(itemTitle != null ? itemTitle : "");
            // 强制刷新视图
            titleEditText.invalidate();
            titleEditText.requestLayout();
        } else {
        }
        // 延迟设置点击事件，确保文本设置完成后再添加TextWatcher
        titleArea.post(() -> {
            setupTitleClickEvents(titleArea, titleTextView, titleEditText, buttonContainer, item, position);
        });

        return titleArea;
    }

    /**
     * 创建按钮容器
     */
    private LinearLayout createButtonContainer(OutlineItem item, int position, TextView titleTextView, EditText titleEditText) {
        LinearLayout buttonContainer = new LinearLayout(this);
        buttonContainer.setOrientation(LinearLayout.HORIZONTAL);
        buttonContainer.setGravity(Gravity.CENTER_VERTICAL);
        buttonContainer.setVisibility(View.GONE); // 默认隐藏

        // 添加按钮
        ImageButton addButton = new ImageButton(this);
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(dpToPx(32), dpToPx(32));
        addParams.setMargins(dpToPx(4), 0, dpToPx(4), 0);
        addButton.setLayoutParams(addParams);
        addButton.setBackgroundColor(Color.TRANSPARENT);
        addButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        addButton.setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));
        // 这里应该设置添加图标，暂时用文字代替
        // addButton.setImageResource(R.drawable.ic_add);
        buttonContainer.addView(addButton);

        // 删除按钮
        ImageButton deleteButton = new ImageButton(this);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dpToPx(32), dpToPx(32));
        deleteParams.setMargins(dpToPx(4), 0, dpToPx(4), 0);
        deleteButton.setLayoutParams(deleteParams);
        deleteButton.setBackgroundColor(Color.TRANSPARENT);
        deleteButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        deleteButton.setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));
        // 这里应该设置删除图标，暂时用文字代替
        // deleteButton.setImageResource(R.drawable.ic_delete);
        buttonContainer.addView(deleteButton);

        // 设置按钮点击事件
        addButton.setOnClickListener(v -> addNewOutlineItem(position));
        deleteButton.setOnClickListener(v -> deleteOutlineItem(position));

        return buttonContainer;
    }

    /**
     * 设置标题点击事件
     */
    private void setupTitleClickEvents(View titleArea, TextView titleTextView, EditText titleEditText,
                                     LinearLayout buttonContainer, OutlineItem item, int position) {
        // 点击标题区域进入编辑模式
        titleArea.setOnClickListener(v -> {
            // 确保EditText的文本与item的文本一致
            String currentTitle = item.getTitle();
            if (currentTitle != null && !currentTitle.trim().isEmpty()) {
                titleEditText.setText(currentTitle);
            } else {
                titleEditText.setText("");
            }
            
            // 切换到编辑模式
            titleTextView.setVisibility(View.GONE);
            titleEditText.setVisibility(View.VISIBLE);
            buttonContainer.setVisibility(View.VISIBLE);

            // 设置背景为选中状态（灰色边框）
            titleArea.setBackgroundResource(R.drawable.bg_outline_item_selected_gray);

            // 请求焦点并显示键盘
            titleEditText.requestFocus();
            titleEditText.setSelection(titleEditText.getText().length());

            // 确保EditText可见，延迟滚动到EditText位置
            titleEditText.post(() -> {
                if (isKeyboardVisible) {
                    scrollToEditText(titleEditText);
                }
            });
        });

        // EditText失去焦点时退出编辑模式
        titleEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                // 从EditText读取最终值并更新
                String finalTitle = titleEditText.getText().toString();
                
                // 验证内容有效性
                if (isValidContent(finalTitle)) {
                    item.setTitle(finalTitle);
                    titleTextView.setText(finalTitle);
                    titleTextView.setTextColor(getColor(R.color.figma_text_primary));
                } else {
                    // 如果内容无效，恢复原始值
                    String originalTitle = item.getTitle();
                    if (originalTitle != null && !originalTitle.trim().isEmpty()) {
                        titleEditText.setText(originalTitle);
                        titleTextView.setText(originalTitle);
                        titleTextView.setTextColor(getColor(R.color.figma_text_primary));
                    } else {
                        titleTextView.setText("请输入标题");
                        titleTextView.setTextColor(getColor(R.color.figma_text_secondary));
                    }
                }

                // 切换回显示模式
                titleEditText.setVisibility(View.GONE);
                titleTextView.setVisibility(View.VISIBLE);
                buttonContainer.setVisibility(View.GONE);

                // 恢复背景为透明
                titleArea.setBackgroundColor(Color.TRANSPARENT);

                // 通知ViewModel更新数据
                updateOutlineItemInViewModel(item, position);
            }
        });

        // 处理EditText的回车键
        titleEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                titleEditText.clearFocus();
                return true;
            }
            return false;
        });
        titleEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                // 保存编辑内容
                String newTitle = titleEditText.getText().toString();

                // 验证内容有效性
                if (isValidContent(newTitle)) {
                    item.setTitle(newTitle);
                    titleTextView.setText(newTitle);
                    titleTextView.setTextColor(getColor(R.color.figma_text_primary));
                } else {
                    // 内容无效时显示提示文本和Toast
                    if (!newTitle.isEmpty()) { // 只有在用户输入了内容但无效时才提示
                        Toast.makeText(PptOutlineEditActivity.this, "请输入有效内容", Toast.LENGTH_SHORT).show();
                    }
                    item.setTitle(""); // 清空无效内容
                    titleTextView.setText("请输入标题");
                    titleTextView.setTextColor(getColor(R.color.figma_text_secondary));
                }
            }
        });
    }

    /**
     * 验证内容是否有效（不为空且不只包含空格和换行符）
     */
    private boolean isValidContent(String content) {
        if (content == null) {
            return false;
        }
        // 去除所有空白字符（包括空格、制表符、换行符等）
        String trimmedContent = content.replaceAll("\\s+", "");
        return !trimmedContent.isEmpty();
    }

    /**
     * 添加新的大纲项
     */
    private void addNewOutlineItem(int position) {
        // 检查是否已经存在空的标题
        for (OutlineItem item : currentOutlineItems) {
            if (!isValidContent(item.getTitle())) {
                // 已经存在无效标题，显示提示并返回
                Toast.makeText(this, "请输入有效内容", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        OutlineItem newItem = new OutlineItem(null, "", 1);
        currentOutlineItems.add(position + 1, newItem);

        // 重新创建视图
        updateOutlineViews(currentOutlineItems);

        // 通知ViewModel
//        viewModel.insertOutlineItem(position + 1, newItem);
    }

    /**
     * 删除大纲项
     */
    private void deleteOutlineItem(int position) {
        if (position >= 0 && position < currentOutlineItems.size()) {
            currentOutlineItems.remove(position);

            // 重新创建视图
            updateOutlineViews(currentOutlineItems);

            // 通知ViewModel
            viewModel.removeOutlineItem(position);
        }
    }

    /**
     * 更新ViewModel中的大纲项
     */
    private void updateOutlineItemInViewModel(OutlineItem item, int position) {
        // 这里可以添加更新ViewModel的逻辑
        // 暂时只是更新本地数据
        if (position >= 0 && position < currentOutlineItems.size()) {
            currentOutlineItems.set(position, item);
        }
    }

    /**
     * 创建子项目容器
     */
    private LinearLayout createSubItemsContainer(List<OutlineItem> subItems, int parentPosition) {
        LinearLayout subContainer = new LinearLayout(this);
        subContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subParams.setMargins(0, 0, 0, 0); // 移除边距，由子项目自己控制
        subContainer.setLayoutParams(subParams);

        
        for (int i = 0; i < subItems.size(); i++) {
            // 在循环开始时立即获取subItem，避免引用问题
            final OutlineItem subItem = subItems.get(i);  // 使用final确保变量捕获正确
            if (subItem == null) {
                continue;
            }
            
            // 立即获取并保存关键属性，避免后续引用问题
            String subItemTitle = subItem.getTitle();
            int subItemHashCode = System.identityHashCode(subItem);
            String subItemId = subItem.getId();
            final int finalSubPosition = i;  // 使用final确保位置变量捕获正确
            
            
            // 验证subItem是否真的是不同的对象
            if (i > 0) {
                OutlineItem prevSubItem = subItems.get(i - 1);
                if (prevSubItem != null && prevSubItem.getId().equals(subItemId)) {
                }
            }
            
            View subItemView = createSubItemView(subItem, parentPosition, finalSubPosition);
            if (subItemView != null) {
                subContainer.addView(subItemView);
            } else {
            }
        }

        return subContainer;
    }

    /**
     * 创建子项目视图
     */
    private View createSubItemView(OutlineItem subItem, int parentPosition, int subPosition) {
//        RelativeLayout subItemContainer = new RelativeLayout(this);
//        LinearLayout.LayoutParams subItemParams = new LinearLayout.LayoutParams(
//            LinearLayout.LayoutParams.MATCH_PARENT,
//            dpToPx(28) // 设置固定高度为28dp
//        );
//        subItemParams.setMargins(dpToPx(16), 0, dpToPx(16), dpToPx(2)); // 距离容器边缘16dp
//        subItemContainer.setLayoutParams(subItemParams);
//        subItemContainer.setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2)); // 添加2dp内边距让边框包裹输入框
//        // 设置背景为透明，选中时显示灰色边框
//        subItemContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT);
//        subItemContainer.setClickable(true);
//        subItemContainer.setFocusable(true);
//
//        // 创建圆点图标
//        View dotIcon = new View(this);
//        RelativeLayout.LayoutParams dotParams = new RelativeLayout.LayoutParams(dpToPx(5), dpToPx(5));
//        dotParams.addRule(RelativeLayout.ALIGN_PARENT_START);
//        dotParams.addRule(RelativeLayout.CENTER_VERTICAL);
//        dotParams.setMargins(dpToPx(2), 0, dpToPx(8), 0); // 左边距2dp适应padding，右边距8dp
//        dotIcon.setLayoutParams(dotParams);
//        dotIcon.setBackground(getResources().getDrawable(R.drawable.bg_home_indicator));
////        dotIcon.setBackgroundColor(android.graphics.Color.parseColor("#1E1E1E"));
//        dotIcon.setId(View.generateViewId());
//        subItemContainer.addView(dotIcon);
//
//        // 创建子标题容器
//        RelativeLayout subTitleContainer = new RelativeLayout(this);
//        RelativeLayout.LayoutParams subTitleContainerParams = new RelativeLayout.LayoutParams(
//            RelativeLayout.LayoutParams.MATCH_PARENT,
//            RelativeLayout.LayoutParams.MATCH_PARENT
//        );
//        subTitleContainerParams.addRule(RelativeLayout.END_OF, dotIcon.getId());
//        subTitleContainerParams.addRule(RelativeLayout.CENTER_VERTICAL);
//        subTitleContainerParams.setMargins(0, 0, dpToPx(62), 0); // 为按钮留出62dp空间（包含padding）
//        subTitleContainer.setLayoutParams(subTitleContainerParams);
//        subTitleContainer.setId(View.generateViewId());
//        subItemContainer.addView(subTitleContainer);
//
//        // 创建子标题TextView（显示模式）
//        TextView subTitleTextView = new TextView(this);
//        RelativeLayout.LayoutParams subTitleTextParams = new RelativeLayout.LayoutParams(
//            RelativeLayout.LayoutParams.MATCH_PARENT,
//            RelativeLayout.LayoutParams.MATCH_PARENT
//        );
//        subTitleTextView.setLayoutParams(subTitleTextParams);
//        String subTitleText = subItem.getTitle() != null ? subItem.getTitle() : "子标题" + subPosition;
//        subTitleTextView.setText(subTitleText);
//        subTitleTextView.setTextSize(16); // 字体大小16sp（与标题一致）
//        subTitleTextView.setTextColor(android.graphics.Color.parseColor("#333333"));
//        android.util.Log.d("PptOutlineEdit", "设置子标题文本: " + subTitleText);
//        subTitleTextView.setPadding(dpToPx(2), 0, dpToPx(2), 0); // 减少padding适应边框
//        subTitleTextView.setGravity(android.view.Gravity.CENTER_VERTICAL); // 垂直居中
//        subTitleTextView.setLineSpacing(0, 1.0f); // 行高等于块高度
//        subTitleTextView.setId(View.generateViewId());
//        subTitleContainer.addView(subTitleTextView);
//
//        // 创建子标题EditText（编辑模式）
//        EditText subTitleEditText = new EditText(this);
//        RelativeLayout.LayoutParams subTitleEditParams = new RelativeLayout.LayoutParams(
//            RelativeLayout.LayoutParams.WRAP_CONTENT,
//            RelativeLayout.LayoutParams.MATCH_PARENT
//        );
//        subTitleEditText.setLayoutParams(subTitleEditParams);
//        subTitleEditText.setText(subItem.getTitle() != null ? subItem.getTitle() : "");
//        subTitleEditText.setTextSize(16); // 字体大小16sp（与标题一致）
//        subTitleEditText.setTextColor(android.graphics.Color.parseColor("#333333"));
//        subTitleEditText.setBackgroundColor(android.graphics.Color.WHITE);
//        subTitleEditText.setPadding(dpToPx(2), 0, dpToPx(2), 0); // 减少padding适应边框
//        subTitleEditText.setGravity(android.view.Gravity.CENTER_VERTICAL); // 垂直居中
//        subTitleEditText.setLineSpacing(0, 1.0f); // 行高等于块高度
//        subTitleEditText.setVisibility(View.GONE); // 默认隐藏
//        subTitleEditText.setId(View.generateViewId());
//        subTitleContainer.addView(subTitleEditText);


//        // 创建子项目按钮容器
//        LinearLayout subButtonContainer = createSubButtonContainer(subItem, parentPosition, subPosition, subTitleTextView, subTitleEditText);
//        RelativeLayout.LayoutParams subButtonParams = new RelativeLayout.LayoutParams(
//            RelativeLayout.LayoutParams.WRAP_CONTENT,
//            RelativeLayout.LayoutParams.WRAP_CONTENT
//        );
//        subButtonParams.addRule(RelativeLayout.ALIGN_PARENT_END);
//        subButtonParams.addRule(RelativeLayout.CENTER_VERTICAL);
//        subButtonContainer.setLayoutParams(subButtonParams);
//        subItemContainer.addView(subButtonContainer);

       View subItemContainer = LayoutInflater.from(this).inflate(R.layout.item_ppt_sub,null);
        TextView subTitleTextView = subItemContainer.findViewById(R.id.tv_name);
        EditText subTitleEditText = (EditText)subItemContainer.findViewById(R.id.ed_name);
        LinearLayout subButtonContainer = subItemContainer.findViewById(R.id.ll_actions);

        View addSubButton = subButtonContainer.findViewById(R.id.iv_sub_add);
        View deleteSubButton = subButtonContainer.findViewById(R.id.iv_sub_delete);
//        addSubButton.setOnClickListener(v -> addNewSubItem(parentPosition, subPosition));
        deleteSubButton.setOnClickListener(v -> deleteSubItem(parentPosition, subPosition));

        String subItemTitle = subItem.getTitle();
        int subItemHashCode = System.identityHashCode(subItem);
        String subTitleText = subItemTitle != null ? subItemTitle : "子标题" + subPosition;
        
        
        // 确保TextView和EditText都设置了正确的文本
        if (subTitleTextView != null) {
            subTitleTextView.setText(subTitleText);
            // 强制刷新视图，确保文本正确显示
            subTitleTextView.invalidate();
            subTitleTextView.requestLayout();
        } else {
        }

        if (subTitleEditText != null) {
            // 设置文本前先清除焦点，避免触发监听器
            subTitleEditText.clearFocus();
            // 移除之前的TextWatcher（如果有）
            subTitleEditText.removeTextChangedListener(null);
            // 先设置文本，此时还没有添加TextWatcher，所以不会触发回调
            subTitleEditText.setText(subItemTitle != null ? subItemTitle : "");
            // 强制刷新视图
            subTitleEditText.invalidate();
            subTitleEditText.requestLayout();
        } else {
        }
        
        // 延迟设置子项目点击事件，确保文本设置完成后再添加TextWatcher
        subItemContainer.post(() -> {
            setupSubItemClickEvents(subItemContainer, subTitleTextView, subTitleEditText, subButtonContainer, subItem, parentPosition, subPosition);
        });

        return subItemContainer;
    }

    /**
     * 创建子项目按钮容器
     */
    private LinearLayout createSubButtonContainer(OutlineItem subItem, int parentPosition, int subPosition,
                                                TextView subTitleTextView, EditText subTitleEditText) {
        LinearLayout subButtonContainer = new LinearLayout(this);
        subButtonContainer.setOrientation(LinearLayout.HORIZONTAL);
        subButtonContainer.setGravity(Gravity.CENTER_VERTICAL);
        subButtonContainer.setVisibility(View.GONE); // 默认隐藏

        // 添加子项目按钮
        ImageButton addSubButton = new ImageButton(this);
        LinearLayout.LayoutParams addSubParams = new LinearLayout.LayoutParams(dpToPx(28), dpToPx(28));
        addSubParams.setMargins(dpToPx(4), 0, dpToPx(4), 0);
        addSubButton.setLayoutParams(addSubParams);
//        addSubButton.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        addSubButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        addSubButton.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        addSubButton.setBackground(getResources().getDrawable(R.drawable.ic_add));
        subButtonContainer.addView(addSubButton);

        // 删除子项目按钮
        ImageButton deleteSubButton = new ImageButton(this);
        LinearLayout.LayoutParams deleteSubParams = new LinearLayout.LayoutParams(dpToPx(28), dpToPx(28));
        deleteSubParams.setMargins(dpToPx(4), 0, dpToPx(4), 0);
        deleteSubButton.setLayoutParams(deleteSubParams);
        deleteSubButton.setBackgroundColor(Color.TRANSPARENT);
        deleteSubButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        deleteSubButton.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        addSubButton.setBackground(getResources().getDrawable(R.drawable.ic_delete_red));
        subButtonContainer.addView(deleteSubButton);

        // 设置按钮点击事件
        addSubButton.setOnClickListener(v -> addNewSubItem(parentPosition, subPosition));
        deleteSubButton.setOnClickListener(v -> deleteSubItem(parentPosition, subPosition));

        return subButtonContainer;
    }

    /**
     * 设置子项目点击事件
     */
    private void setupSubItemClickEvents(View subItemContainer, TextView subTitleTextView, EditText subTitleEditText,
                                       LinearLayout subButtonContainer, final OutlineItem subItem, final int parentPosition, final int subPosition) {
        // 点击子项目区域进入编辑模式
        subItemContainer.setOnClickListener(v -> {
            // 确保EditText的文本与subItem的文本一致
            String currentTitle = subItem.getTitle();
            if (currentTitle != null && !currentTitle.trim().isEmpty()) {
                subTitleEditText.setText(currentTitle);
            } else {
                subTitleEditText.setText("");
            }
            
            // 切换到编辑模式
            subTitleTextView.setVisibility(View.GONE);
            subTitleEditText.setVisibility(View.VISIBLE);
            subButtonContainer.setVisibility(View.VISIBLE);

            // 设置背景为选中状态（灰色边框）
            subItemContainer.setBackgroundResource(R.drawable.bg_outline_item_selected_gray);

            // 请求焦点并显示键盘
            subTitleEditText.requestFocus();
            subTitleEditText.setSelection(subTitleEditText.getText().length());

            // 确保EditText可见，延迟滚动到EditText位置
            subTitleEditText.post(() -> {
                if (isKeyboardVisible) {
                    scrollToEditText(subTitleEditText);
                }
            });
        });

        // EditText失去焦点时退出编辑模式
        subTitleEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                // 从EditText读取最终值并更新
                String finalTitle = subTitleEditText.getText().toString();

                // 验证内容有效性
                if (isValidContent(finalTitle)) {
                    subItem.setTitle(finalTitle);
                    subTitleTextView.setText(finalTitle);
                } else {
                    // 如果内容无效，恢复原始值
                    String originalTitle = subItem.getTitle();
                    if (originalTitle != null && !originalTitle.trim().isEmpty()) {
                        subTitleEditText.setText(originalTitle);
                        subTitleTextView.setText(originalTitle);
                    } else {
                        subTitleTextView.setText("请输入子标题");
                    }
                }

                // 切换回显示模式
                subTitleEditText.setVisibility(View.GONE);
                subTitleTextView.setVisibility(View.VISIBLE);
                subButtonContainer.setVisibility(View.GONE);

                // 恢复背景为透明
                subItemContainer.setBackgroundColor(Color.TRANSPARENT);

                // 通知ViewModel更新数据
                updateSubItemInViewModel(subItem, parentPosition, subPosition);
            }
        });

        // 处理EditText的回车键
        subTitleEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                subTitleEditText.clearFocus();
                return true;
            }
            return false;
        });

        // 添加文本变化监听器进行实时验证
        // 使用final变量确保正确捕获，避免闭包问题
        final TextView finalSubTitleTextView = subTitleTextView;
        final EditText finalSubTitleEditText = subTitleEditText;
        final OutlineItem finalSubItem = subItem;
        
        subTitleEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                // 使用final变量，确保引用正确
                String newTitle = finalSubTitleEditText.getText().toString();

                // 验证内容有效性
                if (isValidContent(newTitle)) {
                    finalSubItem.setTitle(newTitle);
                    finalSubTitleTextView.setText(newTitle);
                } else {
                    // 内容无效时显示提示文本和Toast
                    if (!newTitle.isEmpty()) { // 只有在用户输入了内容但无效时才提示
                        Toast.makeText(PptOutlineEditActivity.this, "请输入有效内容", Toast.LENGTH_SHORT).show();
                    }
                    finalSubItem.setTitle(""); // 清空无效内容
                    finalSubTitleTextView.setText("请输入子标题");
                }
            }
        });
    }

    /**
     * 添加新的子项目
     */
    private void addNewSubItem(int parentPosition, int subPosition) {
        if (parentPosition >= 0 && parentPosition < currentOutlineItems.size()) {
            OutlineItem parentItem = currentOutlineItems.get(parentPosition);
            List<OutlineItem> subItems = parentItem.getSubItems();
            if (subItems == null) {
                subItems = new ArrayList<>();
                parentItem.setSubItems(subItems);
            }

            // 检查是否已经存在空的子标题
            for (OutlineItem subItem : subItems) {
                if (!isValidContent(subItem.getTitle()) || "新子标题".equals(subItem.getTitle())) {
                    // 已经存在无效子标题，显示提示并返回
                    Toast.makeText(this, "请输入有效内容", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            ZUtils.print("before subItems = "+subItems.size());

            OutlineItem newSubItem = new OutlineItem("新子标题", "", 2);
            newSubItem.setParentId(parentItem.getId());
            subItems.add(subPosition + 1, newSubItem);

            ZUtils.print("parentPosition = "+parentPosition+" subPosition = "+subPosition);
            ZUtils.print("after subItems = "+subItems.size());
            ZUtils.print("after parentItem.getSubItems() = "+parentItem.getSubItems().size());
            // 重新创建视图
//            updateOutlineViews(currentOutlineItems);
//            viewModel.insertOutlineItem(parentPosition + 1, newItem);
            ZUtils.print("after 重新创建视图 currentOutlineItems = "+currentOutlineItems.size());
            viewModel.getOutlineItems().setValue(currentOutlineItems);
        }
    }

    /**
     * 删除子项目
     */
    private void deleteSubItem(int parentPosition, int subPosition) {
        if (parentPosition >= 0 && parentPosition < currentOutlineItems.size()) {
            OutlineItem parentItem = currentOutlineItems.get(parentPosition);
            List<OutlineItem> subItems = parentItem.getSubItems();
            if (subItems != null && subPosition >= 0 && subPosition < subItems.size()) {
                subItems.remove(subPosition);

                // 重新创建视图
                updateOutlineViews(currentOutlineItems);
            }
        }
    }

    /**
     * 更新ViewModel中的子项目
     */
    private void updateSubItemInViewModel(OutlineItem subItem, int parentPosition, int subPosition) {
        // 这里可以添加更新ViewModel的逻辑
        // 暂时只是更新本地数据
        if (parentPosition >= 0 && parentPosition < currentOutlineItems.size()) {
            OutlineItem parentItem = currentOutlineItems.get(parentPosition);
            List<OutlineItem> subItems = parentItem.getSubItems();
            if (subItems != null && subPosition >= 0 && subPosition < subItems.size()) {
                subItems.set(subPosition, subItem);
            }
        }
    }

    /**
     * dp转px工具方法
     */
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * 验证所有大纲内容是否有效
     */
    private boolean validateAllOutlineContent() {
        List<OutlineItem> items = viewModel.getOutlineItems().getValue();
        if (items == null || items.isEmpty()) {
            Toast.makeText(this, "请先添加大纲内容", Toast.LENGTH_SHORT).show();
            return false;
        }

        // 检查所有主标题
        for (int i = 0; i < items.size(); i++) {
            OutlineItem item = items.get(i);
            if (!isValidContent(item.getTitle())) {
                Toast.makeText(this, "请输入有效内容", Toast.LENGTH_SHORT).show();
                Timber.tag("PptOutlineEditActivity").d( "主标题 " + i + " 内容无效: '" + item.getTitle() + "'");
                return false;
            }

            // 检查所有子标题
            if (item.getSubItems() != null) {
                for (int j = 0; j < item.getSubItems().size(); j++) {
                    OutlineItem subItem = item.getSubItems().get(j);
                    if (!isValidContent(subItem.getTitle())) {
                        Toast.makeText(this, "请输入有效内容", Toast.LENGTH_SHORT).show();
                        Timber.tag("PptOutlineEditActivity").d( "子标题 " + i + "-" + j + " 内容无效: '" + subItem.getTitle() + "'");
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void navigateToTemplateSelection() {
        // 验证所有大纲内容
        if (!validateAllOutlineContent()) {
            return; // 验证失败，不跳转
        }

        // 保存当前大纲状态
        if (viewModel != null && viewModel.getOutlineItems().getValue() != null) {
            stateManager.updateProjectOutline(viewModel.getOutlineItems().getValue());
        }

        // 确保主题信息正确传递
        String currentTopic = viewModel.getCurrentTopicValue();
        if (currentTopic != null && !currentTopic.isEmpty()) {
            stateManager.updateCurrentTopic(currentTopic);
            Timber.tag("PptOutlineEditActivity").d( "更新状态管理器中的主题: " + currentTopic);
        }

        // 准备跳转数据
        android.os.Bundle templateData = viewModel.prepareTemplateSelectionData();

        // 创建Intent
        Intent intent = new Intent(this, PptTemplateSelectionActivity.class);
        intent.putExtras(templateData);

        Timber.tag("PptOutlineEditActivity").d( "跳转到模板选择页面，主? " + currentTopic);
        startActivity(intent);
    }

    private void showRegenerateConfirmDialog() {


        new CommonDialog.Builder(this)
                .setTitle("重新生成大纲")
                .setMessage("重新生成将覆盖当前大纲内容，是否继续?")
                .setConfirmText("确定")
                .setCancelText("取消")
                .setOnClickListener(new CommonDialog.OnDialogClickListener() {
                    @Override
                    public void onConfirm() {
                        viewModel.regenerateOutline();
                    }

                    @Override
                    public void onCancel() {
                        // 用户选择稍后，不做任何操作
                    }
                })
                .show();
//        new AlertDialog.Builder(this)
//                .setTitle("重新生成大纲")
//                .setMessage("重新生成将覆盖当前大纲内容，是否继续?")
//                .setPositiveButton("确定", (dialog, which) -> viewModel.regenerateOutline())
//                .setNegativeButton("取消", null)
//                .show();
    }

    private void showAddOutlineDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_outline, null);
        EditText titleEditText = dialogView.findViewById(R.id.title_edit_text);
        EditText contentEditText = dialogView.findViewById(R.id.content_edit_text);

        new AlertDialog.Builder(this)
                .setTitle("添加大纲?")
                .setView(dialogView)
                .setPositiveButton("添加", (dialog, which) -> {
                    String title = titleEditText.getText().toString().trim();
                    String content = contentEditText.getText().toString().trim();

                    if (!title.isEmpty()) {
                        viewModel.addOutlineItem(title, content);
                    } else {
                        Toast.makeText(this, "标题不能为空", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // setupItemTouchHelper方法已移除，因为不再使用RecyclerView

    private void showDeleteConfirmDialog(int position) {
        new AlertDialog.Builder(this)
                .setTitle("删除大纲?")
                .setMessage("确定要删除这个大纲项吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    viewModel.removeOutlineItem(position);
                })
                .setNegativeButton("取消", null)
                .setOnCancelListener(null)
                .show();
    }

    /**
     * 更新思考状态为已完?
     */
    private void updateThinkingStatusToCompleted() {
        if (thinkingStartTime > 0) {
            long duration = System.currentTimeMillis() - thinkingStartTime;
            int seconds = (int) (duration / 1000);
            thinkingStatusText.setText("已深度思考（用时" + seconds + "秒）");
            Timber.tag("PptOutlineEditActivity").d( "思考完成，用时: " + seconds + "?");
        } else {
            thinkingStatusText.setText("已深度思考（用时12秒）");
        }
    }

    // OutlineAdapter类已移除，改用LinearLayout动态添加View的方式





    /**
     * 创建测试数据
     */
    private void createTestData() {
        List<OutlineItem> testItems = new ArrayList<>();

        // 创建第一个主标题
        OutlineItem item1 = new OutlineItem("第一章：项目概述", "", 1);
        List<OutlineItem> subItems1 = new ArrayList<>();
        subItems1.add(new OutlineItem("项目背景", "", 2));
        subItems1.add(new OutlineItem("项目目标", "", 2));
        subItems1.add(new OutlineItem("项目范围", "", 2));
        item1.setSubItems(subItems1);
        testItems.add(item1);

        // 创建第二个主标题
        OutlineItem item2 = new OutlineItem("第二章：技术方案", "", 1);
        List<OutlineItem> subItems2 = new ArrayList<>();
        subItems2.add(new OutlineItem("架构设计", "", 2));
        subItems2.add(new OutlineItem("技术选型", "", 2));
        subItems2.add(new OutlineItem("实现方案", "", 2));
        item2.setSubItems(subItems2);
        testItems.add(item2);

        // 创建第三个主标题
        OutlineItem item3 = new OutlineItem("第三章：项目计划", "", 1);
        List<OutlineItem> subItems3 = new ArrayList<>();
        subItems3.add(new OutlineItem("时间安排", "", 2));
        subItems3.add(new OutlineItem("资源配置", "", 2));
        subItems3.add(new OutlineItem("风险评估", "", 2));
        item3.setSubItems(subItems3);
        testItems.add(item3);

        // 更新视图
        updateOutlineViews(testItems);

        Timber.tag("PptOutlineEditActivity").d( "创建了测试数据，包含 " + testItems.size() + " 个主标题");
    }

    /**
     * 设置ScrollView滚动监听器
     */
    private void setupScrollListener() {
        if (mainScrollView != null) {
            // 添加触摸监听器来检测用户手动滚动
            mainScrollView.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        isUserScrolling = true;
                        Timber.tag("PptOutlineEditActivity").d( "用户开始触摸滚动");
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        isUserScrolling = false;
                        Timber.tag("PptOutlineEditActivity").d( "用户结束触摸滚动");
                        // 延迟检查是否在底部
                        mainScrollView.postDelayed(() -> checkIfAtBottom(), 100);
                        break;
                }
                return false; // 不消费事件，让ScrollView正常处理滚动
            });

            // 添加滚动变化监听器
            mainScrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
                checkIfAtBottom();
            });
        }
    }

    /**
     * 检查是否在底部附近
     */
    private void checkIfAtBottom() {
        if (mainScrollView != null && mainScrollView.getChildCount() > 0) {
            int scrollY = mainScrollView.getScrollY();
            int scrollViewHeight = mainScrollView.getHeight();
            int contentHeight = mainScrollView.getChildAt(0).getHeight();

            // 计算距离底部的距离
            int distanceFromBottom = contentHeight - scrollY - scrollViewHeight;

            // 如果用户滚动到距离底部超过150px，则禁用自动滚动
            if (distanceFromBottom > 150) {
                allowAutoScroll = false;
                Timber.tag("PptOutlineEditActivity").d( "距离底部 " + distanceFromBottom + "px，禁用自动滚动");
            } else if (distanceFromBottom <= 50) {
                // 如果用户滚动到接近底部，重新启用自动滚动
                allowAutoScroll = true;
                Timber.tag("PptOutlineEditActivity").d( "距离底部 " + distanceFromBottom + "px，启用自动滚动");
            }
        }
    }

    /**
     * 设置键盘监听器
     */
    private void setupKeyboardListener() {
        // 监听根布局的变化来检测键盘状态
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                android.graphics.Rect rect = new android.graphics.Rect();
                rootView.getWindowVisibleDisplayFrame(rect);

                int screenHeight = rootView.getRootView().getHeight();
                int keypadHeight = screenHeight - rect.bottom;

                boolean keyboardVisible = keypadHeight > screenHeight * 0.15; // 如果键盘高度超过屏幕15%认为键盘可见

                if (keyboardVisible != isKeyboardVisible) {
                    isKeyboardVisible = keyboardVisible;
                    onKeyboardVisibilityChanged(keyboardVisible);
                }
            });
        }
    }

    /**
     * 键盘可见性变化回调
     */
    private void onKeyboardVisibilityChanged(boolean isVisible) {
        Timber.tag("PptOutlineEditActivity").d( "键盘可见性变化: " + isVisible);

        if (isVisible) {
            // 键盘弹起时，暂时禁用自动滚动，避免与键盘调整冲突
            allowAutoScroll = false;
            Timber.tag("PptOutlineEditActivity").d( "键盘弹起，暂时禁用自动滚动");
        } else {
            // 键盘收起时，延迟重新启用自动滚动
            mainScrollView.postDelayed(() -> {
                allowAutoScroll = true;
                Timber.tag("PptOutlineEditActivity").d( "键盘收起，重新启用自动滚动");
            }, 300);
        }
    }

    /**
     * 自动滚动到底部
     */
    private void autoScrollToBottom() {
        if (!allowAutoScroll) {
            Timber.tag("PptOutlineEditActivity").d( "自动滚动被禁用，跳过滚动");
            return;
        }

        if (mainScrollView != null) {
            // 延迟执行，等待布局完成
            mainScrollView.post(() -> {
                try {
                    // 再次检查是否允许自动滚动
                    if (!allowAutoScroll) {
                        return;
                    }

                    // 强制重新测量布局
                    mainScrollView.requestLayout();

                    // 再次延迟，确保布局完成
                    mainScrollView.postDelayed(() -> {
                        if (!allowAutoScroll) {
                            return;
                        }

                        // 获取内容总高度
                        int contentHeight = mainScrollView.getChildAt(0).getHeight();
                        int scrollViewHeight = mainScrollView.getHeight();

                        // 只有当内容高度超过ScrollView高度时才需要滚动
                        if (contentHeight > scrollViewHeight) {
                            // 计算需要滚动的距离
                            int targetScrollY = contentHeight - scrollViewHeight;

                            // 平滑滚动到底部
                            mainScrollView.smoothScrollTo(0, targetScrollY);

                            Timber.tag("PptOutlineEditActivity").d(
                                "自动滚动到底部 - contentHeight: " + contentHeight +
                                ", scrollViewHeight: " + scrollViewHeight +
                                ", targetScrollY: " + targetScrollY);
                        } else {
                            Timber.tag("PptOutlineEditActivity").d( "内容未超出ScrollView高度，无需滚动");
                        }
                    }, 50); // 50ms延迟确保布局完成
                } catch (Exception e) {
                    Timber.tag("PptOutlineEditActivity").e( "自动滚动时出错"+ e);
                }
            });
        }
    }

    /**
     * 智能滚动到最新内容（类似聊天界面）
     */
    private void smartScrollToLatest() {
        if (!allowAutoScroll || isUserScrolling) {
            return;
        }

        if (mainScrollView != null && outlineLinearContainer != null) {
            mainScrollView.post(() -> {
                try {
                    // 获取最后一个大纲项的View
                    int childCount = outlineLinearContainer.getChildCount();
                    if (childCount > 0) {
                        View lastChild = outlineLinearContainer.getChildAt(childCount - 1);

                        // 强制重新布局
                        lastChild.requestLayout();

                        lastChild.post(() -> {
                            if (!allowAutoScroll || isUserScrolling) {
                                return;
                            }

                            int lastChildHeight = lastChild.getHeight();
                            int scrollViewHeight = mainScrollView.getHeight();

                            // 如果最后一个子View的高度超过ScrollView高度，滚动到能完全显示该View
                            if (lastChildHeight > scrollViewHeight) {
                                // 滚动到最后一个View的底部
                                int[] location = new int[2];
                                lastChild.getLocationInWindow(location);
                                int[] scrollLocation = new int[2];
                                mainScrollView.getLocationInWindow(scrollLocation);

                                int targetScrollY = location[1] - scrollLocation[1] + lastChildHeight - scrollViewHeight;
                                mainScrollView.smoothScrollTo(0, Math.max(0, targetScrollY));
                            } else {
                                // 正常滚动到底部
                                autoScrollToBottom();
                            }
                        });
                    }
                } catch (Exception e) {
                    Timber.tag("PptOutlineEditActivity").e( "智能滚动时出错"+ e);
                }
            });
        }
    }

    /**
     * 滚动到EditText位置，确保编辑时可见
     */
    private void scrollToEditText(EditText editText) {
        if (mainScrollView != null && editText != null) {
            editText.post(() -> {
                try {
                    // 获取EditText在屏幕中的位置
                    int[] location = new int[2];
                    editText.getLocationOnScreen(location);

                    // 获取ScrollView在屏幕中的位置
                    int[] scrollLocation = new int[2];
                    mainScrollView.getLocationOnScreen(scrollLocation);

                    // 计算EditText相对于ScrollView的位置
                    int editTextTop = location[1] - scrollLocation[1];
                    int editTextBottom = editTextTop + editText.getHeight();

                    // 获取ScrollView的可见区域
                    int scrollViewHeight = mainScrollView.getHeight();
                    int currentScrollY = mainScrollView.getScrollY();

                    // 如果EditText不在可见区域内，滚动到合适位置
                    if (editTextTop < 0) {
                        // EditText在可见区域上方，向上滚动
                        int targetScrollY = currentScrollY + editTextTop - dpToPx(20); // 留20dp边距
                        mainScrollView.smoothScrollTo(0, Math.max(0, targetScrollY));
                    } else if (editTextBottom > scrollViewHeight) {
                        // EditText在可见区域下方，向下滚动
                        int targetScrollY = currentScrollY + (editTextBottom - scrollViewHeight) + dpToPx(20); // 留20dp边距
                        mainScrollView.smoothScrollTo(0, targetScrollY);
                    }

                    Timber.tag("PptOutlineEditActivity").d(
                        "滚动到EditText - editTextTop: " + editTextTop +
                        ", editTextBottom: " + editTextBottom +
                        ", scrollViewHeight: " + scrollViewHeight);
                } catch (Exception e) {
                    Timber.tag("PptOutlineEditActivity").e( "滚动到EditText时出错"+ e);
                }
            });
        }
    }

    /**
     * 停止生成大纲
     */
    private void stopGeneration() {
        viewModel.cancelOutlineGeneration();
        Toast.makeText(this, "已停止生成", Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ZUtils.stopService(this);
    }


    private void showConfirmExitDialog() {
        if (confirmExitDialog != null && confirmExitDialog.isShowing()) {
            return;
        }
        String title = "PPT大纲正在生成，返回将不保存";
        if (outlineLinearContainer != null && outlineLinearContainer.getChildCount() > 0 ){
            title = "PPT大纲已生成，返回将不保存";
        }
        confirmExitDialog = new ConfirmDialog(this).setTitle(title).setSubtitle("请确认是否退出").
                setCancelText("取消").setConfirmText("确定").setOnConfirmDialogListener(new ConfirmDialog.OnConfirmDialogListener() {
            @Override
            public void onConfirm() {
                ZUtils.stopService(PptOutlineEditActivity.this);
                finish();
            }

            @Override
            public void onCancel() {
                // 取消退出，什么都不做
            }
        });

        confirmExitDialog.show();
    }

}
