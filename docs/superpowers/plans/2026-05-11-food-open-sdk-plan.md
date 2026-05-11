# food-open-sdk Java SDK Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `/api/open/**` 接口开发独立的 Java SDK 子模块 `food-open-sdk/`，使接入方通过链式 Builder 入口和按业务域分组的 API 方法调用开放 API。

**Architecture:** 独立 Maven 子模块，Apache HttpClient 连接池 + Gson JSON 解析 + 自动 MD5 签名 + 指数退避重试，Java 1.6 兼容，SLF4J 可选依赖。

**Tech Stack:** Java 1.6+, Maven, Apache HttpClient 4.5.14, Gson 2.8.9, SLF4J 1.7.36 (provided), JUnit 5 (test)

---

## File Structure

| 文件 | 职责 |
|------|------|
| `food-open-sdk/pom.xml` | Maven 构建配置，Java 1.6 目标，依赖声明 |
| `food-open-sdk/src/main/java/com/jiahao/food/sdk/exception/FoodOpenException.java` | 异常基类 |
| `food-open-sdk/src/main/java/com/jiahao/food/sdk/exception/ClientException.java` | 客户端异常 |
| `food-open-sdk/src/main/java/com/jiahao/food/sdk/exception/ServerException.java` | 服务端异常 |
| `food-open-sdk/src/main/java/com/jiahao/food/sdk/exception/NetworkException.java` | 网络异常 |
| `food-open-sdk/src/main/java/com/jiahao/food/sdk/config/ClientConfig.java` | SDK 配置类 + Builder |
| `food-open-sdk/src/main/java/com/jiahao/food/sdk/http/HttpResponse.java` | HTTP 响应包装 |
| `food-open-sdk/src/main/java/com/jiahao/food/sdk/http/SdkHttpClient.java` | HTTP 客户端（连接池） |
| `food-open-sdk/src/main/java/com/jiahao/food/sdk/log/FoodOpenLogger.java` | 日志（SLF4J/JDK 自动降级） |
| `food-open-sdk/src/main/java/com/jiahao/food/sdk/internal/JsonUtil.java` | Gson 封装 |
| `food-open-sdk/src/main/java/com/jiahao/food/sdk/auth/SignUtil.java` | MD5 签名生成 |
| `food-open-sdk/src/main/java/com/jiahao/food/sdk/retry/RetryPolicy.java` | 指数退避重试 |
| `food-open-sdk/src/main/java/com/jiahao/food/sdk/internal/ApiExecutor.java` | API 执行器（签名+重试） |
| `food-open-sdk/src/main/java/com/jiahao/food/sdk/model/AreaDTO.java` | 行政区 DTO |
| `food-open-sdk/src/main/java/com/jiahao/food/sdk/model/AreaDetailDTO.java` | 行政区详情 DTO |
| `food-open-sdk/src/main/java/com/jiahao/food/sdk/model/AreaTreeDTO.java` | 行政区树 DTO |
| `food-open-sdk/src/main/java/com/jiahao/food/sdk/model/OrgInfoDTO.java` | 组织信息 DTO |
| `food-open-sdk/src/main/java/com/jiahao/food/sdk/model/TownshipOrgDTO.java` | 乡镇组织 DTO |
| `food-open-sdk/src/main/java/com/jiahao/food/sdk/api/AreaApi.java` | 行政区 API（10 个方法） |
| `food-open-sdk/src/main/java/com/jiahao/food/sdk/api/GeoApi.java` | 地理/组织 API（2 个方法） |
| `food-open-sdk/src/main/java/com/jiahao/food/sdk/FoodOpenClient.java` | SDK 入口类 |
| `food-open-sdk/src/test/java/com/jiahao/food/sdk/auth/SignUtilTest.java` | 签名工具单元测试 |
| `food-open-sdk/src/test/java/com/jiahao/food/sdk/config/ClientConfigTest.java` | 配置类单元测试 |
| `food-open-sdk/src/test/java/com/jiahao/food/sdk/retry/RetryPolicyTest.java` | 重试策略单元测试 |

---

### Task 1: Maven 项目骨架和异常体系

**Files:**
- Create: `food-open-sdk/pom.xml`
- Create: `food-open-sdk/src/main/java/com/jiahao/food/sdk/exception/FoodOpenException.java`
- Create: `food-open-sdk/src/main/java/com/jiahao/food/sdk/exception/ClientException.java`
- Create: `food-open-sdk/src/main/java/com/jiahao/food/sdk/exception/ServerException.java`
- Create: `food-open-sdk/src/main/java/com/jiahao/food/sdk/exception/NetworkException.java`

- [ ] **Step 1: 创建 pom.xml**

