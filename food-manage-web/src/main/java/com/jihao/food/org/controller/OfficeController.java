package com.jihao.food.org.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jihao.food.common.Result;
import com.jihao.food.org.dto.OrganizationDTO;
import com.jihao.food.org.entity.Organization;
import com.jihao.food.org.service.OrganizationService;
import com.jihao.food.relation.entity.AreaRelation;
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
@RequestMapping("/api/admin/office")
@RequiredArgsConstructor
public class OfficeController {

    private final OrganizationService organizationService;
    private final AreaRelationService areaRelationService;

    @GetMapping("/list")
    public Result<Page<OrganizationDTO>> list(
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Integer state,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(organizationService.listByType(Organization.TYPE_OFFICE, regionId, state, page, size));
    }

    @PostMapping("/create")
    public Result<Organization> create(@Validated @RequestBody CreateOfficeRequest request) {
        Organization org = new Organization();
        org.setName(request.getName());
        org.setType(Organization.TYPE_OFFICE);
        org.setParentId(request.getRegionId());
        Organization created = organizationService.create(org);
        if (request.getStreetIds() != null && !request.getStreetIds().isEmpty()) {
            areaRelationService.bindStreetsWithValidation(created.getId(), request.getStreetIds(), request.getRegionId());
        }
        return Result.success(created);
    }

    @PostMapping("/update")
    public Result<Organization> update(@Validated @RequestBody UpdateOfficeRequest request) {
        return Result.success(organizationService.update(request.getId(), request.getName(), request.getState()));
    }

    @PostMapping("/delete")
    public Result<Void> delete(@RequestParam Long id) {
        organizationService.delete(id);
        return Result.success(null);
    }

    @PostMapping("/bind-streets")
    public Result<Integer> bindStreets(@Validated @RequestBody BindStreetsRequest request) {
        Organization office = organizationService.getById(request.getOrgId());
        Long parentOrgId = office != null ? office.getParentId() : null;
        int count = areaRelationService.bindStreetsWithValidation(request.getOrgId(), request.getStreetIds(), parentOrgId);
        return Result.success(count);
    }

    @GetMapping("/streets")
    public Result<List<AreaRelation>> streets(@RequestParam Long orgId) {
        return Result.success(areaRelationService.listByDistrictId(orgId));
    }

    @GetMapping("/selectable-area-tree")
    public Result<List<AreaRelationService.AreaTreeNode>> selectableAreaTree(@RequestParam Long orgId) {
        Organization office = organizationService.getById(orgId);
        Long parentOrgId = office != null ? office.getParentId() : null;
        return Result.success(areaRelationService.getSelectableAreaTree(orgId, parentOrgId));
    }

    @Data
    static class CreateOfficeRequest {
        @NotBlank(message = "办事处名称不能为空")
        private String name;
        @NotNull(message = "所属大区不能为空")
        private Long regionId;
        private String remark;
        private List<Long> streetIds;
    }

    @Data
    static class UpdateOfficeRequest {
        private Long id;
        @NotBlank(message = "办事处名称不能为空")
        private String name;
        private Integer state;
        private String remark;
    }

    @Data
    static class BindStreetsRequest {
        @NotNull(message = "办事处 ID 不能为空")
        private Long orgId;
        @NotEmpty(message = "街道 ID 列表不能为空")
        private List<Long> streetIds;
    }
}
