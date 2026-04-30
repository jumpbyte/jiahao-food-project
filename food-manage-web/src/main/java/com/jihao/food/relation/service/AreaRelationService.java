package com.jihao.food.relation.service;

import com.jihao.food.area.entity.Area;
import com.jihao.food.area.mapper.AreaMapper;
import com.jihao.food.common.exception.BusinessException;
import com.jihao.food.org.entity.Organization;
import com.jihao.food.org.mapper.OrganizationMapper;
import com.jihao.food.relation.entity.AreaRelation;
import com.jihao.food.relation.mapper.AreaRelationMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AreaRelationService {

    private final AreaRelationMapper areaRelationMapper;
    private final OrganizationMapper organizationMapper;
    private final AreaMapper areaMapper;

    public List<AreaRelation> listByDistrictId(Long districtId) {
        return areaRelationMapper.selectByOrgId(districtId);
    }

    public List<Long> listAreaIdsByOrgId(Long orgId) {
        Organization org = organizationMapper.selectById(orgId);
        if (org == null) return Collections.emptyList();
        return areaRelationMapper.selectAreaIdsByOrgIdAndType(orgId, org.getType());
    }

    @Transactional
    public int bindStreets(Long orgId, List<Long> streetIds) {
        int count = 0;
        for (Long streetId : streetIds) {
            AreaRelation existing = areaRelationMapper.selectByAreaIdAndOrgId(streetId, orgId);
            if (existing == null) {
                AreaRelation relation = new AreaRelation();
                relation.setAreaId(streetId);
                relation.setOrgId(orgId);
                relation.setDelFlag(0);
                areaRelationMapper.insert(relation);
                count++;
            }
        }
        return count;
    }

    @Transactional
    public int bindStreetsWithValidation(Long orgId, List<Long> streetIds, Long parentOrgId) {
        if (parentOrgId == null || parentOrgId == 0) {
            return bindStreets(orgId, streetIds);
        }
        List<Long> parentAreaIds = areaRelationMapper.selectAreaIdsByParentOrg(parentOrgId);
        Set<Long> parentAreaIdSet = new HashSet<>(parentAreaIds);
        List<Long> invalidStreets = streetIds.stream()
                .filter(id -> !parentAreaIdSet.contains(id))
                .toList();
        if (!invalidStreets.isEmpty()) {
            Area firstInvalid = areaMapper.selectById(invalidStreets.get(0));
            String name = firstInvalid != null ? firstInvalid.getName() : String.valueOf(invalidStreets.get(0));
            throw new BusinessException(400, "街道[" + name + "]不在上级组织管辖范围内");
        }
        return bindStreets(orgId, streetIds);
    }

    @Transactional
    public boolean unbindStreet(Long orgId, Long streetId) {
        AreaRelation relation = areaRelationMapper.selectByAreaIdAndOrgId(streetId, orgId);
        if (relation == null) {
            throw new BusinessException(400, "绑定关系不存在");
        }
        relation.setDelFlag(1);
        areaRelationMapper.updateById(relation);
        return true;
    }

    public List<AreaRelation> getOrgByAreaId(Long areaId) {
        return areaRelationMapper.selectByAreaId(areaId);
    }

    public List<AreaTreeNode> getSelectableAreaTree(Long orgId, Long parentOrgId) {
        // 1. 查出该组织已绑定的街道ID
        Set<Long> boundStreetIds = new HashSet<>(listAreaIdsByOrgId(orgId));

        // 2. 确定要展示的街道集合
        Set<Long> targetStreetIds;
        boolean isFullTree = false;
        if (parentOrgId != null && parentOrgId != 0) {
            // 办事处/片区：只展示父组织（大区/办事处）已绑定的街道
            targetStreetIds = new HashSet<>(areaRelationMapper.selectAreaIdsByParentOrg(parentOrgId));
        } else {
            // 大区：没有已绑定街道时返回完整树，有已绑定时只返回已绑定的
            if (boundStreetIds.isEmpty()) {
                // 返回完整行政区树，供用户选择新街道
                isFullTree = true;
            }
            targetStreetIds = new HashSet<>(boundStreetIds);
        }

        if (targetStreetIds.isEmpty() && !isFullTree) {
            return Collections.emptyList();
        }

        // 3. 加载数据
        List<Area> townships;
        List<Long> countyIds;
        List<Area> counties;
        List<Long> cityIds;
        List<Area> cities;
        List<Long> provinceIds;
        List<Area> provinces;

        if (isFullTree) {
            // 完整树：加载全部层级
            counties = areaMapper.selectByLevel(Area.LEVEL_COUNTY);
            cities = areaMapper.selectByLevel(Area.LEVEL_CITY);
            provinces = areaMapper.selectByLevel(Area.LEVEL_PROVINCE);
            // 加载所有街道
            countyIds = counties.stream().map(Area::getId).distinct().toList();
            townships = areaMapper.selectByPidsAndLevel(countyIds, Area.LEVEL_TOWNSHIP);
            // 所有街道都可选择
            targetStreetIds = townships.stream().map(Area::getId).collect(Collectors.toSet());
            cityIds = cities.stream().map(Area::getId).distinct().toList();
            provinceIds = provinces.stream().map(Area::getId).distinct().toList();
        } else {
            // 按需加载：只加载涉及的层级路径
            townships = areaMapper.selectByIdsAndLevel(new ArrayList<>(targetStreetIds), Area.LEVEL_TOWNSHIP);
            countyIds = townships.stream().map(Area::getPid).distinct().toList();
            if (countyIds.isEmpty()) return Collections.emptyList();

            counties = areaMapper.selectByIdsAndLevel(countyIds, Area.LEVEL_COUNTY);
            cityIds = counties.stream().map(Area::getPid).distinct().toList();

            cities = areaMapper.selectByIdsAndLevel(cityIds, Area.LEVEL_CITY);
            provinceIds = cities.stream().map(Area::getPid).distinct().toList();

            provinces = areaMapper.selectByIdsAndLevel(provinceIds, Area.LEVEL_PROVINCE);
        }

        Map<Long, List<Area>> townshipByCounty = townships.stream()
                .collect(Collectors.groupingBy(Area::getPid));
        Map<Long, List<Area>> countyByCity = counties.stream()
                .collect(Collectors.groupingBy(Area::getPid));
        Map<Long, List<Area>> cityByProvince = cities.stream()
                .collect(Collectors.groupingBy(Area::getPid));

        List<AreaTreeNode> result = new ArrayList<>();
        for (Area province : provinces) {
            AreaTreeNode node = buildProvinceNode(province, cityByProvince,
                    countyByCity, townshipByCounty, boundStreetIds, targetStreetIds);
            if (node != null) {
                result.add(node);
            }
        }
        return result;
    }

    private AreaTreeNode buildProvinceNode(Area province,
                                           Map<Long, List<Area>> cityByProvince,
                                           Map<Long, List<Area>> countyByCity,
                                           Map<Long, List<Area>> townshipByCounty,
                                           Set<Long> boundStreetIds,
                                           Set<Long> targetStreetIds) {
        List<Area> cities = cityByProvince.getOrDefault(province.getId(), Collections.emptyList());
        List<AreaTreeNode> cityNodes = new ArrayList<>();
        for (Area city : cities) {
            AreaTreeNode cityNode = buildCityNode(city, countyByCity, townshipByCounty,
                    boundStreetIds, targetStreetIds);
            if (cityNode != null) {
                cityNodes.add(cityNode);
            }
        }
        if (cityNodes.isEmpty()) return null;
        AreaTreeNode node = new AreaTreeNode();
        node.setId(province.getId());
        node.setName(province.getName());
        node.setLevel(province.getLevel());
        node.setSelectable(false);
        node.setSelected(false);
        node.setChildren(cityNodes);
        return node;
    }

    private AreaTreeNode buildCityNode(Area city,
                                       Map<Long, List<Area>> countyByCity,
                                       Map<Long, List<Area>> townshipByCounty,
                                       Set<Long> boundStreetIds,
                                       Set<Long> targetStreetIds) {
        List<Area> counties = countyByCity.getOrDefault(city.getId(), Collections.emptyList());
        List<AreaTreeNode> countyNodes = new ArrayList<>();
        for (Area county : counties) {
            AreaTreeNode countyNode = buildCountyNode(county, townshipByCounty,
                    boundStreetIds, targetStreetIds);
            if (countyNode != null) {
                countyNodes.add(countyNode);
            }
        }
        if (countyNodes.isEmpty()) return null;
        AreaTreeNode node = new AreaTreeNode();
        node.setId(city.getId());
        node.setName(city.getName());
        node.setLevel(city.getLevel());
        node.setSelectable(false);
        node.setSelected(false);
        node.setChildren(countyNodes);
        return node;
    }

    private AreaTreeNode buildCountyNode(Area county,
                                         Map<Long, List<Area>> townshipByCounty,
                                         Set<Long> boundStreetIds,
                                         Set<Long> targetStreetIds) {
        List<Area> townships = townshipByCounty.getOrDefault(county.getId(), Collections.emptyList());
        List<AreaTreeNode> townshipNodes = new ArrayList<>();
        for (Area township : townships) {
            boolean inTarget = targetStreetIds.contains(township.getId());
            boolean isBound = boundStreetIds.contains(township.getId());
            AreaTreeNode node = new AreaTreeNode();
            node.setId(township.getId());
            node.setName(township.getName());
            node.setLevel(township.getLevel());
            node.setSelectable(inTarget);
            node.setSelected(isBound);
            townshipNodes.add(node);
        }
        if (townshipNodes.isEmpty()) return null;
        AreaTreeNode node = new AreaTreeNode();
        node.setId(county.getId());
        node.setName(county.getName());
        node.setLevel(county.getLevel());
        node.setSelectable(false);
        node.setSelected(false);
        node.setChildren(townshipNodes);
        return node;
    }

    @Data
    public static class AreaTreeNode {
        private Long id;
        private String name;
        private Integer level;
        private Boolean selectable;
        private Boolean selected;
        private List<AreaTreeNode> children;
    }
}
