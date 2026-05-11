package com.jiahao.food.sdk.auth;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class SignUtilTest {

    @Test
    public void generateSign_withNoParams_shouldProduceUppercaseMd5() {
        String sign = SignUtil.generateSign("demo", 1234567890L, "test-nonce", null, null, "secret");
        assertEquals(32, sign.length());
        assertEquals(sign, sign.toUpperCase());
    }

    @Test
    public void generateSign_withParams_shouldSortByKey() {
        Map<String, String> params = new HashMap<String, String>();
        params.put("cityId", "100");
        params.put("provinceId", "1");

        String sign1 = SignUtil.generateSign("demo", 1234567890L, "nonce", params, null, "secret");

        // 相同参数不同顺序应产生相同签名
        Map<String, String> paramsReversed = new HashMap<String, String>();
        paramsReversed.put("provinceId", "1");
        paramsReversed.put("cityId", "100");

        String sign2 = SignUtil.generateSign("demo", 1234567890L, "nonce", paramsReversed, null, "secret");
        assertEquals(sign1, sign2);
    }

    @Test
    public void generateSign_withBody_shouldIncludeInContent() {
        String sign1 = SignUtil.generateSign("demo", 1234567890L, "nonce", null, null, "secret");
        String sign2 = SignUtil.generateSign("demo", 1234567890L, "nonce", null, "", "secret");
        // null body 和 "" body 应产生相同签名
        assertEquals(sign1, sign2);

        String sign3 = SignUtil.generateSign("demo", 1234567890L, "nonce", null, "{\"key\":\"value\"}", "secret");
        assertNotEquals(sign1, sign3);
    }

    @Test
    public void generateSign_differentSecret_shouldProduceDifferentSign() {
        String sign1 = SignUtil.generateSign("demo", 1234567890L, "nonce", null, null, "secret1");
        String sign2 = SignUtil.generateSign("demo", 1234567890L, "nonce", null, null, "secret2");
        assertNotEquals(sign1, sign2);
    }

    @Test
    public void buildSortedQueryString_withEmptyMap_shouldReturnEmpty() {
        assertEquals("", SignUtil.buildSortedQueryString(new HashMap<String, String>()));
    }

    @Test
    public void buildSortedQueryString_withNull_shouldReturnEmpty() {
        assertEquals("", SignUtil.buildSortedQueryString(null));
    }

    @Test
    public void buildSortedQueryString_shouldSortByKeyAscending() {
        Map<String, String> params = new HashMap<String, String>();
        params.put("cityId", "100");
        params.put("provinceId", "1");
        params.put("areaId", "50");

        String result = SignUtil.buildSortedQueryString(params);
        assertEquals("areaId=50&cityId=100&provinceId=1", result);
    }
}
