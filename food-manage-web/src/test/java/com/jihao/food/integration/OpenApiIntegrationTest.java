package com.jihao.food.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jihao.food.common.util.SignUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

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
        String nonce = UUID.randomUUID().toString();
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
            .collect(Collectors.joining("&"));
    }

    // ===== 签名验证测试 =====

    @Test
    void preHandle_withValidSign_shouldReturnSuccess() throws Exception {
        mockMvc.perform(signedGet("/api/open/area/provinces", null))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void preHandle_wrongSecret_shouldReturnSignError() throws Exception {
        long timestamp = System.currentTimeMillis();
        String nonce = "test-nonce-wrong-secret";
        // 用错误的 secret 计算签名
        String wrongSign = SignUtil.generateSign(APP_KEY, timestamp, nonce, "", "wrong_secret_value");

        mockMvc.perform(get("/api/open/area/provinces")
                .header("appKey", APP_KEY)
                .header("timestamp", String.valueOf(timestamp))
                .header("nonce", nonce)
                .header("sign", wrongSign))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.message").value("签名验证失败"));
    }

    @Test
    void preHandle_missingHeaders_shouldReturnMissingParamError() throws Exception {
        mockMvc.perform(get("/api/open/area/provinces"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.message").value("签名参数缺失"));
    }

    @Test
    void preHandle_expiredTimestamp_shouldReturnExpiredError() throws Exception {
        long expiredTs = System.currentTimeMillis() - 600_000; // 10 分钟前
        String nonce = "test-nonce-expired";
        String sign = SignUtil.generateSign(APP_KEY, expiredTs, nonce, "", APP_SECRET);

        mockMvc.perform(get("/api/open/area/provinces")
                .header("appKey", APP_KEY)
                .header("timestamp", String.valueOf(expiredTs))
                .header("nonce", nonce)
                .header("sign", sign))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.message").value("请求已过期"));
    }
}
