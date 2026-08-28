package com.fxzs.lingxiagent.view.user;

import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.fxzs.lingxiagent.R;

import timber.log.Timber;

public class UserSettingsActivity extends UserAppSettingsActivity {

    protected int getLayoutResource() {
        return R.layout.activity_user_settings;
    }


    @Override
    protected void onResume() {
        super.onResume();
        adjustRootLayout();
        resetStatusBar();
        setTransparentStatusBar();
    }


    @Override
    public void resetStatusBar() {
        super.resetStatusBar();
        getWindow().getDecorView().postDelayed(() -> {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            }, 100);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            );
        }
    }

    /**
     * 重置状态栏样式（白色背景+黑色文字）
     */

    // 底部导航栏沉浸式
    private void setTransparentStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION); // 清除旧 flag
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);  // 导航栏透明

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |  // 布局延伸到导航栏下方
                                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                );
            } else {
                window.getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                );
            }
        }
    }

    protected void adjustRootLayout() {
        ViewGroup content = findViewById(android.R.id.content);
        Timber.tag("BaseActivity").d(" 手机=%s", isTablet());
        if (content == null || content.getChildCount() == 0) return;

        ViewGroup rootLayout = (ViewGroup) content.getChildAt(0);
        if (rootLayout == null) return;

        // 手机全屏
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        rootLayout.setLayoutParams(params);
    }
}