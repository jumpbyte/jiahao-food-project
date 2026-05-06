# /api/open/** 接口集成测试

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为所有 13 个 `/api/open/**` 接口编写 MockMvc 和 RestTemplate 两种集成测试，验证签名校验和接口返回正确。

**Architecture:** 共用签名工具类 `OpenApiTestSupport` 生成签名头，MockMvc 测试通过 `@AutoConfigureMockMvc` 加载拦截器链路，RestTemplate 测试通过 `@SpringBootTest(webEnvironment = RANDOM_PORT)` 发真实 HTTP 请求。两套测试覆盖同一批接口。

**Tech Stack:** Java 17, Spring Boot 3.2.5, JUnit 5, Spring MockMvc, TestRestTemplate

---

## File Structure

| 操作 | 文件 | 职责 |
|------|------|------|
| 创建 | `food-manage-web/src/test/java/com/jihao/food/open/OpenApiTestSupport.java` | 签名生成工具、测试常量 |
| 创建 | `food-manage-web/src/test/java/com/jihao/food/open/OpenApiMockMvcTest.java` | MockMvc 集成测试（13 个接口） |
| 创建 | `food-manage-web/src/test/java/com/jihao/food/open/OpenApiRestTemplateTest.java` | RestTemplate HTTP 测试（13 个接口） |

---

### Task 1: 创建 OpenApiTestSupport 共用工具类

**Files:**
- Create: `food-manage-web/src/test/java/com/jihao/food/open/OpenApiTestSupport.java`

- [ ] **Step 1: 创建目录和工具类**

创建 `food-manage-web/src/test/java/com/jihao/food/open/OpenApiTestSupport.java`：

```java
package com.jihao.food.open;

import org.apache.commons.codec.digest.DigestUtils;

import java.net.URI;
import java.util.Map;
import java.util.TreeMap;

public final class OpenApiTestSupport {

    static final String APP_KEY = "demo";
    static final String SECRET = "e02ca276f7d444c099f571d7bee8fac6";

    private OpenApiTestSupport() {}

    /**
     * 生成签名
     * sign = MD5(appKey + timestamp + nonce + sortedQuery + secret)
     */
    public static String generateSign(String sortedQuery) {
        long timestamp = System.currentTimeMillis();
        String nonce = "test-nonce-" + timestamp;
        return generateSign(APP_KEY, timestamp, nonce, sortedQuery, SECRET);
    }

    /**
     * 生成签名 + 返回时间戳和 nonce 供调用者使用
     */
    public static SignInfo createSignInfo(String sortedQuery) {
        long timestamp = System.currentTimeMillis();
        String nonce = "test-nonce-" + timestamp;
        String sign = generateSign(APP_KEY, timestamp, nonce, sortedQuery, SECRET);
        return new SignInfo(APP_KEY, timestamp, nonce, sign);
    }

    public static String generateSign(String appKey, long timestamp, String nonce, String sortedQuery, String secret) {
        String signStr = appKey + timestamp + nonce + sortedQuery + secret;
        return DigestUtils.md5Hex(signStr).toUpperCase();
    }

    /**
     * 将 query params 按 key 排序后拼接
     */
    public static String sortedQuery(String... keyValuePairs) {
        if (keyValuePairs.length == 0) {
            return "";
        }
        Map<String, String> map = new TreeMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            map.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (sb.length() > 0) sb.append('&');
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.toString();
    }

    public record SignInfo(String appKey, long timestamp, String nonce, String sign) {}

    /**
     * 构建带 query params 的 URI
     */
    public static URI uri(String path, String... keyValuePairs) {
        String query = sortedQuery(keyValuePairs);
        if (query.isEmpty()) {
            return URI.create(path);
        }
        return URI.create(path + "?" + query);
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd food-manage-web && export JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home" && mvn test-compile
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add food-manage-web/src/test/java/com/jihao/food/open/OpenApiTestSupport.java
git commit -m "test: add OpenApiTestSupport utility for open API integration tests

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 2: 创建 OpenApiMockMvcTest

**Files:**
- Create: `food-manage-web/src/test/java/com/jihao/food/open/OpenApiMockMvcTest.java`

- [ ] **Step 1: 创建 MockMvc 测试类**

创建 `food-manage-web/src/test/java/com/jihao/food/open/OpenApiMockMvcTest.java`：

```java
package com.jihao.food.open;

