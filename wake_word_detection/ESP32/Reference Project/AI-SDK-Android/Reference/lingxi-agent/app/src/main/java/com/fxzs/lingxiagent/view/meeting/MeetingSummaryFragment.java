package com.fxzs.lingxiagent.view.meeting;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.util.WordExportUtil;
import com.fxzs.lingxiagent.util.audio.TTSManager;
import com.fxzs.lingxiagent.view.chat.SuperChatFragment;
import com.fxzs.lingxiagent.view.common.BaseFragment;
import com.fxzs.lingxiagent.view.common.CommonDialog;
import com.fxzs.lingxiagent.view.common.DataBindingUtils;
import com.fxzs.lingxiagent.view.common.GlobalToast;
import com.fxzs.lingxiagent.viewmodel.chat.VMChat;
import com.fxzs.lingxiagent.viewmodel.meeting.VMMeetingSummary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class MeetingSummaryFragment extends BaseFragment<VMMeetingSummary> {

    public static final String ARG_MEETING_ID = "meeting_id";
    public static final String ARG_TRANSCRIPTION_RESULT = "transcription_result";
    public static final String ARG_BOTKEY = "botKey";

    private String meetingId;
    private String transcriptionResult;
//    private AIResponseView aiResponseView;
    
    // 标签相关UI元素
    private TextView tagBySection;
    private TextView tagByTopic;
    private TextView tagDetailed;
    private int currentSelectedTag = 0; // 0: 按章节, 1: 按主题, 2: 详细
    
    // 新增状态指示器UI元素（参考StreamActivity）
    private LinearLayout llStatusContainer;
    private TextView tvSummaryStatus;
    private ProgressBar progressSummary;
    
    // 观察者引用，用于清理
    private Observer<String> streamContentObserver;
    private Observer<Boolean> isGeneratingObserver;
    private Observer<Integer> progressObserver;

    // 自动滚动相关
    private boolean userHasScrolled = false; // 用户是否手动滚动过
    private long lastUserScrollTime = 0; // 最后一次用户滚动的时间
    private SuperChatFragment superChatFragment1;
    private SuperChatFragment superChatFragment2;
    private SuperChatFragment superChatFragment3;

    private Fragment currentFragment;
    private boolean fragmentsInitialized = false; // Fragment是否已初始化

    public static MeetingSummaryFragment newInstance(String meetingId, String transcriptionResult) {
        MeetingSummaryFragment fragment = new MeetingSummaryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MEETING_ID, meetingId);
        args.putString(ARG_TRANSCRIPTION_RESULT, transcriptionResult);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            meetingId = getArguments().getString(ARG_MEETING_ID);
            transcriptionResult = getArguments().getString(ARG_TRANSCRIPTION_RESULT);
        }
        // 延迟Fragment创建到View完全初始化后
    }
    
    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_meeting_summary;
    }
    
    @Override
    protected Class<VMMeetingSummary> getViewModelClass() {
        return VMMeetingSummary.class;
    }
    
    @Override
    protected void initializeViews(View view) {
//        aiResponseView = view.findViewById(R.id.ai_response_view);
        
        // 初始化标签UI元素
        View summaryTags = view.findViewById(R.id.summary_tags);
        if (summaryTags != null) {
            tagBySection = summaryTags.findViewById(R.id.tag_by_section);
            tagByTopic = summaryTags.findViewById(R.id.tag_by_topic);
            tagDetailed = summaryTags.findViewById(R.id.tag_detailed);
        }
        
        setupAIResponseView();
        setupTagListeners();
        setDefaultContent();
        
        // 在View完全初始化后创建Fragment
        initFragments();
        selectTab(0);
    }
    
    @Override
    protected void setupDataBinding() {
        // AIResponseView会自动处理内容绑定
        // 绑定状态指示器（如果存在）
        if (tvSummaryStatus != null) {
            DataBindingUtils.bindTextView(tvSummaryStatus, viewModel.getSummaryStatus(), this);
        }
    }
    
    @Override
    protected void setupObservers() {
        // 观察流式摘要内容变化
        streamContentObserver = content -> {
//            if (aiResponseView != null && content != null && !content.isEmpty()) {
//                // Timber.tag("MeetingSummaryFragment").d( "流式内容更新: " + content.length() + " 字符");
//
//                // 更新内容
//                aiResponseView.setContent(content);
//
//                // 检查是否需要自动滚动
//                checkAndAutoScroll();
//            }
        };
        viewModel.getSummaryStreamContent().observeForever(streamContentObserver);
        
        // 观察进度变化，更新进度条（参考StreamActivity）
        progressObserver = progress -> {
            if (progress != null && progressSummary != null) {
                progressSummary.setProgress(progress);
                // Timber.tag("MeetingSummaryFragment").d( "进度更新: " + progress + "%");
            }
        };

        
        // 观察错误状态
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            handleErrorDisplay(error);
        });
        
        // 观察成功状态
        viewModel.getSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null) {
                // android.util.Log.i("MeetingSummaryFragment", "摘要生成成功: " + success);
            }
        });
    }
    
    private void setDefaultContent() {
        // 初始化标签样式
        updateTagStyles();

        if (transcriptionResult != null && !transcriptionResult.isEmpty()) {
            generateSummaryFromTranscription();
        } else {
            // 设置初始状态
//            aiResponseView.setContent("当前未检测到有效会议内容，暂时无法生成会议摘要...");
            viewModel.getSummaryStatus().set("准备就绪");
        }
    }
    
    private void generateSummaryFromTranscription() {//TODO
        if (transcriptionResult == null || transcriptionResult.isEmpty()) {
//            aiResponseView.setContent("暂无会议内容");
            return;
        }

        // 根据当前选中的标签生成对应类型的总结
        Integer meetingIdInt = parseMeetingId();
        String botKey = getBotKeyForSelectedTag();
        viewModel.generateMeetingSummaryWithCheck(transcriptionResult, meetingIdInt, botKey);
    }
    
    private Integer parseMeetingId() {
        if (meetingId != null && !meetingId.isEmpty()) {
            try {
                return Integer.valueOf(meetingId.replaceAll("\\D", ""));
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        return 1;
    }
    
    
    private void handleErrorDisplay(String error) {
//        if (error != null) {
//            if (error.contains("账号未登录") || error.contains("请重新登录")) {
//                aiResponseView.setContent("登录状态已过期\n\n您的登录已失效，请重新登录后再试。\n\n点击下方按钮返回登录页面。");
//
//                // 3秒后自动跳转到登录页面
//                if (getView() != null) {
//                    getView().postDelayed(() -> {
//                        if (getActivity() != null) {
//                            android.content.Intent intent = new android.content.Intent(getActivity(), com.fxzs.smartassist.view.auth.RegisterActivity.class);
//                            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                            startActivity(intent);
//                            getActivity().finish();
//                        }
//                    }, 3000);
//                }
//            } else {
//                aiResponseView.setContent("摘要生成失败\n\n" + error + "\n\n请点击刷新重试。");
//            }
//        }
    }
    
    private void setupAIResponseView() {
//        if (aiResponseView != null) {
//            // 启用Markdown渲染
//            aiResponseView.setMarkdownEnabled(true);
//
//            // 设置刷新按钮监听器
//            aiResponseView.setOnRefreshClickListener(new AIResponseView.OnRefreshClickListener() {
//                @Override
//                public void onRefreshClick() {
//                    refreshContent();
//                }
//            });
//
//            // AIResponseView 已经内置了复制和语音播放功能
//            // 显示所有按钮
//            aiResponseView.showCopyButton(true);
//            aiResponseView.showSpeakButton(true);
//            aiResponseView.showDivider(true);
//
//            // 设置滚动监听器，检测用户是否手动滚动
//            // setupScrollListener(); // 暂时注释掉，避免编译错误
//        }
    }
    
    private void setupTagListeners() {
        if (tagBySection != null) {
            tagBySection.setOnClickListener(v -> switchToTag(0));
        }
        if (tagByTopic != null) {
            tagByTopic.setOnClickListener(v -> switchToTag(1));
        }
        if (tagDetailed != null) {
            tagDetailed.setOnClickListener(v -> switchToTag(2));
        }
    }
    
    private void switchToTag(int tagIndex) {
        if (currentSelectedTag == tagIndex) return;
        if (viewModel.getGeneratingFlag().get()){
            CommonDialog.showConfirmOneBtnDialog(getActivity(), "当前摘要正在生成中",
                    "暂无法切换，请您耐心等待", "我知道了",
                    new CommonDialog.OnDialogClickListener() {
                        @Override
                        public void onConfirm() {

//                            resetAutoScrollState();
//                            currentSelectedTag = tagIndex;
//                            updateTagStyles();
//                            updateContentForSelectedTag();
                        }

                        @Override
                        public void onCancel() {
                            // 用户点击不同意，不做任何操作
                        }
                    });
        }else {

            resetAutoScrollState();
            currentSelectedTag = tagIndex;
            updateTagStyles();
            updateContentForSelectedTag();
            selectTab(currentSelectedTag);
        }
    }
    
    private void updateTagStyles() {
        // 重置所有标签样式
        resetTagStyle(tagBySection);
        resetTagStyle(tagByTopic);
        resetTagStyle(tagDetailed);
        
        // 设置选中标签样式
        TextView selectedTag = null;
        switch (currentSelectedTag) {
            case 0:
                selectedTag = tagBySection;
                break;
            case 1:
                selectedTag = tagByTopic;
                break;
            case 2:
                selectedTag = tagDetailed;
                break;
        }
        
        if (selectedTag != null) {
            setSelectedTagStyle(selectedTag);
        }
    }
    
    private void resetTagStyle(TextView tag) {
        if (tag != null) {
            tag.setTextColor(0xFF999999);
            tag.setBackgroundResource(R.drawable.bg_tag_default);
            tag.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }
    
    private void setSelectedTagStyle(TextView tag) {
        if (tag != null) {
            tag.setTextColor(0xFF1C77FF);
            tag.setBackgroundResource(R.drawable.bg_tag_selected);
            tag.setTypeface(null, android.graphics.Typeface.BOLD);
        }
    }
    
    private void updateContentForSelectedTag() {
        // 根据选中的标签生成不同类型的总结
        if (transcriptionResult != null && !transcriptionResult.isEmpty()) {
            Integer meetingIdInt = parseMeetingId();
            String botKey = getBotKeyForSelectedTag();
            // android.util.Log.i("MeetingSummaryFragment", "切换到标签: " + getSelectedTagName() + ", botKey: " + botKey);
            viewModel.generateMeetingSummaryWithCheck(transcriptionResult, meetingIdInt, botKey);
        }
    }
    
    private String getBotKeyForSelectedTag() {
        switch (currentSelectedTag) {
            case 0: return "5"; // 按章节
            case 1: return "4"; // 按主题
            case 2: return "3"; // 详细
            default: return "5"; // 默认按章节
        }
    }

    private String getBotKeyForSelectedTag(int currentSelectedTag) {
        switch (currentSelectedTag) {
            case 0: return "5"; // 按章节
            case 1: return "4"; // 按主题
            case 2: return "3"; // 详细
            default: return "5"; // 默认按章节
        }
    }
    
    private String getSelectedTagName() {
        switch (currentSelectedTag) {
            case 0: return "按章节";
            case 1: return "按主题";
            case 2: return "详细";
            default: return "按章节";
        }
    }
    
    private String getSummaryTypePrompt() {
        switch (currentSelectedTag) {
            case 0: 
                return "请按照会议进行的时间顺序，将内容分为不同章节进行总结，每个章节包含主要讨论点和决议。";
            case 1: 
                return "请按照讨论的主题分类整理会议内容，将相关的讨论点归纳到对应主题下，突出核心议题。";
            case 2: 
                return "请提供详细完整的会议总结，包含所有重要讨论内容、决策过程、行动项目和后续安排。";
            default: 
                return "请按照会议进行的时间顺序，将内容分为不同章节进行总结。";
        }
    }
    
    // 复制功能已由 AIResponseView 内部处理
    
    private void refreshContent() {
        if (viewModel == null) {
            GlobalToast.show(getActivity(), "刷新失败，请重试", GlobalToast.Type.ERROR);
            return;
        }
        
        // 检查是否正在生成
        if (Boolean.TRUE.equals(viewModel.getIsGenerating().get())) {
            return;
        }
        


        if (transcriptionResult != null && !transcriptionResult.trim().isEmpty()) {
            // 重置自动滚动状态
            resetAutoScrollState();

            Integer meetingIdInt = parseMeetingId();
            String botKey = getBotKeyForSelectedTag();
            // 使用强制重新生成方法，不管是否已有内容都重新生成
            viewModel.forceRegenerateSummary(transcriptionResult, meetingIdInt, botKey);
        } else {
            GlobalToast.show(getActivity(), "需要先完成语音识别", GlobalToast.Type.ERROR);
        }
    }
    
    public void showMoreOptions(View anchor) {
        PopupMenu popup = new PopupMenu(getContext(), anchor);
        popup.getMenuInflater().inflate(R.menu.menu_meeting_more, popup.getMenu());
        
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_export) {
                exportToWord();
                return true;
            } else if (itemId == R.id.action_share) {
                GlobalToast.show(getActivity(), "分享功能开发中", GlobalToast.Type.NORMAL);
                return true;
            } else if (itemId == R.id.action_translate) {
                GlobalToast.show(getActivity(), "翻译功能开发中", GlobalToast.Type.NORMAL);
                return true;
            }
            return false;
        });
        
        popup.show();
    }

    /**
     * 设置滚动监听器，检测用户是否手动滚动
     */
