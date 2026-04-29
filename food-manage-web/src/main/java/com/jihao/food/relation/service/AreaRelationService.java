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
        Set<Long> selectableAreaIds = new HashSet<>(listAreaIdsByOrgId(orgId));
        Set<Long> availableAreaIds;
        if (parentOrgId != null && parentOrgId != 0) {
            availableAreaIds = new HashSet<>(areaRelationMapper.selectAreaIdsByParentOrg(parentOrgId));
        } else {
            availableAreaIds = null;
        }
        List<Area> townships = areaMapper.selectByLevel(Area.LEVEL_TOWNSHIP);
        Map<Long, List<Area>> townshipByCounty = townships.stream()
                .collect(Collectors.groupingBy(Area::getPid));
        List<Area> counties = areaMapper.selectByLevel(Area.LEVEL_COUNTY);
        Map<Long, List<Area>> countyByCity = counties.stream()
                .collect(Collectors.groupingBy(Area::getPid));
        List<Area> cities = areaMapper.selectByLevel(Area.LEVEL_CITY);
        Map<Long, List<Area>> cityByProvince = cities.stream()
                .collect(Collectors.groupingBy(Area::getPid));
        List<Area> provinces = areaMapper.selectByLevel(Area.LEVEL_PROVINCE);

        List<AreaTreeNode> result = new ArrayList<>();
        for (Area province : provinces) {
            AreaTreeNode provinceNode = buildProvinceNode(province, cityByProvince,
                    countyByCity, townshipByCounty, selectableAreaIds, availableAreaIds);
            if (provinceNode != null) {
                result.add(provinceNode);
            }
        }
        return result;
    }

    private AreaTreeNode buildProvinceNode(Area province,
                                           Map<Long, List<Area>> cityByProvince,
                                           Map<Long, List<Area>> countyByCity,
                                           Map<Long, List<Area>> townshipByCounty,
                                           Set<Long> selectableAreaIds,
                                           Set<Long> availableAreaIds) {
        List<Area> cities = cityByProvince.getOrDefault(province.getId(), Collections.emptyList());
        List<AreaTreeNode> cityNodes = new ArrayList<>();
        for (Area city : cities) {
            AreaTreeNode cityNode = buildCityNode(city, countyByCity, townshipByCounty,
                    selectableAreaIds, availableAreaIds);
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
                                       Set<Long> selectableAreaIds,
                                       Set<Long> availableAreaIds) {
        List<Area> counties = countyByCity.getOrDefault(city.getId(), Collections.emptyList());
        List<AreaTreeNode> countyNodes = new ArrayList<>();
        for (Area county : counties) {
            AreaTreeNode countyNode = buildCountyNode(county, townshipByCounty,
                    selectableAreaIds, availableAreaIds);
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
                                         Set<Long> selectableAreaIds,
                                         Set<Long> availableAreaIds) {
        List<Area> townships = townshipByCounty.getOrDefault(county.getId(), Collections.emptyList());
        List<AreaTreeNode> townshipNodes = new ArrayList<>();
        for (Area township : townships) {
            boolean selectable = selectableAreaIds.contains(township.getId());
            if (availableAreaIds != null) {
                selectable = selectable && availableAreaIds.contains(township.getId());
            }
            AreaTreeNode node = new AreaTreeNode();
            node.setId(township.getId());
            node.setName(township.getName());
            node.setLevel(township.getLevel());
            node.setSelectable(selectable);
            node.setSelected(selectableAreaIds.contains(township.getId()));
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
