package com.fxzs.lingxiagent.view.meeting;

import android.content.Intent;
import android.os.Bundle;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.viewmodel.meeting.VMMeeting;

public class MeetingActivity extends BaseActivity<VMMeeting> {

    private static final String EXTRA_MEETING_ID = "meeting_id";
    private static final String EXTRA_TRANSCRIPTION_RESULT = "transcription_result";
    private static final String EXTRA_TAB_TYPE = "tab_type";
    private static final String EXTRA_MEETING_TITLE = "meeting_title";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 获取传递的数据
        Intent intent = getIntent();
        String meetingId = intent.getStringExtra(EXTRA_MEETING_ID);
        String transcriptionResult = intent.getStringExtra(EXTRA_TRANSCRIPTION_RESULT);
        int tabType = intent.getIntExtra(EXTRA_TAB_TYPE, 0);
        String meetingTitle = intent.getStringExtra(EXTRA_MEETING_TITLE);

        // 如果没有传递标题，使用默认标题
        if (meetingTitle == null || meetingTitle.trim().isEmpty()) {
            meetingTitle = "会议详情";
        }

        // 创建并显示MeetingTabFragment
        MeetingTabFragment fragment = MeetingTabFragment.newInstance(tabType, meetingTitle, meetingId, transcriptionResult);
        
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_meeting_container;
    }

    @Override
    protected Class<VMMeeting> getViewModelClass() {
        return VMMeeting.class;
    }

    @Override
    protected void setupDataBinding() {
        // Activity只是容器，不需要数据绑定
    }

    @Override
    protected void initializeViews() {
        // Fragment会处理所有UI初始化
    }

    @Override
    public void onBackPressed() {
        // 返回到上一页，而不是首页
        super.onBackPressed();
    }

    @Override
    protected void setupObservers() {
        // Fragment会处理所有观察者
    }

    @Override
    protected void handleLoadingState(boolean loading) {
        // Fragment会处理加载状态
    }

    /**
     * 创建Intent来启动MeetingActivity
     * @param meetingId 会议ID
     * @param transcriptionResult 转写结果
     * @param tabType Tab类型
     * @return Intent
     */
    public static Intent createIntent(android.content.Context context, String meetingId, String transcriptionResult, int tabType) {
        return createIntent(context, meetingId, transcriptionResult, tabType, null);
    }

    public static Intent createIntent(android.content.Context context, String meetingId, String transcriptionResult, int tabType, String meetingTitle) {
        Intent intent = new Intent(context, MeetingActivity.class);
        intent.putExtra(EXTRA_MEETING_ID, meetingId);
        intent.putExtra(EXTRA_TRANSCRIPTION_RESULT, transcriptionResult);
        intent.putExtra(EXTRA_TAB_TYPE, tabType);
        intent.putExtra(EXTRA_MEETING_TITLE, meetingTitle);
        return intent;
    }
}