```bash
mkdir -p food-open-sdk/src/main/java/com/jiahao/food/sdk/{config,http,auth,retry,log,exception,model,api,internal}
mkdir -p food-open-sdk/src/test/java/com/jiahao/food/sdk/{auth,config,retry}
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.jiahao.food</groupId>
        <artifactId>food-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <groupId>jiahao.food.com</groupId>
    <artifactId>food-open-sdk</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <java.version>1.6</java.version>
        <maven.compiler.source>1.6</maven.compiler.source>
        <maven.compiler.target>1.6</maven.compiler.target>
        <httpclient.version>4.5.14</httpclient.version>
        <gson.version>2.8.9</gson.version>
        <slf4j.version>1.7.36</slf4j.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.apache.httpcomponents</groupId>
            <artifactId>httpclient</artifactId>
            <version>${httpclient.version}</version>
        </dependency>
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>${gson.version}</version>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>${slf4j.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.13.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

**注意：** 测试使用 JUnit 4（`junit:junit:4.13.2`）而非 JUnit 5，因为 JUnit 4 对 Java 1.6 兼容性更好，且 SDK 测试不需要 Spring 容器。

- [ ] **Step 2: 创建异常基类 FoodOpenException**

文件: `food-open-sdk/src/main/java/com/jiahao/food/sdk/exception/FoodOpenException.java`

```java
package com.jiahao.food.sdk.exception;

/**
 * SDK 异常基类。
 */
public abstract class FoodOpenException extends Exception {

    private final String errorCode;
    private final String errorMessage;

