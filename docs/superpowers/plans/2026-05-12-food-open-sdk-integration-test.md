# food-open-sdk 集成测试 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 对 food-open-sdk 进行集成测试，使用 `FoodOpenClient` 连接 `http://localhost:8080/api/open` 发起真实 HTTP 请求，调用所有 12 个 `/api/open/**` 接口。

**Architecture:** 单文件集成测试类，通过 `FoodOpenClient.builder()` 构建客户端，真实 HTTP 请求连接后端服务，验证签名、参数传递、响应解析全链路。

**Tech Stack:** JUnit 4（与现有测试一致），Apache HttpClient（SDK 已依赖），Gson（SDK 已依赖）

---

### Task 1: 创建集成测试类

**Files:**
- Create: `food-open-sdk/src/test/java/com/jiahao/food/sdk/integration/FoodOpenClientIntegrationTest.java`

- [ ] **Step 1: 创建测试目录**

```bash
mkdir -p food-open-sdk/src/test/java/com/jiahao/food/sdk/integration
```

- [ ] **Step 2: 创建测试类**

File: `food-open-sdk/src/test/java/com/jiahao/food/sdk/integration/FoodOpenClientIntegrationTest.java`

```java
package com.jiahao.food.sdk.integration;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jiahao.food.sdk.FoodOpenClient;
import com.jiahao.food.sdk.exception.ClientException;
import com.jiahao.food.sdk.exception.FoodOpenException;
import com.jiahao.food.sdk.exception.NetworkException;
import com.jiahao.food.sdk.exception.ServerException;
import com.jiahao.food.sdk.model.AreaDTO;
import com.jiahao.food.sdk.model.AreaDetailDTO;
import com.jiahao.food.sdk.model.AreaTreeDTO;
import com.jiahao.food.sdk.model.OrgInfoDTO;
import com.jiahao.food.sdk.model.TownshipOrgDTO;
import org.junit.After;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.*;

/**
 * SDK 集成测试：连接真实后端服务 http://localhost:8080/api/open。
 * 后端服务必须先运行，否则所有测试将被 skip。
 */
public class FoodOpenClientIntegrationTest {

    private static final String APP_KEY = "demo";
    private static final String APP_SECRET = "e02ca276f7d444c099f571d7bee8fac6";
    private static final String SERVER_URL = "http://localhost:8080/api/open";

    private static boolean backendAvailable = false;
    private FoodOpenClient client;

    @BeforeClass
    public static void checkBackendAvailability() {
        try {
            URL url = new URL("http://localhost:8080");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.connect();
            conn.disconnect();
            backendAvailable = true;
        } catch (Exception e) {
            backendAvailable = false;
        }
    }

    @After
    public void tearDown() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private FoodOpenClient createClient() throws FoodOpenException {
        Assume.assumeTrue("后端服务未运行，跳过测试。请先启动 food-manage-web 服务。", backendAvailable);
        return FoodOpenClient.builder()
                .appKey(APP_KEY)
                .appSecret(APP_SECRET)
                .serverUrl(SERVER_URL)
                .maxRetries(0)
                .enableLogging(false)
                .build();
    }

    // ===== 签名验证测试 =====

    @Test
    public void signValidation_correctSign_shouldReturnCode0() throws FoodOpenException {
        client = createClient();
        List<AreaDTO> provinces = client.area().provinces();
        assertNotNull(provinces);
        assertTrue(provinces.size() > 0);
        assertNotNull(provinces.get(0).getId());
        assertNotNull(provinces.get(0).getName());
    }

    // ===== 行政区查询接口测试 =====

    @Test
    public void provinces_shouldReturnNonEmptyList() throws FoodOpenException {
        client = createClient();
        List<AreaDTO> provinces = client.area().provinces();
        assertNotNull(provinces);
        assertTrue("省列表不应为空", provinces.size() > 0);
        assertNotNull(provinces.get(0).getId());
        assertNotNull(provinces.get(0).getName());
    }

    @Test
    public void cities_withValidProvinceId_shouldReturnCities() throws FoodOpenException {
        client = createClient();
        Long provinceId = getFirstProvinceId();
        Assume.assumeNotNull("无省数据", provinceId);

        List<AreaDTO> cities = client.area().cities(provinceId);
        assertNotNull(cities);
        assertTrue("城市列表不应为空", cities.size() > 0);
    }

    @Test
    public void counties_withValidCityId_shouldReturnCounties() throws FoodOpenException {
        client = createClient();
        Long cityId = getFirstCityId();
        Assume.assumeNotNull("无城市数据", cityId);

        List<AreaDTO> counties = client.area().counties(cityId);
        assertNotNull(counties);
        assertTrue("区县列表不应为空", counties.size() > 0);
    }

    @Test
    public void townships_withValidCountyId_shouldReturnTownships() throws FoodOpenException {
        client = createClient();
        Long countyId = getFirstCountyId();
        Assume.assumeNotNull("无区县数据", countyId);

        List<AreaDTO> townships = client.area().townships(countyId);
        assertNotNull(townships);
        assertTrue("乡镇列表不应为空", townships.size() > 0);
    }

    @Test
    public void detail_withValidAreaId_shouldReturnDetail() throws FoodOpenException {
        client = createClient();
        Long provinceId = getFirstProvinceId();
        Assume.assumeNotNull("无省数据", provinceId);

        AreaDetailDTO detail = client.area().detail(provinceId);
        assertNotNull(detail);
        assertNotNull(detail.getName());
    }

    @Test
    public void tree_withDepth2_shouldReturnTreeStructure() throws FoodOpenException {
        client = createClient();
        List<AreaTreeDTO> tree = client.area().tree(null, null, 2);
        assertNotNull(tree);
        assertTrue("树结构不应为空", tree.size() > 0);
        assertNotNull(tree.get(0).getName());
    }

    // ===== 行政区列表接口测试 =====

    @Test
    public void provinceList_shouldReturnNonEmptyList() throws FoodOpenException {
        client = createClient();
        List<AreaDTO> provinces = client.area().provinceList();
        assertNotNull(provinces);
        assertTrue("省列表不应为空", provinces.size() > 0);
    }

    @Test
    public void cityList_shouldReturnList() throws FoodOpenException {
        client = createClient();
        List<AreaDTO> cities = client.area().cityList();
        assertNotNull(cities);
    }

    @Test
    public void countyList_shouldReturnList() throws FoodOpenException {
        client = createClient();
        List<AreaDTO> counties = client.area().countyList();
        assertNotNull(counties);
    }

    @Test
    public void townshipList_shouldReturnList() throws FoodOpenException {
        client = createClient();
        List<AreaDTO> townships = client.area().townshipList();
        assertNotNull(townships);
    }

    // ===== 组织归属查询接口测试 =====

    @Test
    public void getOrgByAreaId_withValidTownshipId_shouldReturnResult() throws FoodOpenException {
        client = createClient();
        Long townshipId = getFirstTownshipId();
        Assume.assumeNotNull("无乡镇数据", townshipId);

        OrgInfoDTO org = client.geo().getOrgByAreaId(townshipId);
        assertNotNull(org);
    }

    @Test
    public void getOrgByTownshipId_withValidTownshipId_shouldReturnResult() throws FoodOpenException {
        client = createClient();
        Long townshipId = getFirstTownshipId();
        Assume.assumeNotNull("无乡镇数据", townshipId);

        TownshipOrgDTO org = client.geo().getOrgByTownshipId(townshipId);
        assertNotNull(org);
    }

    // ===== 辅助方法 =====

    private Long getFirstProvinceId() throws FoodOpenException {
        List<AreaDTO> provinces = client.area().provinces();
        if (provinces == null || provinces.isEmpty()) return null;
        return provinces.get(0).getId();
    }

    private Long getFirstCityId() throws FoodOpenException {
        Long provinceId = getFirstProvinceId();
        if (provinceId == null) return null;
        List<AreaDTO> cities = client.area().cities(provinceId);
        if (cities == null || cities.isEmpty()) return null;
        return cities.get(0).getId();
    }

    private Long getFirstCountyId() throws FoodOpenException {
        Long cityId = getFirstCityId();
        if (cityId == null) return null;
        List<AreaDTO> counties = client.area().counties(cityId);
        if (counties == null || counties.isEmpty()) return null;
        return counties.get(0).getId();
    }

    private Long getFirstTownshipId() throws FoodOpenException {
        Long countyId = getFirstCountyId();
        if (countyId == null) return null;
        List<AreaDTO> townships = client.area().townships(countyId);
        if (townships == null || townships.isEmpty()) return null;
        return townships.get(0).getId();
    }
}
```

