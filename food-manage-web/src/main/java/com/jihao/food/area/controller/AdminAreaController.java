package com.jihao.food.area.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jihao.food.area.dto.AreaDetailDTO;
import com.jihao.food.area.dto.AreaImportDTO;
import com.jihao.food.area.dto.AreaTreeDTO;
import com.jihao.food.area.entity.Area;
import com.jihao.food.area.service.AreaQueryService;
import com.jihao.food.area.service.AreaService;
import com.jihao.food.common.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/admin-area")
@RequiredArgsConstructor
public class AdminAreaController {

    private final AreaService areaService;
    private final AreaQueryService areaQueryService;

    @GetMapping("/tree")
    public Result<List<AreaTreeDTO>> tree(
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false, defaultValue = "1") Integer depth) {
        return Result.success(areaQueryService.getTree(type, areaId, depth));
    }

    @GetMapping("/detail")
    public Result<AreaDetailDTO> detail(@RequestParam Long id) {
        return Result.success(areaQueryService.getDetail(id));
    }

    @PostMapping("/create")
    public Result<Area> create(@Validated @RequestBody CreateAreaRequest request) {
        Area area = new Area();
        area.setId(request.getId());
        area.setPid(request.getPid());
        area.setName(request.getName());
        area.setShortName(request.getShortName());
        area.setFullName(request.getFullName());
        area.setAdcode(request.getAdcode());
        area.setZipCode(request.getZipCode());
        area.setLng(request.getLng());
        area.setLat(request.getLat());
        return Result.success(areaService.create(area));
    }

    @PostMapping("/update")
    public Result<Area> update(@Validated @RequestBody UpdateAreaRequest request) {
        return Result.success(areaService.update(request.getId(), request.getName(),
                request.getShortName(), request.getLng(), request.getLat()));
    }

    @PostMapping("/delete")
    public Result<Void> delete(@RequestParam Long id) {
        areaService.delete(id);
        return Result.success(null);
    }

    @PostMapping("/status")
    public Result<Void> toggleStatus(@RequestParam Long id, @RequestParam Integer state) {
        areaService.toggleStatus(id, state);
        return Result.success(null);
    }

    @PostMapping("/import")
    public Result<Integer> doImport(@Valid @RequestBody List<Area> importData) {
        return Result.success(areaService.doImport(importData));
    }

    @PostMapping("/import-preview")
    public Result<List<AreaImportDTO>> importPreview(@Valid @RequestBody List<Area> importData) {
        return Result.success(areaService.previewImport(importData));
    }

    @GetMapping("/disabled")
    public Result<Page<Area>> listDisabled(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(areaService.listDisabled(page, size));
    }

    @Data
    static class CreateAreaRequest {
        private Long id;
        private Long pid;
        @NotNull(message = "行政区名称不能为空")
        private String name;
        private String shortName;
        private String fullName;
        private String adcode;
        private String zipCode;
        private BigDecimal lng;
        private BigDecimal lat;
    }

    @Data
    static class UpdateAreaRequest {
        private Long id;
        private String name;
        private String shortName;
        private BigDecimal lng;
        private BigDecimal lat;
    }
}
