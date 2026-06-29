package com.seckill.common.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5 加密工具类
 * <p>
 * 用于密码加密、接口签名等场景。
 */
public class MD5Util {

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    /**
     * MD5 加密
     */
    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    /**
     * 带盐 MD5
     */
    public static String md5WithSalt(String input, String salt) {
        return md5(input + salt);
    }

    /**
     * 两次 MD5 加密（用于密码传输）
     */
    public static String md5Twice(String input, String salt) {
        return md5(md5(input) + salt);
    }

    /**
     * 生成秒杀路径的 MD5（防刷）
     */
    public static String generateSeckillPath(long productId, String salt) {
        return md5(salt + "/" + productId + "/" + System.currentTimeMillis());
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hex = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hex[i * 2] = HEX_CHARS[v >>> 4];
            hex[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(hex);
    }
}
