package com.jiahao.food.sdk.internal;

import com.google.gson.reflect.TypeToken;
import com.jiahao.food.sdk.auth.SignUtil;
import com.jiahao.food.sdk.config.ClientConfig;
import com.jiahao.food.sdk.exception.FoodOpenException;
import com.jiahao.food.sdk.exception.NetworkException;
import com.jiahao.food.sdk.exception.ServerException;
import com.jiahao.food.sdk.http.HttpResponse;
import com.jiahao.food.sdk.http.SdkHttpClient;
import com.jiahao.food.sdk.log.FoodOpenLogger;
import com.jiahao.food.sdk.retry.RetryPolicy;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * API 执行器：负责签名、重试、响应解析。
 */
public class ApiExecutor {

    private final ClientConfig config;
    private final SdkHttpClient httpClient;
    private final RetryPolicy retryPolicy;
    private final FoodOpenLogger logger;

    public ApiExecutor(ClientConfig config, SdkHttpClient httpClient, RetryPolicy retryPolicy, FoodOpenLogger logger) {
        this.config = config;
        this.httpClient = httpClient;
        this.retryPolicy = retryPolicy;
        this.logger = logger;
    }

    public <T> T execute(final String path, final Map<String, String> params, final Type dataType) throws FoodOpenException {
        return retryPolicy.execute(new RetryPolicy.Callable<T>() {
            public T call() throws FoodOpenException {
                return doRequest(path, params, dataType);
            }
        });
    }

    private <T> T doRequest(String path, Map<String, String> params, Type dataType) throws FoodOpenException {
        String url = config.getServerUrl() + path;
        long timestamp = System.currentTimeMillis();
        String nonce = UUID.randomUUID().toString();
        String sign = SignUtil.generateSign(config.getAppKey(), timestamp, nonce, params, "", config.getAppSecret());

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("appKey", config.getAppKey());
        headers.put("timestamp", String.valueOf(timestamp));
        headers.put("nonce", nonce);
        headers.put("sign", sign);

        String fullUrl = buildUrlWithParams(url, params);
        logger.debug("Request: " + fullUrl);

        HttpResponse response = httpClient.execute(fullUrl, headers);
        logger.debug("Response: " + response.getBody());

        Type resultType = new ApiResultType(dataType);
        ApiResult<T> result = JsonUtil.fromJson(response.getBody(), resultType);

        if (result.getCode() != 0) {
            throw new ServerException(result.getCode(), result.getMessage());
        }
        return result.getData();
    }

    private String buildUrlWithParams(String url, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return url;
        }
        StringBuilder sb = new StringBuilder(url);
        sb.append("?");
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }
        return sb.toString();
    }

    /**
     * API 响应包装类，与后端 Result<T> 结构一致。
     */
    static class ApiResult<T> {
        private int code;
        private String message;
        private T data;
        private long timestamp;
        private String traceId;

        public int getCode() { return code; }
        public String getMessage() { return message; }
        public T getData() { return data; }
        public long getTimestamp() { return timestamp; }
        public String getTraceId() { return traceId; }
    }

    /**
     * ParameterizedType implementation for ApiResult<T> to avoid type erasure.
     */
    private static class ApiResultType implements ParameterizedType {
        private final Type dataType;

        ApiResultType(Type dataType) {
            this.dataType = dataType;
        }

        public Type[] getActualTypeArguments() {
            return new Type[]{dataType};
        }

        public Type getRawType() {
            return ApiResult.class;
        }

        public Type getOwnerType() {
            return null;
        }
    }
}
