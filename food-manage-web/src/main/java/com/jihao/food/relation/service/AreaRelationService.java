package com.jihao.food.relation.service;

import com.jihao.food.area.entity.Area;
import com.jihao.food.area.mapper.AreaMapper;
import com.jihao.food.area.service.AreaCacheService;
import com.jihao.food.common.exception.BusinessException;
import com.jihao.food.org.entity.Organization;
import com.jihao.food.org.mapper.OrganizationMapper;
import com.jihao.food.relation.dto.StreetInfoDTO;
import com.jihao.food.relation.entity.AreaRelation;
import com.jihao.food.relation.mapper.AreaRelationMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    private final AreaCacheService areaCacheService;

    public List<AreaRelation> listByDistrictId(Long districtId) {
        return areaRelationMapper.selectByOrgId(districtId);
    }

    /** 返回组织已绑定街道的完整信息（含名称和省市区路径） */
    public List<StreetInfoDTO> listStreetsWithInfo(Long orgId) {
        List<AreaRelation> relations = areaRelationMapper.selectByOrgId(orgId);
        if (relations.isEmpty()) return Collections.emptyList();

        List<Long> areaIds = relations.stream().map(AreaRelation::getAreaId).toList();
        List<Area> streets = areaMapper.selectByIdsAndLevel(areaIds, Area.LEVEL_TOWNSHIP);
        Map<Long, Area> streetMap = streets.stream().collect(Collectors.toMap(Area::getId, a -> a));

        List<Long> countyIds = streets.stream().map(Area::getPid).filter(Objects::nonNull).distinct().toList();
        List<Area> counties = countyIds.isEmpty() ? Collections.emptyList() : areaMapper.selectByIdsAndLevel(countyIds, Area.LEVEL_COUNTY);
        Map<Long, Area> countyMap = counties.stream().collect(Collectors.toMap(Area::getId, a -> a));

        List<Long> cityIds = counties.stream().map(Area::getPid).filter(Objects::nonNull).distinct().toList();
        List<Area> cities = cityIds.isEmpty() ? Collections.emptyList() : areaMapper.selectByIdsAndLevel(cityIds, Area.LEVEL_CITY);
        Map<Long, Area> cityMap = cities.stream().collect(Collectors.toMap(Area::getId, a -> a));

        List<Long> provinceIds = cities.stream().map(Area::getPid).filter(Objects::nonNull).distinct().toList();
        List<Area> provinces = provinceIds.isEmpty() ? Collections.emptyList() : areaMapper.selectByIdsAndLevel(provinceIds, Area.LEVEL_PROVINCE);
        Map<Long, Area> provinceMap = provinces.stream().collect(Collectors.toMap(Area::getId, a -> a));

        return relations.stream().map(rel -> {
            Area street = streetMap.get(rel.getAreaId());
            if (street == null) return null;
            Area county = countyMap.get(street.getPid());
            Area city = county != null ? cityMap.get(county.getPid()) : null;
            Area province = city != null ? provinceMap.get(city.getPid()) : null;

            StringBuilder path = new StringBuilder();
            if (province != null) path.append(province.getName());
            if (city != null) path.append('/').append(city.getName());
            if (county != null) path.append('/').append(county.getName());

            StreetInfoDTO dto = new StreetInfoDTO();
            dto.setId(rel.getId());
            dto.setAreaId(street.getId());
            dto.setName(street.getName());
            dto.setProvinceName(province != null ? province.getName() : null);
            dto.setCityName(city != null ? city.getName() : null);
            dto.setCountyName(county != null ? county.getName() : null);
            dto.setPathText(path.toString());
            return dto;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public List<Long> listAreaIdsByOrgId(Long orgId) {
        Organization org = organizationMapper.selectById(orgId);
        if (org == null) return Collections.emptyList();
        return areaRelationMapper.selectAreaIdsByOrgIdAndType(orgId, org.getType());
    }

    /**
     * 校验街道是否被同级组织占用。
     * 返回冲突信息：按组织聚合，如"XX街道,YY街道被华东大区占用；AA街道被南京办事处占用"
     */
    public String validateStreetConflict(Long orgId, int orgType, List<Long> streetIds) {
        if (streetIds == null || streetIds.isEmpty()) return null;
        List<Map<String, Object>> conflicts = areaRelationMapper.selectStreetOrgConflicts(streetIds, orgType, orgId);
        if (conflicts.isEmpty()) return null;

        // 按组织分组：orgId -> [streetId, ...]
        Map<Long, List<Long>> orgToStreets = new LinkedHashMap<>();
        for (Map<String, Object> c : conflicts) {
            Long conflictOrgId = ((Number) c.get("org_id")).longValue();
            Long streetId = ((Number) c.get("area_id")).longValue();
            orgToStreets.computeIfAbsent(conflictOrgId, k -> new ArrayList<>()).add(streetId);
        }

        // 批量查询街道名和组织名
        Set<Long> allStreetIds = orgToStreets.values().stream().flatMap(List::stream).collect(Collectors.toSet());
        Set<Long> allOrgIds = orgToStreets.keySet();
        List<Area> streets = areaMapper.selectByIdsAndLevel(new ArrayList<>(allStreetIds), Area.LEVEL_TOWNSHIP);
        Map<Long, String> streetNameMap = streets.stream().collect(Collectors.toMap(Area::getId, Area::getName));
        Map<Long, String> orgNameMap = new LinkedHashMap<>();
        for (Long oid : allOrgIds) {
            Organization org = organizationMapper.selectById(oid);
            orgNameMap.put(oid, org != null ? org.getName() : "未知组织");
        }

        // 聚合拼接
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Long, List<Long>> entry : orgToStreets.entrySet()) {
            if (sb.length() > 0) sb.append("；");
            String orgName = orgNameMap.get(entry.getKey());
            String streetNames = entry.getValue().stream()
                    .map(sid -> streetNameMap.getOrDefault(sid, "未知街道"))
                    .collect(Collectors.joining(","));
            sb.append(streetNames).append("被").append(orgName).append("占用");
        }
        return sb.toString();
    }

    @CacheEvict(value = "selectableAreaTree", allEntries = true)
    @Transactional
    public int bindStreets(Long orgId, List<Long> streetIds) {
        Set<Long> newStreetIds = streetIds != null ? new HashSet<>(streetIds) : Collections.emptySet();

        // 1. 查出该组织当前已绑定的街道
        List<AreaRelation> existingRelations = areaRelationMapper.selectByOrgId(orgId);
        Set<Long> existingStreetIds = existingRelations.stream()
                .map(AreaRelation::getAreaId)
                .collect(Collectors.toSet());

        int count = 0;

        // 2. 新增：传入但未绑定的街道
        for (Long streetId : newStreetIds) {
            if (!existingStreetIds.contains(streetId)) {
                AreaRelation relation = new AreaRelation();
                relation.setAreaId(streetId);
                relation.setOrgId(orgId);
                relation.setDelFlag(0);
                areaRelationMapper.insert(relation);
                count++;
            }
        }

        // 3. 删除：已绑定但本次未传入的街道（软删除）
        for (AreaRelation rel : existingRelations) {
            if (!newStreetIds.contains(rel.getAreaId())) {
                rel.setDelFlag(1);
                areaRelationMapper.updateById(rel);
                count++;
            }
        }

        return count;
    }

    @CacheEvict(value = "selectableAreaTree", allEntries = true)
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

    @CacheEvict(value = "selectableAreaTree", allEntries = true)
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

    @Cacheable(value = "selectableAreaTree", key = "#orgId + '-' + #parentOrgId")
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
            // 大区：返回已绑定的街道 + 未绑定任何大区的空闲街道
            List<Long> unboundStreets = areaRelationMapper.selectUnboundStreetsByOrgType(Organization.TYPE_REGION);
            targetStreetIds = new HashSet<>(boundStreetIds);
            targetStreetIds.addAll(unboundStreets);
            if (targetStreetIds.isEmpty()) {
                isFullTree = true;
            }
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
            // 完整树：从内存加载全部层级
            counties = areaCacheService.getByLevel(Area.LEVEL_COUNTY);
            cities = areaCacheService.getByLevel(Area.LEVEL_CITY);
            provinces = areaCacheService.getByLevel(Area.LEVEL_PROVINCE);
            // 加载所有街道
            List<Long> allCountyIds = counties.stream().map(Area::getId).distinct().toList();
            townships = areaCacheService.getByPids(allCountyIds);
            // 所有街道都可选择
            targetStreetIds = townships.stream().map(Area::getId).collect(Collectors.toSet());
            cityIds = cities.stream().map(Area::getId).distinct().toList();
            provinceIds = provinces.stream().map(Area::getId).distinct().toList();
        } else {
            // 按需加载：从内存加载涉及的层级路径
            townships = areaCacheService.getByIds(targetStreetIds);
            countyIds = townships.stream().map(Area::getPid).filter(Objects::nonNull).distinct().toList();
            if (countyIds.isEmpty()) return Collections.emptyList();

            counties = areaCacheService.getByIds(new HashSet<>(countyIds));
            cityIds = counties.stream().map(Area::getPid).filter(Objects::nonNull).distinct().toList();

            cities = cityIds.isEmpty() ? Collections.emptyList() : areaCacheService.getByIds(new HashSet<>(cityIds));
            provinceIds = cities.stream().map(Area::getPid).filter(Objects::nonNull).distinct().toList();

            provinces = provinceIds.isEmpty() ? Collections.emptyList() : areaCacheService.getByIds(new HashSet<>(provinceIds));
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
