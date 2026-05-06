# API 签名增强：支持 GET Query Params 参与签名

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 URL query parameters（按 key 排序拼接）纳入 API 签名计算范围，修复 GET 请求参数不被签名保护的漏洞。

**Architecture:** 修改 `ApiSignInterceptor` 中的签名内容获取逻辑：解析 query string → 按 key 升序排序 → 拼接为 `key1=value1&key2=value2` 格式 → 与 requestBody 组合后传入 `SignUtil.verifySign()`。`SignUtil` 本身无需改动。新增单元测试验证排序和签名逻辑。

**Tech Stack:** Java 17, Spring Boot 3.2.5, JUnit 5

---

## File Structure

| 操作 | 文件 | 职责 |
|------|------|------|
| 修改 | `food-manage-web/src/main/java/com/jihao/food/interceptor/ApiSignInterceptor.java` | 新增 `getSignContent()` 方法，替换 `getRequestBody()` 调用 |
| 新增 | `food-manage-web/src/test/java/com/jihao/food/interceptor/ApiSignInterceptorTest.java` | 拦截器签名校验的单元测试 |
| 新增 | `food-manage-web/src/test/java/com/jihao/food/common/util/SignUtilTest.java` | SignUtil 的单元测试（已验证现有逻辑正确） |

---

### Task 1: 新增 SignUtil 单元测试

**Files:**
- Create: `food-manage-web/src/test/java/com/jihao/food/common/util/SignUtilTest.java`

- [ ] **Step 1: 创建测试目录**

```bash
mkdir -p food-manage-web/src/test/java/com/jihao/food/common/util
```

- [ ] **Step 2: 编写 SignUtil 测试**

创建文件 `food-manage-web/src/test/java/com/jihao/food/common/util/SignUtilTest.java`：

```java
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
```

- [ ] **Step 3: 运行测试**

```bash
cd food-manage-web && export JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home" && mvn test -Dtest=SignUtilTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: All 8 tests PASS

- [ ] **Step 4: 提交**

```bash
git add food-manage-web/src/test/java/com/jihao/food/common/util/SignUtilTest.java
git commit -m "test: add SignUtil unit tests

验证 generateSign、verifySign、isWithinTimeWindow 方法的正确性。"
```

---

### Task 2: 修改 ApiSignInterceptor 支持 Query Params 排序签名

**Files:**
- Modify: `food-manage-web/src/main/java/com/jihao/food/interceptor/ApiSignInterceptor.java`

- [ ] **Step 1: 修改拦截器**

将 `ApiSignInterceptor.java` 完整替换为以下内容：

```java
package com.jihao.food.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jihao.food.common.Result;
import com.jihao.food.common.annotation.IgnoreSign;
import com.jihao.food.common.util.SignUtil;
import com.jihao.food.system.entity.SysApiKey;
import com.jihao.food.system.mapper.SysApiKeyMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiSignInterceptor implements HandlerInterceptor {

    @Value("${api.sign.time-window}")
    private long timeWindowMs;

    private final SysApiKeyMapper sysApiKeyMapper;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        if (handlerMethod.hasMethodAnnotation(IgnoreSign.class)
                || handlerMethod.getBeanType().isAnnotationPresent(IgnoreSign.class)) {
            return true;
        }

        String appKey = request.getHeader("appKey");
        String timestamp = request.getHeader("timestamp");
        String nonce = request.getHeader("nonce");
        String sign = request.getHeader("sign");

        if (appKey == null || timestamp == null || nonce == null || sign == null) {
            sendError(response, "签名参数缺失", 401);
            return false;
        }

        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            sendError(response, "时间戳格式错误", 401);
            return false;
        }

        if (!SignUtil.isWithinTimeWindow(ts, timeWindowMs)) {
            sendError(response, "请求已过期", 401);
            return false;
        }

        SysApiKey apiKey = sysApiKeyMapper.findByAppKey(appKey);
        if (apiKey == null) {
            sendError(response, "appKey 无效", 401);
            return false;
        }

        if (apiKey.getState() != 1) {
            sendError(response, "appKey 已禁用", 403);
            return false;
        }

        String signContent = getSignContent(request);
        if (!SignUtil.verifySign(appKey, ts, nonce, signContent, apiKey.getAppSecret(), sign)) {
            sendError(response, "签名验证失败", 401);
            return false;
        }

        return true;
    }

    /**
     * 获取签名内容：排序后的 query params + request body
     */
    private String getSignContent(HttpServletRequest request) {
        String sortedQuery = getSortedQueryString(request);
        String body = getRequestBody(request);
        return sortedQuery + body;
    }

    /**
     * 将 query params 按 key 升序排序后拼接为 key1=value1&key2=value2
     */
    private String getSortedQueryString(HttpServletRequest request) {
        Map<String, String[]> params = request.getParameterMap();
        if (params.isEmpty()) {
            return "";
        }

        return params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    String key = entry.getKey();
                    String value = entry.getValue().length > 0 ? entry.getValue()[0] : "";
                    return key + "=" + value;
                })
                .collect(Collectors.joining("&"));
    }

    private String getRequestBody(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper wrapper) {
            byte[] content = wrapper.getContentAsByteArray();
            if (content.length > 0) {
                try {
                    return new String(content, request.getCharacterEncoding());
                } catch (UnsupportedEncodingException e) {
                    return new String(content, StandardCharsets.UTF_8);
                }
            }
        }
        return "";
    }

    private void sendError(HttpServletResponse response, String message, int code) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(code, message)));
    }
}
```

关键变化：
- 新增 `getSignContent()` 方法：组合排序后的 query string + request body
- 新增 `getSortedQueryString()` 方法：从 `request.getParameterMap()` 获取参数，按 key 升序排序，拼接为 `key=value` 格式
- `preHandle` 中调用 `getSignContent(request)` 替代原来的 `getRequestBody(request)`

- [ ] **Step 2: 编译验证**

```bash
cd food-manage-web && export JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home" && mvn clean compile
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add food-manage-web/src/main/java/com/jihao/food/interceptor/ApiSignInterceptor.java
git commit -m "fix: support sorted query params in API signature

