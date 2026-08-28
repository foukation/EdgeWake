package com.fxzs.lingxiagent.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.content.ContextCompat;
import com.fxzs.lingxiagent.lingxi.float_manager.FloatWindowHelper;
import com.fxzs.lingxiagent.service.WakeUpService;
import com.fxzs.lingxiagent.view.common.CommonDialog;

public class WakeUpPermissionHelper {

    public static final int REQUEST_CODE_RECORD_AUDIO = 1004;
    public static final int REQUEST_CODE_POST_NOTIFICATIONS = 1003;
    public static final int REQUEST_CODE_FLOATING = 1001;
    public static final int REQUEST_CODE_FLOATING_WINDOW = 1002;
    public static final int REQUEST_CODE_BATTERY_OPT = 1002;
    public static final String PREF_NAME = "user_settings";
    public static final String KEY_WAKEUP_ENABLED = "wakeup_enabled";
    public static final String KEY_WAKEUP_POWER_ENABLED = "wakeup_power_enabled";
    public static final String KEY_WAKEUP_KEYBORD_ENABLED = "wakeup_keyboard_enabled";
    /**
     * 检查所有唤醒所需权限是否已授予
     */
    public static boolean checkAllPermissionsGranted(Context context) {
        boolean hasRecordAudio = checkRecordAudioPermission(context);
        boolean hasFloating = checkFloatingWindowPermission(context);
        boolean hasNotification = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || checkPostNotifications(context);

        return hasRecordAudio && hasFloating && hasNotification;
    }

    /**
     * 检查是否关闭电池优化（最关键！）
     */
    public static boolean checkBatteryOptimization(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.os.PowerManager pm = (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return pm != null && pm.isIgnoringBatteryOptimizations(context.getPackageName());
        }
        return true;
    }

