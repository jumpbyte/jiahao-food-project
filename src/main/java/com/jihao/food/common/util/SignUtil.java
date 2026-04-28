package com.jihao.food.common.util;

import org.apache.commons.codec.digest.DigestUtils;

public final class SignUtil {

    private SignUtil() {}

    /**
     * 生成 API 签名
     * 公式: sign = MD5(appKey + timestamp + nonce + body + secret)
     */
    public static String generateSign(String appKey, long timestamp, String nonce, String body, String secret) {
        String signStr = appKey + timestamp + nonce + body + secret;
        return DigestUtils.md5Hex(signStr).toUpperCase();
    }

    /**
     * 校验签名
     */
    public static boolean verifySign(String appKey, long timestamp, String nonce, String body, String secret, String sign) {
        String expected = generateSign(appKey, timestamp, nonce, body, secret);
        return expected.equals(sign);
    }

    /**
     * 校验时间戳是否在允许的时间窗口内
     */
    public static boolean isWithinTimeWindow(long timestamp, long timeWindowMs) {
        long now = System.currentTimeMillis();
        return Math.abs(now - timestamp) <= timeWindowMs;
    }
}
