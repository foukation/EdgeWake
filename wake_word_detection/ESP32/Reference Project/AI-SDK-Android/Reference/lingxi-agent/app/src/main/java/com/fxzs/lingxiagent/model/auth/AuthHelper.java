package com.fxzs.lingxiagent.model.auth;

import android.content.Context;
import android.os.Build;

import com.cmic.sso.sdk.auth.AuthnHelper;
import com.cmic.sso.sdk.auth.TokenListener;
import com.fxzs.lingxiagent.BuildConfig;
import com.fxzs.lingxiagent.model.auth.repository.AuthRepository;
import com.fxzs.lingxiagent.model.auth.repository.AuthRepositoryImpl;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class AuthHelper {
    private AuthnHelper mAuthnHelper;
    private AuthRepository authRepository;
    private String packageName;

    private AuthHelper() {}

    private static final class Holder {
        private static final AuthHelper INSTANCE = new AuthHelper();
    }

    public static AuthHelper getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * 初始化鉴权工具类
     * @param context 上下文环境
     */
    public void init(Context context) {
        mAuthnHelper = AuthnHelper.getInstance(context);
        //设置是否输出sdk日志
        mAuthnHelper.setDebugMode(BuildConfig.DEBUG);
        //设置Appid和Appkey
        mAuthnHelper.init(Constants.APP_ID, Constants.APP_KEY);
        //设置超时时间，默认8s，时间单位毫秒
        mAuthnHelper.setTimeOut(12000);
        authRepository = new AuthRepositoryImpl();

        // APP启动后刷新token
//        refreshToken();

        packageName = context.getPackageName();
    }



    /**
     * 一键登录预取号。将返回用户手机号码脱敏中间四位的掩码字段，业务可以使用该字段构建授权页面。
     * 注意：第一次调用本方法时，用户手机终端须打开数据网络
     * @param listener 请求回调接口
     */
    public void umcLoginPre(TokenListener listener) {
        // 是否使用缓存取号，缓存有效期为2个月。为保证成功率，建议填true
        // 用户手机终端在变更数据流量SIM卡时，缓存将自动失效重新做数据流量取号。
        if ("1".equals(getOperatorType())) {
            mAuthnHelper.umcLoginPre(listener, true);
        }
    }

    /**
     * 获取运营商类型。0=未知，1=移动，2=联通，3=电信
     * @return 运营商类型
     */
    public String getOperatorType() {
        String operatorType = "0";
        JSONObject networkJson = getNetworkType();
        if (networkJson != null && networkJson.has("operatortype")) {
            operatorType = networkJson.optString("operatortype");
        }
        return operatorType;
    }

    /**
     * 获取品牌信息。获取当前手机号码对应的品牌信息,业务可以使用该字段构建授权页面
     */
    public void getBrandInfo(TokenListener listener) {
        mAuthnHelper.getBrandInfo(listener);
    }

    /**
     * 取号请求成功后，在用户授权操作后，调用授权请求方法，获取取号token。
     * 注意：务必在授权页上得到用户的授权同意才允许调用此授权请求方法！
     * @param listener 请求回调接口
     */
    public void getTokenImp(TokenListener listener) {
        mAuthnHelper.getTokenImp(AuthnHelper.AUTH_TYPE_WAP, listener, true);
    }

    /**
     * 获取短信验证码。目前短信验证码只支持移动登录
     * @param phoneNum 手机号码
     */
    public void sendSMS(String phoneNum, TokenListener listener) {
        mAuthnHelper.sendSMS(phoneNum, listener);
    }

    /**
     * 短信验证码登录。用于校验通过sendSMS获取短信验证码方法获取到的短验消息
     * @param phoneNum 手机号码
     * @param authCode 验证码
     */
    public void getTokenSms(String phoneNum, String authCode, TokenListener listener) {
        mAuthnHelper.getTokenSms(phoneNum, authCode, listener);
    }

    /**
     * 清除缓存信息。
     * 当不想使用缓存信息进行登录的时候，可以使用该方法将缓存信息清除，然后再进行登录操作。
     * 缓存清除后，用户终端如需重新登录，须打开手机数据网络。
     */
    public void clearCache() {
        mAuthnHelper.clearCache();
    }

    /**
     * 获取网络状态和运营商类型。
     * operatortype：运营商类型：0.未知；1.移动流量；2.联通流量；3.电信流量
     * networktype：网络类型：0.未知；1.流量；2.wifi；3.数据流量+wifi
     * @return 网络状态和运营商类型
     */
    public JSONObject getNetworkType() {
        if (mAuthnHelper == null){
            return null;
        }
        return mAuthnHelper.getNetworkType();
    }

    /**
     * 超时设置。默认为8秒，在预取号、隐式登录阶段时，如果需要更改超时时间，可使用该方法配置。
     * 受配置影响的SDK方法包括：umcLoginPre、getTokenImp、sendSMS、getTokenSms
     * @param timeout 超时时间（单位：毫秒）
     */
    public void setTimeOut(int timeout) {
        mAuthnHelper.setTimeOut(timeout);
    }

    // 获取 SDK版本号
    public String getCoreSdkVersion() {
        return mAuthnHelper.getCoreSdkVersion();
    }

    public boolean isLogin() {
        String token = SharedPreferencesUtil.getToken();
        long expires = SharedPreferencesUtil.getExpires();
        // 存在token，且未过期
        return !token.isEmpty() && expires > System.currentTimeMillis();
    }

    public void refreshToken() {
        String refreshToken = SharedPreferencesUtil.getRefreshToken();
        if (!refreshToken.isEmpty()) {
            authRepository.refreshToken(refreshToken);
        }
    }

    // 生成签名
    public String getSignatureSHA256() {
        String brand = Build.BRAND;
        // 处理品牌名称的特殊情况
        if (brand == null) {
            brand = "unknown";
        }
        // 拼接原始字符串
        String rawString = packageName + brand + System.currentTimeMillis();

        try {
            // 创建 SHA-256 哈希
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawString.getBytes(StandardCharsets.UTF_8));

            // 将字节数组转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (Exception e) {
            return rawString;
        }
    }
}
