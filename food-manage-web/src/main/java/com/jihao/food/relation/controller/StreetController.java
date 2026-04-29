package com.jihao.food.relation.controller;

import com.jihao.food.common.Result;
import com.jihao.food.relation.entity.AreaRelation;
import com.jihao.food.relation.service.AreaRelationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/street")
@RequiredArgsConstructor
public class StreetController {

    private final AreaRelationService areaRelationService;

    @GetMapping("/list")
    public Result<List<AreaRelation>> list(@RequestParam(required = false) Long areaId) {
        if (areaId == null) {
            return Result.success(List.of());
        }
        return Result.success(areaRelationService.listByDistrictId(areaId));
    }
}
