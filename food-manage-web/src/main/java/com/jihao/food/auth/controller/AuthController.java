package com.jihao.food.auth.controller;

import com.jihao.food.auth.dto.LoginRequest;
import com.jihao.food.auth.dto.LoginResponse;
import com.jihao.food.auth.service.AuthService;
import com.jihao.food.common.Result;
import com.jihao.food.common.annotation.IgnoreAuth;
import com.jihao.food.common.annotation.IgnoreSign;
import com.jihao.food.interceptor.AuthInterceptor;
import com.jihao.food.system.entity.SysUser;
import com.jihao.food.system.mapper.SysUserMapper;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@IgnoreSign
public class AuthController {

    private final AuthService authService;
    private final SysUserMapper sysUserMapper;

    @PostMapping("/login")
    @IgnoreAuth
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }

    /**
     * 获取当前登录用户信息（适配 RuoYi 前端）
     */
    @GetMapping("/getInfo")
    public Result<Map<String, Object>> getInfo() {
        Long userId = AuthInterceptor.USER_ID.get();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.getId());
        userInfo.put("userName", user.getUsername());
        userInfo.put("nickName", user.getRealName());
        userInfo.put("avatar", "");
        result.put("user", userInfo);

        // 根据角色设置 roles
        String role = user.getRole() != null ? user.getRole() : "operator";
        if ("admin".equals(role)) {
            result.put("roles", Collections.singletonList("admin"));
        } else {
            result.put("roles", Collections.singletonList("ROLE_DEFAULT"));
        }
        result.put("permissions", Collections.emptyList());
        result.put("pwdChrtype", 0);
        result.put("isDefaultModifyPwd", false);
        result.put("isPasswordExpired", false);

        return Result.success(result);
    }

    /**
     * 获取路由菜单（适配 RuoYi 前端）
     */
    @GetMapping("/getRouters")
    public Result<List<Map<String, Object>>> getRouters() {
        List<Map<String, Object>> routes = new java.util.ArrayList<>();

        // 系统管理
        Map<String, Object> systemMenu = new java.util.LinkedHashMap<>();
        systemMenu.put("path", "/system");
        systemMenu.put("component", "Layout");
        systemMenu.put("name", "System");
        systemMenu.put("meta", Map.of("title", "系统管理", "icon", "system"));
        systemMenu.put("alwaysShow", true);
        List<Map<String, Object>> systemChildren = new java.util.ArrayList<>();
        systemChildren.add(Map.of(
            "path", "user", "name", "User", "component", "system/user/index",
            "meta", Map.of("title", "用户管理", "icon", "user")
        ));
        systemChildren.add(Map.of(
            "path", "api-key", "name", "ApiKey", "component", "system/apiKey/index",
            "meta", Map.of("title", "API Key管理", "icon", "tool")
        ));
        systemMenu.put("children", systemChildren);
        routes.add(systemMenu);

        // 区域管理
        Map<String, Object> orgMenu = new java.util.LinkedHashMap<>();
        orgMenu.put("path", "/area-manage");
        orgMenu.put("component", "Layout");
        orgMenu.put("name", "AreaManageRoot");
        orgMenu.put("meta", Map.of("title", "区域管理", "icon", "tree"));
        orgMenu.put("alwaysShow", true);
        orgMenu.put("redirect", "noRedirect");
        List<Map<String, Object>> orgChildren = new java.util.ArrayList<>();
        orgChildren.add(Map.of(
            "path", "unified", "name", "OrgUnified", "component", "org/unified/index",
            "meta", Map.of("title", "区域维护", "icon", "edit")
        ));
        orgMenu.put("children", orgChildren);
        routes.add(orgMenu);

        // 行政区管理
        Map<String, Object> areaMenu = new java.util.LinkedHashMap<>();
        areaMenu.put("path", "/area");
        areaMenu.put("component", "Layout");
        areaMenu.put("name", "Area");
        areaMenu.put("meta", Map.of("title", "行政区管理", "icon", "international"));
        areaMenu.put("alwaysShow", true);
        List<Map<String, Object>> areaChildren = new java.util.ArrayList<>();
        areaChildren.add(Map.of(
            "path", "manage", "name", "AreaManage", "component", "area/manage/index",
            "meta", Map.of("title", "行政区维护", "icon", "edit")
        ));
        areaMenu.put("children", areaChildren);
        routes.add(areaMenu);

        return Result.success(routes);
    }
}
