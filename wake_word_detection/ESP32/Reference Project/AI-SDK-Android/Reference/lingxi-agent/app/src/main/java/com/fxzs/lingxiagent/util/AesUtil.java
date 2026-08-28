package com.fxzs.lingxiagent.util;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AesUtil {
    private static final byte[] SALTED;
    private static final String transformation = "AES/CBC/PKCS5Padding";
    private static final String UTF_8 = String.valueOf(StandardCharsets.UTF_8);

    static {
        SALTED = "Salted__".getBytes(StandardCharsets.US_ASCII);
    }

    public static String encrypt(String content, String key) {
        try {
            return Base64.getEncoder().encodeToString(encrypt(content.getBytes(UTF_8), key.getBytes(UTF_8)));
        } catch (Exception e) {
            return content;
        }
    }

    private static byte[] encrypt(byte[] content, byte[] key) throws Exception {
        byte[] salt = new byte[8];
        ThreadLocalRandom.current().nextBytes(salt);
        Object[] keyIv = deriveKeyAndIv(key, salt);
        Cipher cipher = Cipher.getInstance(transformation);
        cipher.init(1, new SecretKeySpec((byte[])keyIv[0], "AES"), new IvParameterSpec((byte[])keyIv[1]));
        byte[] enc = cipher.doFinal(content);
        return concat(concat(SALTED, salt), enc);
    }

    public static String aes128Encryt(String phone, String key) {
        try {
            byte[] keySec = DigestUtils.md5(key);
            SecretKeySpec sKeySpec = new SecretKeySpec(keySec, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, sKeySpec);
            byte[] result = cipher.doFinal(phone.getBytes("utf-8"));
            return Hex.encodeHexString(result).toLowerCase();
        } catch (Exception e) {
            return null;
        }
    }

    public static String decrypt(String content, String key) {
        try {
            return new String(decrypt(Base64.getDecoder().decode(content), key.getBytes(UTF_8)), UTF_8);
        } catch (Exception e) {
            return content;
        }
    }

    private static byte[] decrypt(byte[] data, byte[] passphrase) throws Exception {
        byte[] salt = Arrays.copyOfRange(data, 8, 16);
        if (Arrays.equals(Arrays.copyOfRange(data, 0, 8), SALTED)) {
            Object[] keyIv = deriveKeyAndIv(passphrase, salt);
            Cipher cipher = Cipher.getInstance(transformation);
            cipher.init(2, new SecretKeySpec((byte[])keyIv[0], "AES"), new IvParameterSpec((byte[])keyIv[1]));
            return cipher.doFinal(data, 16, data.length - 16);
        } else {
            throw new IllegalArgumentException("Invalid crypted data");
        }
    }

    private static Object[] deriveKeyAndIv(byte[] passphrase, byte[] salt) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] passSalt = concat(passphrase, salt);
        byte[] dx = new byte[0];
        byte[] di = new byte[0];

        for(int i = 0; i < 3; ++i) {
            di = md5.digest(concat(di, passSalt));
            dx = concat(dx, di);
        }

        return new Object[]{Arrays.copyOfRange(dx, 0, 32), Arrays.copyOfRange(dx, 32, 48)};
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }
}
