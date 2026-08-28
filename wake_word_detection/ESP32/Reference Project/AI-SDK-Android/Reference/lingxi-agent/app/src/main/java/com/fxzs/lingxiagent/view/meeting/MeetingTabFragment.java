package com.fxzs.lingxiagent.view.meeting;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.view.common.BaseFragment;
import com.fxzs.lingxiagent.viewmodel.meeting.VMMeeting;

import timber.log.Timber;

public class MeetingTabFragment extends BaseFragment<VMMeeting> {
    
    private static final String ARG_TAB_TYPE = "tab_type";
    private static final String ARG_TAB_TITLE = "tab_title";
    private static final String ARG_MEETING_ID = "meeting_id";
    private static final String ARG_TRANSCRIPTION_RESULT = "transcription_result";
    
    private int tabType;
    private String tabTitle;
    private String meetingId;
    private String transcriptionResult;
    private Fragment currentFragment;
    private Fragment contentFragment;
    private Fragment summaryFragment;
    private Fragment topicFragment;
    private Fragment qaFragment;
    private boolean hasPreloadedTabs = false;
    
    // Tab views
    private TextView tabContent;
    private TextView tabSummary;
    private TextView tabTopics;
    private TextView tabQa;
    private View tabIndicator;
    ImageView ivExport;
    
    public static MeetingTabFragment newInstance(int tabType, String tabTitle) {
        return newInstance(tabType, tabTitle, null, null);
    }
    
    public static MeetingTabFragment newInstance(int tabType, String tabTitle, String meetingId, String transcriptionResult) {
        MeetingTabFragment fragment = new MeetingTabFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TAB_TYPE, tabType);
        args.putString(ARG_TAB_TITLE, tabTitle);
        args.putString(ARG_MEETING_ID, meetingId);
        args.putString(ARG_TRANSCRIPTION_RESULT, transcriptionResult);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tabType = getArguments().getInt(ARG_TAB_TYPE);
            tabTitle = getArguments().getString(ARG_TAB_TITLE);
            meetingId = getArguments().getString(ARG_MEETING_ID);
            transcriptionResult = getArguments().getString(ARG_TRANSCRIPTION_RESULT);
        }
