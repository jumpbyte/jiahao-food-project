package com.jihao.food.org.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jihao.food.common.Result;
import com.jihao.food.org.dto.OrganizationDTO;
import com.jihao.food.org.entity.Organization;
import com.jihao.food.org.service.OrganizationService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/office")
@RequiredArgsConstructor
public class OfficeController {

    private final OrganizationService organizationService;

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
        return Result.success(organizationService.create(org));
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

    @Data
    static class CreateOfficeRequest {
        @NotBlank(message = "办事处名称不能为空")
        private String name;
        @NotNull(message = "所属大区不能为空")
        private Long regionId;
        private String remark;
    }

    @Data
    static class UpdateOfficeRequest {
        private Long id;
        @NotBlank(message = "办事处名称不能为空")
        private String name;
        private Integer state;
        private String remark;
    }
}
