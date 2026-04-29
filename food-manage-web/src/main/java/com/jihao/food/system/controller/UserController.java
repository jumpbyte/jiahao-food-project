package com.jihao.food.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jihao.food.common.Result;
import com.jihao.food.interceptor.AuthInterceptor;
import com.jihao.food.system.entity.SysUser;
import com.jihao.food.system.service.UserService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/list")
    public Result<Page<SysUser>> list(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) Integer state,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(userService.list(username, realName, state, page, size));
    }

    @PostMapping("/create")
    public Result<SysUser> create(@Validated @RequestBody CreateUserRequest request) {
        return Result.success(userService.create(
                request.getUsername(),
                request.getPassword(),
                request.getRealName(),
                request.getPhone(),
                request.getEmail(),
                request.getRole()
        ));
    }

    @PostMapping("/update")
    public Result<SysUser> update(@Validated @RequestBody UpdateUserRequest request) {
        return Result.success(userService.update(
                request.getId(),
                request.getRealName(),
                request.getPhone(),
                request.getEmail(),
                request.getRole()
        ));
    }

    @PostMapping("/status")
    public Result<SysUser> status(@Validated @RequestBody UpdateStatusRequest request) {
        return Result.success(userService.updateStatus(request.getId(), request.getState()));
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Validated @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request.getId(), request.getNewPassword());
        return Result.success(null);
    }

    @PostMapping("/change-password")
    public Result<Void> changePassword(@Validated @RequestBody ChangePasswordRequest request) {
        Long userId = AuthInterceptor.USER_ID.get();
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return Result.success(null);
    }

    @Data
    static class CreateUserRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
        @NotBlank(message = "真实姓名不能为空")
        private String realName;
        private String phone;
        private String email;
        private String role;
    }

    @Data
    static class UpdateUserRequest {
        @NotNull(message = "用户 ID 不能为空")
        private Long id;
        @NotBlank(message = "真实姓名不能为空")
        private String realName;
        private String phone;
        private String email;
        @NotBlank(message = "角色不能为空")
        private String role;
    }

    @Data
    static class UpdateStatusRequest {
        @NotNull(message = "用户 ID 不能为空")
        private Long id;
        @NotNull(message = "状态不能为空")
        private Integer state;
    }

    @Data
    static class ResetPasswordRequest {
        @NotNull(message = "用户 ID 不能为空")
        private Long id;
        @NotBlank(message = "新密码不能为空")
        private String newPassword;
    }

    @Data
    static class ChangePasswordRequest {
        @NotBlank(message = "旧密码不能为空")
        private String oldPassword;
        @NotBlank(message = "新密码不能为空")
        private String newPassword;
    }
}