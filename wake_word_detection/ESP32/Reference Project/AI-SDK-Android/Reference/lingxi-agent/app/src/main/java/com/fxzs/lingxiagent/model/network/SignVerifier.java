package com.fxzs.lingxiagent.model.network;

import com.fxzs.lingxiagent.model.common.BaseResponse;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.util.SignatureUtil;
import com.fxzs.lingxiagent.BuildConfig;
import timber.log.Timber;
/**
 * 创建者：ZyOng
 * 描述：
 * 创建时间：2025/11/21 下午12:42
 */
public class SignVerifier {

    /**
     * 全局通用签名校验方法
     *
     * @param raw 原始响应字符串，例如 response.body().toString()
     * @return true = 校验成功，false = 校验失败
     */
    public static boolean verify(String raw) {
        // 1. 根据当前构建类型选择秘钥
        String secret = getClientId();
        Timber.tag("SignVerify").e("secret: %s", secret);
        // 2. 计算签名
        String sign = SignatureUtil.setMd5Signature(raw + secret);
        Timber.tag("SignVerify").e("生成签名: %s", sign);

        // 3. 与服务端固定签名对比
        return sign != null && sign.equals(Constants.X_AI_SIGN);
    }


    /**
     * 校验失败时返回一个通用错误结构，方便外部直接使用
     */
    public static <T> BaseResponse<T> buildErrorResponse() {
        BaseResponse<T> base = new BaseResponse<>();
        base.setMessage("");
        return base;
    }

    public static String getClientId(){
        return  BuildConfig.FLAVOR.contains("tablet")
                ? Constants.X_SECRET_PAD
                : Constants.KEY_ALIAS;
    }
}

