package com.jiahao.food.sdk.config;

import com.jiahao.food.sdk.exception.ClientException;
import org.junit.Test;

import static org.junit.Assert.*;

public class ClientConfigTest {

    @Test
    public void build_withValidParams_shouldSucceed() throws ClientException {
        ClientConfig config = new ClientConfig.Builder()
                .appKey("demo")
                .appSecret("test-secret")
                .serverUrl("https://api.example.com")
                .build();

        assertEquals("demo", config.getAppKey());
        assertEquals("test-secret", config.getAppSecret());
        assertEquals("https://api.example.com", config.getServerUrl());
        assertEquals(5000, config.getConnectTimeout());
        assertEquals(10000, config.getReadTimeout());
        assertEquals(3, config.getMaxRetries());
        assertFalse(config.isEnableLogging());
    }

    @Test(expected = ClientException.class)
    public void build_withMissingAppKey_shouldThrow() throws ClientException {
        new ClientConfig.Builder()
                .appSecret("test-secret")
                .serverUrl("https://api.example.com")
                .build();
    }

    @Test(expected = ClientException.class)
    public void build_withMissingAppSecret_shouldThrow() throws ClientException {
        new ClientConfig.Builder()
                .appKey("demo")
                .serverUrl("https://api.example.com")
                .build();
    }

    @Test(expected = ClientException.class)
    public void build_withMissingServerUrl_shouldThrow() throws ClientException {
        new ClientConfig.Builder()
                .appKey("demo")
                .appSecret("test-secret")
                .build();
    }

    @Test
    public void build_withCustomTimeouts_shouldApply() throws ClientException {
        ClientConfig config = new ClientConfig.Builder()
                .appKey("demo")
                .appSecret("test-secret")
                .serverUrl("https://api.example.com")
                .connectTimeout(3000)
                .readTimeout(8000)
                .maxRetries(5)
                .enableLogging(true)
                .build();

        assertEquals(3000, config.getConnectTimeout());
        assertEquals(8000, config.getReadTimeout());
        assertEquals(5, config.getMaxRetries());
        assertTrue(config.isEnableLogging());
    }
}
