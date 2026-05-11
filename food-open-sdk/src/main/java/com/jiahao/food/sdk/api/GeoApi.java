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
