package com.fxzs.lingxiagent.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.fxzs.lingxiagent.model.auth.AuthHelper;
import com.fxzs.lingxiagent.view.auth.OneClickLoginActivity;
import com.fxzs.lingxiagent.view.common.GlobalToast;

import timber.log.Timber;

/**
 * 语音按钮安全处理工具类
 * 提供完整的错误处理和空值检查，避免语音功能相关的崩溃
 */
public class VoiceButtonSafetyUtil {
    private static final String TAG = "VoiceButtonSafetyUtil";
    
    /**
     * 安全的语音按钮点击处理
     * @param context 上下文环境
     * @param callback 回调接口
     * @param switchModeCallback 模式切换回调
     * @return 是否成功处理点击事件
     */
    public static boolean handleVoiceButtonClick(Context context, Runnable callback, Runnable switchModeCallback) {
        try {
            // 1. 检查基本参数
            if (context == null) {
                Timber.tag(TAG).e( "Context is null, cannot handle voice button click");
                return false;
            }
            
            // 2. 检查登录状态
            if (!isUserLoggedInSafely()) {
                Timber.tag(TAG).d( "User not logged in, redirecting to login page");
                redirectToLogin(context);
                return false;
            }
            
            // 3. 检查网络状态
            if (!NetworkUtils.isNetworkAvailable(context)) {
                showNetworkError(context);
                return false;
            }
            
            // 4. 执行模式切换回调
            if (switchModeCallback != null) {
                try {
                    switchModeCallback.run();
                } catch (Exception e) {
                    Timber.tag(TAG).e( "Error in switch mode callback", e);
                    showGenericError(context);
                    return false;
                }
            }
            
            // 5. 执行主要回调
            if (callback != null) {
                try {
                    callback.run();
                } catch (Exception e) {
                    Timber.tag(TAG).e( "Error in voice callback", e);
                    showGenericError(context);
                    return false;
                }
            }
            
            Timber.tag(TAG).d( "Voice button click handled successfully");
            return true;
            
        } catch (Exception e) {
            Timber.tag(TAG).e( "Unexpected error in handleVoiceButtonClick", e);
            showGenericError(context);
            return false;
        }
    }
    
    /**
     * 安全检查用户登录状态
     */
    private static boolean isUserLoggedInSafely() {
        try {
            AuthHelper authHelper = AuthHelper.getInstance();
            if (authHelper == null) {
                Timber.tag(TAG).w( "AuthHelper instance is null");
                return false;
            }
            return authHelper.isLogin();
        } catch (Exception e) {
            Timber.tag(TAG).e( "Error checking login status", e);
            return false;
        }
    }
    
    /**
     * 安全跳转到登录页面
     */
    private static void redirectToLogin(Context context) {
        try {
            Intent intent = new Intent(context, OneClickLoginActivity.class);
            context.startActivity(intent);
        } catch (Exception e) {
            Timber.tag(TAG).e( "Error redirecting to login", e);
            showGenericError(context);
        }
    }
    
    /**
     * 显示网络错误提示
     */
    private static void showNetworkError(Context context) {
        try {
            GlobalToast.show((Activity) context, "网络错误，请检查网络连接", GlobalToast.Type.ERROR);
        } catch (Exception e) {
            Timber.tag(TAG).e( "Error showing network error toast", e);
        }
    }
    
    /**
     * 显示通用错误提示
     */
    public static void showGenericError(Context context) {
        try {
            GlobalToast.show((Activity) context, "操作失败，请稍后重试", GlobalToast.Type.ERROR);
        } catch (Exception e) {
            Timber.tag(TAG).e( "Error showing generic error toast", e);
        }
    }
    
    /**
     * 安全的View可见性设置
     */
    public static void setViewVisibilitySafely(View view, int visibility) {
        try {
            if (view != null) {
                view.setVisibility(visibility);
            } else {
                Timber.tag(TAG).w("Attempted to set visibility on null view");
            }
        } catch (Exception e) {
            Timber.tag(TAG).e( "Error setting view visibility", e);
        }
    }
    
    /**
     * 安全的findViewById
     */
    public static <T extends View> T findViewByIdSafely(View parent, int id, Class<T> type) {
        try {
            if (parent == null) {
                Timber.tag(TAG).w( "Parent view is null for findViewById");
                return null;
            }
            
            View foundView = parent.findViewById(id);
            if (foundView == null) {
                Timber.tag(TAG).w( "View with id " + id + " not found");
                return null;
            }
            
            if (type.isInstance(foundView)) {
                return type.cast(foundView);
            } else {
                Timber.tag(TAG).w("Found view is not of expected type " + type.getSimpleName());
                return null;
            }
            
        } catch (Exception e) {
            Timber.tag(TAG).e( "Error in findViewById", e);
            return null;
        }
    }
    
    /**
     * 检查必要的视图是否都已初始化
     */
    public static boolean validateRequiredViews(View... views) {
        try {
            if (views == null || views.length == 0) {
                Timber.tag(TAG).w("No views to validate");
                return false;
            }
            
            for (int i = 0; i < views.length; i++) {
                if (views[i] == null) {
                    Timber.tag(TAG).w("Required view at index " + i + " is null");
                    return false;
                }
            }
            
            Timber.tag(TAG).d( "All required views validated successfully");
            return true;
            
        } catch (Exception e) {
            Timber.tag(TAG).e( "Error validating views", e);
            return false;
        }
    }
}