package com.fxzs.lingxiagent.view.drawing;

import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingSampleDto;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.view.agent.HomeViewPagerAdapter;
import com.fxzs.lingxiagent.view.chat.HistoryBottomSheetFragment;
import com.fxzs.lingxiagent.view.common.BaseFragment;
import com.fxzs.lingxiagent.viewmodel.drawing.VMDrawing;
import com.fxzs.lingxiagent.viewmodel.history.VMHistory;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class DrawingNewFragment extends BaseFragment<VMDrawing> {

    private TextView tvAgentTitle;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private HomeViewPagerAdapter adapter;

    private List<com.fxzs.lingxiagent.model.drawing.dto.DrawingSampleDto> categories = new ArrayList<>();
    private LinearLayout recordingInputContainer;

    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_drawing_new;
    }

    @Override
    protected Class<VMDrawing> getViewModelClass() {
        return VMDrawing.class;
    }

    @Override
    protected void initializeViews(View view) {
//        tvAgentTitle = findViewById(R.id.tv_agent_title);

        recordingInputContainer = findViewById(R.id.recording_input_container);
        tabLayout = view.findViewById(R.id.tabs);
        viewPager = view.findViewById(R.id.view_pager);
        viewPager.setAdapter(adapter);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                // Smoothly move TabLayout indicator with finger swipe
                tabLayout.setScrollPosition(position, positionOffset, true);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // Ensure selected tab updates after settling
                tabLayout.selectTab(tabLayout.getTabAt(position), true);
            }
        });

//        setupTabs();
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Enable smooth scroll when clicking tabs
                viewPager.setCurrentItem(tab.getPosition(), true);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // 可以在这里处理未选中的Tab
                int position = tab.getPosition();
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        requestData();
        setupRecordingInput();

        findViewById(R.id.iv_history).setOnClickListener(v -> {
            Timber.tag("DrawingNewFragment").d( "History button clicked!");
            showHistoryBottomSheet();
        });
    }

    @Override
    protected void setupDataBinding() {
        // 设置数据绑定
        // 观察错误与加载状态（如需要可扩展到 UI）
        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty()) {
                Timber.tag("DrawingNewFragment").e( "error: " + err);
            }
        });
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            // 可根据 loading 显示/隐藏全局 loading
        });
    }

    @Override
    protected void setupObservers() {
        // 观察数据变化
    }




    private void requestData() {
        // 使用 ViewModel 的分类 LiveData
        viewModel.getCategories().observe(getViewLifecycleOwner(), list -> {
            if (list == null || list.isEmpty()) return;
            categories.clear();
            categories.addAll(list);
            setUI();
        });
    }


    private void setUI() {
        if (categories == null || categories.size() == 0){
            return;
        }
        List<Fragment> fragments = new ArrayList<>();

        for (int i = 0; i < categories.size(); i++) {
            DrawingSampleDto plate = categories.get(i);
            fragments.add(new DrawingSubFragment(plate.getId().intValue()));
        }

        adapter = new HomeViewPagerAdapter(getActivity(), fragments);
        viewPager.setAdapter(adapter);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                // Smoothly move TabLayout indicator with finger swipe
                tabLayout.setScrollPosition(position, positionOffset, true);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // Ensure selected tab updates after settling
                tabLayout.selectTab(tabLayout.getTabAt(position), true);
            }
        });
        viewPager.setOffscreenPageLimit(adapter.getItemCount());

        setupTabs();
    }
    private void setupTabs() {
        for (int i = 0; i < adapter.getItemCount(); i++) {
            TabLayout.Tab tab = tabLayout.newTab();

            tab.view.setLongClickable(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                tab.view.setTooltipText("");
                // 遍历子视图，禁用 tooltip
                ZUtils.disableTooltipForChildViews(tab.view);
            }
            tab.view.setOnLongClickListener(null);
            DrawingSampleDto bean = categories.get(i);
            tab.setText(bean.getName());
            tabLayout.addTab(tab);
        }
    }

    // 设置录音输入
    private void setupRecordingInput() {
        recordingInputContainer.setOnClickListener(v -> {
            // 跳转到绘画聊天界面
            Intent intent = new Intent(getActivity(), DrawingActivity.class);
            startActivity(intent);
        });
    }


    /**
     * 显示历史记录底部抽屉，默认选中绘画历史
     */
    private void showHistoryBottomSheet() {
        Timber.tag("DrawingFragment").d( "showHistoryBottomSheet called");
        try {
            HistoryBottomSheetFragment bottomSheet = HistoryBottomSheetFragment.newInstance( VMHistory.TAB_DRAWING);
            // 传递绘画tab索引，默认选中绘画历史
            bottomSheet.show(getChildFragmentManager(), "HistoryBottomSheet");
            Timber.tag("DrawingFragment").d( "BottomSheet shown successfully with drawing tab selected");
        } catch (Exception e) {
            Timber.tag("DrawingFragment").e( "Error showing bottom sheet"+ e);
        }
    }
}