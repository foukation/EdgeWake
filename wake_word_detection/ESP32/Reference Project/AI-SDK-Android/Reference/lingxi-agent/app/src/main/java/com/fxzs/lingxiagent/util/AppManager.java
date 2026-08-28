package com.fxzs.lingxiagent.util;

import android.app.Activity;

import java.util.ArrayList;
import java.util.List;

/**
 * 创建者：ZyOng
 * 描述：
 * 创建时间：2026/3/9 11:17
 */
public class AppManager {
    private static List<Activity> activityList = new ArrayList<>();

    public static void addActivity(Activity activity) {
        activityList.add(activity);
    }

    public static void removeActivity(Activity activity) {
        activityList.remove(activity);
    }

    // 结束特定 Activity
    public static void finishActivity(Class<?> cls) {
        for (Activity activity : activityList) {
            if (activity.getClass().equals(cls)) {
                activity.finish();
                break; // 如果只想结束一个
            }
        }
    }

    // 结束所有 Activity
    public static void finishAll() {
        for (Activity activity : activityList) {
            if (!activity.isFinishing()) {
                activity.finish();
            }
        }
        activityList.clear();
    }

    public static boolean isActivityInStack(Class<?> cls) {
        for (Activity activity : activityList) {
            if (activity.getClass().equals(cls)) {
                return true;
            }
        }
        return false;
    }

    public static Activity getActivityInStack(Class<?> cls) {
        for (Activity activity : activityList) {
            if (activity.getClass().equals(cls)) {
                return activity;
            }
        }
        return null;
    }
}
