package com.jihao.food.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jihao.food.common.annotation.IgnoreSign;
import com.jihao.food.common.util.SignUtil;
import com.jihao.food.system.entity.SysApiKey;
import com.jihao.food.system.mapper.SysApiKeyMapper;
import com.jihao.food.common.util.SignUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiSignInterceptorTest {

    private ApiSignInterceptor interceptor;
    private SysApiKeyMapper sysApiKeyMapper;

    @BeforeEach
    void setUp() {
        sysApiKeyMapper = mock(SysApiKeyMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        interceptor = new ApiSignInterceptor(sysApiKeyMapper, objectMapper);
        interceptor.setTimeWindowMs(300000);
    }

    @Test
    void preHandle_getRequestWithSortedParams_shouldVerifySign() throws Exception {
        // 准备：GET 请求带 query params
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/open/area/cities");
        request.addParameter("provinceId", "1");
        request.addParameter("depth", "2");

        String appKey = "test-key";
        String secret = "test-secret";
        long timestamp = System.currentTimeMillis();
        String nonce = "abc123";

        // 预期排序后的 query string: depth=2&provinceId=1
        String expectedSortedQuery = "depth=2&provinceId=1";
        String sign = SignUtil.generateSign(appKey, timestamp, nonce, expectedSortedQuery, secret);

        request.addHeader("appKey", appKey);
        request.addHeader("timestamp", String.valueOf(timestamp));
        request.addHeader("nonce", nonce);
        request.addHeader("sign", sign);

        MockHttpServletResponse response = new MockHttpServletResponse();
        SysApiKey apiKey = createApiKey(appKey, secret, 1);
        when(sysApiKeyMapper.findByAppKey(appKey)).thenReturn(apiKey);

        HandlerMethod handlerMethod = mockHandlerMethod();

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertTrue(result);
    }

    @Test
    void preHandle_wrongParamOrderInClientSign_shouldFail() throws Exception {
        // 客户端用错误顺序拼接签名，应该失败
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/open/area/cities");
        request.addParameter("provinceId", "1");
        request.addParameter("depth", "2");

        String appKey = "test-key";
        String secret = "test-secret";
        long timestamp = System.currentTimeMillis();
        String nonce = "abc123";

        // 客户端用 provinceId 在前拼接（错误顺序）
        String wrongQuery = "provinceId=1&depth=2";
        String wrongSign = SignUtil.generateSign(appKey, timestamp, nonce, wrongQuery, secret);

        request.addHeader("appKey", appKey);
        request.addHeader("timestamp", String.valueOf(timestamp));
        request.addHeader("nonce", nonce);
        request.addHeader("sign", wrongSign);

        MockHttpServletResponse response = new MockHttpServletResponse();
        when(sysApiKeyMapper.findByAppKey(appKey)).thenReturn(createApiKey(appKey, secret, 1));
        HandlerMethod handlerMethod = mockHandlerMethod();

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertFalse(result);
        String body = response.getContentAsString(StandardCharsets.UTF_8);
        assertTrue(body.contains("签名验证失败"));
    }

    @Test
    void preHandle_noParams_emptyBody_shouldVerifySignWithEmptyContent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/open/area/provinces");

        String appKey = "test-key";
        String secret = "test-secret";
        long timestamp = System.currentTimeMillis();
        String nonce = "abc123";

        String sign = SignUtil.generateSign(appKey, timestamp, nonce, "", secret);

        request.addHeader("appKey", appKey);
        request.addHeader("timestamp", String.valueOf(timestamp));
        request.addHeader("nonce", nonce);
        request.addHeader("sign", sign);

        MockHttpServletResponse response = new MockHttpServletResponse();
        when(sysApiKeyMapper.findByAppKey(appKey)).thenReturn(createApiKey(appKey, secret, 1));
        HandlerMethod handlerMethod = mockHandlerMethod();

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertTrue(result);
    }

    @Test
    void preHandle_getRequest_missingSignHeaders_shouldReturnError() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/open/area/provinces");
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handlerMethod = mockHandlerMethod();

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertFalse(result);
        // 拦截器返回 HTTP 200，错误码在 JSON body 中
        String body = response.getContentAsString(StandardCharsets.UTF_8);
        assertTrue(body.contains("签名参数缺失"));
    }

    private SysApiKey createApiKey(String appKey, String secret, int state) {
        SysApiKey apiKey = new SysApiKey();
        apiKey.setAppKey(appKey);
        apiKey.setAppSecret(secret);
        apiKey.setState(state);
        return apiKey;
    }

    private HandlerMethod mockHandlerMethod() throws Exception {
        HandlerMethod handlerMethod = mock(HandlerMethod.class);
        when(handlerMethod.hasMethodAnnotation(IgnoreSign.class)).thenReturn(false);
        when(handlerMethod.getBeanType()).thenAnswer(invocation -> Object.class);
        return handlerMethod;
    }
}
