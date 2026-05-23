package com.jihao.food.org.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jihao.food.common.Result;
import com.jihao.food.common.exception.BusinessException;
import com.jihao.food.org.dto.OrganizationDTO;
import com.jihao.food.org.entity.Organization;
import com.jihao.food.org.service.OrganizationService;
import com.jihao.food.relation.dto.StreetInfoDTO;
import com.jihao.food.relation.service.AreaRelationService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/district")
@RequiredArgsConstructor
public class DistrictController {

    private final OrganizationService organizationService;
    private final AreaRelationService areaRelationService;

    @GetMapping("/list")
    public Result<Page<OrganizationDTO>> list(
            @RequestParam(required = false) Long officeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(organizationService.listByType(Organization.TYPE_DISTRICT, officeId, null, page, size));
    }

    @PostMapping("/create")
    public Result<Organization> create(@Validated @RequestBody CreateDistrictRequest request) {
        Organization org = new Organization();
        org.setName(request.getName());
        org.setType(Organization.TYPE_DISTRICT);
        org.setParentId(request.getOfficeId());
        Organization created = organizationService.create(org);
        if (request.getStreetIds() != null && !request.getStreetIds().isEmpty()) {
            String conflict = areaRelationService.validateStreetConflict(created.getId(), Organization.TYPE_DISTRICT, request.getStreetIds());
            if (conflict != null) {
                throw new BusinessException(601, conflict);
            }
            areaRelationService.bindStreetsWithValidation(created.getId(), request.getStreetIds(), request.getOfficeId());
        }
        return Result.success(created);
    }

    @PostMapping("/update")
    public Result<Organization> update(@Validated @RequestBody UpdateDistrictRequest request) {
        return Result.success(organizationService.update(request.getId(), request.getName(), request.getState()));
    }

    @PostMapping("/delete")
    public Result<Void> delete(@RequestParam Long id) {
        organizationService.delete(id);
        return Result.success(null);
    }

    @PostMapping("/bind-streets")
    public Result<Integer> bindStreets(@Validated @RequestBody BindStreetsRequest request) {
        Organization district = organizationService.getById(request.getOrgId());
        Long parentOrgId = district != null ? district.getParentId() : null;
        String conflict = areaRelationService.validateStreetConflict(request.getOrgId(), Organization.TYPE_DISTRICT, request.getStreetIds());
        if (conflict != null) {
            throw new BusinessException(601, conflict);
        }
        int count = areaRelationService.bindStreetsWithValidation(request.getOrgId(), request.getStreetIds(), parentOrgId);
        return Result.success(count);
    }

    @GetMapping("/selectable-area-tree")
    public Result<List<AreaRelationService.AreaTreeNode>> selectableAreaTree(
            @RequestParam Long orgId,
            @RequestParam(required = false) Long parentOrgId) {
        if (parentOrgId == null) {
            Organization district = organizationService.getById(orgId);
            parentOrgId = district != null ? district.getParentId() : null;
        }
        return Result.success(areaRelationService.getSelectableAreaTree(orgId, parentOrgId));
    }

    @PostMapping("/unbind-street")
    public Result<Void> unbindStreet(@RequestParam Long areaId, @RequestParam Long streetId) {
        areaRelationService.unbindStreet(areaId, streetId);
        return Result.success(null);
    }

    @GetMapping("/streets")
    public Result<List<StreetInfoDTO>> streets(@RequestParam Long areaId) {
        return Result.success(areaRelationService.listStreetsWithInfo(areaId));
    }

    @GetMapping("/parent")
    public Result<ParentInfoDTO> getParent(@RequestParam Long areaId) {
        Organization district = organizationService.getById(areaId);
        if (district == null) {
            return Result.success(null);
        }
        Organization office = organizationService.getById(district.getParentId());
        Organization region = office != null ? organizationService.getById(office.getParentId()) : null;

        ParentInfoDTO dto = new ParentInfoDTO();
        dto.setAreaId(district.getId());
        dto.setAreaName(district.getName());
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

    @Data
    static class CreateDistrictRequest {
        @NotBlank(message = "片区名称不能为空")
        private String name;
        @NotNull(message = "所属办事处不能为空")
        private Long officeId;
        private String remark;
        private List<Long> streetIds;
    }

    @Data
    static class UpdateDistrictRequest {
        private Long id;
        @NotBlank(message = "片区名称不能为空")
        private String name;
        private Integer state;
        private String remark;
    }

    @Data
    static class BindStreetsRequest {
        @NotNull(message = "片区 ID 不能为空")
        private Long orgId;
        @NotEmpty(message = "街道 ID 列表不能为空")
        private List<Long> streetIds;
    }

    @Data
    static class ParentInfoDTO {
        private Long areaId;
        private String areaName;
        private Long officeId;
        private String officeName;
        private Long regionId;
        private String regionName;
    }
}
