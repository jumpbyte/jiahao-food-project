# Open API 集成测试 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `/api/open/**` 所有 12 个接口编写 Spring Boot `@SpringBootTest` + `MockMvc` 集成测试，覆盖真实签名拦截器 → Controller → Service → Mapper → MySQL 开发数据库全链路。

**Architecture:** 使用 `@AutoConfigureMockMvc` 启动完整 Spring 容器，通过 `@ActiveProfiles("dev")` 连接 `jiahao_food_db` 开发库，自动计算 MD5 签名。测试类包含签名验证边界测试 + 所有接口 happy path 测试。

**Tech Stack:** Spring Boot Test, MockMvc, JUnit 5, Jackson, commons-codec, MySQL

---

## File Structure

| 文件 | 操作 | 说明 |
|------|------|------|
| `food-manage-web/src/test/java/com/jihao/food/integration/OpenApiIntegrationTest.java` | 新建 | 集成测试主类，包含签名辅助方法和全部测试用例 |

---

## Task 1: 创建测试类骨架和签名辅助方法

**Files:**
- Create: `food-manage-web/src/test/java/com/jihao/food/integration/OpenApiIntegrationTest.java`

- [ ] **Step 1: 创建测试类文件**

```java
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
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.LinkedHashMap;
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
```

- [ ] **Step 2: 验证编译**

Run: `cd food-manage-web && export JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home" && mvn test-compile -q`

Expected: No compilation errors

- [ ] **Step 3: 验证测试框架能启动**

Run: `cd food-manage-web && export JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home" && mvn test -Dtest=OpenApiIntegrationTest -q 2>&1 | tail -5`

Expected: "Tests run: 0, Failures: 0, Errors: 0"（还没有测试用例）

- [ ] **Step 4: 提交**

```bash
git add food-manage-web/src/test/java/com/jihao/food/integration/OpenApiIntegrationTest.java
git commit -m "test: add Open API integration test skeleton with sign helper"
```

---

## Task 2: 签名验证测试

**Files:**
- Modify: `food-manage-web/src/test/java/com/jihao/food/integration/OpenApiIntegrationTest.java`

在类的 `}` 之前添加以下 4 个测试方法：

- [ ] **Step 1: 添加签名验证测试**

```java
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
```

- [ ] **Step 2: 运行测试验证**

Run: `cd food-manage-web && export JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home" && mvn test -Dtest=OpenApiIntegrationTest#preHandle* -q 2>&1 | tail -10`

Expected: Tests run: 4, Failures: 0, Errors: 0

- [ ] **Step 3: 提交**

```bash
git add food-manage-web/src/test/java/com/jihao/food/integration/OpenApiIntegrationTest.java
git commit -m "test: add signature validation integration tests"
```

---

## Task 3: 行政区查询接口测试

**Files:**
- Modify: `food-manage-web/src/test/java/com/jihao/food/integration/OpenApiIntegrationTest.java`

在签名验证测试之后添加：

- [ ] **Step 1: 添加 provinces 测试**

```java
// ===== 行政区查询接口测试 =====

@Test
void provinces_shouldReturnNonEmptyList() throws Exception {
    String result = mockMvc.perform(signedGet("/api/open/area/provinces", null))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data").isArray())
        .andReturn().getResponse().getContentAsString();

    JsonNode data = objectMapper.readTree(result).get("data");
    assertThat(data.size()).isGreaterThan(0);
    assertThat(data.get(0).has("name")).isTrue();
    assertThat(data.get(0).has("id")).isTrue();
}
```

- [ ] **Step 2: 添加 cities 测试**

```java
@Test
void cities_withValidProvinceId_shouldReturnCities() throws Exception {
    // 先获取一个 province ID
    String provinceResult = mockMvc.perform(signedGet("/api/open/area/provinces", null))
        .andReturn().getResponse().getContentAsString();
    Long provinceId = objectMapper.readTree(provinceResult).get("data").get(0).get("id").asLong();

    Map<String, Object> params = Map.of("provinceId", provinceId);
    String result = mockMvc.perform(signedGet("/api/open/area/cities", params))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data").isArray())
        .andReturn().getResponse().getContentAsString();

    JsonNode data = objectMapper.readTree(result).get("data");
    assertThat(data.size()).isGreaterThan(0);
}
```

- [ ] **Step 3: 添加 counties 测试**

```java
@Test
void counties_withValidCityId_shouldReturnCounties() throws Exception {
    // 先获取 province → city → county
    String provinceResult = mockMvc.perform(signedGet("/api/open/area/provinces", null))
        .andReturn().getResponse().getContentAsString();
    Long provinceId = objectMapper.readTree(provinceResult).get("data").get(0).get("id").asLong();

    String cityResult = mockMvc.perform(signedGet("/api/open/area/cities", Map.of("provinceId", provinceId)))
        .andReturn().getResponse().getContentAsString();
    JsonNode cities = objectMapper.readTree(cityResult).get("data");
    if (cities.isEmpty()) {
        return; // 跳过
    }
    Long cityId = cities.get(0).get("id").asLong();

    String result = mockMvc.perform(signedGet("/api/open/area/counties", Map.of("cityId", cityId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data").isArray())
        .andReturn().getResponse().getContentAsString();

    JsonNode data = objectMapper.readTree(result).get("data");
    assertThat(data.size()).isGreaterThan(0);
}
```

