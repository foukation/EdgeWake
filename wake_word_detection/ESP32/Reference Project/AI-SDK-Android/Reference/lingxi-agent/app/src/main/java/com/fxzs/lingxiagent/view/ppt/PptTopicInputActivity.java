package com.fxzs.lingxiagent.view.ppt;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.fxzs.lingxiagent.JumpParameterManager;
import com.fxzs.lingxiagent.MainActivity;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.ErrorHandler;
import com.fxzs.lingxiagent.util.LoadingManager;
import com.fxzs.lingxiagent.util.PptLifecycleManager;
import com.fxzs.lingxiagent.util.PptNavigationHelper;
import com.fxzs.lingxiagent.util.PptStateManager;
import com.fxzs.lingxiagent.util.UXOptimizer;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.common.GlobalToast;
import com.fxzs.lingxiagent.viewmodel.ppt.VMPptTopicInput;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

import timber.log.Timber;

public class PptTopicInputActivity extends BaseActivity<VMPptTopicInput> {

    private ImageView ivBack;
    private ImageView ivHistory;
    private ImageView logo;
    private TextView titleTextView;
    private TextView subtitleTextView;
    private EditText topicInput;
    private ImageView voiceIcon;
    private ImageView sendButton;
    private TextView tag1, tag2, tag3, tag4, tag5;

    // 保留原有的组件引用以兼容现有功能
    private ImageButton backButton;
    private EditText topicInputEditText;
    private TextView charCountTextView;
    private ChipGroup suggestionChipGroup;
    private TextView efficiencyTipTextView;
    private TextView networkStatusText;
    private TextView errorMessageText;

    private LoadingManager loadingManager;
    private PptStateManager stateManager;
    private PptLifecycleManager lifecycleManager;
    // 提交主题防抖相关
    private static final long SUBMIT_DEBOUNCE_INTERVAL = 2800L; // ms，防止短时间内多次点击
    private long lastSubmitClickTime = 0L;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化管理器
        loadingManager = new LoadingManager(this);
        stateManager = PptStateManager.getInstance(this);
        lifecycleManager = new PptLifecycleManager(this);

        // 注册生命周期观察者
        getLifecycle().addObserver(lifecycleManager);

        // 恢复状态
        PptNavigationHelper.restoreActivityState(this, savedInstanceState);

