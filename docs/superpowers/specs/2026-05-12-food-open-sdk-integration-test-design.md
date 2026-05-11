# food-open-sdk 集成测试设计

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 对 food-open-sdk 进行集成测试，使用 `FoodOpenClient` 连接 `http://localhost/api/open` 发起真实 HTTP 请求，调用所有 12 个 `/api/open/**` 接口验证 SDK 正确性。

**Architecture:** 在 SDK 模块 test 目录下创建集成测试类，通过 `FoodOpenClient.builder()` 构建客户端，使用真实 HTTP 请求连接后端服务，验证签名、参数传递、响应解析全链路。

**Tech Stack:** JUnit 4（与现有测试一致），Apache HttpClient（SDK 已依赖）

---

## 测试架构

```
FoodOpenClientIntegrationTest (JUnit 4)
    ↓
FoodOpenClient.builder()
    .appKey("demo")
    .appSecret("e02ca276f7d444c099f571d7bee8fac6")
    .serverUrl("http://localhost/api/open")
    .build()
    ↓
真实 HTTP 请求 → 后端 ApiSignInterceptor → Controller → Service → Mapper → MySQL
```

---

## 测试文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `food-open-sdk/src/test/java/com/jiahao/food/sdk/integration/FoodOpenClientIntegrationTest.java` | 新建 | 集成测试主类，包含所有接口的 happy path 测试 + 连通性检查 |

---

## 测试用例设计

### 连通性检查（@BeforeClass）

```java
@BeforeClass
public static void checkBackendAvailability() {
    // 尝试连接 http://localhost，失败则 skipAllTests = true
    // 后端未运行时所有测试 gracefully skip，不报错
}
```

### 签名正确性测试

- `signValidation_correctSign_shouldReturnCode0` — 调用 `/area/provinces`，验证返回 `code=0`，确认签名被后端正确验证

### 行政区查询接口测试（6 个）

- `provinces_shouldReturnNonEmptyList` — 省列表，验证 `data.size() > 0`，第一条有 `name` 和 `id`
- `cities_withValidProvinceId_shouldReturnCities` — 省→市，验证返回非空
- `counties_withValidCityId_shouldReturnCounties` — 省→市→县，验证返回非空
- `townships_withValidCountyId_shouldReturnTownships` — 省→市→县→乡镇，验证返回非空
- `detail_withValidAreaId_shouldReturnDetail` — 行政区详情，验证返回对象包含 `name` 字段
- `tree_withDepth2_shouldReturnTreeStructure` — 树形结构，验证返回数组

### 行政区列表接口测试（4 个）

- `provinceList_shouldReturnNonEmptyList` — 全部省列表
- `cityList_shouldReturnList` — 全部城市列表
- `countyList_shouldReturnList` — 全部区县列表
- `townshipList_shouldReturnList` — 全部乡镇列表

### 组织归属查询接口测试（2 个）

- `getOrgByAreaId_withValidTownshipId_shouldReturnResult` — 根据行政区 ID 查询组织归属
- `getOrgByTownshipId_withValidTownshipId_shouldReturnResult` — 根据乡镇 ID 查询组织归属

### 资源清理（@After）

- 每次测试后调用 `client.close()` 释放连接池

---

## 辅助方法

- `getTownshipId()` — 通过级联查询（省→市→县→乡镇）获取一个乡镇 ID，用于下级测试
- 所有级联查询都带空数据保护（`if (isEmpty()) skip`），防止测试数据库无数据时 NPE

## pom.xml 依赖调整

当前 `food-open-sdk/pom.xml` 已有 JUnit 4.13.2 和 Apache HttpClient，无需额外依赖。

---

## Self-Review

1. **Placeholder scan:** 无 TBD/TODO，所有类名、方法名、路径明确
2. **Internal consistency:** 所有接口路径与后端 `AreaQueryController` 和 `GeoQueryController` 一致；appKey/appSecret 与用户指定一致；JUnit 4 与现有测试风格一致
3. **Scope check:** 单文件，约 14 个测试方法，聚焦 SDK 全链路验证，不修改后端代码
4. **Ambiguity check:**
   - 后端服务需预先运行，测试类通过 `@BeforeClass` 做连通性检查，失败则跳过所有测试
   - 后端 URL 使用 `http://localhost/api/open`，不含端口号（后端默认 8080 端口，如果非标准端口需另行配置）
   - 级联查询（省→市→县→乡镇）在每个测试中独立执行，不共享状态
