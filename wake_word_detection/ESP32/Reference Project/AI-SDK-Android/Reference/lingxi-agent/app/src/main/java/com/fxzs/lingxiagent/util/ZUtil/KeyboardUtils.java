package com.fxzs.smartassist.util.ZUtil;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;

public class KeyboardUtils {

    private static int decorViewInVisibleHeightPre = 0;
    private static int decorViewDelta = 0;
    private static ViewTreeObserver.OnGlobalLayoutListener globalLayoutListener;

    /**
     * 弹出软键盘
     */
    public static void showKeyboard(EditText editText) {
        if (!editText.requestFocus()) {
            return;
        }
        InputMethodManager imm = (InputMethodManager)
                editText.getContext().getApplicationContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    /**
     * 收起软键盘
     */
    public static void hideKeyboard(View view) {
        InputMethodManager inputManager = (InputMethodManager)
                view.getContext().getApplicationContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputManager != null) {
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    /**
     * 软键盘是否弹出
     */
    public static boolean isKeyboardShown(Activity activity) {
        ViewGroup content = activity.findViewById(Window.ID_ANDROID_CONTENT);
        View child = content.getChildAt(0);
        if (child == null) {
            return false;
        }
        Rect rect = new Rect();
        child.getWindowVisibleDisplayFrame(rect);
        float density = child.getResources().getDisplayMetrics().density;
        int softKeyboardHeightDp = 100;
        int heightDiff = child.getBottom() - rect.bottom;
        return heightDiff > softKeyboardHeightDp * density;
    }

    /**==========================================================
     * 获取不可见高度（软键盘高度 + 导航栏高度差）
     *==========================================================*/
    private static int getDecorViewInVisibleHeight(Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        Rect outRect = new Rect();
        decorView.getWindowVisibleDisplayFrame(outRect);
        int delta = Math.abs(decorView.getBottom() - outRect.bottom);
        int navBar = ScreenUtils.getNavBarHeight();
        if (delta <= navBar) {
            decorViewDelta = delta;
            return 0;
        }
        return delta - decorViewDelta;
    }

    /**
     * 注册键盘高度监听器
     * @param activity 当前 Activity
     * @param onChanged 回调 height（px）
     */
    public static void registerKeyboardHeightListener(Activity activity, final OnKeyboardHeightChanged onChanged) {
        int flags = activity.getWindow().getAttributes().flags;
        if ((flags & WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS) != 0) {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
        FrameLayout content = activity.findViewById(android.R.id.content);
        decorViewInVisibleHeightPre = getDecorViewInVisibleHeight(activity);
        globalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int invisibleHeight = getDecorViewInVisibleHeight(activity);
                if (decorViewInVisibleHeightPre != invisibleHeight) {
                    onChanged.onHeightChanged(invisibleHeight);
                    decorViewInVisibleHeightPre = invisibleHeight;
                }
            }
        };
        content.getViewTreeObserver().addOnGlobalLayoutListener(globalLayoutListener);
    }

    /**
     * 注销键盘高度监听
     */
    public static void unregisterKeyboardHeightListener(Activity activity) {
        FrameLayout content = activity.findViewById(android.R.id.content);
        if (content == null || globalLayoutListener == null) {
            return;
        }
        content.getViewTreeObserver().removeOnGlobalLayoutListener(globalLayoutListener);
        globalLayoutListener = null;
    }

    /**
     * 键盘高度改变回调接口（单位 px）
     */
    public interface OnKeyboardHeightChanged {
        void onHeightChanged(int height);
    }
}
