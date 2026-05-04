package com.jihao.food.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jihao.food.common.Result;
import com.jihao.food.system.entity.SysApiKey;
import com.jihao.food.system.service.ApiKeyService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/api-key")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @GetMapping("/list")
    public Result<Page<SysApiKey>> list(
            @RequestParam(required = false) String appName,
            @RequestParam(required = false) Integer state,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(apiKeyService.list(appName, state, page, size));
    }

    @PostMapping("/create")
    public Result<SysApiKey> create(@Validated @RequestBody CreateApiKeyRequest request) {
        return Result.success(apiKeyService.create(request.getAppName(), request.getRemark()));
    }

    @PostMapping("/update")
    public Result<SysApiKey> update(@Validated @RequestBody UpdateApiKeyRequest request) {
        return Result.success(apiKeyService.update(request.getId(), request.getAppName(), request.getRemark()));
    }

    @PostMapping("/status")
    public Result<SysApiKey> status(@Validated @RequestBody UpdateStatusRequest request) {
        return Result.success(apiKeyService.updateStatus(request.getId(), request.getState()));
    }

    @PostMapping("/regenerate-secret")
    public Result<SysApiKey> regenerateSecret(@Validated @RequestBody RegenerateSecretRequest request) {
        return Result.success(apiKeyService.regenerateSecret(request.getId()));
    }

    @Data
    static class CreateApiKeyRequest {
        @NotBlank(message = "应用名称不能为空")
        private String appName;
        private String remark;
    }

    @Data
    static class UpdateApiKeyRequest {
        @NotNull(message = "ID 不能为空")
        private Long id;
        @NotBlank(message = "应用名称不能为空")
        private String appName;
        private String remark;
    }

    @Data
    static class UpdateStatusRequest {
        @NotNull(message = "ID 不能为空")
        private Long id;
        @NotNull(message = "状态不能为空")
        private Integer state;
    }

    @Data
    static class RegenerateSecretRequest {
        @NotNull(message = "ID 不能为空")
        private Long id;
    }
}