**设计说明：**
- `@BeforeClass` 检查后端连通性：尝试连接 `http://localhost:8080`，失败则设 `backendAvailable = false`
- `createClient()` 使用 `Assume.assumeTrue` 在后端不可用时 gracefully skip 所有测试（JUnit 显示为 "Ignored" 而非失败）
- `maxRetries(0)` 关闭重试，加速测试
- 所有级联查询辅助方法都带空数据保护（`if (isEmpty) return null`），配合 `Assume.assumeNotNull` 实现 graceful skip
- `@After` 确保每次测试后 `client.close()` 释放连接池

- [ ] **Step 3: 验证编译**

Run: `cd food-open-sdk && export JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home" && mvn test-compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 运行集成测试（后端未运行时所有测试应 skip）**

Run: `cd food-open-sdk && export JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home" && mvn test -Dtest=FoodOpenClientIntegrationTest 2>&1 | grep -E "Tests run:|Ignored|BUILD"`
Expected: Tests run: 14, Failures: 0, Errors: 0, Skipped: 14, BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add food-open-sdk/src/test/java/com/jiahao/food/sdk/integration/
git commit -m "test: add food-open-sdk integration tests for all /api/open/** endpoints"
```

---

### Task 2: 运行完整集成测试（需后端服务运行）

此 Task 在 Task 1 提交后手动执行，验证 SDK 能正确调用后端所有接口。

- [ ] **Step 1: 启动后端服务**

```bash
cd food-manage-web && export JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home" && mvn spring-boot:run &
# 等待服务启动完成（约 15-30 秒）
```

- [ ] **Step 2: 运行集成测试**

Run: `cd food-open-sdk && export JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home" && mvn test -Dtest=FoodOpenClientIntegrationTest 2>&1 | tail -20`
Expected: Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS

