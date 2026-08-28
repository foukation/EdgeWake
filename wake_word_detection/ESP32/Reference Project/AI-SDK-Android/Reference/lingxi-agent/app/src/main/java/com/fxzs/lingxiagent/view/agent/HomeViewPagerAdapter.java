package com.fxzs.lingxiagent.view.agent;


import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

public class HomeViewPagerAdapter extends FragmentStateAdapter {
    private final List<Fragment> mFragments;


    public HomeViewPagerAdapter(@NonNull FragmentActivity fa, List<Fragment> fragments) {
        super(fa);
        mFragments = fragments;
    }
    public HomeViewPagerAdapter(@NonNull Fragment fragment, List<Fragment> fragments) {
        super(fragment);
        mFragments = fragments;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return mFragments.get(position);
    }

    @Override
    public int getItemCount() {
        return mFragments.size();
    }
}