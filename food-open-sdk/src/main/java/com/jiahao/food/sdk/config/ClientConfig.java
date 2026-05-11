package com.jiahao.food.sdk.config;

import com.jiahao.food.sdk.exception.ClientException;

/**
 * SDK 客户端配置。
 */
public class ClientConfig {

    private final String appKey;
    private final String appSecret;
    private final String serverUrl;
    private final int connectTimeout;
    private final int readTimeout;
    private final int maxRetries;
    private final boolean enableLogging;

    private ClientConfig(Builder builder) {
        this.appKey = builder.appKey;
        this.appSecret = builder.appSecret;
        this.serverUrl = builder.serverUrl;
        this.connectTimeout = builder.connectTimeout;
        this.readTimeout = builder.readTimeout;
        this.maxRetries = builder.maxRetries;
        this.enableLogging = builder.enableLogging;
    }

    public String getAppKey() { return appKey; }
    public String getAppSecret() { return appSecret; }
    public String getServerUrl() { return serverUrl; }
    public int getConnectTimeout() { return connectTimeout; }
    public int getReadTimeout() { return readTimeout; }
    public int getMaxRetries() { return maxRetries; }
    public boolean isEnableLogging() { return enableLogging; }

    public static class Builder {
        private String appKey;
        private String appSecret;
        private String serverUrl;
        private int connectTimeout = 5000;
        private int readTimeout = 10000;
        private int maxRetries = 3;
        private boolean enableLogging = false;

        public Builder appKey(String appKey) { this.appKey = appKey; return this; }
        public Builder appSecret(String appSecret) { this.appSecret = appSecret; return this; }
        public Builder serverUrl(String serverUrl) { this.serverUrl = serverUrl; return this; }
        public Builder connectTimeout(int connectTimeout) { this.connectTimeout = connectTimeout; return this; }
        public Builder readTimeout(int readTimeout) { this.readTimeout = readTimeout; return this; }
        public Builder maxRetries(int maxRetries) { this.maxRetries = maxRetries; return this; }
        public Builder enableLogging(boolean enableLogging) { this.enableLogging = enableLogging; return this; }

        public ClientConfig build() throws ClientException {
            if (appKey == null || appKey.length() == 0) {
                throw new ClientException("appKey is required");
            }
            if (appSecret == null || appSecret.length() == 0) {
                throw new ClientException("appSecret is required");
            }
            if (serverUrl == null || serverUrl.length() == 0) {
                throw new ClientException("serverUrl is required");
            }
            return new ClientConfig(this);
        }
    }
}
