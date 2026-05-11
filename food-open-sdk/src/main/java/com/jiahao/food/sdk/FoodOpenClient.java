package com.jiahao.food.sdk;

import com.jiahao.food.sdk.api.AreaApi;
import com.jiahao.food.sdk.api.GeoApi;
import com.jiahao.food.sdk.config.ClientConfig;
import com.jiahao.food.sdk.exception.FoodOpenException;
import com.jiahao.food.sdk.http.SdkHttpClient;
import com.jiahao.food.sdk.internal.ApiExecutor;
import com.jiahao.food.sdk.log.FoodOpenLogger;
import com.jiahao.food.sdk.retry.RetryPolicy;

import java.io.IOException;

/**
 * 开放 API SDK 入口类。
 */
public class FoodOpenClient {

    private final ClientConfig config;
    private final SdkHttpClient httpClient;
    private final AreaApi areaApi;
    private final GeoApi geoApi;

    private FoodOpenClient(ClientConfig config) throws FoodOpenException {
        this.config = config;
        this.httpClient = new SdkHttpClient(config);
        FoodOpenLogger logger = new FoodOpenLogger(config.isEnableLogging());
        RetryPolicy retryPolicy = new RetryPolicy(config.getMaxRetries(), logger);
        ApiExecutor apiExecutor = new ApiExecutor(config, httpClient, retryPolicy, logger);
        this.areaApi = new AreaApi(apiExecutor);
        this.geoApi = new GeoApi(apiExecutor);
    }

    public static Builder builder() {
        return new Builder();
    }

    public AreaApi area() {
        return areaApi;
    }

    public GeoApi geo() {
        return geoApi;
    }

    public void close() throws IOException {
        httpClient.close();
    }

    /**
     * 链式 Builder。
     */
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

        public FoodOpenClient build() throws FoodOpenException {
            ClientConfig config = new ClientConfig.Builder()
                    .appKey(appKey)
                    .appSecret(appSecret)
                    .serverUrl(serverUrl)
                    .connectTimeout(connectTimeout)
                    .readTimeout(readTimeout)
                    .maxRetries(maxRetries)
                    .enableLogging(enableLogging)
                    .build();
            return new FoodOpenClient(config);
        }
    }
}