//    private void setupScrollListener() {
//        if (aiResponseView != null) {
//            // 查找包含AIResponseView的ScrollView
//            android.widget.ScrollView scrollView = findParentScrollView(aiResponseView);
//            if (scrollView != null) {
//                scrollView.setOnScrollChangeListener(new android.view.View.OnScrollChangeListener() {
//                    @Override
//                    public void onScrollChange(android.view.View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
//                        // 检测用户是否手动滚动（滚动距离大于阈值）
//                        Timber.tag("MeetingSummaryFragment").d( "scrollY"+scrollY+" oldScrollY = "+oldScrollY);
//                        if (Math.abs(scrollY - oldScrollY) > 100) {
////                            userHasScrolled = true;
//                            lastUserScrollTime = System.currentTimeMillis();
//                            Timber.tag("MeetingSummaryFragment").d( "用户手动滚动，暂停自动滚动");
//                        }
//                    }
//                });
//            }
//        }
//    }

    /**
     * 查找包含指定View的ScrollView
     */
    private android.widget.ScrollView findParentScrollView(android.view.View view) {
        Timber.tag("MeetingSummaryFragment").d( "开始查找ScrollView，起始View: " + view.getClass().getSimpleName());

        android.view.ViewParent parent = view.getParent();
        int level = 0;

        while (parent != null) {
            level++;
            Timber.tag("MeetingSummaryFragment").d( "第" + level + "层父视图: " + parent.getClass().getSimpleName());

            if (parent instanceof android.widget.ScrollView) {
                Timber.tag("MeetingSummaryFragment").d( "找到ScrollView在第" + level + "层");
                return (android.widget.ScrollView) parent;
            }

            if (parent instanceof android.view.View) {
                parent = ((android.view.View) parent).getParent();
            } else {
                Timber.tag("MeetingSummaryFragment").d( "父视图不是View类型，停止查找");
                break;
            }

            // 防止无限循环
            if (level > 10) {
                Timber.tag("MeetingSummaryFragment").w("查找层级过深，停止查找");
                break;
            }
        }

        Timber.tag("MeetingSummaryFragment").w( "未找到ScrollView");
        return null;
    }

    /**
     * 检查并执行自动滚动
     */
