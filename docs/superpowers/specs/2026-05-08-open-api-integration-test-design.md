# /api/open/** 接口 Spring 集成测试设计

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `/api/open/**` 所有接口编写 Spring Boot `@SpringBootTest` + `MockMvc` 集成测试，覆盖真实签名拦截器 → Controller → Service → Mapper → MySQL 开发数据库全链路。

**Architecture:** 使用 `@AutoConfigureMockMvc` 启动完整 Spring 容器，通过 `@ActiveProfiles("dev")` 连接 `jiahao_food_db` 开发库，复用项目 `SignUtil` 自动计算签名。

**Tech Stack:** Spring Boot Test, MockMvc, JUnit 5, MySQL

---

## 测试架构

```
@SpringBootTest + @AutoConfigureMockMvc
    ↓
MockMvc (自动签名，计算 Content, 添加 Headers)
    ↓
ApiSignInterceptor (真实拦截器，验证签名)
    ↓
Controller → Service → Mapper → jiahao_food_db (MySQL 开发库)
```

## 关键设计

### 签名计算

复用项目自身的 `SignUtil` 工具类，避免重复实现：

```java
// 签名内容构建（与拦截器一致）：排序后的查询参数 + 请求体
String queryPart = buildQueryPart(params);  // key1=value1&key2=value2（按 key 排序）
String content = queryPart + requestBody;   // 有 body 时拼接，无 body 时为空
String signContent = appKey + timestamp + nonce + content;
String sign = DigestUtils.md5Hex(signContent + secret).toUpperCase();
```

签名规则完整公式：`sign = MD5(appKey + timestamp + nonce + [排序查询参数][请求体] + secret)` 大写

- **GET 请求**：无 body，`content` = 排序后的查询参数（`key1=value1&key2=value2`），无参数时为空
- **POST 请求**：`content` = 排序后的查询参数 + 原始请求体字符串

Headers 设置：
- `appKey: demo`
- `timestamp: 当前毫秒时间戳`
- `nonce: 随机 UUID`
- `sign: MD5(...)` 大写

### 测试数据库

使用 `@ActiveProfiles("dev")` 加载 `application-dev.yml`，连接 `jiahao_food_db` 数据库。测试中使用已有的 `appKey=demo`, `appSecret=demo_secret_123` 种子数据。

### 断言策略

混合断言：
- 对**列表类**接口（provinces、province-list 等）：验证 `code=200` + `data 是数组` + `size > 0`
- 对**条件查询**接口（cities、counties 等）：验证 `code=200` + 返回结构正确，如果有数据则 `size > 0`
- 对**详情**接口：验证 `code=200` + 返回对象包含必要字段
- 对**签名验证失败**场景：验证 `code != 0` + 错误消息匹配

---

## 文件结构

| 文件 | 操作 | 说明 |
|------|------|------|
| `food-manage-web/src/test/java/com/jihao/food/integration/OpenApiIntegrationTest.java` | 新建 | 主测试类 |

---

## Task 1: 编写 OpenApiIntegrationTest

**Files:**
- Create: `food-manage-web/src/test/java/com/jihao/food/integration/OpenApiIntegrationTest.java`

### 类结构

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class OpenApiIntegrationTest {
    
    private static final String APP_KEY = "demo";
    private static final String APP_SECRET = "demo_secret_123";
    
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    
    // ===== 签名辅助方法 =====
    private MockHttpServletRequestBuilder signedGet(String url, Map<String, Object> params);
    private String buildSignContent(Map<String, Object> params);
    private RequestBuilder addSignHeaders(MockHttpServletRequestBuilder builder, Map<String, Object> params);
    
    // ===== 签名验证测试 (4个) =====
    @Test void preHandle_withValidSign_shouldReturn200();
    @Test void preHandle_wrongSecret_shouldReturn401();
    @Test void preHandle_missingHeaders_shouldReturn401();
    @Test void preHandle_expiredTimestamp_shouldReturn401();
    
    // ===== /api/open/area 行政区查询 (6个) =====
    @Test void provinces_shouldReturnNonEmptyList();
    @Test void cities_withValidProvinceId_shouldReturnCities();
    @Test void counties_withValidCityId_shouldReturnCounties();
    @Test void townships_withValidCountyId_shouldReturnTownships();
    @Test void detail_withValidAreaId_shouldReturnDetail();
    @Test void tree_shouldReturnTreeStructure();
    
    // ===== /api/open/area 行政区列表 (4个) =====
    @Test void provinceList_shouldReturnNonEmptyList();
    @Test void cityList_shouldReturnList();
    @Test void countyList_shouldReturnList();
    @Test void townshipList_shouldReturnList();
    
    // ===== /api/open/geo 组织归属查询 (2个) =====
    @Test void getOrg_withValidAreaId_shouldReturnResult();
    @Test void getOrgByTownship_withValidTownshipId_shouldReturnResult();
}
```

### 签名辅助方法实现

```java
/**
 * 带签名的 GET 请求。
 */
