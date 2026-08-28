package com.fxzs.lingxiagent.view.agent;

import android.os.Build;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker.TrackerUtils;
import com.fxzs.lingxiagent.network.ZNet.ApiResponse;
import com.fxzs.lingxiagent.network.ZNet.HttpRequest;
import com.fxzs.lingxiagent.network.ZNet.bean.GetMenuBean;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.view.common.BaseFragment;
import com.fxzs.lingxiagent.viewmodel.agent.VMAgent;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class AgentFragment extends BaseFragment<VMAgent> {
    
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private HomeViewPagerAdapter adapter;

//    List<HashMap<Integer,String>> fakeData = new ArrayList<>();
    List<GetMenuBean> list = new ArrayList<>();
    private int currentPosition = 0;
    
    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_agent;
    }
    
    @Override
    protected Class<VMAgent> getViewModelClass() {
        return VMAgent.class;
    }
    
    @Override
    protected void initializeViews(View view) {
        tabLayout = view.findViewById(R.id.tabs);
        viewPager = view.findViewById(R.id.view_pager);
        requestData();
    }
    
    @Override
    protected void setupDataBinding() {
        // 设置数据绑定
    }
    
    @Override
    protected void setupObservers() {
        // 观察数据变化
    }




    private void requestData() {
        HttpRequest request = new HttpRequest();
        request.getMenuList(new Observer<ApiResponse<List<GetMenuBean>>>() {

            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(ApiResponse<List<GetMenuBean>> res) {

                if (res.getCode() == 0) {
                    List<GetMenuBean> listApiResponse =  res.getData();
                    list.addAll(listApiResponse);
                    setUI();
                }
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });

    }


    private void setUI() {
        if (list == null || list.size() == 0){
            return;
        }
        List<Fragment> fragments = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            GetMenuBean plate = list.get(i);
            String data = plate.getName();
            fragments.add(new ZNSubFragment(plate.getId()));

        }

        adapter = new HomeViewPagerAdapter(getActivity(), fragments);
        viewPager.setAdapter(adapter);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                // Smoothly move TabLayout indicator with finger swipe
                tabLayout.setScrollPosition(position, positionOffset, true);
                currentPosition = position;
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
                View view = tab.getCustomView();
                if (view != null){
                    ImageView ivTab =  view.findViewById(R.id.iv_tab);
                    ivTab.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // 可以在这里处理未选中的Tab
                int position = tab.getPosition();
                View view = tab.getCustomView();
                if (view != null){
                    ImageView ivTab =  view.findViewById(R.id.iv_tab);
                    ivTab.setVisibility(View.GONE);
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        viewPager.setOffscreenPageLimit(adapter.getItemCount());

        setupTabs();
    }
    private void setupTabs() {
        for (int i = 0; i < list.size(); i++) {
            TabLayout.Tab tab = tabLayout.newTab();
            tab.setCustomView(R.layout.item_agent_tab);
            GetMenuBean bean = list.get(i);
            String text = bean.getName();
            View view = tab.getCustomView();
            TextView title = view.findViewById(R.id.tv_tab_title);
            title.setText(text);
            tab.view.setLongClickable(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                tab.view.setTooltipText("");
                // 遍历子视图，禁用 tooltip
                ZUtils.disableTooltipForChildViews(tab.view);
            }
            tab.view.setOnLongClickListener(null);
            tabLayout.addTab(tab);
        }
        viewPager.setCurrentItem(currentPosition);
    }

    @Override
    public void onResume() {
        super.onResume();
        TrackerUtils.trackEnterAgentPageEvent();
    }
}