import com.jihao.food.open.OpenApiTestSupport.SignInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static com.jihao.food.open.OpenApiTestSupport.createSignInfo;
import static com.jihao.food.open.OpenApiTestSupport.sortedQuery;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void provinces() throws Exception {
        SignInfo sign = createSignInfo("");
        mockMvc.perform(withSign("/api/open/area/provinces", sign))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void cities() throws Exception {
        SignInfo sign = createSignInfo(sortedQuery("provinceId", "1"));
        mockMvc.perform(withSign("/api/open/area/cities", sign, "provinceId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void counties() throws Exception {
        SignInfo sign = createSignInfo(sortedQuery("cityId", "2"));
        mockMvc.perform(withSign("/api/open/area/counties", sign, "cityId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void townships() throws Exception {
        SignInfo sign = createSignInfo(sortedQuery("countyId", "3"));
        mockMvc.perform(withSign("/api/open/area/townships", sign, "countyId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void detail() throws Exception {
        SignInfo sign = createSignInfo(sortedQuery("areaId", "1"));
        mockMvc.perform(withSign("/api/open/area/detail", sign, "areaId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void tree_singleParam() throws Exception {
        SignInfo sign = createSignInfo(sortedQuery("depth", "1"));
        mockMvc.perform(withSign("/api/open/area/tree", sign, "depth", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void tree_multiParams_sorted() throws Exception {
        SignInfo sign = createSignInfo(sortedQuery("type", "1", "depth", "2"));
        mockMvc.perform(withSign("/api/open/area/tree", sign, "type", "1", "depth", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void provinceList() throws Exception {
        SignInfo sign = createSignInfo("");
        mockMvc.perform(withSign("/api/open/area/province-list", sign))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void cityList() throws Exception {
        SignInfo sign = createSignInfo("");
        mockMvc.perform(withSign("/api/open/area/city-list", sign))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void countyList() throws Exception {
        SignInfo sign = createSignInfo("");
        mockMvc.perform(withSign("/api/open/area/county-list", sign))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void townshipList() throws Exception {
        SignInfo sign = createSignInfo("");
        mockMvc.perform(withSign("/api/open/area/township-list", sign))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void getOrgByAreaId() throws Exception {
        SignInfo sign = createSignInfo(sortedQuery("areaId", "1"));
        mockMvc.perform(withSign("/api/open/geo/org", sign, "areaId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void getOrgByTownshipId() throws Exception {
        SignInfo sign = createSignInfo(sortedQuery("townshipId", "1"));
        mockMvc.perform(withSign("/api/open/geo/org/by-township", sign, "townshipId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private ResultActions withSign(String path, SignInfo sign, String... keyValuePairs) throws Exception {
        var req = get(OpenApiTestSupport.uri(path, keyValuePairs));
        req.header("appKey", sign.appKey());
        req.header("timestamp", String.valueOf(sign.timestamp()));
        req.header("nonce", sign.nonce());
        req.header("sign", sign.sign());
        return mockMvc.perform(req);
    }
}
```

- [ ] **Step 2: 运行测试**

```bash
cd food-manage-web && export JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home" && mvn test -Dtest=OpenApiMockMvcTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: All 13 tests PASS

- [ ] **Step 3: 提交**

```bash
git add food-manage-web/src/test/java/com/jihao/food/open/OpenApiMockMvcTest.java
git commit -m "test: add MockMvc integration tests for all /api/open/** endpoints

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 3: 创建 OpenApiRestTemplateTest

**Files:**
- Create: `food-manage-web/src/test/java/com/jihao/food/open/OpenApiRestTemplateTest.java`

- [ ] **Step 1: 创建 RestTemplate 测试类**

创建 `food-manage-web/src/test/java/com/jihao/food/open/OpenApiRestTemplateTest.java`：

```java
package com.jihao.food.open;

import com.jihao.food.open.OpenApiTestSupport.SignInfo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static com.jihao.food.open.OpenApiTestSupport.createSignInfo;
import static com.jihao.food.open.OpenApiTestSupport.sortedQuery;
import static com.jihao.food.open.OpenApiTestSupport.uri;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiRestTemplateTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    void provinces() {
        SignInfo sign = createSignInfo("");
        String url = baseUrl() + "/api/open/area/provinces";
        ResponseEntity<Map> resp = get(url, sign);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(0, resp.getBody().get("code"));
    }

    @Test
    void cities() {
        SignInfo sign = createSignInfo(sortedQuery("provinceId", "1"));
        String url = baseUrl() + "/api/open/area/cities?provinceId=1";
        ResponseEntity<Map> resp = get(url, sign);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(0, resp.getBody().get("code"));
    }

    @Test
    void counties() {
        SignInfo sign = createSignInfo(sortedQuery("cityId", "2"));
        String url = baseUrl() + "/api/open/area/counties?cityId=2";
        ResponseEntity<Map> resp = get(url, sign);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(0, resp.getBody().get("code"));
    }

    @Test
    void townships() {
        SignInfo sign = createSignInfo(sortedQuery("countyId", "3"));
        String url = baseUrl() + "/api/open/area/townships?countyId=3";
        ResponseEntity<Map> resp = get(url, sign);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(0, resp.getBody().get("code"));
    }

    @Test
    void detail() {
        SignInfo sign = createSignInfo(sortedQuery("areaId", "1"));
        String url = baseUrl() + "/api/open/area/detail?areaId=1";
        ResponseEntity<Map> resp = get(url, sign);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(0, resp.getBody().get("code"));
    }

    @Test
    void tree_singleParam() {
        SignInfo sign = createSignInfo(sortedQuery("depth", "1"));
        String url = baseUrl() + "/api/open/area/tree?depth=1";
        ResponseEntity<Map> resp = get(url, sign);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(0, resp.getBody().get("code"));
    }

    @Test
    void tree_multiParams_sorted() {
        SignInfo sign = createSignInfo(sortedQuery("type", "1", "depth", "2"));
        String url = baseUrl() + "/api/open/area/tree?type=1&depth=2";
        ResponseEntity<Map> resp = get(url, sign);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(0, resp.getBody().get("code"));
    }

    @Test
    void provinceList() {
        SignInfo sign = createSignInfo("");
        String url = baseUrl() + "/api/open/area/province-list";
        ResponseEntity<Map> resp = get(url, sign);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(0, resp.getBody().get("code"));
    }

    @Test
    void cityList() {
        SignInfo sign = createSignInfo("");
        String url = baseUrl() + "/api/open/area/city-list";
        ResponseEntity<Map> resp = get(url, sign);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(0, resp.getBody().get("code"));
    }

    @Test
    void countyList() {
        SignInfo sign = createSignInfo("");
        String url = baseUrl() + "/api/open/area/county-list";
        ResponseEntity<Map> resp = get(url, sign);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(0, resp.getBody().get("code"));
    }

    @Test
    void townshipList() {
        SignInfo sign = createSignInfo("");
        String url = baseUrl() + "/api/open/area/township-list";
        ResponseEntity<Map> resp = get(url, sign);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(0, resp.getBody().get("code"));
    }

    @Test
    void getOrgByAreaId() {
        SignInfo sign = createSignInfo(sortedQuery("areaId", "1"));
        String url = baseUrl() + "/api/open/geo/org?areaId=1";
        ResponseEntity<Map> resp = get(url, sign);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(0, resp.getBody().get("code"));
    }

    @Test
    void getOrgByTownshipId() {
        SignInfo sign = createSignInfo(sortedQuery("townshipId", "1"));
        String url = baseUrl() + "/api/open/geo/org/by-township?townshipId=1";
        ResponseEntity<Map> resp = get(url, sign);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(0, resp.getBody().get("code"));
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private ResponseEntity<Map> get(String url, SignInfo sign) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("appKey", sign.appKey());
        headers.set("timestamp", String.valueOf(sign.timestamp()));
        headers.set("nonce", sign.nonce());
        headers.set("sign", sign.sign());
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    }
}
```

- [ ] **Step 2: 运行测试**

```bash
cd food-manage-web && export JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home" && mvn test -Dtest=OpenApiRestTemplateTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: All 13 tests PASS

- [ ] **Step 3: 提交**

```bash
git add food-manage-web/src/test/java/com/jihao/food/open/OpenApiRestTemplateTest.java
git commit -m "test: add RestTemplate HTTP tests for all /api/open/** endpoints

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 4: 运行全量测试并提交

- [ ] **Step 1: 运行全量测试**

```bash
cd food-manage-web && export JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home" && mvn test
```

Expected: All tests PASS (SignUtilTest + ApiSignInterceptorTest + OpenApiMockMvcTest + OpenApiRestTemplateTest = 12 + 13 + 13 = 38 tests)

- [ ] **Step 2: 最终提交**

```bash
git log --oneline -3
```

确认所有提交记录完整。