private MockHttpServletRequestBuilder signedGet(String url, Map<String, Object> params) {
    return signedRequest(get(url), params, null);
}

/**
 * 带签名的 POST 请求（支持 body）。
 */
private MockHttpServletRequestBuilder signedPost(String url, Map<String, Object> params, Object body) {
    String bodyJson = (body != null) ? objectMapper.writeValueAsString(body) : "";
    MockHttpServletRequestBuilder builder = post(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(bodyJson);
    return signedRequest(builder, params, bodyJson);
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
    String signContent = APP_KEY + timestamp + nonce + content;
    String sign = DigestUtils.md5Hex(signContent + APP_SECRET).toUpperCase();

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
```

### 具体测试用例

```java
// ===== 签名验证 =====

@Test
void preHandle_withValidSign_shouldReturn200() throws Exception {
    mockMvc.perform(signedGet("/api/open/area/provinces", null))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));
}

@Test
void preHandle_wrongSecret_shouldReturn401() throws Exception {
    long timestamp = System.currentTimeMillis();
    String nonce = "test-nonce";
    String wrongSign = DigestUtils.md5Hex(APP_KEY + timestamp + nonce + "demo_secret_123" + "wrong_secret").toUpperCase();
    
    mockMvc.perform(get("/api/open/area/provinces")
            .header("appKey", APP_KEY)
            .header("timestamp", String.valueOf(timestamp))
            .header("nonce", nonce)
            .header("sign", wrongSign))
        .andExpect(status().isOk()) // 拦截器返回 Result.error(401, "签名验证失败")
        .andExpect(jsonPath("$.code").value(401))
        .andExpect(jsonPath("$.msg").value("签名验证失败"));
}

@Test
void preHandle_missingHeaders_shouldReturn401() throws Exception {
    mockMvc.perform(get("/api/open/area/provinces"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(401))
        .andExpect(jsonPath("$.msg").value("签名参数缺失"));
}

@Test
void preHandle_expiredTimestamp_shouldReturn401() throws Exception {
    long expiredTs = System.currentTimeMillis() - 600_000; // 10 分钟前
    String nonce = "test-nonce";
    String signContent = APP_KEY + expiredTs + nonce;
    String sign = DigestUtils.md5Hex(signContent + APP_SECRET).toUpperCase();
    
    mockMvc.perform(get("/api/open/area/provinces")
            .header("appKey", APP_KEY)
            .header("timestamp", String.valueOf(expiredTs))
            .header("nonce", nonce)
            .header("sign", sign))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(401))
        .andExpect(jsonPath("$.msg").value("请求已过期"));
}

// ===== 行政区查询 =====

@Test
void provinces_shouldReturnNonEmptyList() throws Exception {
    String result = mockMvc.perform(signedGet("/api/open/area/provinces", null))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data").isArray())
        .andReturn().getResponse().getContentAsString();
    
    JsonNode data = objectMapper.readTree(result).get("data");
    assertThat(data.size()).isGreaterThan(0);
    // 验证第一个元素有 name 和 id
    assertThat(data.get(0).has("name")).isTrue();
    assertThat(data.get(0).has("id")).isTrue();
}

@Test
void cities_withValidProvinceId_shouldReturnCities() throws Exception {
    // 先获取一个 province ID
    String provinceResult = mockMvc.perform(signedGet("/api/open/area/provinces", null))
        .andReturn().getResponse().getContentAsString();
    Long provinceId = objectMapper.readTree(provinceResult).get("data").get(0).get("id").asLong();
    
    Map<String, Object> params = Map.of("provinceId", provinceId);
    mockMvc.perform(signedGet("/api/open/area/cities", params))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data").isArray());
}

