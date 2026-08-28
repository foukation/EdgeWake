package com.fxzs.lingxiagent.view.meeting;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.BillDialogHelper;
import com.fxzs.lingxiagent.util.audio.TTSManager;
import com.fxzs.lingxiagent.view.chat.HistoryBottomSheetFragment;
import com.fxzs.lingxiagent.view.common.BaseFragment;
import com.fxzs.lingxiagent.viewmodel.history.VMHistory;
import com.fxzs.lingxiagent.viewmodel.meeting.VMMeeting;

import timber.log.Timber;

public class MeetingFragment extends BaseFragment<VMMeeting> {
    
    // Tab elements
    private TextView tabRealtime;
    private TextView tabAudioFile;
    private View tabIndicator;
    private View tabIndicatorSecond;
    private ImageView ivHistory;
    private ImageView ivBack;
    
    // Sub fragments
    private RealtimeMeetingFragment realtimeMeetingFragment;
    private AIMeetingSub2Fragment audioTranscriptionFragment;
    private Fragment currentFragment;

    // Current tab (0: realtime, 1: transcription)
    private int currentTab = 0;
    
    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_meeting;
    }
    
    @Override
    protected Class<VMMeeting> getViewModelClass() {
        return VMMeeting.class;
    }
    
    @Override
    protected void initializeViews(View view) {
        // Initialize tabs
        tabRealtime = findViewById(R.id.tab_realtime);
        tabAudioFile = findViewById(R.id.tab_audio_file);
        tabIndicator = findViewById(R.id.tab_indicator);
        tabIndicatorSecond = findViewById(R.id.tab_indicator_second);
        ivHistory = findViewById(R.id.iv_history);
        ivBack = findViewById(R.id.iv_back);
        
        // Set tab click listeners
        findViewById(R.id.tab_realtime_container).setOnClickListener(v -> selectTab(0));
        findViewById(R.id.tab_transcription_container).setOnClickListener(v -> selectTab(1));
        ivHistory.setOnClickListener(v -> showHistoryBottomSheet());
        ivBack.setOnClickListener(v -> navigateBack());
        
        // Initialize sub fragments
        initializeSubFragments();
        
        // Set default tab to RealtimeMeetingFragment (tab 0 = AI实时会议)
        selectTab(0);
    }
    
    @Override
    protected void setupDataBinding() {
        // Main container doesn't need specific data binding
        // Sub fragments handle their own data binding
    }

    @Override
    protected void setupObservers() {

        viewModel.getShowBillDialogEvent().observe(
                getViewLifecycleOwner(),
                msg -> {
                    if (msg == null || !isAdded()) {
                        return;
                    }
                    BillDialogHelper.showBillDialog(
                            requireActivity(),
                            msg,
                            () -> requireActivity().finish()
                    );
                });
    }
    
    /**
     * Initialize sub fragments
     */
    private void initializeSubFragments() {
        realtimeMeetingFragment = new RealtimeMeetingFragment();
        audioTranscriptionFragment = new AIMeetingSub2Fragment();
    }

    /**
     * 显示历史记录底部抽屉，默认选中会议历史
     */
    private void showHistoryBottomSheet() {
        Timber.tag("MeetingFragment").d( "showHistoryBottomSheet called");
        try {
            HistoryBottomSheetFragment bottomSheet = HistoryBottomSheetFragment.newInstance( VMHistory.TAB_MEETING);
            // 传递会议tab索引，默认选中会议历史
            bottomSheet.show(getChildFragmentManager(), "HistoryBottomSheet");
            Timber.tag("MeetingFragment").d( "BottomSheet shown successfully with meeting tab selected");
        } catch (Exception e) {
            Timber.tag("MeetingFragment").e( "Error showing bottom sheet"+ e);
        }
    }
    
    private void navigateBack() {
        if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }
    
    /**
     * Select tab and switch fragment
     */
    private void selectTab(int tab) {
        currentTab = tab;
        
        Fragment targetFragment;
        if (tab == 0) {
            // Real-time meeting tab
            tabRealtime.setTextColor(getResources().getColor(R.color.figma_text_primary));
            tabRealtime.setTextSize(20);
            tabRealtime.setTypeface(null, android.graphics.Typeface.BOLD);
            
            tabAudioFile.setTextColor(getResources().getColor(R.color.figma_text_secondary));
            tabAudioFile.setTextSize(20);
            tabAudioFile.setTypeface(null, android.graphics.Typeface.NORMAL);
            
            // Show first tab indicator, hide second
            tabIndicator.setVisibility(View.VISIBLE);
            tabIndicatorSecond.setVisibility(View.GONE);
            
            targetFragment = realtimeMeetingFragment;
        } else {
            // Audio transcription tab
            tabRealtime.setTextColor(getResources().getColor(R.color.figma_text_secondary));
            tabRealtime.setTextSize(20);
            tabRealtime.setTypeface(null, android.graphics.Typeface.NORMAL);
            
            tabAudioFile.setTextColor(getResources().getColor(R.color.figma_text_primary));
            tabAudioFile.setTextSize(20);
            tabAudioFile.setTypeface(null, android.graphics.Typeface.BOLD);
            
            // Hide first tab indicator, show second
            tabIndicator.setVisibility(View.GONE);
            tabIndicatorSecond.setVisibility(View.VISIBLE);
            
            targetFragment = audioTranscriptionFragment;
        }
        
        // Switch fragment
        switchToFragment(targetFragment);
    }
    
    /**
     * Switch to the specified fragment
     */
    private void switchToFragment(Fragment fragment) {
        if (currentFragment == fragment) {
            return;
        }
        
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        
        // Hide current fragment if exists
        if (currentFragment != null) {
            transaction.hide(currentFragment);
        }
        
        // Show target fragment
        if (!fragment.isAdded()) {
            transaction.add(R.id.fragment_container, fragment);
        } else {
            transaction.show(fragment);
        }
        
        transaction.commit();
        currentFragment = fragment;
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // 切换界面时停止TTS播放
        TTSManager.Companion.getInstance().stop();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // 确保销毁时停止TTS播放
        TTSManager.Companion.getInstance().stop();
    }
    
}