package com.jihao.food.org.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jihao.food.common.Result;
import com.jihao.food.org.dto.OrganizationDTO;
import com.jihao.food.org.entity.Organization;
import com.jihao.food.org.service.OrganizationService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/region")
@RequiredArgsConstructor
public class RegionController {

    private final OrganizationService organizationService;

    @GetMapping("/list")
    public Result<Page<OrganizationDTO>> list(
            @RequestParam(required = false) Integer state,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(organizationService.listByType(Organization.TYPE_REGION, null, state, page, size));
    }

    @PostMapping("/create")
    public Result<Organization> create(@Validated @RequestBody CreateRegionRequest request) {
        Organization org = new Organization();
        org.setName(request.getName());
        org.setType(Organization.TYPE_REGION);
        org.setParentId(0L);
        return Result.success(organizationService.create(org));
    }

    @PostMapping("/update")
    public Result<Organization> update(@Validated @RequestBody UpdateRegionRequest request) {
        return Result.success(organizationService.update(request.getId(), request.getName(), request.getState()));
    }

    @PostMapping("/delete")
    public Result<Void> delete(@RequestParam Long id) {
        organizationService.delete(id);
        return Result.success(null);
    }

    @Data
    static class CreateRegionRequest {
        @NotBlank(message = "大区名称不能为空")
        private String name;
        private String remark;
    }

    @Data
    static class UpdateRegionRequest {
        private Long id;
        @NotBlank(message = "大区名称不能为空")
        private String name;
        private Integer state;
        private String remark;
    }
}
