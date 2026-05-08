# 用户管理与 API Key 管理实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为销售区域管理后台新增用户管理和 API Key 管理两个模块，包含完整 CRUD、角色分配、密码管理、Secret 重置功能。

**Architecture:** 在现有 `system/` 模块下新增 Service 和 Controller，复用已有 Entity/Mapper。用户管理通过 AuthInterceptor 的 ThreadLocal 获取当前登录用户 ID。API Key 自动生成 appKey 和 appSecret。

**Tech Stack:** Spring Boot 3.2.5 + MyBatis-Plus 3.5.7 + Java 17 + BCrypt (spring-security-crypto) + jjwt 0.12.5

---

### Task 1: sys_user 表增加 role 字段

**Files:**
- Modify: `food-manage-web/src/main/resources/db/schema.sql`
- Modify: `food-manage-web/src/main/java/com/jihao/food/system/entity/SysUser.java`

- [ ] **Step 1: 更新 schema.sql 中 sys_user 表定义**

在 `email` 字段之后、`state` 字段之前添加 `role` 字段：

```sql
-- 在 schema.sql 的 sys_user 表 CREATE TABLE 中，email 行之后添加：
`role` varchar(20) NOT NULL DEFAULT 'operator' COMMENT '角色 admin-管理员 operator-运营人员 viewer-只读人员',
```

同时更新初始数据 INSERT，加上 role 值：

