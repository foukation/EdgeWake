package com.fxzs.lingxiagent.util;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import timber.log.Timber;

public class SignatureGenerator {
    // 对应JavaScript中的APP_SECRET
    private static final String APP_SECRET = "ASSURANCE_INFORMATION_STATE_APP";
    // 固定的拼接字符串
    private static final String FIXED_STRING = "74960c687e50a3b00c9b3542c1d6143b0d93bcc333f0eb4890e349e517542c2";

    public static String generateSignature(String reqId, String deviceId) {
        // 构建消息字符串，与JavaScript中msg的生成方式一致
        String msg = reqId + deviceId;
        Timber.i(msg);

        try {
            // 创建HMAC-SHA256实例
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    APP_SECRET.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            hmacSha256.init(secretKey);

            // 计算HMAC
            byte[] hmacBytes = hmacSha256.doFinal(msg.getBytes(StandardCharsets.UTF_8));

            // 转换为小写十六进制字符串
            return bytesToHex(hmacBytes).toLowerCase();

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            // 处理异常，实际应用中可能需要更完善的异常处理
            throw new RuntimeException("生成签名失败", e);
        }
    }

    // 将字节数组转换为十六进制字符串
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