    /**
     * 检查悬浮窗权限（TYPE_APPLICATION_OVERLAY）
     */
    public static boolean checkFloatingWindowPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true; // Android 6.0 以下默认允许
    }

    /**
     * Android 13+ 需要通知权限才能显示前台服务通知
     */
    public static boolean checkPostNotifications(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    // WakeUpPermissionHelper.java

    /**
     * 尝试按顺序引导缺失权限，并在全部满足后启动唤醒服务
     * @param activity 需要弹窗的 Activity
     * @param onAllGranted 完全授权后的回调（可选）
     */
    public static void promptAndEnableWakeUpIfPossible(Activity activity, Runnable onAllGranted) {
        // 1. 检查录音
        if (!checkRecordAudioPermission(activity)) {
//            ActivityCompat.requestPermissions(activity,
//                    new String[]{Manifest.permission.RECORD_AUDIO},
//                    REQUEST_CODE_RECORD_AUDIO);
            AppPermissionRequestManager.requestAudioPermission(activity, REQUEST_CODE_RECORD_AUDIO, AppPermissionRequestManager.PERMISSION_AUDIO_MESSAGE_WAKEUP);
            return;
        }

        // 2. 检查通知（Android 13+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !checkPostNotifications(activity)) {
//            ActivityCompat.requestPermissions(activity,
//                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
//                    REQUEST_CODE_POST_NOTIFICATIONS);
            AppPermissionRequestManager.requestNotificationsPermission(activity, REQUEST_CODE_POST_NOTIFICATIONS,"请授权APP通知权限，以唤醒服务可以正常使用");
            return;
        }

        // 3. 检查浮窗
        if (!checkFloatingWindowPermission(activity)) {
            AppPermissionRequestManager.requestOverlayPermissionDialog(activity, new CommonDialog.OnDialogClickListener() {
                @Override
                public void onConfirm() {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    intent.setData(Uri.parse("package:" + activity.getPackageName()));
                    activity.startActivityForResult(intent, REQUEST_CODE_FLOATING);
                }
                @Override
                public void onCancel() {
                }
            });
            return;
        }

        // ✅ 全部满足！
        toggleWakeUpService(activity, true);
        if (onAllGranted != null) {
            onAllGranted.run();
        }
    }
    /**
     * 检查录音权限（语音唤醒的基础）
     */
    public static boolean checkRecordAudioPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 检查唤醒服务是否正在运行
     */
    public static boolean isWakeUpServiceRunning(Context context) {
        android.app.ActivityManager activityManager =
                (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        if (activityManager != null) {
            for (android.app.ActivityManager.RunningServiceInfo service :
                    activityManager.getRunningServices(Integer.MAX_VALUE)) {
                if (WakeUpService.class.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 根据开关状态启动或停止唤醒服务
     */
    public static void toggleWakeUpService(Context context, boolean enable) {
        Intent intent = new Intent(context, WakeUpService.class);

        if (enable) {
            if (isWakeUpServiceRunning(context)) {
                android.util.Log.d("WakeUpPermissionHelper", "WakeUpService 已在运行，无需重复启动");
                return;
            }

            android.util.Log.d("WakeUpPermissionHelper", "Starting WakeUpService");
            ContextCompat.startForegroundService(context, intent);
        } else {
            android.util.Log.d("WakeUpPermissionHelper", "Stopping WakeUpService");
            context.stopService(intent);
        }
    }

    /** 聆听模式开始录音：暂停 DuWakeup，释放麦克风 */
    public static void pauseWakeUpForListenMode(Context context) {
        if (!isWakeUpServiceRunning(context)) {
            return;
        }
        Intent intent = new Intent(context, WakeUpService.class);
        intent.setAction(WakeUpService.ACTION_PAUSE_WAKEUP);
        context.startService(intent);
    }

    /** 聆听模式结束录音：恢复 DuWakeup */
    public static void resumeWakeUpAfterListenMode(Context context) {
        if (!isWakeUpServiceRunning(context)) {
            return;
        }
        Intent intent = new Intent(context, WakeUpService.class);
        intent.setAction(WakeUpService.ACTION_RESUME_WAKEUP);
        context.startService(intent);
    }
    // 外部APP调用代码
    public static void startWakeUpServiceAndShowFloat(Context context) {
        FloatWindowHelper.showFloatWindow(context, FloatWindowHelper.createDefaultCallback(context));
    }

    /**
     * 获取用户是否曾开启语音唤醒
     */
    public static boolean isWakeUpEnabled(Context context) {
        // 跨进程读取：必须和写入保持同一个 MODE_MULTI_PROCESS
        return context.getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS)
                .getBoolean(KEY_WAKEUP_ENABLED, false);
    }

    /**
     * 保存语音唤醒开关状态
     */
    public static void setWakeUpEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS)
                .edit()
                .putBoolean(KEY_WAKEUP_ENABLED, enabled)
                .apply();
        sendSwitchBroadcast(context);
    }

    // ========== 内部读取：永远以 SP 为准（最准）==========
    public static boolean isWakeUpPowerEnabled(Context context) {
        // 跨进程读取
        return context.getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS)
                .getBoolean(KEY_WAKEUP_POWER_ENABLED, false);
    }

    // ========== 写入：SP + 文件 双写（强一致）==========
    public static void setWakeUpPowerEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS)
                .edit()
                .putBoolean(KEY_WAKEUP_POWER_ENABLED, enabled)
                .apply();
        sendSwitchBroadcast(context);
    }

    public static boolean isWakeUpKeyBordEnabled(Context context) {
        // 跨进程读取
        return context.getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS)
                .getBoolean(KEY_WAKEUP_KEYBORD_ENABLED, false);
    }

    public static void setKeyWakeupKeybordEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS)
                .edit()
                .putBoolean(KEY_WAKEUP_KEYBORD_ENABLED, enabled)
                .apply();

        sendSwitchBroadcast(context);
    }
    public static void sendSwitchBroadcast(Context context) {
        Intent intent = new Intent("SWITCH_STATE_CHANGED");
        intent.putExtra("wakeup", isWakeUpEnabled(context));
        intent.putExtra("power", isWakeUpPowerEnabled(context));
        intent.putExtra("keyboard", isWakeUpKeyBordEnabled(context));
        context.sendBroadcast(intent);
    }

}