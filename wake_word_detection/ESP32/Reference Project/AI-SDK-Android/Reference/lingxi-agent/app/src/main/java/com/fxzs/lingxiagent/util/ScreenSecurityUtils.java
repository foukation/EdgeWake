package com.fxzs.lingxiagent.util;

import android.app.Activity;
import android.view.WindowManager;

public class ScreenSecurityUtils {

    /**
     * 禁止截屏和录屏
     */
    public static void disableScreenshot(Activity activity) {
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
    }

    /**
     * 允许截屏和录屏
     */
    public static void enableScreenshot(Activity activity) {
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }
}

