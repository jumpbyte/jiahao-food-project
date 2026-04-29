package com.jihao.food.org.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jihao.food.common.Result;
import com.jihao.food.org.dto.OrganizationDTO;
import com.jihao.food.org.entity.Organization;
import com.jihao.food.org.service.OrganizationService;
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
@RequestMapping("/api/area")
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
        return Result.success(organizationService.create(org));
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
        int count = areaRelationService.bindStreets(request.getAreaId(), request.getStreetIds());
        return Result.success(count);
    }

    @PostMapping("/unbind-street")
    public Result<Void> unbindStreet(@Validated @RequestBody UnbindStreetRequest request) {
        areaRelationService.unbindStreet(request.getAreaId(), request.getStreetId());
        return Result.success(null);
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
        private Long areaId;
        @NotEmpty(message = "街道 ID 列表不能为空")
        private List<Long> streetIds;
    }

    @Data
    static class UnbindStreetRequest {
        @NotNull(message = "片区 ID 不能为空")
        private Long areaId;
        @NotNull(message = "街道 ID 不能为空")
        private Long streetId;
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
