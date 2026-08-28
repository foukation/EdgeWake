package com.fxzs.lingxiagent.view.meeting;

import android.content.Intent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.viewmodel.meeting.VMMeeting;

/**
 * 会议详情查看Activity
 * 使用MeetingTabFragment系统展示会议内容、摘要、话题、问答
 */
public class MeetingViewActivity extends BaseActivity<VMMeeting> {
    
    private String meetingId;
    private String transcriptionResult;
    
    // Tab navigation
    private LinearLayout tabContent;
    private LinearLayout tabSummary; 
    private LinearLayout tabTopic;
    private LinearLayout tabQA;
    
    private TextView tvTabContent;
    private TextView tvTabSummary;
    private TextView tvTabTopic;
    private TextView tvTabQA;
    
    private Fragment currentFragment;
    private int currentTab = 0; // Default to content tab
    
    @Override
    protected int getLayoutResource() {
        return R.layout.activity_meeting_view;
    }
    
    @Override
    protected Class<VMMeeting> getViewModelClass() {
        return VMMeeting.class;
    }
    
    @Override
    protected void initializeViews() {
        // Get meeting info from intent
        Intent intent = getIntent();
        meetingId = intent.getStringExtra("meeting_id");
        transcriptionResult = intent.getStringExtra("transcription_result");
        
        if (meetingId == null) {
            showToast("会议ID不能为空");
            finish();
            return;
        }
        
        // Initialize toolbar
        ImageView ivBack = findViewById(R.id.iv_back);
        TextView tvTitle = findViewById(R.id.tv_title);
        
        tvTitle.setText("会议详情");
        ivBack.setOnClickListener(v -> finish());
        
        // Initialize tabs
        tabContent = findViewById(R.id.tab_content);
        tabSummary = findViewById(R.id.tab_summary);
        tabTopic = findViewById(R.id.tab_topic);
        tabQA = findViewById(R.id.tab_qa);
        
        tvTabContent = findViewById(R.id.tv_tab_content);
        tvTabSummary = findViewById(R.id.tv_tab_summary);
        tvTabTopic = findViewById(R.id.tv_tab_topic);
        tvTabQA = findViewById(R.id.tv_tab_qa);
        
        // Set tab click listeners
        tabContent.setOnClickListener(v -> selectTab(0));
        tabSummary.setOnClickListener(v -> selectTab(1));
        tabTopic.setOnClickListener(v -> selectTab(2));
        tabQA.setOnClickListener(v -> selectTab(3));
        
        // Default to content tab
        selectTab(0);
    }
    
    @Override
    protected void setupDataBinding() {
        // No specific data binding needed
    }
    
    @Override
    protected void setupObservers() {
        // No specific observers needed
    }
    
    /**
     * Select tab and switch fragment
     */
    private void selectTab(int tabIndex) {
        if (currentTab == tabIndex) {
            return;
        }
        
        currentTab = tabIndex;
        
        // Reset all tab states
        resetTabStates();
        
        // Create appropriate fragment based on tab
        Fragment newFragment;
        String tabTitle;
        
        switch (tabIndex) {
            case 0: // Content
                newFragment = MeetingTabFragment.newInstance(0, "会议内容", meetingId, transcriptionResult);
                tabTitle = "会议内容";
                setTabSelected(tvTabContent, true);
                break;
            case 1: // Summary
                newFragment = MeetingTabFragment.newInstance(1, "会议摘要", meetingId, transcriptionResult);
                tabTitle = "会议摘要";
                setTabSelected(tvTabSummary, true);
                break;
            case 2: // Topic
                newFragment = MeetingTabFragment.newInstance(2, "会议话题", meetingId, transcriptionResult);
                tabTitle = "会议话题";
                setTabSelected(tvTabTopic, true);
                break;
            case 3: // QA
                newFragment = MeetingTabFragment.newInstance(3, "智能问答", meetingId, transcriptionResult);
                tabTitle = "智能问答";
                setTabSelected(tvTabQA, true);
                break;
            default:
                return;
        }
        
        // Switch fragment
        switchFragment(newFragment);
    }
    
    /**
     * Reset all tab states
     */
    private void resetTabStates() {
        setTabSelected(tvTabContent, false);
        setTabSelected(tvTabSummary, false);
        setTabSelected(tvTabTopic, false);
        setTabSelected(tvTabQA, false);
    }
    
    /**
     * Set tab selected state
     */
    private void setTabSelected(TextView tabView, boolean selected) {
        if (selected) {
            tabView.setTextColor(getResources().getColor(R.color.text_primary));
        } else {
            tabView.setTextColor(getResources().getColor(R.color.text_secondary));
        }
    }
    
    /**
     * Switch fragment
     */
    private void switchFragment(Fragment fragment) {
        if (currentFragment == fragment) {
            return;
        }
        
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        
        // Hide current fragment
        if (currentFragment != null) {
            transaction.hide(currentFragment);
        }
        
        // Add or show new fragment
        if (!fragment.isAdded()) {
            transaction.add(R.id.fragment_container, fragment);
        } else {
            transaction.show(fragment);
        }
        
        transaction.commit();
        currentFragment = fragment;
    }
}