        setupUI();
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_ppt_topic_input;
    }

    @Override
    protected Class<VMPptTopicInput> getViewModelClass() {
        return VMPptTopicInput.class;
    }

    @Override
    protected void initializeViews() {
        // 新的UI组件
        ivBack = findViewById(R.id.iv_back);
        ivHistory = findViewById(R.id.iv_history);
        logo = findViewById(R.id.logo);
        titleTextView = findViewById(R.id.title_text);
        subtitleTextView = findViewById(R.id.subtitle_text);
        topicInput = findViewById(R.id.topicInput);
        voiceIcon = findViewById(R.id.voiceIcon);
        sendButton = findViewById(R.id.sendButton);

        // 推荐标签
        tag1 = findViewById(R.id.tag1);
        tag2 = findViewById(R.id.tag2);
        tag3 = findViewById(R.id.tag3);
        tag4 = findViewById(R.id.tag4);
        tag5 = findViewById(R.id.tag5);

        // 兼容原有组件（映射到新组件）
        topicInputEditText = topicInput; // 将新的topicInput映射到原有的topicInputEditText

        // 查找错误和状态显示组件
        errorMessageText = findViewById(R.id.error_message_text);

        // 设置点击监听器
        ivBack.setOnClickListener(v -> backToMain());
        ivHistory.setOnClickListener(v -> {
            // 显示历史记录底部弹窗，默认选中PPT tab
            Timber.tag("PptTopicInput").e ("=== 点击历史按钮 ===");
            Timber.i("=== PptTopicInput: 点击历史按钮 ===");
            showHistoryBottomSheet();
        });
        voiceIcon.setOnClickListener(v -> startVoiceInput());
        sendButton.setOnClickListener(v -> submitTopic());
        topicInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String input = editable.toString();
                // 比如，实时显示输入内容
                Timber.tag("EditText").d( "当前输入: " + input);
                if(input.length() >= 40){
                    GlobalToast.show(PptTopicInputActivity.this, "超出限定长度", GlobalToast.Type.NORMAL);
                }
            }
        });
        topicInputEditText.setOnKeyListener((v, keyCode, event) -> {

            if (event.getAction() == KeyEvent.ACTION_DOWN) {

                if (keyCode == KeyEvent.KEYCODE_ENTER
                        || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {

                    submitTopic();
                    return true;
                }

            }

            return false;
        });

        // 为按钮添加点击反馈效果
//        UXOptimizer.addClickFeedback(sendButton);
//        UXOptimizer.addClickFeedback(voiceIcon);
//        UXOptimizer.addClickFeedback(ivBack);
//        UXOptimizer.addClickFeedback(ivHistory);

        // 设置标签点击监听器
        setupTagClickListeners();
    }

    private void backToMain() {
        if (JumpParameterManager.INSTANCE.isMainActivityInStack(this)) {
            // 存在 → 直接 finish，系统自动返回动画
            finish();
        } else {
            // 不存在 → 跳 Main，用系统返回动画
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            finish();
        }
    }

    @Override
    protected void setupDataBinding() {
        topicInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.getTopicText().set(s.toString());

                // 实时验证输入
                if (s.length() > 40) {
                    topicInput.setError("主题长度不能超过40个字符");
                    UXOptimizer.errorFeedback(PptTopicInputActivity.this, topicInput);
                } else {
                    topicInput.setError(null);
                }

                // 根据输入内容显示/隐藏发送按钮
                updateSendButtonVisibility(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 设置输入框焦点监听
        topicInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // 获得焦点时清除错误提示
                topicInput.setError(null);
                viewModel.clearError();
            }
        });
    }

    @Override
    protected void setupObservers() {
        // 移除super.setupObservers()调用，因为BaseActivity中是抽象方法

        // 观察错误信息
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                showErrorMessage(error);
            } else {
                hideErrorMessage();
            }
        });

        // 观察网络状态
        viewModel.getNetworkStatus().observe(this, status -> {
            if (networkStatusText != null) {
                networkStatusText.setText(status);
                networkStatusText.setVisibility(View.VISIBLE);
            }
        });

        // 观察加载状态
        viewModel.getLoading().observe(this, loading -> {
            Toast toast = GlobalToast.show(this, "正在优化主题...", GlobalToast.Type.LOADING);
            if (!loading && toast != null) {
                // 隐藏加载提示
                toast.cancel();
            }
        });

        // 观察标题优化完成事件
        viewModel.getTitleOptimized().observe(this, optimizedTitle -> {
            if (optimizedTitle != null && !optimizedTitle.trim().isEmpty()) {
                Timber.tag("PptTopicInputActivity").d( "收到标题优化完成通知，优化后标题: " + optimizedTitle);

                // 保持用户输入框内容不变，直接跳转到大纲编辑页面并传递优化后的标题
                proceedToOutlineEdit(optimizedTitle);
            }
        });

        // 观察成功状态消息
        viewModel.getSuccess().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                // 可以在这里显示成功提示，比如Toast
                Timber.tag("PptTopicInputActivity").d( "状态更新: " + message);
            }
        });
    }

    /**
     * 设置标签点击监听器
     */
    private void setupTagClickListeners() {
        View.OnClickListener tagClickListener = v -> {
            TextView tag = (TextView) v;
            String tagText = tag.getText().toString();
            selectTag(tagText);
        };

        tag1.setOnClickListener(tagClickListener);
        tag2.setOnClickListener(tagClickListener);
        tag3.setOnClickListener(tagClickListener);
        tag4.setOnClickListener(tagClickListener);
        tag5.setOnClickListener(tagClickListener);

        // 为标签添加点击反馈效果
        UXOptimizer.addClickFeedback(tag1);
        UXOptimizer.addClickFeedback(tag2);
        UXOptimizer.addClickFeedback(tag3);
        UXOptimizer.addClickFeedback(tag4);
        UXOptimizer.addClickFeedback(tag5);
    }

    /**
     * 显示错误信息
     */
    private void showErrorMessage(String error) {
        if (errorMessageText != null) {
            errorMessageText.setText(error);
            errorMessageText.setVisibility(View.VISIBLE);
            UXOptimizer.fadeIn(errorMessageText, 300);
        } else {
            ErrorHandler.showErrorToast(this, error);
        }
    }

    /**
     * 隐藏错误信息
     */
    private void hideErrorMessage() {
        if (errorMessageText != null && errorMessageText.getVisibility() == View.VISIBLE) {
            UXOptimizer.fadeOut(errorMessageText, 300, null);
        }
    }

    /**
     * 显示历史记录底部弹窗
     */
    private void showHistoryBottomSheet() {
        Timber.tag("PptTopicInput").e ("=== 开始显示历史记录弹窗 ===");
        Timber.i("=== PptTopicInput: 开始显示历史记录弹窗 ===");

        try {
            // 直接使用带参数的newInstance方法
            com.fxzs.lingxiagent.view.chat.HistoryBottomSheetFragment historyFragment =
                com.fxzs.lingxiagent.view.chat.HistoryBottomSheetFragment.newInstance(
                    com.fxzs.lingxiagent.viewmodel.history.VMHistory.TAB_PPT);

            Timber.tag("PptTopicInput").e( "=== 历史记录Fragment创建成功，准备显示 ===");
            Timber.i("=== PptTopicInput: 历史记录Fragment创建成功，准备显示 ===");

            historyFragment.show(getSupportFragmentManager(), "HistoryBottomSheet");

            Timber.tag("PptTopicInput").e ("=== 历史记录弹窗显示完成 ===");
            Timber.i("=== PptTopicInput: 历史记录弹窗显示完成 ===");
        } catch (Exception e) {
            Timber.tag("PptTopicInput").e ("=== 显示历史记录弹窗失败 ==="+ e);
            Timber.i("=== PptTopicInput: 显示历史记录弹窗失败: " + e.getMessage() + " ===");
        }
    }

    /**
     * 提交主题时的错误处理
     */
    private void submitTopic() {
        // 防抖处理：限制短时间内多次点击
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSubmitClickTime < SUBMIT_DEBOUNCE_INTERVAL) {
            Timber.tag("PptTopicInputActivity").d("submitTopic 被防抖拦截，间隔过短");
            return;
        }
        Timber.tag("PptTopicInputActivity").d("submitTopic 通过 = currentTime" +currentTime+
                "  lastSubmitClickTime = " +lastSubmitClickTime+
                "  currentTime - lastSubmitClickTime  = " +(currentTime - lastSubmitClickTime));

        lastSubmitClickTime = currentTime;

        // 检查网络连接
        if (!viewModel.checkNetworkConnection()) {
            ErrorHandler.showNetworkErrorDialog(this, new ErrorHandler.ErrorCallback() {
                @Override
                public void onRetry() {
                    submitTopic(); // 重试
                }

                @Override
                public void onCancel() {
                    // 用户取消，不做任何操作
                }
            });
            return;
        }

        // 验证输入
        if (!viewModel.validateTopic()) {
            UXOptimizer.errorFeedback(PptTopicInputActivity.this, topicInput);
            return;
        }

        // 提供成功反馈
        UXOptimizer.successFeedback(this, sendButton);

        // 使用新的提交方法（包含标题优化）
        viewModel.submitTopicAndGenerateOutline();

        // 注意：不再直接跳转，而是在Observer中监听加载完成后跳转
    }

    /**
     * 继续到大纲编辑界面
     * @param optimizedTopic 优化后的标题（用于后台处理，不影响用户输入框显示）
     */
    private void proceedToOutlineEdit(String optimizedTopic) {
        Timber.tag("PptTopicInputActivity").d( "跳转到大纲编辑页面，使用优化标题: " + optimizedTopic);

        // 保存当前输入状态（用户原始输入）
        // lifecycleManager.saveCurrentInput(topicInput.getText().toString());

        // 保持输入框内容不变，不清空用户输入
        // topicInput.setText(""); // 已移除，保持用户原始输入

        // 使用导航助手进行页面跳转，传递优化后的标题
        PptNavigationHelper.navigateToOutlineEdit(this, optimizedTopic);
    }

    /**
     * 选择标签
     */
    private void selectTag(String tagText) {
        topicInput.setText(tagText);
        viewModel.selectSampleTopic(tagText);
        UXOptimizer.successFeedback(this, topicInput);
    }

    /**
     * 开始语音输入
     */
    private void startVoiceInput() {
        // TODO: 实现语音输入功能
        showToast("语音输入功能开发中...");
    }

    /**
     * 更新发送按钮可见性
     */
    private void updateSendButtonVisibility(String text) {
        if (text.isEmpty()) {
//            sendButton.setti(View.GONE);
            ZUtils.setIvBg(this,sendButton,R.drawable.home_send_grey);
//            voiceIcon.setVisibility(View.VISIBLE);
        } else {
//            sendButton.setVisibility(View.VISIBLE);
            ZUtils.setIvBg(this,sendButton,R.drawable.ic_send_new);
//            voiceIcon.setVisibility(View.GONE);
//            UXOptimizer.fadeIn(sendButton, 200);
        }
    }

    /**
     * 显示Toast消息
     */
    @Override
    protected void showToast(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loadingManager != null) {
            loadingManager.destroy();
        }

        // 移除生命周期观察者
        if (lifecycleManager != null) {
            getLifecycle().removeObserver(lifecycleManager);
        }
    }





    private void setupUI() {
        setupTitleText();
        // 示例主题现在使用固定标签，不需要动态加载
    }

    private void setupTitleText() {
        // 使用LinearGradient为整个标题文字设置渐变效果
        String titleText = "AI生成PPT";
        titleTextView.setText(titleText);

        // 在下一帧设置渐变，确保TextView已经测量完成
        titleTextView.post(() -> {
            float width = titleTextView.getPaint().measureText(titleText);
            LinearGradient gradient = new LinearGradient(
                0, 0, width, 0,
                new int[]{Color.parseColor("#7922E7"), Color.parseColor("#43CCFD")},
                null,
                Shader.TileMode.CLAMP
            );
            titleTextView.getPaint().setShader(gradient);
            titleTextView.invalidate();
        });
    }

    /**
     * 更新示例主题Chips（动态加载）
     */
    private void updateSuggestionChips(List<String> suggestions) {
        suggestionChipGroup.removeAllViews();

        if (suggestions != null && !suggestions.isEmpty()) {
            for (String suggestion : suggestions) {
                Chip chip = new Chip(this);
                chip.setText(suggestion);
                chip.setChipBackgroundColorResource(R.color.chip_background);
                chip.setTextColor(ContextCompat.getColor(this, R.color.chip_text));
                chip.setChipStrokeWidth(0);
                chip.setOnClickListener(v -> {
                    viewModel.selectSampleTopic(suggestion);
                    topicInputEditText.setText(suggestion);
                    topicInputEditText.setSelection(suggestion.length());
                });
                suggestionChipGroup.addView(chip);
            }
        }
    }



    /**
     * 更新加载状态
     */
    private void updateLoadingState(Boolean isLoading) {
        if (isLoading != null && isLoading) {
            // 显示加载状态
        }
    }





    @Override
    protected void handleLoadingState(boolean loading) {
        super.handleLoadingState(loading);
        updateLoadingState(loading);
    }

    /**
     * 清空输入内容
     */
    private void clearInput() {
        viewModel.clearInput();
        topicInput.setText("");
        topicInput.setError(null);
    }

    /**
     * 显示输入提示
     */
    private void showInputHint() {
        if (topicInput.getText().toString().trim().isEmpty()) {
            topicInput.setHint("请输入PPT主题，不超过40字");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        showInputHint();

        // 恢复保存的输入内容
        String savedInput = lifecycleManager.getSavedInput();
        if (savedInput != null && !savedInput.isEmpty() && topicInput.getText().toString().isEmpty()) {
            topicInput.setText(savedInput);
            topicInput.setSelection(savedInput.length());
        }

        // 如果有缓存的主题，恢复显示
        String cachedTopic = viewModel.getCurrentTopic();
        if (!cachedTopic.isEmpty() && topicInput.getText().toString().isEmpty()) {
            topicInput.setText(cachedTopic);
            topicInput.setSelection(cachedTopic.length());
        }
    }

    @Override
    public void onBackPressed() {
        // 保存当前输入状态
//        lifecycleManager.saveCurrentInput(topicInput.getText().toString());

        // 尝试使用导航助手返回
//        if (!PptNavigationHelper.navigateBack(this)) {
            // 清除会话缓存
            viewModel.clearInput();
            super.onBackPressed();
//        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // 保存Activity状态
        PptNavigationHelper.saveActivityState(this, outState);
        lifecycleManager.saveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // 恢复Activity状态
        lifecycleManager.restoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // 处理新Intent
        PptNavigationHelper.handleNewIntent(this, intent);
    }

    private static class GradientColorSpan extends ForegroundColorSpan {
        private final int[] colors;

        public GradientColorSpan(int[] colors) {
            super(colors[0]);
            this.colors = colors;
        }
    }
}