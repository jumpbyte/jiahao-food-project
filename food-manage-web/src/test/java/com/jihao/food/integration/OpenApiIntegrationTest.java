package com.jihao.food.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jihao.food.common.util.SignUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiIntegrationTest {

    private static final String APP_KEY = "demo";
    private static final String APP_SECRET = "e02ca276f7d444c099f571d7bee8fac6";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 带签名的 GET 请求。
     */
    private MockHttpServletRequestBuilder signedGet(String url, Map<String, Object> params) {
        return signedRequest(get(url), params, null);
    }

    /**
     * 通用签名方法：构建签名内容（排序查询参数 + 请求体），计算签名并添加 Headers。
     */
    private MockHttpServletRequestBuilder signedRequest(
            MockHttpServletRequestBuilder builder,
            Map<String, Object> params,
            String requestBody) {
        long timestamp = System.currentTimeMillis();
        String nonce = java.util.UUID.randomUUID().toString();
        String queryPart = buildQueryPart(params);
        String content = queryPart + (requestBody != null ? requestBody : "");
        String sign = SignUtil.generateSign(APP_KEY, timestamp, nonce, content, APP_SECRET);

        if (params != null) {
            params.forEach((k, v) -> builder.param(k, v != null ? v.toString() : null));
        }
        builder.header("appKey", APP_KEY)
               .header("timestamp", String.valueOf(timestamp))
               .header("nonce", nonce)
               .header("sign", sign);
        return builder;
    }

    /**
     * 构建签名内容中的查询参数部分：按 key 升序排序，格式为 key1=value1&key2=value2。
     */
    private String buildQueryPart(Map<String, Object> params) {
        if (params == null || params.isEmpty()) return "";
        return params.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(java.util.stream.Collectors.joining("&"));
    }
}