    protected FoodOpenException(String errorCode, String errorMessage, Throwable cause) {
        super("[" + errorCode + "] " + errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
```

- [ ] **Step 3: 创建 ClientException**

文件: `food-open-sdk/src/main/java/com/jiahao/food/sdk/exception/ClientException.java`

```java
package com.jiahao.food.sdk.exception;

/**
 * 客户端异常：参数错误、配置错误。
 */
public class ClientException extends FoodOpenException {

    public ClientException(String message) {
        super("CLIENT_ERROR", message, null);
    }
}
```

- [ ] **Step 4: 创建 ServerException**

文件: `food-open-sdk/src/main/java/com/jiahao/food/sdk/exception/ServerException.java`

```java
package com.jiahao.food.sdk.exception;

/**
 * 服务端异常：API 返回 code != 0。
 */
public class ServerException extends FoodOpenException {

    private final int statusCode;

    public ServerException(int statusCode, String message) {
        super(String.valueOf(statusCode), message, null);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
```

- [ ] **Step 5: 创建 NetworkException**

文件: `food-open-sdk/src/main/java/com/jiahao/food/sdk/exception/NetworkException.java`

```java
package com.jiahao.food.sdk.exception;

/**
 * 网络异常：连接超时、IO 异常。
 */
public class NetworkException extends FoodOpenException {

    public NetworkException(String message, Throwable cause) {
        super("NETWORK_ERROR", message, cause);
    }
}
```

- [ ] **Step 6: 验证编译**

Run: `cd food-open-sdk && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: 提交**

```bash
git add food-open-sdk/pom.xml
git add food-open-sdk/src/main/java/com/jiahao/food/sdk/exception/
git commit -m "feat: add food-open-sdk Maven project with exception hierarchy"
```

---

### Task 2: 配置类和 HTTP 客户端

**Files:**
- Create: `food-open-sdk/src/main/java/com/jiahao/food/sdk/config/ClientConfig.java`
- Create: `food-open-sdk/src/main/java/com/jiahao/food/sdk/http/HttpResponse.java`
- Create: `food-open-sdk/src/main/java/com/jiahao/food/sdk/http/SdkHttpClient.java`
- Create: `food-open-sdk/src/main/java/com/jiahao/food/sdk/log/FoodOpenLogger.java`
- Create: `food-open-sdk/src/test/java/com/jiahao/food/sdk/config/ClientConfigTest.java`

- [ ] **Step 1: 创建 ClientConfig**

文件: `food-open-sdk/src/main/java/com/jiahao/food/sdk/config/ClientConfig.java`

```java
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
```

- [ ] **Step 2: 创建 HttpResponse**

文件: `food-open-sdk/src/main/java/com/jiahao/food/sdk/http/HttpResponse.java`

```java
package com.jiahao.food.sdk.http;

/**
 * HTTP 响应包装。
 */
public class HttpResponse {

    private final int statusCode;
    private final String body;

    public HttpResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    public int getStatusCode() { return statusCode; }
    public String getBody() { return body; }
}
```

- [ ] **Step 3: 创建 SdkHttpClient**

文件: `food-open-sdk/src/main/java/com/jiahao/food/sdk/http/SdkHttpClient.java`

```java
package com.jiahao.food.sdk.http;

import com.jiahao.food.sdk.config.ClientConfig;
import com.jiahao.food.sdk.exception.NetworkException;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Map;

/**
 * SDK HTTP 客户端（连接池）。
 */
public class SdkHttpClient {

    private final CloseableHttpClient httpClient;

    public SdkHttpClient(ClientConfig config) {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(50);
        cm.setDefaultMaxPerRoute(10);
        this.httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(config.getConnectTimeout())
                        .setSocketTimeout(config.getReadTimeout())
                        .build())
                .build();
    }

    public HttpResponse execute(String url, Map<String, String> headers) throws NetworkException {
        HttpGet request = new HttpGet(url);
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                request.addHeader(entry.getKey(), entry.getValue());
            }
        }
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();
            String body = entity != null ? EntityUtils.toString(entity, "UTF-8") : "";
            return new HttpResponse(response.getStatusLine().getStatusCode(), body);
        } catch (IOException e) {
            throw new NetworkException("HTTP request failed: " + url, e);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    public void close() throws IOException {
        httpClient.close();
    }
}
```

**注意：** Java 1.6 不支持 try-with-resources，使用 finally 块手动关闭 response。

- [ ] **Step 4: 创建 FoodOpenLogger**

文件: `food-open-sdk/src/main/java/com/jiahao/food/sdk/log/FoodOpenLogger.java`

```java
package com.jiahao.food.sdk.log;

/**
 * SDK 日志器：SLF4J 存在时使用，否则降级 JDK Logger。
 */
public class FoodOpenLogger {

    private final boolean enabled;
    private final Object delegate;
    private final boolean useSlf4j;

    public FoodOpenLogger(boolean enabled) {
        this.enabled = enabled;
        boolean slf4j = false;
        Object logger = null;
        try {
            Class<?> factoryClass = Class.forName("org.slf4j.LoggerFactory");
            java.lang.reflect.Method method = factoryClass.getMethod("getLogger", String.class);
            logger = method.invoke(null, "com.jiahao.food.sdk");
            slf4j = true;
        } catch (Exception e) {
            logger = java.util.logging.Logger.getLogger("com.jiahao.food.sdk");
        }
        this.delegate = logger;
        this.useSlf4j = slf4j;
    }

    public void info(String msg) {
        if (!enabled) return;
        if (useSlf4j) {
            ((org.slf4j.Logger) delegate).info(msg);
        } else {
            ((java.util.logging.Logger) delegate).info(msg);
        }
    }

    public void warn(String msg) {
        if (!enabled) return;
        if (useSlf4j) {
            ((org.slf4j.Logger) delegate).warn(msg);
        } else {
            ((java.util.logging.Logger) delegate).warning(msg);
        }
    }

    public void debug(String msg) {
        if (!enabled) return;
        if (useSlf4j) {
            ((org.slf4j.Logger) delegate).debug(msg);
        } else {
            ((java.util.logging.Logger) delegate).fine(msg);
        }
    }

    public void error(String msg, Throwable t) {
        if (!enabled) return;
        if (useSlf4j) {
            ((org.slf4j.Logger) delegate).error(msg, t);
        } else {
            ((java.util.logging.Logger) delegate).severe(msg + " - " + t.getMessage());
        }
    }
}
```

- [ ] **Step 5: 创建 ClientConfigTest**

文件: `food-open-sdk/src/test/java/com/jiahao/food/sdk/config/ClientConfigTest.java`

```java
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
```

- [ ] **Step 6: 验证编译和测试**

Run: `cd food-open-sdk && mvn test -q 2>&1 | tail -5`
Expected: Tests run: 5, Failures: 0, Errors: 0

- [ ] **Step 7: 提交**

```bash
git add food-open-sdk/src/main/java/com/jiahao/food/sdk/config/
git add food-open-sdk/src/main/java/com/jiahao/food/sdk/http/
git add food-open-sdk/src/main/java/com/jiahao/food/sdk/log/
git add food-open-sdk/src/test/java/com/jiahao/food/sdk/config/
git commit -m "feat: add ClientConfig, SdkHttpClient, FoodOpenLogger with tests"
```

---

### Task 3: 签名工具和 JSON 工具

**Files:**
- Create: `food-open-sdk/src/main/java/com/jiahao/food/sdk/auth/SignUtil.java`
- Create: `food-open-sdk/src/main/java/com/jiahao/food/sdk/internal/JsonUtil.java`
- Create: `food-open-sdk/src/test/java/com/jiahao/food/sdk/auth/SignUtilTest.java`

- [ ] **Step 1: 创建 SignUtil**

文件: `food-open-sdk/src/main/java/com/jiahao/food/sdk/auth/SignUtil.java`

```java
package com.jiahao.food.sdk.auth;

import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

/**
 * API 签名工具。
 * 公式: sign = MD5(appKey + timestamp + nonce + content + secret) 大写
 * content = 排序后的 query params (key1=value1&key2=value2)
 */
public class SignUtil {

    public static String generateSign(String appKey, long timestamp, String nonce,
                                      Map<String, String> queryParams, String secret) {
        String sortedQuery = buildSortedQueryString(queryParams);
        String signStr = appKey + timestamp + nonce + sortedQuery + secret;
        return md5(signStr).toUpperCase();
    }

    static String buildSortedQueryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        TreeMap<String, String> sorted = new TreeMap<String, String>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            return bytesToHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
}
```

**注意：** SDK 不使用项目后端的 `commons-codec`，自行实现 MD5 保持依赖最小。`buildSortedQueryString` 包级可见以便测试。

- [ ] **Step 2: 创建 SignUtilTest**

文件: `food-open-sdk/src/test/java/com/jiahao/food/sdk/auth/SignUtilTest.java`

```java
package com.jiahao.food.sdk.auth;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class SignUtilTest {

    @Test
    public void generateSign_withNoParams_shouldProduceUppercaseMd5() {
        String sign = SignUtil.generateSign("demo", 1234567890L, "test-nonce", null, "secret");
        assertEquals(32, sign.length());
        assertEquals(sign, sign.toUpperCase());
    }

    @Test
    public void generateSign_withParams_shouldSortByKey() {
        Map<String, String> params = new HashMap<String, String>();
        params.put("cityId", "100");
        params.put("provinceId", "1");

        String sign1 = SignUtil.generateSign("demo", 1234567890L, "nonce", params, "secret");

        // 相同参数不同顺序应产生相同签名
        Map<String, String> paramsReversed = new HashMap<String, String>();
        paramsReversed.put("provinceId", "1");
        paramsReversed.put("cityId", "100");

        String sign2 = SignUtil.generateSign("demo", 1234567890L, "nonce", paramsReversed, "secret");
        assertEquals(sign1, sign2);
    }

    @Test
    public void generateSign_differentSecret_shouldProduceDifferentSign() {
        String sign1 = SignUtil.generateSign("demo", 1234567890L, "nonce", null, "secret1");
        String sign2 = SignUtil.generateSign("demo", 1234567890L, "nonce", null, "secret2");
        assertNotSame(sign1, sign2);
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
```

- [ ] **Step 3: 创建 JsonUtil**

文件: `food-open-sdk/src/main/java/com/jiahao/food/sdk/internal/JsonUtil.java`

```java
package com.jiahao.food.sdk.internal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Type;

/**
 * Gson 封装工具。
 */
public class JsonUtil {

    private static final Gson GSON = new GsonBuilder().create();

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    public static <T> T fromJson(String json, Type typeOfT) {
        return GSON.fromJson(json, typeOfT);
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        return GSON.fromJson(json, classOfT);
    }
}
```

- [ ] **Step 4: 验证编译和测试**

Run: `cd food-open-sdk && mvn test -q 2>&1 | tail -5`
Expected: Tests run: 11 (5 from Config + 6 from SignUtil), Failures: 0, Errors: 0

- [ ] **Step 5: 提交**

```bash
git add food-open-sdk/src/main/java/com/jiahao/food/sdk/auth/
git add food-open-sdk/src/main/java/com/jiahao/food/sdk/internal/
git add food-open-sdk/src/test/java/com/jiahao/food/sdk/auth/
git commit -m "feat: add SignUtil, JsonUtil with SignUtil tests"
```

---

### Task 4: 重试策略和 API 执行器

**Files:**
- Create: `food-open-sdk/src/main/java/com/jiahao/food/sdk/retry/RetryPolicy.java`
- Create: `food-open-sdk/src/main/java/com/jiahao/food/sdk/internal/ApiExecutor.java`
- Create: `food-open-sdk/src/test/java/com/jiahao/food/sdk/retry/RetryPolicyTest.java`

- [ ] **Step 1: 创建 RetryPolicy**

文件: `food-open-sdk/src/main/java/com/jiahao/food/sdk/retry/RetryPolicy.java`

```java
package com.jiahao.food.sdk.retry;

import com.jiahao.food.sdk.exception.FoodOpenException;
import com.jiahao.food.sdk.exception.NetworkException;
import com.jiahao.food.sdk.log.FoodOpenLogger;

/**
 * 重试策略：指数退避，仅对 NetworkException 重试。
 */
public class RetryPolicy {

    private final int maxRetries;
    private final FoodOpenLogger logger;

    public RetryPolicy(int maxRetries, FoodOpenLogger logger) {
        this.maxRetries = maxRetries;
        this.logger = logger;
    }

    /**
     * 执行可重试操作。指数退避：200ms, 400ms, 800ms...
     */
    public <T> T execute(Callable<T> callable) throws FoodOpenException {
        int attempt = 0;
        while (true) {
            try {
                return callable.call();
            } catch (NetworkException e) {
                if (attempt >= maxRetries) {
                    throw e;
                }
                long delay = 200L * (1L << attempt);
                logger.warn("Retry attempt " + (attempt + 1) + "/" + maxRetries + " after " + delay + "ms");
                sleep(delay);
                attempt++;
            }
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Java 1.6 兼容的 Callable 接口。
     */
    public interface Callable<T> {
        T call() throws FoodOpenException;
    }
}
```

- [ ] **Step 2: 创建 RetryPolicyTest**

文件: `food-open-sdk/src/test/java/com/jiahao/food/sdk/retry/RetryPolicyTest.java`

```java
package com.jiahao.food.sdk.retry;

import com.jiahao.food.sdk.exception.FoodOpenException;
import com.jiahao.food.sdk.exception.NetworkException;
import com.jiahao.food.sdk.exception.ServerException;
import com.jiahao.food.sdk.log.FoodOpenLogger;
import org.junit.Test;

import static org.junit.Assert.*;

public class RetryPolicyTest {

    private final FoodOpenLogger logger = new FoodOpenLogger(false);

    @Test
    public void execute_onSuccess_shouldReturnValue() throws FoodOpenException {
        RetryPolicy policy = new RetryPolicy(3, logger);
        String result = policy.execute(new RetryPolicy.Callable<String>() {
            public String call() { return "ok"; }
        });
        assertEquals("ok", result);
    }

    @Test(expected = ServerException.class)
    public void execute_onServerException_shouldNotRetry() throws FoodOpenException {
        RetryPolicy policy = new RetryPolicy(3, logger);
        final int[] calls = new int[1];
        policy.execute(new RetryPolicy.Callable<String>() {
            public String call() throws FoodOpenException {
                calls[0]++;
                throw new ServerException(500, "error");
            }
        });
        // Should only be called once (no retry for ServerException)
        assertEquals(1, calls[0]);
    }

    @Test
    public void execute_onNetworkException_shouldRetryThenSucceed() throws FoodOpenException {
        RetryPolicy policy = new RetryPolicy(3, logger);
        final int[] calls = new int[1];
        String result = policy.execute(new RetryPolicy.Callable<String>() {
            public String call() throws FoodOpenException {
                calls[0]++;
                if (calls[0] < 3) {
                    throw new NetworkException("timeout", null);
                }
                return "recovered";
            }
        });
        assertEquals("recovered", result);
        assertEquals(3, calls[0]);
    }

    @Test(expected = NetworkException.class)
    public void execute_onPersistentNetworkException_shouldExhaustRetries() throws FoodOpenException {
        RetryPolicy policy = new RetryPolicy(2, logger);
        policy.execute(new RetryPolicy.Callable<String>() {
            public String call() throws FoodOpenException {
                throw new NetworkException("persistent failure", null);
            }
        });
    }
}
```

- [ ] **Step 3: 创建 ApiExecutor**

文件: `food-open-sdk/src/main/java/com/jiahao/food/sdk/internal/ApiExecutor.java`

```java
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
        String sign = SignUtil.generateSign(config.getAppKey(), timestamp, nonce, params, config.getAppSecret());

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("appKey", config.getAppKey());
        headers.put("timestamp", String.valueOf(timestamp));
        headers.put("nonce", nonce);
        headers.put("sign", sign);

        String fullUrl = buildUrlWithParams(url, params);
        logger.debug("Request: " + fullUrl);

        HttpResponse response = httpClient.execute(fullUrl, headers);
        logger.debug("Response: " + response.getBody());

        ApiResult<T> result = JsonUtil.fromJson(response.getBody(),
                new TypeToken<ApiResult<T>>() {}.getType());

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
}
```

- [ ] **Step 4: 验证编译和测试**

Run: `cd food-open-sdk && mvn test -q 2>&1 | tail -5`
Expected: Tests run: 15 (5 Config + 6 SignUtil + 4 RetryPolicy), Failures: 0, Errors: 0

- [ ] **Step 5: 提交**

```bash
git add food-open-sdk/src/main/java/com/jiahao/food/sdk/retry/
git add food-open-sdk/src/main/java/com/jiahao/food/sdk/internal/
git add food-open-sdk/src/test/java/com/jiahao/food/sdk/retry/
git commit -m "feat: add RetryPolicy, ApiExecutor with RetryPolicy tests"
```

---

### Task 5: Model DTO 类

**Files:**
- Create: `food-open-sdk/src/main/java/com/jiahao/food/sdk/model/AreaDTO.java`
- Create: `food-open-sdk/src/main/java/com/jiahao/food/sdk/model/AreaDetailDTO.java`
- Create: `food-open-sdk/src/main/java/com/jiahao/food/sdk/model/AreaTreeDTO.java`
- Create: `food-open-sdk/src/main/java/com/jiahao/food/sdk/model/OrgInfoDTO.java`
- Create: `food-open-sdk/src/main/java/com/jiahao/food/sdk/model/TownshipOrgDTO.java`

- [ ] **Step 1: 创建 AreaDTO**

文件: `food-open-sdk/src/main/java/com/jiahao/food/sdk/model/AreaDTO.java`

```java
package com.jiahao.food.sdk.model;

import java.math.BigDecimal;

/**
 * 行政区基础信息 DTO。
 */
public class AreaDTO {
    private Long id;
    private String name;
    private String shortName;
    private String adcode;
    private Integer level;
    private BigDecimal lng;
    private BigDecimal lat;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }
    public String getAdcode() { return adcode; }
    public void setAdcode(String adcode) { this.adcode = adcode; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public BigDecimal getLng() { return lng; }
    public void setLng(BigDecimal lng) { this.lng = lng; }
    public BigDecimal getLat() { return lat; }
    public void setLat(BigDecimal lat) { this.lat = lat; }
}
```

- [ ] **Step 2: 创建 AreaDetailDTO**

文件: `food-open-sdk/src/main/java/com/jiahao/food/sdk/model/AreaDetailDTO.java`

```java
package com.jiahao.food.sdk.model;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 行政区详情 DTO。
 */
public class AreaDetailDTO {
    private Long id;
    private String name;
    private String shortName;
    private String fullName;
    private String adcode;
    private Integer level;
    private Integer state;
    private Long provinceId;
    private String provinceName;
    private Long cityId;
    private String cityName;
    private Long countyId;
    private String countyName;
    private Long townshipId;
    private String townshipName;
    private BigDecimal lng;
    private BigDecimal lat;
    private String path;
    private Date createTime;
    private Date updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getAdcode() { return adcode; }
    public void setAdcode(String adcode) { this.adcode = adcode; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public Integer getState() { return state; }
    public void setState(Integer state) { this.state = state; }
    public Long getProvinceId() { return provinceId; }
    public void setProvinceId(Long provinceId) { this.provinceId = provinceId; }
    public String getProvinceName() { return provinceName; }
    public void setProvinceName(String provinceName) { this.provinceName = provinceName; }
    public Long getCityId() { return cityId; }
    public void setCityId(Long cityId) { this.cityId = cityId; }
    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }
    public Long getCountyId() { return countyId; }
    public void setCountyId(Long countyId) { this.countyId = countyId; }
    public String getCountyName() { return countyName; }
    public void setCountyName(String countyName) { this.countyName = countyName; }
    public Long getTownshipId() { return townshipId; }
    public void setTownshipId(Long townshipId) { this.townshipId = townshipId; }
    public String getTownshipName() { return townshipName; }
    public void setTownshipName(String townshipName) { this.townshipName = townshipName; }
    public BigDecimal getLng() { return lng; }
    public void setLng(BigDecimal lng) { this.lng = lng; }
    public BigDecimal getLat() { return lat; }
    public void setLat(BigDecimal lat) { this.lat = lat; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
```

- [ ] **Step 3: 创建 AreaTreeDTO**

文件: `food-open-sdk/src/main/java/com/jiahao/food/sdk/model/AreaTreeDTO.java`

```java
package com.jiahao.food.sdk.model;

import java.util.List;

/**
 * 行政区树节点 DTO。
 */
public class AreaTreeDTO {
    private Long id;
    private Long parentId;
    private String name;
    private Integer level;
    private List<AreaTreeDTO> children;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public List<AreaTreeDTO> getChildren() { return children; }
    public void setChildren(List<AreaTreeDTO> children) { this.children = children; }
}
```

- [ ] **Step 4: 创建 OrgInfoDTO**

文件: `food-open-sdk/src/main/java/com/jiahao/food/sdk/model/OrgInfoDTO.java`

```java
package com.jiahao.food.sdk.model;

/**
 * 组织归属信息 DTO。
 */
public class OrgInfoDTO {
    private Long areaId;
    private String areaName;
    private Long orgId;
    private String orgName;
    private Long officeId;
    private String officeName;
    private Long regionId;
    private String regionName;

    public Long getAreaId() { return areaId; }
    public void setAreaId(Long areaId) { this.areaId = areaId; }
    public String getAreaName() { return areaName; }
    public void setAreaName(String areaName) { this.areaName = areaName; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getOrgName() { return orgName; }
    public void setOrgName(String orgName) { this.orgName = orgName; }
    public Long getOfficeId() { return officeId; }
    public void setOfficeId(Long officeId) { this.officeId = officeId; }
    public String getOfficeName() { return officeName; }
    public void setOfficeName(String officeName) { this.officeName = officeName; }
    public Long getRegionId() { return regionId; }
    public void setRegionId(Long regionId) { this.regionId = regionId; }
    public String getRegionName() { return regionName; }
    public void setRegionName(String regionName) { this.regionName = regionName; }
}
```

- [ ] **Step 5: 创建 TownshipOrgDTO**

文件: `food-open-sdk/src/main/java/com/jiahao/food/sdk/model/TownshipOrgDTO.java`

```java
package com.jiahao.food.sdk.model;

/**
 * 乡镇组织信息 DTO。
 */
public class TownshipOrgDTO {
    private Long townshipId;
    private String townshipName;
    private Long regionId;
    private String regionName;
    private Long officeId;
    private String officeName;
    private Long districtId;
    private String districtName;

    public Long getTownshipId() { return townshipId; }
    public void setTownshipId(Long townshipId) { this.townshipId = townshipId; }
    public String getTownshipName() { return townshipName; }
    public void setTownshipName(String townshipName) { this.townshipName = townshipName; }
    public Long getRegionId() { return regionId; }
    public void setRegionId(Long regionId) { this.regionId = regionId; }
    public String getRegionName() { return regionName; }
    public void setRegionName(String regionName) { this.regionName = regionName; }
    public Long getOfficeId() { return officeId; }
    public void setOfficeId(Long officeId) { this.officeId = officeId; }
    public String getOfficeName() { return officeName; }
    public void setOfficeName(String officeName) { this.officeName = officeName; }
    public Long getDistrictId() { return districtId; }
    public void setDistrictId(Long districtId) { this.districtId = districtId; }
    public String getDistrictName() { return districtName; }
    public void setDistrictName(String districtName) { this.districtName = districtName; }
}
```

- [ ] **Step 6: 验证编译**

Run: `cd food-open-sdk && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: 提交**

```bash
git add food-open-sdk/src/main/java/com/jiahao/food/sdk/model/
git commit -m "feat: add all Model DTOs (AreaDTO, AreaDetailDTO, AreaTreeDTO, OrgInfoDTO, TownshipOrgDTO)"
```

---

### Task 6: API 接口层和入口类

**Files:**
- Create: `food-open-sdk/src/main/java/com/jiahao/food/sdk/api/AreaApi.java`
- Create: `food-open-sdk/src/main/java/com/jiahao/food/sdk/api/GeoApi.java`
- Create: `food-open-sdk/src/main/java/com/jiahao/food/sdk/FoodOpenClient.java`

- [ ] **Step 1: 创建 AreaApi**

文件: `food-open-sdk/src/main/java/com/jiahao/food/sdk/api/AreaApi.java`

```java
package com.jiahao.food.sdk.api;

import com.google.gson.reflect.TypeToken;
import com.jiahao.food.sdk.exception.FoodOpenException;
import com.jiahao.food.sdk.internal.ApiExecutor;
import com.jiahao.food.sdk.model.AreaDTO;
import com.jiahao.food.sdk.model.AreaDetailDTO;
import com.jiahao.food.sdk.model.AreaTreeDTO;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 行政区查询 API。
 */
public class AreaApi {

    private final ApiExecutor executor;

    public AreaApi(ApiExecutor executor) {
        this.executor = executor;
    }

    /**
     * 查询省列表。
     */
    public List<AreaDTO> provinces() throws FoodOpenException {
        Type type = new TypeToken<List<AreaDTO>>() {}.getType();
        return executor.execute("/area/provinces", null, type);
    }

    /**
     * 查询指定省下的城市列表。
     */
    public List<AreaDTO> cities(Long provinceId) throws FoodOpenException {
        Map<String, String> params = new HashMap<String, String>();
        params.put("provinceId", String.valueOf(provinceId));
        Type type = new TypeToken<List<AreaDTO>>() {}.getType();
        return executor.execute("/area/cities", params, type);
    }

    /**
     * 查询指定市下的区县列表。
     */
    public List<AreaDTO> counties(Long cityId) throws FoodOpenException {
        Map<String, String> params = new HashMap<String, String>();
        params.put("cityId", String.valueOf(cityId));
        Type type = new TypeToken<List<AreaDTO>>() {}.getType();
        return executor.execute("/area/counties", params, type);
    }

    /**
     * 查询指定区县下的乡镇列表。
     */
    public List<AreaDTO> townships(Long countyId) throws FoodOpenException {
        Map<String, String> params = new HashMap<String, String>();
        params.put("countyId", String.valueOf(countyId));
        Type type = new TypeToken<List<AreaDTO>>() {}.getType();
        return executor.execute("/area/townships", params, type);
    }

    /**
     * 查询行政区详情。
     */
    public AreaDetailDTO detail(Long areaId) throws FoodOpenException {
        Map<String, String> params = new HashMap<String, String>();
        params.put("areaId", String.valueOf(areaId));
        return executor.execute("/area/detail", params, AreaDetailDTO.class);
    }

    /**
     * 查询行政区树。
     */
    public List<AreaTreeDTO> tree(Integer type, Long areaId, Integer depth) throws FoodOpenException {
        Map<String, String> params = new HashMap<String, String>();
        if (type != null) params.put("type", String.valueOf(type));
        if (areaId != null) params.put("areaId", String.valueOf(areaId));
        if (depth != null) params.put("depth", String.valueOf(depth));
        Type treeType = new TypeToken<List<AreaTreeDTO>>() {}.getType();
        return executor.execute("/area/tree", params, treeType);
    }

    /**
     * 查询省列表（简化）。
     */
    public List<AreaDTO> provinceList() throws FoodOpenException {
        Type type = new TypeToken<List<AreaDTO>>() {}.getType();
        return executor.execute("/area/province-list", null, type);
    }

    /**
     * 查询城市列表（全部）。
     */
    public List<AreaDTO> cityList() throws FoodOpenException {
        Type type = new TypeToken<List<AreaDTO>>() {}.getType();
        return executor.execute("/area/city-list", null, type);
    }

    /**
     * 查询区县列表（全部）。
     */
    public List<AreaDTO> countyList() throws FoodOpenException {
        Type type = new TypeToken<List<AreaDTO>>() {}.getType();
        return executor.execute("/area/county-list", null, type);
    }

    /**
     * 查询乡镇列表（全部）。
     */
    public List<AreaDTO> townshipList() throws FoodOpenException {
        Type type = new TypeToken<List<AreaDTO>>() {}.getType();
        return executor.execute("/area/township-list", null, type);
    }
}
```

- [ ] **Step 2: 创建 GeoApi**

文件: `food-open-sdk/src/main/java/com/jiahao/food/sdk/api/GeoApi.java`

```java
package com.jiahao.food.sdk.api;

import com.jiahao.food.sdk.exception.FoodOpenException;
import com.jiahao.food.sdk.internal.ApiExecutor;
import com.jiahao.food.sdk.model.OrgInfoDTO;
import com.jiahao.food.sdk.model.TownshipOrgDTO;

import java.util.HashMap;
import java.util.Map;

/**
 * 组织归属查询 API。
 */
public class GeoApi {

    private final ApiExecutor executor;

    public GeoApi(ApiExecutor executor) {
        this.executor = executor;
    }

    /**
     * 根据行政区 ID 查询归属组织信息。
     */
    public OrgInfoDTO getOrgByAreaId(Long areaId) throws FoodOpenException {
        Map<String, String> params = new HashMap<String, String>();
        params.put("areaId", String.valueOf(areaId));
        return executor.execute("/geo/org", params, OrgInfoDTO.class);
    }

    /**
     * 根据乡镇 ID 查询归属组织信息。
     */
    public TownshipOrgDTO getOrgByTownshipId(Long townshipId) throws FoodOpenException {
        Map<String, String> params = new HashMap<String, String>();
        params.put("townshipId", String.valueOf(townshipId));
        return executor.execute("/geo/org/by-township", params, TownshipOrgDTO.class);
    }
}
```

- [ ] **Step 3: 创建 FoodOpenClient**

文件: `food-open-sdk/src/main/java/com/jiahao/food/sdk/FoodOpenClient.java`

```java
package com.jiahao.food.sdk;

import com.jiahao.food.sdk.config.ClientConfig;
import com.jiahao.food.sdk.exception.ClientException;
import com.jiahao.food.sdk.exception.FoodOpenException;
import com.jiahao.food.sdk.http.SdkHttpClient;
import com.jiahao.food.sdk.internal.ApiExecutor;
import com.jiahao.food.sdk.log.FoodOpenLogger;
import com.jiahao.food.sdk.retry.RetryPolicy;
import com.jiahao.food.sdk.api.AreaApi;
import com.jiahao.food.sdk.api.GeoApi;

import java.io.IOException;

/**
 * 开放 API SDK 入口类。
 */
public class FoodOpenClient {

    private final ClientConfig config;
    private final SdkHttpClient httpClient;
    private final ApiExecutor apiExecutor;
    private final AreaApi areaApi;
    private final GeoApi geoApi;

    private FoodOpenClient(ClientConfig config) throws ClientException {
        this.config = config;
        this.httpClient = new SdkHttpClient(config);
        FoodOpenLogger logger = new FoodOpenLogger(config.isEnableLogging());
        RetryPolicy retryPolicy = new RetryPolicy(config.getMaxRetries(), logger);
        this.apiExecutor = new ApiExecutor(config, httpClient, retryPolicy, logger);
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
```

- [ ] **Step 4: 验证编译**

Run: `cd food-open-sdk && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add food-open-sdk/src/main/java/com/jiahao/food/sdk/api/
git add food-open-sdk/src/main/java/com/jiahao/food/sdk/FoodOpenClient.java
git commit -m "feat: add AreaApi, GeoApi, and FoodOpenClient entry class"
```

---

### Task 7: 父 POM 注册子模块并验证整体编译

**Files:**
- Modify: `food-manage-web/../pom.xml` (project root pom.xml)

- [ ] **Step 1: 检查父 POM 结构**

Run: `cat pom.xml`
Check: 确认父 POM 的 `<modules>` 部分或 `<packaging>` 类型

- [ ] **Step 2: 添加 food-open-sdk 到父 POM modules**

如果父 POM 已有 `<modules>` 标签，在其中添加：
```xml
<module>food-open-sdk</module>
```

如果父 POM 没有 `<modules>` 标签，在 `<project>` 根下添加：
```xml
<modules>
    <module>food-manage-web</module>
    <module>food-open-sdk</module>
</modules>
```

- [ ] **Step 3: 验证父 POM 编译**

Run: `mvn compile -pl food-open-sdk -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 运行 SDK 全部测试**

Run: `mvn test -pl food-open-sdk -q 2>&1 | tail -5`
Expected: Tests run: 15, Failures: 0, Errors: 0

- [ ] **Step 5: 提交**

```bash
git add pom.xml
git commit -m "chore: register food-open-sdk as Maven submodule"
```

---

## Self-Review

### 1. Spec coverage

| Spec 需求 | 对应 Task |
|-----------|-----------|
| Maven 构建 (pom.xml, Java 1.6, 依赖) | Task 1 |
| 异常体系 (FoodOpenException, ClientException, ServerException, NetworkException) | Task 1 |
| 配置类 (ClientConfig + Builder) | Task 2 |
| HTTP 客户端 (SdkHttpClient + HttpResponse) | Task 2 |
| 日志 (FoodOpenLogger, SLF4J/JDK 降级) | Task 2 |
| 签名工具 (SignUtil, 自实现 MD5) | Task 3 |
| JSON 工具 (JsonUtil, Gson) | Task 3 |
| 重试策略 (RetryPolicy, 指数退避) | Task 4 |
| API 执行器 (ApiExecutor, 签名+重试) | Task 4 |
| Model DTOs (5 个) | Task 5 |
| API 接口层 (AreaApi 10 方法, GeoApi 2 方法) | Task 6 |
| 入口类 (FoodOpenClient + Builder) | Task 6 |
| 父 POM 注册子模块 | Task 7 |

全部覆盖，无遗漏。

### 2. Placeholder scan
- 无 TBD/TODO
- 所有步骤包含完整代码
- 所有运行命令包含预期输出
- 无 "add tests for the above" 等模糊描述

### 3. Type consistency
- `ApiExecutor.execute(String path, Map<String, String> params, Type dataType)` 签名在 Task 4 定义，Task 6 中 AreaApi/GeoApi 调用一致
- `FoodOpenClient.Builder.build()` 委托给 `ClientConfig.Builder.build()` 再构造 `FoodOpenClient`
- `FoodOpenException` 是所有异常的基类，`build()` 方法抛出 `ClientException`（继承自 `FoodOpenException`）
- `RetryPolicy.Callable<T>` 接口在 Task 4 定义，Task 4 ApiExecutor 内部使用一致
- 所有 DTO 字段类型与后端一致（Long, String, Integer, BigDecimal, Date）

### 4. Scope check
7 个 Task，每个 Task 产出独立可编译的变更，聚焦 SDK 开发，不涉及后端修改。
