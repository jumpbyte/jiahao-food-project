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