- [ ] **Step 4: 添加 townships 测试**

```java
@Test
void townships_withValidCountyId_shouldReturnTownships() throws Exception {
    // province → city → county → township
    String provinceResult = mockMvc.perform(signedGet("/api/open/area/provinces", null))
        .andReturn().getResponse().getContentAsString();
    Long provinceId = objectMapper.readTree(provinceResult).get("data").get(0).get("id").asLong();

    String cityResult = mockMvc.perform(signedGet("/api/open/area/cities", Map.of("provinceId", provinceId)))
        .andReturn().getResponse().getContentAsString();
    Long cityId = objectMapper.readTree(cityResult).get("data").get(0).get("id").asLong();

    String countyResult = mockMvc.perform(signedGet("/api/open/area/counties", Map.of("cityId", cityId)))
        .andReturn().getResponse().getContentAsString();
    Long countyId = objectMapper.readTree(countyResult).get("data").get(0).get("id").asLong();

    String result = mockMvc.perform(signedGet("/api/open/area/townships", Map.of("countyId", countyId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data").isArray())
        .andReturn().getResponse().getContentAsString();

    JsonNode data = objectMapper.readTree(result).get("data");
    assertThat(data.size()).isGreaterThan(0);
}
```

- [ ] **Step 5: 添加 detail 测试**

```java
@Test
void detail_withValidAreaId_shouldReturnDetail() throws Exception {
    String provinceResult = mockMvc.perform(signedGet("/api/open/area/provinces", null))
        .andReturn().getResponse().getContentAsString();
    Long areaId = objectMapper.readTree(provinceResult).get("data").get(0).get("id").asLong();

    mockMvc.perform(signedGet("/api/open/area/detail", Map.of("areaId", areaId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data").isObject())
        .andExpect(jsonPath("$.data.name").isNotEmpty());
}
```

- [ ] **Step 6: 添加 tree 测试**

```java
@Test
void tree_shouldReturnTreeStructure() throws Exception {
    Map<String, Object> params = Map.of("depth", 2);
    mockMvc.perform(signedGet("/api/open/area/tree", params))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data").isArray());
}
```

- [ ] **Step 7: 运行测试验证**

Run: `cd food-manage-web && export JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home" && mvn test -Dtest=OpenApiIntegrationTest#provinces*,OpenApiIntegrationTest#cities*,OpenApiIntegrationTest#counties*,OpenApiIntegrationTest#townships*,OpenApiIntegrationTest#detail*,OpenApiIntegrationTest#tree* -q 2>&1 | tail -10`

Expected: Tests run: 6, Failures: 0, Errors: 0

- [ ] **Step 8: 提交**

```bash
git add food-manage-web/src/test/java/com/jihao/food/integration/OpenApiIntegrationTest.java
git commit -m "test: add area query API integration tests (provinces/cities/counties/townships/detail/tree)"
```

---

## Task 4: 行政区列表接口测试

**Files:**
- Modify: `food-manage-web/src/test/java/com/jihao/food/integration/OpenApiIntegrationTest.java`

- [ ] **Step 1: 添加 4 个列表测试**

```java
// ===== 行政区列表接口测试 =====

@Test
void provinceList_shouldReturnNonEmptyList() throws Exception {
    String result = mockMvc.perform(signedGet("/api/open/area/province-list", null))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data").isArray())
        .andReturn().getResponse().getContentAsString();

    JsonNode data = objectMapper.readTree(result).get("data");
    assertThat(data.size()).isGreaterThan(0);
}

@Test
void cityList_shouldReturnList() throws Exception {
    String result = mockMvc.perform(signedGet("/api/open/area/city-list", null))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data").isArray())
        .andReturn().getResponse().getContentAsString();

    JsonNode data = objectMapper.readTree(result).get("data");
    assertThat(data.size()).isGreaterThan(0);
}

@Test
void countyList_shouldReturnList() throws Exception {
    String result = mockMvc.perform(signedGet("/api/open/area/county-list", null))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data").isArray())
        .andReturn().getResponse().getContentAsString();

    JsonNode data = objectMapper.readTree(result).get("data");
    assertThat(data.size()).isGreaterThan(0);
}

@Test
void townshipList_shouldReturnList() throws Exception {
    String result = mockMvc.perform(signedGet("/api/open/area/township-list", null))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data").isArray())
        .andReturn().getResponse().getContentAsString();

    JsonNode data = objectMapper.readTree(result).get("data");
    assertThat(data.size()).isGreaterThan(0);
}
```

- [ ] **Step 2: 运行测试验证**

Run: `cd food-manage-web && export JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home" && mvn test -Dtest=OpenApiIntegrationTest#*List* -q 2>&1 | tail -10`

Expected: Tests run: 4, Failures: 0, Errors: 0