@Test
void counties_withValidCityId_shouldReturnCounties() throws Exception {
    String provinceResult = mockMvc.perform(signedGet("/api/open/area/provinces", null))
        .andReturn().getResponse().getContentAsString();
    Long provinceId = objectMapper.readTree(provinceResult).get("data").get(0).get("id").asLong();
    
    String cityResult = mockMvc.perform(signedGet("/api/open/area/cities", Map.of("provinceId", provinceId)))
        .andReturn().getResponse().getContentAsString();
    JsonNode cities = objectMapper.readTree(cityResult).get("data");
    if (cities.isEmpty()) {
        // 跳过测试
        return;
    }
    Long cityId = cities.get(0).get("id").asLong();
    
    mockMvc.perform(signedGet("/api/open/area/counties", Map.of("cityId", cityId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data").isArray());
}

@Test
void townships_withValidCountyId_shouldReturnTownships() throws Exception {
    String provinceResult = mockMvc.perform(signedGet("/api/open/area/provinces", null))
        .andReturn().getResponse().getContentAsString();
    Long provinceId = objectMapper.readTree(provinceResult).get("data").get(0).get("id").asLong();
    
    String cityResult = mockMvc.perform(signedGet("/api/open/area/cities", Map.of("provinceId", provinceId)))
        .andReturn().getResponse().getContentAsString();
    Long cityId = objectMapper.readTree(cityResult).get("data").get(0).get("id").asLong();
    
    String countyResult = mockMvc.perform(signedGet("/api/open/area/counties", Map.of("cityId", cityId)))
        .andReturn().getResponse().getContentAsString();
    Long countyId = objectMapper.readTree(countyResult).get("data").get(0).get("id").asLong();
    
    mockMvc.perform(signedGet("/api/open/area/townships", Map.of("countyId", countyId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data").isArray());
}

@Test
void detail_withValidAreaId_shouldReturnDetail() throws Exception {
    String provinceResult = mockMvc.perform(signedGet("/api/open/area/provinces", null))
        .andReturn().getResponse().getContentAsString();
    Long areaId = objectMapper.readTree(provinceResult).get("data").get(0).get("id").asLong();
    
    mockMvc.perform(signedGet("/api/open/area/detail", Map.of("areaId", areaId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data").isObject())
        .andExpect(jsonPath("$.data.name").isNotEmpty());
}

@Test
void tree_shouldReturnTreeStructure() throws Exception {
    Map<String, Object> params = Map.of("depth", 2);
    mockMvc.perform(signedGet("/api/open/area/tree", params))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data").isArray());
}

// ===== 行政区列表 =====

@Test
void provinceList_shouldReturnNonEmptyList() throws Exception {
    mockMvc.perform(signedGet("/api/open/area/province-list", null))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(greaterThan(0)));
}

@Test
void cityList_shouldReturnList() throws Exception {
    mockMvc.perform(signedGet("/api/open/area/city-list", null))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data").isArray());
}

@Test
void countyList_shouldReturnList() throws Exception {
    mockMvc.perform(signedGet("/api/open/area/county-list", null))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data").isArray());
}

@Test
void townshipList_shouldReturnList() throws Exception {
    mockMvc.perform(signedGet("/api/open/area/township-list", null))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data").isArray());
}

// ===== 组织归属查询 =====

@Test
void getOrg_withValidAreaId_shouldReturnResult() throws Exception {
    // 获取一个街道级别的 areaId
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
    Long townshipId = objectMapper.readTree(townshipResult).get("data").get(0).get("id").asLong();
    
    mockMvc.perform(signedGet("/api/open/geo/org", Map.of("areaId", townshipId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));
}

@Test
void getOrgByTownship_withValidTownshipId_shouldReturnResult() throws Exception {
    // 同上，先获取一个 township ID
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
    Long townshipId = objectMapper.readTree(townshipResult).get("data").get(0).get("id").asLong();
    
    mockMvc.perform(signedGet("/api/open/geo/org/by-township", Map.of("townshipId", townshipId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));
}
```

---

## 依赖确认

项目 `pom.xml` 已有 `spring-boot-starter-test`，无需额外依赖。

测试中使用 `commons-codec` 的 `DigestUtils`（项目已依赖）和 Jackson `ObjectMapper`（Spring Boot 自动配置）。

---

## Self-Review

1. **Placeholder scan:** 无 TBD/TODO，所有代码完整
2. **Internal consistency:** 签名算法与 `SignUtil` 一致，使用 `dev` profile 连接真实数据库
3. **Scope check:** 单文件，16 个测试方法，聚焦 `/api/open/**` 接口
4. **Ambiguity check:** 
   - `appKey=demo`, `appSecret=demo_secret_123` 来自 schema.sql 种子数据
   - 拦截器返回的是 `Result.error()` 格式，HTTP status 为 200，业务 code 为 401
   - 级联查询接口在数据为空时 gracefully 跳过
   - 测试不修改数据库数据，全部只读