//    private void checkAndAutoScroll() {
//        Timber.tag("MeetingSummaryFragment").d( "=== checkAndAutoScroll 开始 ===");
//
//        if (aiResponseView == null) {
//            android.util.Timber.tag(TAG).w(("MeetingSummaryFragment", "aiResponseView 为空，跳过自动滚动");
//            return;
//        }
//
//        // 检查是否正在生成内容
//        Boolean isGenerating = viewModel.getIsGenerating().get();
//        Timber.tag("MeetingSummaryFragment").d( "当前生成状态: " + isGenerating);
//        if (!Boolean.TRUE.equals(isGenerating)) {
//            Timber.tag("MeetingSummaryFragment").d( "不在生成过程中，跳过自动滚动");
//            return; // 不在生成过程中，不需要自动滚动
//        }
//
//        // 检查用户是否最近手动滚动过（3秒内）
//        long currentTime = System.currentTimeMillis();
//        long timeSinceLastScroll = currentTime - lastUserScrollTime;
//        Timber.tag("MeetingSummaryFragment").d( "用户滚动状态 - userHasScrolled: " + userHasScrolled +
//            ", 距离上次滚动: " + timeSinceLastScroll + "ms");
//
//        if (userHasScrolled && timeSinceLastScroll < 3000) {
//            Timber.tag("MeetingSummaryFragment").d( "用户最近手动滚动过，跳过自动滚动");
//            return; // 用户最近手动滚动过，暂停自动滚动
//        }
//
//        // 查找包含AIResponseView的ScrollView
//        android.widget.ScrollView scrollView = findParentScrollView(aiResponseView);
//        Timber.tag("MeetingSummaryFragment").d( "找到的ScrollView: " + (scrollView != null ? "存在" : "不存在"));
//
//        if (scrollView != null) {
//            Timber.tag("MeetingSummaryFragment").d( "ScrollView信息 - 宽度: " + scrollView.getWidth() +
//                ", 高度: " + scrollView.getHeight());
//
//            // 延迟执行滚动，确保内容已经渲染完成
//            scrollView.post(() -> {
//                try {
//                    // 检查是否已经滚动到底部附近（距离底部小于100px）
//                    int scrollY = scrollView.getScrollY();
//                    int scrollViewHeight = scrollView.getHeight();
//
//                    if (scrollView.getChildCount() > 0) {
//                        int contentHeight = scrollView.getChildAt(0).getHeight();
//                        int distanceFromBottom = contentHeight - scrollY - scrollViewHeight;
//
//                        Timber.tag("MeetingSummaryFragment").d(
//                            "滚动详情 - scrollY: " + scrollY +
//                            ", scrollViewHeight: " + scrollViewHeight +
//                            ", contentHeight: " + contentHeight +
//                            ", 距离底部: " + distanceFromBottom);
//
//                        // 如果距离底部小于200px，认为用户在底部，执行自动滚动
//                        if (distanceFromBottom < 200) {
//                            Timber.tag("MeetingSummaryFragment").d( "执行自动滚动到底部，目标位置: " + contentHeight);
//                            scrollView.smoothScrollTo(0, contentHeight);
//                            Timber.tag("MeetingSummaryFragment").d( "自动滚动命令已发送");
//                        } else {
//                            Timber.tag("MeetingSummaryFragment").d( "距离底部太远，不执行自动滚动");
//                        }
//                    } else {
//                        android.util.Timber.tag(TAG).w(("MeetingSummaryFragment", "ScrollView没有子视图");
//                    }
//                } catch (Exception e) {
//                    android.util.Log.e("MeetingSummaryFragment", "自动滚动执行出错: " + e.getMessage());
//                }
//            });
//        } else {
//            android.util.Timber.tag(TAG).w(("MeetingSummaryFragment", "未找到ScrollView，无法执行自动滚动");
//        }
//
//        Timber.tag("MeetingSummaryFragment").d( "=== checkAndAutoScroll 结束 ===");
//    }

    /**
     * 重置自动滚动状态（在开始新的生成时调用）
     */
    private void resetAutoScrollState() {
        userHasScrolled = false;
        lastUserScrollTime = 0;
        Timber.tag("MeetingSummaryFragment").d( "重置自动滚动状态");
    }

    /**
     * 导出AIResponse内容为Word文档（公共方法，供外部调用）
     */
    public void exportToWord() {
//        if (aiResponseView == null) {
//            GlobalToast.show(getActivity(), "暂无内容可导出", GlobalToast.Type.ERROR);
//            return;
//        }
//
//        // 获取AIResponse的内容
//        String content = aiResponseView.getContent();
        String content = "";

        if(currentFragment instanceof SuperChatFragment){
            VMChat  vmChat =   ((SuperChatFragment)currentFragment).getVMChat();
            List<ChatMessage> messages = vmChat.getChatMessages().getValue();
            if(messages != null && messages.size() > 0){
                content =  messages.get(0).getMessage();
            }
        }

        if (content == null || content.trim().isEmpty()) {
            GlobalToast.show(getActivity(), "暂无内容可导出", GlobalToast.Type.ERROR);
            return;
        }

        // 显示导出进度
        GlobalToast.show(getActivity(), "正在导出文档...", GlobalToast.Type.NORMAL);

        // 生成文档标题
        String title = generateDocumentTitle();

        // 测试修复后的中文编码（仅在调试模式下）
        if (android.util.Log.isLoggable("MeetingSummaryFragment", android.util.Log.DEBUG)) {
            WordExportUtil.testFixedChineseEncoding(getContext());
        }

        // 导出Word文档
        WordExportUtil.exportToWord(getContext(), title, content, new WordExportUtil.ExportCallback() {
            @Override
            public void onSuccess(java.io.File file) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // 导出成功后直接显示弹窗，不显示Toast
                        showOpenDocumentDialog(file);
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        GlobalToast.show(getActivity(), "导出失败: " + error, GlobalToast.Type.ERROR);
                    });
                }
            }
        });
    }

    /**
     * 生成文档标题
     */
    private String generateDocumentTitle() {
        // 根据当前选中的摘要类型生成标题
        String baseTitle = "会议摘要";

        if (viewModel != null) {
            String currentBotKey = viewModel.getCurrentBotKey();
            switch (currentBotKey) {
                case "1":
                    baseTitle = "会议摘要";
                    break;
                case "2":
                    baseTitle = "会议话题";
                    break;
                case "3":
                    baseTitle = "会议纪要";
                    break;
                case "4":
                    baseTitle = "会议总结";
                    break;
                default:
                    baseTitle = "会议摘要";
                    break;
            }
        }

        return baseTitle;
    }

    /**
     * 显示打开文档的对话框
     */
    private void showOpenDocumentDialog(java.io.File file) {
        if (getContext() == null) return;

        new CommonDialog.Builder(getContext())
                .setTitle("导出成功")
                .setMessage("文档已保存到:\n" + file.getName() + "\n\n是否立即打开？")
                .setConfirmText("打开")
                .setCancelText("稍后")
                .setOnClickListener(new CommonDialog.OnDialogClickListener() {
                    @Override
                    public void onConfirm() {
                        WordExportUtil.openDocument(getContext(), file);
                    }

                    @Override
                    public void onCancel() {
                        // 用户选择稍后，不做任何操作
                    }
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        // 清理ObservableField观察者，防止内存泄漏
        if (viewModel != null) {
            // 停止正在进行的摘要生成
            if (Boolean.TRUE.equals(viewModel.getIsGenerating().get())) {
                viewModel.stopGenerating();
            }
            
            // 清理流式观察者 - 使用保存的观察者引用
            if (streamContentObserver != null) {
                viewModel.getSummaryStreamContent().removeObserver(streamContentObserver);
                streamContentObserver = null;
            }
            if (isGeneratingObserver != null) {
                viewModel.getIsGenerating().removeObserver(isGeneratingObserver);
                isGeneratingObserver = null;
            }
            if (progressObserver != null) {
                viewModel.getSummaryProgress().removeObserver(progressObserver);
                progressObserver = null;
            }
        }
        
        // 释放AIResponseView资源
//        if (aiResponseView != null) {
//            aiResponseView.release();
//        }
        
        // 确保销毁时停止TTS播放
        TTSManager.Companion.getInstance().stop();

        super.onDestroyView();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // 切换界面时停止TTS播放
        TTSManager.Companion.getInstance().stop();
    }

    private void initFragments() {
        // 防止重复初始化
        if (fragmentsInitialized) {
            return;
        }
        
        superChatFragment1 = new SuperChatFragment(SuperChatFragment.TYPE_MEETING,getSubFragmentArgue(0));
        superChatFragment2 = new SuperChatFragment(SuperChatFragment.TYPE_MEETING,getSubFragmentArgue(1));
        superChatFragment3 = new SuperChatFragment(SuperChatFragment.TYPE_MEETING,getSubFragmentArgue(2));
        
        fragmentsInitialized = true;
    }

    public void ensureChildFragmentsLoaded() {
        if (!fragmentsInitialized) {
            initFragments();
        }
        preloadChildFragments();
        selectTab(currentSelectedTag);
    }

    private void preloadChildFragments() {
        if (!isAdded()) {
            return;
        }
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        if (superChatFragment1 != null && !superChatFragment1.isAdded()) {
            transaction.add(R.id.fragment_container, superChatFragment1);
            transaction.hide(superChatFragment1);
        }
        if (superChatFragment2 != null && !superChatFragment2.isAdded()) {
            transaction.add(R.id.fragment_container, superChatFragment2);
            transaction.hide(superChatFragment2);
        }
        if (superChatFragment3 != null && !superChatFragment3.isAdded()) {
            transaction.add(R.id.fragment_container, superChatFragment3);
            transaction.hide(superChatFragment3);
        }
        transaction.commit();
    }
    private  Map<String,Object> getSubFragmentArgue(int index){

        Map<String,Object> map = new HashMap<>();

        Integer meetingIdInt = parseMeetingId();
        String botKey = getBotKeyForSelectedTag(index);
//        viewModel.generateMeetingSummaryWithCheck(transcriptionResult, meetingIdInt, botKey);

        map.put(MeetingSummaryFragment.ARG_TRANSCRIPTION_RESULT,transcriptionResult);
        map.put(MeetingSummaryFragment.ARG_MEETING_ID,meetingIdInt);
        map.put(MeetingSummaryFragment.ARG_BOTKEY,botKey);
        return map;
    }

    private void selectTab(int tabIndex) {


        // 根据选中的Tab设置状态和显示对应Fragment
        LinearLayout selectedNavItem = null;
        Fragment selectedFragment = null;

        switch (tabIndex) {
            case 0:
                selectedFragment = superChatFragment1;
                break;
            case 1:
                selectedFragment = superChatFragment2;
                break;
            case 2:
                selectedFragment = superChatFragment3;
                break;
        }

        if (selectedFragment != null) {
            // 设置选中状态
            switchFragment(selectedFragment);
        }
    }

    /**
     * 切换Fragment
     * @param fragment 要显示的Fragment
     */
    private void switchFragment(Fragment fragment) {
        if (currentFragment == fragment) {
            return;
        }

        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();

        // 隐藏当前Fragment
        if (currentFragment != null) {
            transaction.hide(currentFragment);
        }

        // 如果Fragment未添加则添加，否则显示
        if (!fragment.isAdded()) {
            transaction.add(R.id.fragment_container, fragment);
        } else {
            transaction.show(fragment);
        }

        transaction.commit();
        currentFragment = fragment;
    }
}