GET 请求的 query parameters 现在按 key 排序后参与签名计算，
修复了之前 URL 参数不被签名保护的问题。"
```

---

### Task 3: 新增 ApiSignInterceptor 签名内容构建测试

**Files:**
- Create: `food-manage-web/src/test/java/com/jihao/food/interceptor/ApiSignInterceptorTest.java`

- [ ] **Step 1: 创建测试目录**

```bash
mkdir -p food-manage-web/src/test/java/com/jihao/food/interceptor
```

- [ ] **Step 2: 编写签名内容构建测试**

创建文件 `food-manage-web/src/test/java/com/jihao/food/interceptor/ApiSignInterceptorTest.java`：

```java
package com.jihao.food.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.util.ContentCachingRequestWrapper;

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
        SysApiKey apiKey = mockApiKey(appKey, secret, 1);
        when(sysApiKeyMapper.findByAppKey(appKey)).thenReturn(apiKey);

        HandlerMethod handlerMethod = mock(HandlerMethod.class);

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
        when(sysApiKeyMapper.findByAppKey(appKey)).thenReturn(mockApiKey(appKey, secret, 1));
        HandlerMethod handlerMethod = mock(HandlerMethod.class);

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
        when(sysApiKeyMapper.findByAppKey(appKey)).thenReturn(mockApiKey(appKey, secret, 1));
        HandlerMethod handlerMethod = mock(HandlerMethod.class);

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertTrue(result);
    }

    @Test
    void preHandle_getRequest_missingSignHeaders_shouldReturnError() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/open/area/provinces");
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handlerMethod = mock(HandlerMethod.class);

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertFalse(result);
        assertEquals(401, response.getStatus());
    }

    private SysApiKey mockApiKey(String appKey, String secret, int state) {
        SysApiKey apiKey = mock(SysApiKey.class);
        when(apiKey.getAppSecret()).thenReturn(secret);
        when(apiKey.getState()).thenReturn(state);
        return apiKey;
    }
}
```

但等等，`ApiSignInterceptor` 有 `@Value` 注入的 `timeWindowMs`，且使用 `@RequiredArgsConstructor`，不能直接 new。需要让测试可以通过。

查看发现 `timeWindowMs` 是 private field 没有 setter。需要给 interceptor 加上可设置的方式。

- [ ] **Step 2b: 给拦截器添加 setter 用于测试**

在 `ApiSignInterceptor.java` 的 `timeWindowMs` 字段上改为：

```java
private long timeWindowMs;

public void setTimeWindowMs(long timeWindowMs) {
    this.timeWindowMs = timeWindowMs;
}
```

即把 `@Value("${api.sign.time-window}")` 注解放到 setter 上，或直接放在字段上保持不变，额外添加一个 setter。

实际上更简单的做法：字段保持 `@Value`，额外添加一个 package-private 的 setter 供测试使用：

```java
void setTimeWindowMs(long timeWindowMs) {
    this.timeWindowMs = timeWindowMs;
}
```

- [ ] **Step 3: 运行测试**

```bash
cd food-manage-web && export JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home" && mvn test -Dtest=ApiSignInterceptorTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: All 4 tests PASS

- [ ] **Step 4: 提交**

```bash
git add food-manage-web/src/main/java/com/jihao/food/interceptor/ApiSignInterceptor.java food-manage-web/src/test/java/com/jihao/food/interceptor/ApiSignInterceptorTest.java
git commit -m "test: add ApiSignInterceptor unit tests for sorted query params

测试 GET 请求参数排序签名、错误顺序签名拒绝、无参数场景。"
```

---

### Task 4: 运行全量测试并提交

- [ ] **Step 1: 运行全量测试**

```bash
cd food-manage-web && export JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home" && mvn test
```

Expected: All tests PASS (SignUtilTest + ApiSignInterceptorTest)

- [ ] **Step 2: 最终提交**

```bash
git log --oneline -3
```

确认提交记录包含：SignUtil 测试、拦截器修改、拦截器测试。