- [ ] **Step 3: 提交**

```bash
git add food-manage-web/src/test/java/com/jihao/food/integration/OpenApiIntegrationTest.java
git commit -m "test: add area list API integration tests (province/city/county/township-list)"
```

---

## Task 5: 组织归属查询接口测试

**Files:**
- Modify: `food-manage-web/src/test/java/com/jihao/food/integration/OpenApiIntegrationTest.java`

- [ ] **Step 1: 添加组织归属查询测试**

```java
// ===== 组织归属查询接口测试 =====

@Test
void getOrg_withValidAreaId_shouldReturnResult() throws Exception {
    // 先获取一个 township ID
    Long townshipId = getTownshipId();

    mockMvc.perform(signedGet("/api/open/geo/org", Map.of("areaId", townshipId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));
}

@Test
void getOrgByTownship_withValidTownshipId_shouldReturnResult() throws Exception {
    Long townshipId = getTownshipId();

    mockMvc.perform(signedGet("/api/open/geo/org/by-township", Map.of("townshipId", townshipId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));
}

/**
 * 辅助方法：从数据库获取一个 township ID。
 * province → city → county → township
 */
private Long getTownshipId() throws Exception {
    String provinceResult = mockMvc.perform(signedGet("/api/open/area/provinces", null))
        .andReturn().getResponse().getContentAsString();
    Long provinceId = objectMapper.readTree(provinceResult).get("data").get(0).get("id").asLong();

    String cityResult = mockMvc.perform(signedGet("/api/open/area/cities", Map.of("provinceId", provinceId)))
        .andReturn().getResponse().getContentAsString();
    Long cityId = objectMapper.readTree(cityResult).get("data").get(0).get("id").asLong();

    String countyResult = mockMvc.perform(signedGet("/api/open/area/counties", Map.of("cityId", cityId)))
        .andReturn().getResponse().getContentAsString();
    Long countyId = objectMapper.readTree(countyResult).get("data").get(0).get("id").asLong();

    String townshipResult = mockMvc.perform(signedGet("/api/open/area/townships", Map.of("countyId", countyId)))
        .andReturn().getResponse().getContentAsString();
    return objectMapper.readTree(townshipResult).get("data").get(0).get("id").asLong();
}
```

- [ ] **Step 2: 运行测试验证**

Run: `cd food-manage-web && export JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home" && mvn test -Dtest=OpenApiIntegrationTest#getOrg* -q 2>&1 | tail -10`

Expected: Tests run: 2, Failures: 0, Errors: 0

- [ ] **Step 3: 运行全部测试验证**

Run: `cd food-manage-web && export JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home" && mvn test -Dtest=OpenApiIntegrationTest -q 2>&1 | tail -10`

Expected: Tests run: 16, Failures: 0, Errors: 0

- [ ] **Step 4: 提交**

```bash
git add food-manage-web/src/test/java/com/jihao/food/integration/OpenApiIntegrationTest.java
git commit -m "test: add geo/org API integration tests"
```

---

## Self-Review

### 1. Spec coverage

| Spec 需求 | 对应 Task |
|-----------|-----------|
| 签名验证（正确/错误/缺失/过期） | Task 2（4个测试） |
| `/api/open/area/provinces` | Task 3 |
| `/api/open/area/cities` | Task 3 |
| `/api/open/area/counties` | Task 3 |
| `/api/open/area/townships` | Task 3 |
| `/api/open/area/detail` | Task 3 |
| `/api/open/area/tree` | Task 3 |
| `/api/open/area/province-list` | Task 4 |
| `/api/open/area/city-list` | Task 4 |
| `/api/open/area/county-list` | Task 4 |
| `/api/open/area/township-list` | Task 4 |
| `/api/open/geo/org` | Task 5 |
| `/api/open/geo/org/by-township` | Task 5 |
| 签名支持 body 参数 | Task 1（signedRequest 方法） |
| 使用 `APP_KEY=demo`, `APP_SECRET=e02ca276f7d444c099f571d7bee8fac6` | Task 1 |
| 连接开发数据库 | `@SpringBootTest` 自动加载 dev profile |
| 混合断言策略 | 所有测试：列表类验证 size>0，条件查询验证 code=0 + 结构 |

全部覆盖，无遗漏。

### 2. Placeholder scan
- 无 TBD/TODO
- 所有测试方法包含完整代码
- 所有运行命令包含完整路径和预期输出

### 3. Type consistency
- `signedGet` 签名：Task 1 定义，Task 3/4/5 复用，参数一致
- `buildQueryPart` 签名：Task 1 定义，签名辅助内部使用
- `getTownshipId` 辅助：Task 5 定义，被同 Task 中两个测试复用
- `Result.code`：成功返回 `0`，失败返回 `401`，与 `Result.java` 一致
- `Result.message`：错误消息字段名是 `message`（不是 `msg`），与 `Result.java` 一致

### 4. Scope check
单文件 16 个测试方法，4 个 Task 分步提交，聚焦 `/api/open/**` 接口，范围清晰。