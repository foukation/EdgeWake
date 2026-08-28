package com.fxzs.lingxiagent.model.user;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.provider.Settings;
import android.text.TextUtils;

import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.model.network.SignVerifier;
import com.fxzs.lingxiagent.util.AesUtil;

import java.util.UUID;

import timber.log.Timber;

public class UserUtil {
    private static final String TAG = "UserUtil";

    public static String formatPhone(String phone) {
        if (phone == null || phone.length() <= 11) {
            return phone;
        }
        String phoneNum = AesUtil.decrypt(phone, SignVerifier.getClientId());
        return phoneNum.substring(0, 3) + "****" + phoneNum.substring(7);
    }

    public static String formatReallyPhone(String phone) {
        if (phone == null || phone.length() <= 11) {
            return phone;
        }
        return AesUtil.decrypt(phone, SignVerifier.getClientId());
    }

    public static boolean isValidMobile(String mobile) {
        return mobile != null && mobile.matches("^1[3-9]\\d{9}$");
    }

    public static int dp2px(Context context, float dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    public static int getAppVersionCode(Context context) {
        try {
            String pkgName = context.getPackageName();
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(pkgName, 0);
            return packageInfo.versionCode;
        } catch (Exception e) {
            Timber.tag(TAG).e(e);
        }
        return 1;
    }

    public static String getAppVersionName(Context context) {
        try {
            String pkgName = context.getPackageName();
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(pkgName, 0);
            return packageInfo.versionName;
        } catch (Exception e) {
            Timber.tag(TAG).e(e);
        }
        return "1.0.0";
    }

    /**
     * 获取密码校验信息
     * @param password 密码
     * @return 校验结果信息，如果密码有效则返回null
     */
    public static String verifyPassword(String password) {
        /* if (password == null || password.isEmpty()) {
            return "请输入密码";
        }

        // 长度校验（优先反馈基础规则）
        if (password.length() < Constants.PASSWORD_MIN_LEN || password.length() > Constants.PASSWORD_MAX_LEN) {
            return "密码长度需在" + Constants.PASSWORD_MIN_LEN + "-" + Constants.PASSWORD_MAX_LEN + "位之间";
        }

        // 字符类型校验（合并提示，不暴露具体缺失项）
        String validPattern = "^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?~`]+$";
        if (!password.matches(validPattern)) {
            return "密码仅支持字母、数字及常用特殊字符";
        }

        // 复杂度校验（合并核心要求，不单独提示缺失项）
        boolean hasUpperCase = password.matches(".*[A-Z].*");
        boolean hasLowerCase = password.matches(".*[a-z].*");
        boolean hasSpecialChar = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?~`].*");
        boolean hasDigit = password.matches(".*\\d.*"); // 补充数字校验，提升复杂度

        if (!(hasUpperCase && hasLowerCase && hasSpecialChar && hasDigit)) {
            return "密码需同时包含大写字母、小写字母、数字及特殊字符";
        }

        return null; // 密码有效*/

        if (password == null || password.isEmpty()) {
            return "请输入密码";
        }

        String validPattern = "^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?~`]+$";

        boolean isLengthValid = password.length() >= Constants.PASSWORD_MIN_LEN && password.length() <= Constants.PASSWORD_MAX_LEN;
        boolean isCharValid = password.matches(validPattern);
        boolean hasUpperCase = password.matches(".*[A-Z].*");
        boolean hasLowerCase = password.matches(".*[a-z].*");
        boolean hasSpecialChar = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?~`].*");
        boolean hasDigit = password.matches(".*\\d.*");

        if (isLengthValid && isCharValid && hasUpperCase && hasLowerCase && hasSpecialChar && hasDigit) {
            return null;
        } else {
            return "密码不符合要求，请检查";
        }
    }
}