```sql
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`, `state`)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'admin', 1);
```

- [ ] **Step 2: SysUser 实体增加 role 属性**

在 `food-manage-web/src/main/java/com/jihao/food/system/entity/SysUser.java` 的 `email` 字段后添加：

```java
private String role;
```

- [ ] **Step 3: 提交**

```bash
git add food-manage-web/src/main/resources/db/schema.sql food-manage-web/src/main/java/com/jihao/food/system/entity/SysUser.java
git commit -m "feat: sys_user 表增加 role 字段，支持管理员/运营/只读三种角色"
```

---

### Task 2: UserService — 用户业务逻辑

**Files:**
- Create: `food-manage-web/src/main/java/com/jihao/food/system/service/UserService.java`

依赖已有文件：
- `food-manage-web/src/main/java/com/jihao/food/system/mapper/SysUserMapper.java`
- `food-manage-web/src/main/java/com/jihao/food/common/exception/BusinessException.java`

- [ ] **Step 1: 创建 UserService**

```java
package com.jihao.food.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jihao.food.common.exception.BusinessException;
import com.jihao.food.system.entity.SysUser;
import com.jihao.food.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper sysUserMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 分页查询用户列表
     */
    public Page<SysUser> list(String username, String realName, Integer state, int page, int size) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isBlank()) {
            wrapper.like(SysUser::getUsername, username);
        }
        if (realName != null && !realName.isBlank()) {
            wrapper.like(SysUser::getRealName, realName);
        }
        if (state != null) {
            wrapper.eq(SysUser::getState, state);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        return sysUserMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 创建用户
     */
    @Transactional
    public SysUser create(String username, String password, String realName, String phone, String email, String role) {
        SysUser existing = sysUserMapper.findByUsername(username);
        if (existing != null) {
            throw new BusinessException(400, "用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setPhone(phone != null ? phone : "");
        user.setEmail(email != null ? email : "");
        user.setRole(role != null ? role : "operator");
        user.setState(1);
        sysUserMapper.insert(user);
        return user;
    }

    /**
     * 更新用户
     */
    @Transactional
    public SysUser update(Long id, String realName, String phone, String email, String role) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setRealName(realName);
        user.setPhone(phone != null ? phone : "");
        user.setEmail(email != null ? email : "");
        user.setRole(role != null ? role : "operator");
        sysUserMapper.updateById(user);
        return user;
    }

    /**
     * 启用/禁用
     */
    @Transactional
    public SysUser updateStatus(Long id, Integer state) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setState(state);
        sysUserMapper.updateById(user);
        return user;
    }

    /**
     * 重置密码
     */
    @Transactional
    public boolean resetPassword(Long id, String newPassword) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(user);
        return true;
    }

    /**
     * 修改自己的密码
     */
    @Transactional
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(400, "旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(user);
        return true;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add food-manage-web/src/main/java/com/jihao/food/system/service/UserService.java
git commit -m "feat: 新增 UserService，包含列表/创建/更新/状态/重置密码/修改密码"
```

---

### Task 3: UserController — 用户管理接口

**Files:**
- Create: `food-manage-web/src/main/java/com/jihao/food/system/controller/UserController.java`
- Modify: `food-manage-web/src/main/java/com/jihao/food/interceptor/AuthInterceptor.java`

依赖已有文件：
- `food-manage-web/src/main/java/com/jihao/food/system/service/UserService.java`
- `food-manage-web/src/main/java/com/jihao/food/common/Result.java`

- [ ] **Step 1: 在 AuthInterceptor 中暴露 role 的 ThreadLocal**

在 `AuthInterceptor.java` 中，添加 ROLE 的 ThreadLocal：

```java
public static final ThreadLocal<String> USER_ROLE = new ThreadLocal<>();
```

在 `afterCompletion` 中清理：

```java
USER_ROLE.remove();
```

> 注意：当前 JWT 中未存储 role，change-password 接口直接从 ThreadLocal USER_ID 取 userId 即可，不需要 role。此处不需要修改 AuthInterceptor，UserController 的 change-password 直接通过 AuthInterceptor.USER_ID.get() 获取当前用户 ID。

（实际不需要修改 AuthInterceptor，UserController 直接用 `AuthInterceptor.USER_ID.get()` 即可）

- [ ] **Step 2: 创建 UserController**

```java
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
```

- [ ] **Step 2: 提交**

```bash
git add food-manage-web/src/main/java/com/jihao/food/system/controller/UserController.java
git commit -m "feat: 新增 UserController，包含列表/创建/更新/状态/重置密码/修改密码接口"
```

---

### Task 4: ApiKeyService — API Key 业务逻辑

**Files:**
- Create: `food-manage-web/src/main/java/com/jihao/food/system/service/ApiKeyService.java`

依赖已有文件：
- `food-manage-web/src/main/java/com/jihao/food/system/mapper/SysApiKeyMapper.java`
- `food-manage-web/src/main/java/com/jihao/food/system/entity/SysApiKey.java`
- `food-manage-web/src/main/java/com/jihao/food/common/exception/BusinessException.java`

- [ ] **Step 1: 创建 ApiKeyService**

```java
package com.jihao.food.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jihao.food.common.exception.BusinessException;
import com.jihao.food.system.entity.SysApiKey;
import com.jihao.food.system.mapper.SysApiKeyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final SysApiKeyMapper sysApiKeyMapper;

    /**
     * 分页查询 API Key 列表
     */
    public Page<SysApiKey> list(String appName, Integer state, int page, int size) {
        LambdaQueryWrapper<SysApiKey> wrapper = new LambdaQueryWrapper<>();
        if (appName != null && !appName.isBlank()) {
            wrapper.like(SysApiKey::getAppName, appName);
        }
        if (state != null) {
            wrapper.eq(SysApiKey::getState, state);
        }
        wrapper.orderByDesc(SysApiKey::getCreateTime);
        return sysApiKeyMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 创建 API Key
     */
    @Transactional
    public SysApiKey create(String appName, String remark) {
        SysApiKey apiKey = new SysApiKey();
        apiKey.setAppKey("APP_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        apiKey.setAppSecret(UUID.randomUUID().toString().replace("-", ""));
        apiKey.setAppName(appName);
        apiKey.setRemark(remark != null ? remark : "");
        apiKey.setState(1);
        sysApiKeyMapper.insert(apiKey);
        return apiKey;
    }

    /**
     * 更新 API Key
     */
    @Transactional
    public SysApiKey update(Long id, String appName, String remark) {
        SysApiKey apiKey = sysApiKeyMapper.selectById(id);
        if (apiKey == null) {
            throw new BusinessException(404, "API Key 不存在");
        }
        apiKey.setAppName(appName);
        apiKey.setRemark(remark != null ? remark : "");
        sysApiKeyMapper.updateById(apiKey);
        return apiKey;
    }

    /**
     * 启用/禁用
     */
    @Transactional
    public SysApiKey updateStatus(Long id, Integer state) {
        SysApiKey apiKey = sysApiKeyMapper.selectById(id);
        if (apiKey == null) {
            throw new BusinessException(404, "API Key 不存在");
        }
        apiKey.setState(state);
        sysApiKeyMapper.updateById(apiKey);
        return apiKey;
    }

    /**
     * 重新生成 Secret
     */
    @Transactional
    public SysApiKey regenerateSecret(Long id) {
        SysApiKey apiKey = sysApiKeyMapper.selectById(id);
        if (apiKey == null) {
            throw new BusinessException(404, "API Key 不存在");
        }
        apiKey.setAppSecret(UUID.randomUUID().toString().replace("-", ""));
        sysApiKeyMapper.updateById(apiKey);
        return apiKey;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add food-manage-web/src/main/java/com/jihao/food/system/service/ApiKeyService.java
git commit -m "feat: 新增 ApiKeyService，包含列表/创建/更新/状态/重新生成Secret"
```

---

### Task 5: ApiKeyController — API Key 管理接口

**Files:**
- Create: `food-manage-web/src/main/java/com/jihao/food/system/controller/ApiKeyController.java`

依赖已有文件：
- `food-manage-web/src/main/java/com/jihao/food/system/service/ApiKeyService.java`
- `food-manage-web/src/main/java/com/jihao/food/common/Result.java`

- [ ] **Step 1: 创建 ApiKeyController**

```java
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
        @NotNull(message = "API Key ID 不能为空")
        private Long id;
        @NotBlank(message = "应用名称不能为空")
        private String appName;
        private String remark;
    }

    @Data
    static class UpdateStatusRequest {
        @NotNull(message = "API Key ID 不能为空")
        private Long id;
        @NotNull(message = "状态不能为空")
        private Integer state;
    }

    @Data
    static class RegenerateSecretRequest {
        @NotNull(message = "API Key ID 不能为空")
        private Long id;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add food-manage-web/src/main/java/com/jihao/food/system/controller/ApiKeyController.java
git commit -m "feat: 新增 ApiKeyController，包含列表/创建/更新/状态/重新生成Secret接口"
```

---

### Task 6: 编译验证与文档更新

**Files:**
- Modify: `food-manage-web/src/main/resources/db/schema.sql` (已在 Task 1 修改)
- Modify: `详细设计/00-产品概要设计v1.0.md`
- Modify: `CLAUDE.md`

- [ ] **Step 1: 编译验证**

```bash
cd food-manage-web && JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home" mvn compile -q 2>&1 | tail -5
```

预期：无错误输出。

- [ ] **Step 2: 更新 00-产品概要设计v1.0.md**

在 API 列表的「认证相关接口」之后、「大区管理接口」之前，新增两个模块的接口文档：

**用户管理接口（后台，需 JWT 登录）**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/user/list` | 用户列表（分页，支持 username/realName/state 筛选） |
| POST | `/api/admin/user/create` | 新建用户 |
| POST | `/api/admin/user/update` | 编辑用户 |
| POST | `/api/admin/user/status` | 启用/禁用 |
| POST | `/api/admin/user/reset-password` | 重置密码 |
| POST | `/api/admin/user/change-password` | 修改自己的密码 |

**API Key 管理接口（后台，需 JWT 登录）**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/api-key/list` | API Key 列表（分页，支持 appName/state 筛选） |
| POST | `/api/admin/api-key/create` | 新建 API Key |
| POST | `/api/admin/api-key/update` | 编辑 API Key |
| POST | `/api/admin/api-key/status` | 启用/禁用 |
| POST | `/api/admin/api-key/regenerate-secret` | 重新生成 Secret |

- [ ] **Step 3: 更新 CLAUDE.md**

在「已完成模块」表中更新映射关系行：

```
| 系统 | `system/` | SysUser、SysApiKey 实体、Mapper、Service、Controller（用户管理 + API Key 管理） |
```

在 API 路由表中新增：

```
| 用户管理 | `/api/admin/user/*` (list, create, update, status, reset-password, change-password) |
| API Key | `/api/admin/api-key/*` (list, create, update, status, regenerate-secret) |
```

- [ ] **Step 4: 提交**

```bash
git add -A
git commit -m "feat: 编译验证通过，更新设计文档和CLAUDE.md"
```