- [ ] **Step 3: 运行 SDK 全部测试（含单元+集成）**

Run: `cd food-open-sdk && export JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home" && mvn test 2>&1 | tail -10`
Expected: Tests run: 30 (16 unit + 14 integration), Failures: 0, Errors: 0

---

## Self-Review

### 1. Spec coverage

| Spec 需求 | 对应 Task |
|-----------|-----------|
| 连通性检查（@BeforeClass） | Task 1 |
| 签名正确性测试 | Task 1（signValidation 测试） |
| 省列表 | Task 1（provinces 测试） |
| 城市列表 | Task 1（cities 测试） |
| 区县列表 | Task 1（counties 测试） |
| 乡镇列表 | Task 1（townships 测试） |
| 行政区详情 | Task 1（detail 测试） |
| 行政区树 | Task 1（tree 测试） |
| 省列表（简化） | Task 1（provinceList 测试） |
| 城市列表（全部） | Task 1（cityList 测试） |
| 区县列表（全部） | Task 1（countyList 测试） |
| 乡镇列表（全部） | Task 1（townshipList 测试） |
| 组织归属查询（areaId） | Task 1（getOrgByAreaId 测试） |
| 组织归属查询（townshipId） | Task 1（getOrgByTownshipId 测试） |
| 资源清理（@After） | Task 1 |
| appKey=demo, appSecret=e02ca276f7d444c099f571d7bee8fac6 | Task 1 |
| serverUrl=http://localhost:8080/api/open | Task 1 |

全部覆盖，无遗漏。

### 2. Placeholder scan
- 无 TBD/TODO
- 所有测试方法包含完整代码
- 所有运行命令包含预期输出
- 无 "add tests for the above" 等模糊描述

### 3. Type consistency
- `FoodOpenClient.builder()` → `.build()` 抛出 `FoodOpenException`（与 `FoodOpenClient.java` 一致）
- `client.area()` 返回 `AreaApi`，方法签名与 `AreaApi.java` 一致
- `client.geo()` 返回 `GeoApi`，方法签名与 `GeoApi.java` 一致
- `client.close()` 抛出 `IOException`（与 `FoodOpenClient.java` 一致）
- 所有 API 方法都抛出 `FoodOpenException`，测试方法正确声明 `throws`
- `Assume.assumeTrue` 和 `Assume.assumeNotNull` 是 JUnit 4 标准用法
- `List<AreaDTO>` 等泛型类型与 Model DTOs 定义一致

### 4. Scope check
2 个 Task：Task 1 创建集成测试文件，Task 2 运行验证。单文件 14 个测试方法。
