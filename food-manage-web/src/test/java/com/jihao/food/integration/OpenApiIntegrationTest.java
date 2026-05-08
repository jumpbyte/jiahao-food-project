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

    @Test
    void townships_withValidCountyId_shouldReturnTownships() throws Exception {
        // province → city → county → township
        String provinceResult = mockMvc.perform(signedGet("/api/open/area/provinces", null))
            .andReturn().getResponse().getContentAsString();
        JsonNode provinces = objectMapper.readTree(provinceResult).get("data");
        if (provinces.isEmpty()) return;
        Long provinceId = provinces.get(0).get("id").asLong();

        String cityResult = mockMvc.perform(signedGet("/api/open/area/cities", Map.of("provinceId", provinceId)))
            .andReturn().getResponse().getContentAsString();
        JsonNode cities = objectMapper.readTree(cityResult).get("data");
        if (cities.isEmpty()) return;
        Long cityId = cities.get(0).get("id").asLong();

        String countyResult = mockMvc.perform(signedGet("/api/open/area/counties", Map.of("cityId", cityId)))
            .andReturn().getResponse().getContentAsString();
        JsonNode counties = objectMapper.readTree(countyResult).get("data");
        if (counties.isEmpty()) return;
        Long countyId = counties.get(0).get("id").asLong();

        String result = mockMvc.perform(signedGet("/api/open/area/townships", Map.of("countyId", countyId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data").isArray())
            .andReturn().getResponse().getContentAsString();

        JsonNode data = objectMapper.readTree(result).get("data");
        assertThat(data.size()).isGreaterThan(0);
    }

    @Test
    void detail_withValidAreaId_shouldReturnDetail() throws Exception {
        String provinceResult = mockMvc.perform(signedGet("/api/open/area/provinces", null))
            .andReturn().getResponse().getContentAsString();
        Long areaId = objectMapper.readTree(provinceResult).get("data").get(0).get("id").asLong();

        mockMvc.perform(signedGet("/api/open/area/detail", Map.of("areaId", areaId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.name").isNotEmpty());
    }

    @Test
    void tree_shouldReturnTreeStructure() throws Exception {
        Map<String, Object> params = Map.of("depth", 2);
        mockMvc.perform(signedGet("/api/open/area/tree", params))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data").isArray());
    }

    // ===== 组织归属查询接口测试 =====

    @Test
    void getOrg_withValidAreaId_shouldReturnResult() throws Exception {
        Long townshipId = getTownshipId();
        if (townshipId == null) return;

        mockMvc.perform(signedGet("/api/open/geo/org", Map.of("areaId", townshipId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void getOrgByTownship_withValidTownshipId_shouldReturnResult() throws Exception {
        Long townshipId = getTownshipId();
        if (townshipId == null) return;

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
        JsonNode provinces = objectMapper.readTree(provinceResult).get("data");
        if (provinces.isEmpty()) return null;
        Long provinceId = provinces.get(0).get("id").asLong();

        String cityResult = mockMvc.perform(signedGet("/api/open/area/cities", Map.of("provinceId", provinceId)))
            .andReturn().getResponse().getContentAsString();
        JsonNode cities = objectMapper.readTree(cityResult).get("data");
        if (cities.isEmpty()) return null;
        Long cityId = cities.get(0).get("id").asLong();

        String countyResult = mockMvc.perform(signedGet("/api/open/area/counties", Map.of("cityId", cityId)))
            .andReturn().getResponse().getContentAsString();
        JsonNode counties = objectMapper.readTree(countyResult).get("data");
        if (counties.isEmpty()) return null;
        Long countyId = counties.get(0).get("id").asLong();

        String townshipResult = mockMvc.perform(signedGet("/api/open/area/townships", Map.of("countyId", countyId)))
            .andReturn().getResponse().getContentAsString();
        JsonNode townships = objectMapper.readTree(townshipResult).get("data");
        if (townships.isEmpty()) return null;
        return townships.get(0).get("id").asLong();
    }

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
}
