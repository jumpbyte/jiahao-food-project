package com.jiahao.food.sdk.auth;

import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

/**
 * API 签名工具。
 * 公式: sign = MD5(appKey + timestamp + nonce + content + secret) 大写
 * content = 排序后的查询参数 (key1=value1&key2=value2) + 请求体（为空时默认 ""）
 */
public class SignUtil {

    public static String generateSign(String appKey, long timestamp, String nonce,
                                      Map<String, String> queryParams, String body, String secret) {
        String sortedQuery = buildSortedQueryString(queryParams);
        String content = sortedQuery + (body != null ? body : "");
        String signStr = appKey + timestamp + nonce + content + secret;
        return md5(signStr).toUpperCase();
    }

    static String buildSortedQueryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        TreeMap<String, String> sorted = new TreeMap<String, String>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            return bytesToHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
}
