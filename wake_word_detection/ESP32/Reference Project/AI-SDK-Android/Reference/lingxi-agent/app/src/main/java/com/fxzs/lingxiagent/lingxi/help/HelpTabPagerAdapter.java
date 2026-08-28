package com.fxzs.lingxiagent.lingxi.help;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

public class HelpTabPagerAdapter extends FragmentStateAdapter {
    private final List<HelpTabConfig> tabs;

    public HelpTabPagerAdapter(@NonNull FragmentActivity fragmentActivity, List<HelpTabConfig> tabs) {
        super(fragmentActivity);
        this.tabs = tabs;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return HelpTabFragment.newInstance(tabs.get(position).getContent());
    }

    @Override
    public int getItemCount() {
        return tabs.size();
    }
}