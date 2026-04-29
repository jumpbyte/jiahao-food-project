package com.jihao.food.org.controller;

import com.jihao.food.common.Result;
import com.jihao.food.org.dto.OrganizationTreeDTO;
import com.jihao.food.org.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/org")
@RequiredArgsConstructor
public class OrgTreeController {

    private final OrganizationService organizationService;

    @GetMapping("/tree")
    public Result<List<OrganizationTreeDTO>> tree(
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Integer state) {
        return Result.success(organizationService.getTree(regionId, state));
    }
}
