package com.jihao.food.relation.controller;

import com.jihao.food.area.entity.Area;
import com.jihao.food.area.mapper.AreaMapper;
import com.jihao.food.common.Result;
import com.jihao.food.org.entity.Organization;
import com.jihao.food.org.mapper.OrganizationMapper;
import com.jihao.food.relation.entity.AreaRelation;
import com.jihao.food.relation.service.AreaRelationService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/open/geo")
@RequiredArgsConstructor
public class GeoQueryController {

    private final AreaRelationService areaRelationService;
    private final OrganizationMapper organizationMapper;
    private final AreaMapper areaMapper;

    @GetMapping("/org")
    public Result<OrgInfoDTO> getOrgByAreaId(@RequestParam Long areaId) {
        List<AreaRelation> relations = areaRelationService.getOrgByAreaId(areaId);
        if (relations == null || relations.isEmpty()) {
            return Result.success(null);
        }

        AreaRelation relation = relations.get(0);
        Organization district = organizationMapper.selectById(relation.getOrgId());
        if (district == null) {
            return Result.success(null);
        }

        OfficeInfo office = findParent(district.getParentId(), Organization.TYPE_OFFICE);
        Organization region = findParentEntity(office != null ? office.getParentId() : district.getParentId(), Organization.TYPE_REGION);

        Area area = areaMapper.selectById(areaId);

        OrgInfoDTO dto = new OrgInfoDTO();
        dto.setAreaId(areaId);
        dto.setAreaName(area != null ? area.getName() : "");
        dto.setOrgId(district.getId());
        dto.setOrgName(district.getName());
        if (office != null) {
            dto.setOfficeId(office.getId());
            dto.setOfficeName(office.getName());
        }
        if (region != null) {
            dto.setRegionId(region.getId());
            dto.setRegionName(region.getName());
        }
        return Result.success(dto);
    }

    @GetMapping("/org/by-township")
    public Result<TownshipOrgDTO> getOrgByTownshipId(@RequestParam Long townshipId) {
        List<AreaRelation> relations = areaRelationService.getOrgByAreaId(townshipId);
        if (relations == null || relations.isEmpty()) {
            return Result.success(null);
        }

        AreaRelation relation = relations.get(0);
        Organization district = organizationMapper.selectById(relation.getOrgId());
        if (district == null) {
            return Result.success(null);
        }

        Area area = areaMapper.selectById(townshipId);
        OfficeInfo office = findParent(district.getParentId(), Organization.TYPE_OFFICE);
        Organization region = findParentEntity(office != null ? office.getParentId() : district.getParentId(), Organization.TYPE_REGION);

        TownshipOrgDTO dto = new TownshipOrgDTO();
        dto.setTownshipId(townshipId);
        dto.setTownshipName(area != null ? area.getName() : "");
        if (region != null) {
            dto.setRegionId(region.getId());
            dto.setRegionName(region.getName());
        }
        if (office != null) {
            dto.setOfficeId(office.getId());
            dto.setOfficeName(office.getName());
        }
        dto.setDistrictId(district.getId());
        dto.setDistrictName(district.getName());
        return Result.success(dto);
    }

    private OfficeInfo findParent(Long id, int type) {
        if (id == null || id == 0) return null;
        Organization org = organizationMapper.selectById(id);
        if (org != null && org.getType() == type) {
            return new OfficeInfo(org.getId(), org.getName(), org.getParentId());
        }
        if (org != null) {
            return findParent(org.getParentId(), type);
        }
        return null;
    }

    private Organization findParentEntity(Long id, int type) {
        if (id == null || id == 0) return null;
        Organization org = organizationMapper.selectById(id);
        if (org != null && org.getType() == type) {
            return org;
        }
        if (org != null) {
            return findParentEntity(org.getParentId(), type);
        }
        return null;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class OrgInfoDTO {
        private Long areaId;
        private String areaName;
        private Long orgId;
        private String orgName;
        private Long officeId;
        private String officeName;
        private Long regionId;
        private String regionName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TownshipOrgDTO {
        private Long townshipId;
        private String townshipName;
        private Long regionId;
        private String regionName;
        private Long officeId;
        private String officeName;
        private Long districtId;
        private String districtName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class OfficeInfo {
        private Long id;
        private String name;
        private Long parentId;
    }
}
