package com.jiahao.food.sdk.integration;

import com.jiahao.food.sdk.FoodOpenClient;
import com.jiahao.food.sdk.exception.FoodOpenException;
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
            URL url = new URL("http://localhost:8080/api/open/area/provinces");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            conn.disconnect();
            // 200 = success, 401/403 = service is up but needs auth/sign
            backendAvailable = (code >= 200 && code < 500);
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
