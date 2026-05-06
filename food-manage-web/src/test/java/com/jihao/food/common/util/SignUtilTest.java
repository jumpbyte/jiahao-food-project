package com.jihao.food.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SignUtilTest {

    @Test
    void generateSign_shouldProduceConsistentMd5() {
        String sign = SignUtil.generateSign("appKey", 1000L, "nonce", "body", "secret");
        assertNotNull(sign);
        assertEquals(32, sign.length());
        assertEquals(sign.toUpperCase(), sign);
    }

    @Test
    void generateSign_sameInputs_sameOutput() {
        String s1 = SignUtil.generateSign("ak", 1L, "n", "b", "sk");
        String s2 = SignUtil.generateSign("ak", 1L, "n", "b", "sk");
        assertEquals(s1, s2);
    }

    @Test
    void generateSign_differentInputs_differentOutput() {
        String s1 = SignUtil.generateSign("ak", 1L, "n", "body1", "sk");
        String s2 = SignUtil.generateSign("ak", 1L, "n", "body2", "sk");
        assertNotEquals(s1, s2);
    }

    @Test
    void verifySign_correctSign_returnsTrue() {
        String sign = SignUtil.generateSign("ak", 1L, "n", "body", "sk");
        assertTrue(SignUtil.verifySign("ak", 1L, "n", "body", "sk", sign));
    }

    @Test
    void verifySign_wrongSign_returnsFalse() {
        assertFalse(SignUtil.verifySign("ak", 1L, "n", "body", "sk", "WRONGSIGN"));
    }

    @Test
    void verifySign_modifiedBody_returnsFalse() {
        String sign = SignUtil.generateSign("ak", 1L, "n", "original", "sk");
        assertFalse(SignUtil.verifySign("ak", 1L, "n", "tampered", "sk", sign));
    }

    @Test
    void isWithinTimeWindow_withinWindow_returnsTrue() {
        long now = System.currentTimeMillis();
        assertTrue(SignUtil.isWithinTimeWindow(now, 300000));
    }

    @Test
    void isWithinTimeWindow_expired_returnsFalse() {
        long past = System.currentTimeMillis() - 600000;
        assertFalse(SignUtil.isWithinTimeWindow(past, 300000));
    }
}
