package com.jihao.food.area.controller;

import com.jihao.food.area.dto.AreaDTO;
import com.jihao.food.area.dto.AreaDetailDTO;
import com.jihao.food.area.dto.AreaTreeDTO;
import com.jihao.food.area.service.AreaQueryService;
import com.jihao.food.common.Result;
import com.jihao.food.common.annotation.IgnoreAuth;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/area")
@RequiredArgsConstructor
@IgnoreAuth
public class AreaQueryController {

    private final AreaQueryService areaQueryService;

    @GetMapping("/provinces")
    public Result<List<AreaDTO>> provinces() {
        return Result.success(areaQueryService.listByLevel(1));
    }

    @GetMapping("/cities")
    public Result<List<AreaDTO>> cities(@RequestParam Long provinceId) {
        return Result.success(areaQueryService.listByParentId(provinceId));
    }

    @GetMapping("/counties")
    public Result<List<AreaDTO>> counties(@RequestParam Long cityId) {
        return Result.success(areaQueryService.listByParentId(cityId));
    }

    @GetMapping("/townships")
    public Result<List<AreaDTO>> townships(@RequestParam Long countyId) {
        return Result.success(areaQueryService.listByParentId(countyId));
    }

    @GetMapping("/detail")
    public Result<AreaDetailDTO> detail(@RequestParam Long areaId) {
        return Result.success(areaQueryService.getDetail(areaId));
    }

    @GetMapping("/tree")
    public Result<List<AreaTreeDTO>> tree(
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false, defaultValue = "1") Integer depth) {
        return Result.success(areaQueryService.getTree(type, areaId, depth));
    }

    @GetMapping("/province-list")
    public Result<List<AreaDTO>> provinceList() {
        return Result.success(areaQueryService.listByLevel(1));
    }

    @GetMapping("/city-list")
    public Result<List<AreaDTO>> cityList() {
        return Result.success(areaQueryService.listByLevel(2));
    }

    @GetMapping("/county-list")
    public Result<List<AreaDTO>> countyList() {
        return Result.success(areaQueryService.listByLevel(3));
    }

    @GetMapping("/township-list")
    public Result<List<AreaDTO>> townshipList() {
        return Result.success(areaQueryService.listByLevel(4));
    }
}
