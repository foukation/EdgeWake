package com.fxzs.lingxiagent.util;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.fxzs.lingxiagent.R;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * 电池优化白名单引导工具类
 * 用于引导用户将 App 加入电池优化白名单（忽略电池优化），确保后台服务不被杀死
 */
public class BatteryOptimizationHelper {

    // 常见厂商关键词（用于判断设备品牌）
    private static final List<String> HUAWEI = Arrays.asList("huawei", "honor");
    private static final List<String> XIAOMI = Arrays.asList("xiaomi", "redmi");
    private static final List<String> OPPO = Arrays.asList("oppo");
    private static final List<String> VIVO = Arrays.asList("vivo");
    private static final List<String> SAMSUNG = Arrays.asList("samsung");
    private static final List<String> MEIZU = Arrays.asList("meizu");
    private static final List<String> LETV = Arrays.asList("letv");
    private static final List<String> SMARTISAN = Arrays.asList("smartisan");

    /**
     * 检查是否已忽略电池优化
     */
    public static boolean isIgnoringBatteryOptimizations(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return pm != null && pm.isIgnoringBatteryOptimizations(context.getPackageName());
        }
        return true; // Android 6.0 以下无需处理
    }

    /**
     * 弹窗引导用户去关闭电池优化
     */
    public static void promptDisableBatteryOptimization(@NonNull Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("需要后台运行权限")
                .setMessage("为了确保“灵犀”能在后台持续监听唤醒词，请允许它在电池优化中不受限制。")
                .setPositiveButton("去设置", (dialog, which) -> openBatteryOptimizationSettings(activity))
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 尝试打开电池优化设置页面（优先跳转厂商定制页）
     */
    public static void openBatteryOptimizationSettings(@NonNull Context context) {
        String brand = getDeviceBrand().toLowerCase();

        try {
            if (HUAWEI.contains(brand)) {
                goHuaweiSetting(context);
            } else if (XIAOMI.contains(brand)) {
                goXiaomiSetting(context);
            } else if (OPPO.contains(brand)) {
                goOppoSetting(context);
            } else if (VIVO.contains(brand)) {
                goVivoSetting(context);
            } else if (SAMSUNG.contains(brand)) {
                goSamsungSetting(context);
            } else if (MEIZU.contains(brand)) {
                goMeizuSetting(context);
            } else if (LETV.contains(brand)) {
                goLetvSetting(context);
            } else if (SMARTISAN.contains(brand)) {
                goSmartisanSetting(context);
            } else {
                // 其他品牌或无法识别，使用系统通用方式
                goSystemSetting(context);
            }
        } catch (Exception e) {
            // 如果厂商方案失败，降级到系统方案
            try {
                goSystemSetting(context);
            } catch (Exception ex) {
                // 最终 fallback：提示用户手动操作
                showManualGuideDialog((Activity) context);
            }
        }
    }

    // ==================== 厂商定制跳转 ====================

    private static void goHuaweiSetting(Context context) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity"));
            intent.putExtra("packageName", context.getPackageName());
            context.startActivity(intent);
        } catch (Exception e) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.huawei.systemmanager",
                    "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"));
            context.startActivity(intent);
        }
    }

    private static void goXiaomiSetting(Context context) {
        Intent intent = new Intent("miui.intent.action.APP_PERM_EDITOR");
        intent.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity");
        intent.putExtra("extra_pkgname", context.getPackageName());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private static void goOppoSetting(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"));
        context.startActivity(intent);
    }

    private static void goVivoSetting(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.iqoo.secure",
                "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"));
        context.startActivity(intent);
    }

    private static void goSamsungSetting(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.samsung.android.sm_cn",
                "com.samsung.android.sm.ui.battery.BatteryActivity"));
        context.startActivity(intent);
    }

    private static void goMeizuSetting(Context context) {
        Intent intent = new Intent("com.meizu.safe.security.SHOW_APPSEC");
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra("packageName", context.getPackageName());
        context.startActivity(intent);
    }

    private static void goLetvSetting(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.letv.android.letvsafe",
                "com.letv.android.letvsafe.AutobootManageActivity"));
        context.startActivity(intent);
    }

    private static void goSmartisanSetting(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.smartisanos.security",
                "com.smartisanos.security.MainActivity"));
        context.startActivity(intent);
    }

    // ==================== 系统通用方案 ====================

    private static void goSystemSetting(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    // ==================== 辅助方法 ====================

    private static String getDeviceBrand() {
        return Build.BRAND;
    }

    /**
     * 显示手动设置引导（当自动跳转失败时）
     */
    private static void showManualGuideDialog(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("请手动设置")
                .setMessage("请进入手机【设置】>【电池】>【应用启动管理】，找到“灵犀”并允许后台活动。")
                .setPositiveButton("知道了", null)
                .show();
    }

    /**
     * （可选）检查某 Activity 是否存在（用于更安全的跳转）
     */
    private static boolean isActivityExists(Context context, String pkg, String cls) {
        Intent intent = new Intent();
        intent.setClassName(pkg, cls);
        return intent.resolveActivity(context.getPackageManager()) != null;
    }
}