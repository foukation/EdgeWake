package com.fxzs.lingxiagent.view.common;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fxzs.lingxiagent.R;

/**
 * Chat页面基础Fragment，处理状态栏设置
 */
public abstract class BaseChatFragment extends Fragment {

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupStatusBar();
        setupStatusBarHeight();
    }

    /**
     * 设置沉浸式状态栏
     */
    private void setupStatusBar() {
        Activity activity = getActivity();
        if (activity == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = activity.getWindow();
            
            // 清除半透明状态栏标志
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            
            // 设置系统栏背景绘制标志
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            
            // 设置状态栏完全透明
            window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
            
            // 设置布局延伸到系统栏区域，状态栏文字为白色
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | 
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                );
            } else {
                window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | 
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                );
            }
        }
    }

    /**
     * 设置状态栏高度
     */
    private void setupStatusBarHeight() {
        if (getView() == null) return;
        
        View statusBarPlaceholder = getView().findViewById(R.id.status_bar_placeholder);
        if (statusBarPlaceholder != null) {
            int statusBarHeight = getStatusBarHeight();
            android.view.ViewGroup.LayoutParams params = statusBarPlaceholder.getLayoutParams();
            params.height = statusBarHeight;
            statusBarPlaceholder.setLayoutParams(params);
        }
    }

    /**
     * 获取状态栏高度
     */
    private int getStatusBarHeight() {
        if (getContext() == null) return 0;
        
        int resourceId = getContext().getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getContext().getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }
}
