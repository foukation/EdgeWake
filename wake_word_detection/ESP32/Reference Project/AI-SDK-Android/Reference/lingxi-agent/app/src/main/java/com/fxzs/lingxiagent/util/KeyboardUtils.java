package com.fxzs.lingxiagent.util;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver;

/**
 * 键盘工具类，用于监听键盘弹起和收起事件
 * 特别适用于需要动态调整布局的场景
 */
public class KeyboardUtils {
    
    private static final int KEYBOARD_THRESHOLD_DP = 200; // 键盘检测阈值
    
    /**
     * 键盘状态监听接口
     */
    public interface OnKeyboardToggleListener {
        /**
         * 键盘状态改变回调
         * @param isVisible 键盘是否可见
         * @param keyboardHeight 键盘高度（像素）
         * @param availableHeight 可用高度（像素）
         */
        void onKeyboardToggle(boolean isVisible, int keyboardHeight, int availableHeight);
    }
    
    /**
     * 为Activity添加键盘监听
     * @param activity 目标Activity
     * @param listener 键盘状态监听器
     * @return ViewTreeObserver.OnGlobalLayoutListener 返回监听器，用于后续移除
     */
    public static ViewTreeObserver.OnGlobalLayoutListener addKeyboardToggleListener(
            Activity activity, OnKeyboardToggleListener listener) {
        
        final View rootView = activity.findViewById(android.R.id.content);
        final ViewTreeObserver.OnGlobalLayoutListener layoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            private boolean wasKeyboardVisible = false;
            private int previousHeight = 0;
            
            @Override
            public void onGlobalLayout() {
                Rect rect = new Rect();
                rootView.getWindowVisibleDisplayFrame(rect);
                
                int screenHeight = rootView.getRootView().getHeight();
                int currentHeight = rect.height();
                int keyboardHeight = screenHeight - currentHeight;
                
                // 转换阈值为像素
                int thresholdPx = (int) (KEYBOARD_THRESHOLD_DP * activity.getResources().getDisplayMetrics().density);
                
                boolean isKeyboardVisible = keyboardHeight > thresholdPx;
                
                // 只有状态改变时才触发回调
                if (isKeyboardVisible != wasKeyboardVisible || Math.abs(currentHeight - previousHeight) > thresholdPx) {
                    wasKeyboardVisible = isKeyboardVisible;
                    previousHeight = currentHeight;
                    
                    if (listener != null) {
                        listener.onKeyboardToggle(isKeyboardVisible, keyboardHeight, currentHeight);
                    }
                }
            }
        };
        
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
        return layoutListener;
    }
    
    /**
     * 移除键盘监听
     * @param activity 目标Activity
     * @param listener 要移除的监听器
     */
    public static void removeKeyboardToggleListener(Activity activity, 
            ViewTreeObserver.OnGlobalLayoutListener listener) {
        if (listener != null) {
            View rootView = activity.findViewById(android.R.id.content);
            rootView.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
        }
    }
    
    /**
     * 设置Activity的软键盘模式
     * @param activity 目标Activity
     * @param mode 软键盘模式，如 WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
     */
    public static void setSoftInputMode(Activity activity, int mode) {
        activity.getWindow().setSoftInputMode(mode);
    }
    
    /**
     * 隐藏软键盘
     * @param activity 目标Activity
     */
    public static void hideSoftKeyboard(Activity activity) {
        View currentFocus = activity.getCurrentFocus();
        if (currentFocus != null) {
            android.view.inputmethod.InputMethodManager imm = 
                (android.view.inputmethod.InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            }
        }
    }
    
    /**
     * 显示软键盘
     * @param activity 目标Activity
     * @param view 要获取焦点的View
     */
    public static void showSoftKeyboard(Activity activity, View view) {
        if (view != null) {
            view.requestFocus();
            android.view.inputmethod.InputMethodManager imm = 
                (android.view.inputmethod.InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }
    
    /**
     * 检查键盘是否当前可见
     * @param activity 目标Activity
     * @return true如果键盘可见
     */
    public static boolean isKeyboardVisible(Activity activity) {
        View rootView = activity.findViewById(android.R.id.content);
        Rect rect = new Rect();
        rootView.getWindowVisibleDisplayFrame(rect);
        
        int screenHeight = rootView.getRootView().getHeight();
        int keyboardHeight = screenHeight - rect.height();
        int thresholdPx = (int) (KEYBOARD_THRESHOLD_DP * activity.getResources().getDisplayMetrics().density);
        
        return keyboardHeight > thresholdPx;
    }
    
    /**
     * 获取当前键盘高度
     * @param activity 目标Activity
     * @return 键盘高度（像素）
     */
    public static int getKeyboardHeight(Activity activity) {
        View rootView = activity.findViewById(android.R.id.content);
        Rect rect = new Rect();
        rootView.getWindowVisibleDisplayFrame(rect);
        
        int screenHeight = rootView.getRootView().getHeight();
        return screenHeight - rect.height();
    }
    
    /**
     * dp转px
     */
    public static int dpToPx(Activity activity, float dp) {
        return (int) (dp * activity.getResources().getDisplayMetrics().density);
    }
    
    /**
     * px转dp
     */
    public static float pxToDp(Activity activity, int px) {
        return px / activity.getResources().getDisplayMetrics().density;
    }
}