//        android.util.Log.i("MeetingTabFragment", "创建Fragment - tabType: " + tabType +
//            ", tabTitle: " + tabTitle + ", 生命周期: " + getLifecycle().getCurrentState());
    }
    
    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_meeting_container;
    }
    
    @Override
    protected Class<VMMeeting> getViewModelClass() {
        return VMMeeting.class;
    }
    
    @Override
    protected void initializeViews(View view) {
        android.util.Log.i("MeetingTabFragment", "初始化Fragment容器 - tabType: " + tabType + ", tabTitle: " + tabTitle);

        // 设置会议标题
        android.widget.TextView tvMeetingTitle = view.findViewById(R.id.tv_meeting_title);
        if (tvMeetingTitle != null && tabTitle != null) {
            tvMeetingTitle.setText(tabTitle);
        }

        // 初始化返回按钮
        android.widget.ImageView ivBack = view.findViewById(R.id.iv_back);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> {
                // 跳转到主页面的会议Tab
//                if (getActivity() != null) {
//                    Timber.tag("MeetingTabFragment").d( "点击home按钮，跳转到主页面会议Tab");
//                    android.content.Intent intent = new android.content.Intent(getActivity(), MainActivity.class);
//                    intent.putExtra("selected_tab", 3); // 会议Tab的索引
//                    // 使用更强的标志确保跳转到主页面
//                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK |
//                                   android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    startActivity(intent);
//                    getActivity().finish();
//                }

                getActivity().finish();
            });
        }

        // 初始化导出按钮
         ivExport = view.findViewById(R.id.iv_export);
        if (ivExport != null) {
            ivExport.setOnClickListener(v -> {
                Timber.tag("MeetingTabFragment").d( "点击导出按钮");
                handleExportClick(v);
            });
        }

        // 初始化Tab控件
        initializeTabs(view);

        // 根据tabType创建对应的子Fragment
        createSubFragment();
    }
    
    @Override
    protected void setupDataBinding() {
        // 容器Fragment不需要双向绑定，由子Fragment处理
    }
    
    @Override
    protected void setupObservers() {
        // 容器Fragment不需要观察者，由子Fragment处理
    }
    
    /**
     * 初始化Tab控件
     */
    private void initializeTabs(View view) {
        // 只有当有会议数据时才显示Tab界面
        if (meetingId == null || meetingId.isEmpty()) {
            return;
        }
        
        tabContent = view.findViewById(R.id.tab_content);
        tabSummary = view.findViewById(R.id.tab_summary);
        tabTopics = view.findViewById(R.id.tab_topics);
        tabQa = view.findViewById(R.id.tab_qa);
        tabIndicator = view.findViewById(R.id.tab_indicator);
        
        // 设置点击事件
        tabContent.setOnClickListener(v -> selectTab(0));
        tabSummary.setOnClickListener(v -> selectTab(1));
        tabTopics.setOnClickListener(v -> selectTab(2));
        tabQa.setOnClickListener(v -> selectTab(3));
        
        // 设置默认选中的Tab
        selectTab(tabType);
        
        // 初始化时设置指示器位置（无动画，延迟确保布局完成）
        if (tabIndicator != null) {
            tabIndicator.post(() -> {
                // 延迟一帧确保所有Tab布局都已完成
                tabIndicator.postDelayed(() -> moveTabIndicator(tabType, false), 50);
            });
        }
    }
    
    /**
     * 选择Tab
     */
    private void selectTab(int newTabType) {
        // 重置所有Tab样式
        resetTabStyles();
        
        // 设置选中Tab的样式
        TextView selectedTab = null;
        ivExport.setVisibility(View.VISIBLE);
        switch (newTabType) {
            case 0:
                selectedTab = tabContent;
                break;
            case 1:
                selectedTab = tabSummary;
                break;
            case 2:
                selectedTab = tabTopics;
                break;
            case 3:
                selectedTab = tabQa;
                ivExport.setVisibility(View.GONE);
                break;
        }
        
        if (selectedTab != null) {
            // 按照Figma设计，选中的Tab使用统一的颜色资源
            selectedTab.setTextColor(getResources().getColor(R.color.tab_text_selected));
            selectedTab.setTypeface(null, android.graphics.Typeface.BOLD);
            
            // 移动指示器
            moveTabIndicator(newTabType);
        }
        
        // 更新当前Tab类型并切换Fragment
        if (tabType != newTabType) {
            tabType = newTabType;
            createSubFragment();
        }
    }
    
    /**
     * 重置所有Tab样式 - 按照Figma设计规范
     */
    private void resetTabStyles() {
        // 按照Figma设计，未选中的Tab使用统一的颜色资源
        int unselectedColor = getResources().getColor(R.color.tab_text_unselected);
        if (tabContent != null) {
            tabContent.setTextColor(unselectedColor);
            tabContent.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        if (tabSummary != null) {
            tabSummary.setTextColor(unselectedColor);
            tabSummary.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        if (tabTopics != null) {
            tabTopics.setTextColor(unselectedColor);
            tabTopics.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        if (tabQa != null) {
            tabQa.setTextColor(unselectedColor);
            tabQa.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }
    
    /**
     * 移动Tab指示器 - 基于实际Tab位置精确居中定位
     */
    private void moveTabIndicator(int tabIndex) {
        moveTabIndicator(tabIndex, true);
    }
    
    /**
     * 移动Tab指示器 - 基于实际Tab位置精确居中定位
     * @param tabIndex 目标Tab索引
     * @param useAnimation 是否使用动画
     */
    private void moveTabIndicator(int tabIndex, boolean useAnimation) {
        if (tabIndicator == null || getActivity() == null) {
            return;
        }
        
        // 获取对应的Tab TextView
        TextView selectedTab = getTabByIndex(tabIndex);
        
        if (selectedTab != null) {
            final TextView finalSelectedTab = selectedTab;
            final View finalTabIndicator = tabIndicator;
            
            // 使用post确保布局完成后再计算位置
            selectedTab.post(() -> {
                // 获取选中Tab的实际位置和尺寸
                float tabLeft = finalSelectedTab.getX();
                float tabWidth = finalSelectedTab.getWidth();
                float indicatorWidth = finalTabIndicator.getWidth();
                
                // 计算指示器居中位置：Tab中心位置 - 指示器宽度的一半
                float targetX = tabLeft + (tabWidth - indicatorWidth) / 2f;
                
                if (useAnimation) {
                    // 使用动画移动指示器到精确的居中位置
                    ObjectAnimator animator = ObjectAnimator.ofFloat(finalTabIndicator, "translationX", targetX);
                    animator.setDuration(200);
                    animator.start();
                } else {
                    // 直接设置位置，无动画
                    finalTabIndicator.setTranslationX(targetX);
                }
            });
        }
    }
    
    /**
     * 根据索引获取对应的Tab TextView
     */
    private TextView getTabByIndex(int tabIndex) {
        switch (tabIndex) {
            case 0:
                return tabContent;
            case 1:
                return tabSummary;
            case 2:
                return tabTopics;
            case 3:
                return tabQa;
            default:
                return null;
        }
    }
    
    /**
     * 根据tabType创建对应的子Fragment
     */
    private void createSubFragment() {
        // 如果没有会议数据，显示录音界面
        if (meetingId == null || meetingId.isEmpty()) {
            android.util.Log.i("MeetingTabFragment", "没有会议数据，显示录音界面");
            Fragment recordFragment = new MeetingFragment();
            if (getChildFragmentManager() != null) {
                getChildFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, recordFragment)
                    .commit();
            }
            currentFragment = recordFragment;
            return;
        }

        // 首次进入且默认在内容页时，预加载摘要和话题页
        if (!hasPreloadedTabs && tabType == 0) {
            preloadInitialTabs();
            hasPreloadedTabs = true;
        }

        Fragment targetFragment = getOrCreateFragment(tabType);
        switchToFragment(targetFragment);
        ensureNestedFragmentsLoaded(targetFragment);
    }

    private void ensureNestedFragmentsLoaded(Fragment targetFragment) {
        if (targetFragment instanceof MeetingSummaryFragment) {
            if (targetFragment.isAdded() && targetFragment.getView() != null) {
                ((MeetingSummaryFragment) targetFragment).ensureChildFragmentsLoaded();
            }
        }
    }

    /**
     * 首次进入预加载tabType=1(摘要)和tabType=2(话题)
     */
    private void preloadInitialTabs() {
        if (getChildFragmentManager() == null) {
            return;
        }

        Fragment summary = getOrCreateFragment(1);
        Fragment topic = getOrCreateFragment(2);

        if (summary == null && topic == null) {
            return;
        }

        androidx.fragment.app.FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        if (summary != null && !summary.isAdded()) {
            transaction.add(R.id.fragment_container, summary, "meeting_summary_preload");
            transaction.hide(summary);
            summary.getLifecycle().addObserver(new androidx.lifecycle.DefaultLifecycleObserver() {
                @Override
                public void onResume(@androidx.annotation.NonNull androidx.lifecycle.LifecycleOwner owner) {
                    summary.getLifecycle().removeObserver(this);
                    ensureNestedFragmentsLoaded(summary);
                }
            });
        }
        if (topic != null && !topic.isAdded()) {
            transaction.add(R.id.fragment_container, topic, "meeting_topic_preload");
            transaction.hide(topic);
        }
        transaction.commit();
    }

    /**
     * 使用show/hide切换父级Tab Fragment，避免replace导致子Fragment视图丢失
     */
    private void switchToFragment(Fragment targetFragment) {
        if (targetFragment == null || getChildFragmentManager() == null) {
            return;
        }

        if (currentFragment == targetFragment && targetFragment.isAdded()) {
            return;
        }

        androidx.fragment.app.FragmentTransaction transaction = getChildFragmentManager().beginTransaction();

        if (currentFragment != null && currentFragment.isAdded()) {
            transaction.hide(currentFragment);
        }

        if (!targetFragment.isAdded()) {
            transaction.add(R.id.fragment_container, targetFragment);
        } else {
            transaction.show(targetFragment);
        }

        transaction.commit();
        currentFragment = targetFragment;
    }

    /**
     * 按tabType获取（或创建）对应Fragment实例
     */
    private Fragment getOrCreateFragment(int type) {
        switch (type) {
            case 0: // 会议内容
                if (contentFragment == null) {
                    contentFragment = MeetingContentFragment.newInstance(meetingId, transcriptionResult);
                }
                return contentFragment;
            case 1: // 会议摘要
                if (summaryFragment == null) {
                    summaryFragment = MeetingSummaryFragment.newInstance(meetingId, transcriptionResult);
                }
                return summaryFragment;
            case 2: // 会议话题
                if (topicFragment == null) {
                    topicFragment = MeetingTopicFragment.newInstance(meetingId, transcriptionResult);
                }
                return topicFragment;
            case 3: // 智能问答
                if (qaFragment == null) {
                    qaFragment = MeetingQAFragment.newInstance(meetingId, transcriptionResult);
                }
                return qaFragment;
            default:
                if (contentFragment == null) {
                    contentFragment = MeetingContentFragment.newInstance(meetingId, transcriptionResult);
                }
                return contentFragment;
        }
    }

    /**
     * 处理导出按钮点击
     */
    private void handleExportClick(android.view.View anchor) {
        if (currentFragment instanceof MeetingSummaryFragment) {
            // 摘要页面：直接导出摘要内容（不再显示更多选项弹窗）
            ((MeetingSummaryFragment) currentFragment).exportToWord();
        } else if (currentFragment instanceof MeetingTopicFragment) {
            // 话题页面：直接导出话题内容
            ((MeetingTopicFragment) currentFragment).exportToWord();
        } else if (currentFragment instanceof MeetingContentFragment) {
            // 内容页面：直接导出会议内容
            ((MeetingContentFragment) currentFragment).exportToWord();
        } else if (currentFragment instanceof MeetingQAFragment) {
            // 问答页面：提示不支持导出
            android.widget.Toast.makeText(getContext(), "问答页面暂不支持导出", android.widget.Toast.LENGTH_SHORT).show();
        } else {
            // 其他页面：提示切换到支持导出的页面
            android.widget.Toast.makeText(getContext(), "当前页面不支持导出", android.widget.Toast.LENGTH_SHORT).show();
        }
    }



    /**
     * 刷新当前子Fragment的内容
     */
    public void updateContent() {
        // 由子Fragment自己处理内容刷新
        if (currentFragment instanceof MeetingContentFragment) {
            // 会议内容Fragment可以刷新
        } else if (currentFragment instanceof MeetingSummaryFragment) {
            // 会议摘要Fragment可以刷新
        } else if (currentFragment instanceof MeetingTopicFragment) {
            // 会议话题Fragment可以刷新
        } else if (currentFragment instanceof MeetingQAFragment) {
            // 智能问答Fragment可以刷新
        }
    }
    
    // Inner class for meeting content structure (保留为了兼容性)
    public static class MeetingContentDto {
        private String transcription;  // 会议内容
        private String summary;        // 会议摘要
        private String topics;         // 会议话题
        private String qaContent;      // 智能问答
        
        public String getTranscription() {
            return transcription;
        }
        
        public void setTranscription(String transcription) {
            this.transcription = transcription;
        }
        
        public String getSummary() {
            return summary;
        }
        
        public void setSummary(String summary) {
            this.summary = summary;
        }
        
        public String getTopics() {
            return topics;
        }
        
        public void setTopics(String topics) {
            this.topics = topics;
        }
        
        public String getQaContent() {
            return qaContent;
        }
        
        public void setQaContent(String qaContent) {
            this.qaContent = qaContent;
        }
    }
}