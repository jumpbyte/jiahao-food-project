# 用户管理与 API Key 管理 设计文档

## 1. 概述

为销售区域管理后台新增两个管理模块：
- **用户管理** — 管理后台系统用户，支持 CRUD、角色分配、密码管理
- **API Key 管理** — 管理对外开放接口的调用方凭证，支持生命周期管理和 Secret 重置

两个模块均属于后台管理接口，路径前缀 `/api/admin/`，需 JWT 登录认证，无需 API 签名。

## 2. 数据库变更

### 2.1 sys_user 表增加 role 字段

```sql
ALTER TABLE `sys_user` ADD COLUMN `role` varchar(20) NOT NULL DEFAULT 'operator' COMMENT '角色 admin-管理员 operator-运营人员 viewer-只读人员' AFTER `email`;
```

角色取值：
| 角色值 | 说明 |
|--------|------|
| `admin` | 管理员 |
| `operator` | 运营人员（默认） |
| `viewer` | 只读人员 |

### 2.2 其余表无变更

sys_api_key 表结构已满足需求，无需修改。

## 3. 用户管理

### 3.1 接口列表

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/user/list` | 用户列表（分页） |
| POST | `/api/admin/user/create` | 新建用户 |
| POST | `/api/admin/user/update` | 编辑用户 |
| POST | `/api/admin/user/status` | 启用/禁用 |
| POST | `/api/admin/user/reset-password` | 重置密码 |
| POST | `/api/admin/user/change-password` | 修改自己的密码 |

### 3.2 用户列表

**GET /api/admin/user/list**

请求参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 否 | 用户名模糊搜索 |
| realName | string | 否 | 真实姓名模糊搜索 |
| state | int | 否 | 状态：1-启用 0-禁用 |
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页数量，默认 10 |

返回字段：id, username, realName, phone, email, role, state, createTime, updateTime

### 3.3 新建用户

**POST /api/admin/user/create**

请求参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名（唯一） |
| password | string | 是 | 初始密码（明文，后端 BCrypt 加密） |
| realName | string | 是 | 真实姓名 |
| phone | string | 否 | 手机号 |
| email | string | 否 | 邮箱 |
| role | string | 否 | 角色，默认 operator |

业务规则：
- username 唯一，重复时拒绝
- password 明文传入，后端使用 BCrypt 加密存储

### 3.4 编辑用户

**POST /api/admin/user/update**

请求参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | long | 是 | 用户 ID |
| realName | string | 是 | 真实姓名 |
| phone | string | 否 | 手机号 |
| email | string | 否 | 邮箱 |
| role | string | 是 | 角色 |

不可修改：username、password（通过专门的重置密码接口）

### 3.5 启用/禁用

**POST /api/admin/user/status**

请求参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | long | 是 | 用户 ID |
| state | int | 是 | 状态：1-启用 0-禁用 |

### 3.6 重置密码

**POST /api/admin/user/reset-password**

请求参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | long | 是 | 用户 ID |
| newPassword | string | 是 | 新密码（明文） |

业务规则：
- 仅管理员可操作（后续通过角色权限控制，当前接口不做拦截）
- 密码明文传入，后端 BCrypt 加密

### 3.7 修改自己的密码

**POST /api/admin/user/change-password**

请求参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| oldPassword | string | 是 | 旧密码（明文） |
| newPassword | string | 是 | 新密码（明文） |

业务规则：
- 从 JWT 中获取当前用户 ID
- 校验旧密码是否正确（BCrypt 比对）
- 不正确时返回 400 错误

## 4. API Key 管理

### 4.1 接口列表

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/api-key/list` | API Key 列表（分页） |
| POST | `/api/admin/api-key/create` | 新建 API Key |
| POST | `/api/admin/api-key/update` | 编辑 API Key |
| POST | `/api/admin/api-key/status` | 启用/禁用 |
| POST | `/api/admin/api-key/regenerate-secret` | 重新生成 Secret |

### 4.2 API Key 列表

**GET /api/admin/api-key/list**

请求参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| appName | string | 否 | 应用名称模糊搜索 |
| state | int | 否 | 状态：1-启用 0-禁用 |
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页数量，默认 10 |

返回字段：id, appKey, appSecret, appName, state, remark, createTime, updateTime

> 列表接口返回完整 appSecret，后台管理可见。

### 4.3 新建 API Key

**POST /api/admin/api-key/create**

请求参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| appName | string | 是 | 应用名称 |
| remark | string | 否 | 备注 |

业务规则：
- appKey 由系统自动生成（格式：`APP_` + UUID 前 8 位大写）
- appSecret 由系统自动生成（UUID 去掉横杠）
- 返回包含完整的 appKey 和 appSecret

### 4.4 编辑 API Key

**POST /api/admin/api-key/update**

请求参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | long | 是 | API Key ID |
| appName | string | 是 | 应用名称 |
| remark | string | 否 | 备注 |

不可修改：appKey、appSecret（通过重新生成 Secret 接口）

### 4.5 启用/禁用

**POST /api/admin/api-key/status**

请求参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | long | 是 | API Key ID |
| state | int | 是 | 状态：1-启用 0-禁用 |

### 4.6 重新生成 Secret

**POST /api/admin/api-key/regenerate-secret**

请求参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | long | 是 | API Key ID |

返回字段：id, appKey, appSecret, appName

业务规则：
- 生成新的 appSecret（UUID 去掉横杠）
- 旧 Secret 立即失效（签名校验使用新 Secret）
- 新 Secret 仅在创建/重新生成时返回一次，列表接口仍返回（当前设计列表也返回 Secret）

## 5. 文件变更清单

| 操作 | 文件 | 说明 |
|------|------|------|
| 修改 | `db/schema.sql` | sys_user 表增加 role 字段 |
| 修改 | `system/entity/SysUser.java` | 增加 role 属性 |
| 新增 | `system/service/UserService.java` | 用户业务逻辑 |
| 新增 | `system/controller/UserController.java` | 用户管理接口 |
| 新增 | `system/dto/CreateUserRequest.java` | DTO（也可用内部类） |
| 新增 | `system/dto/UpdateUserRequest.java` | DTO |
| 新增 | `system/dto/ChangePasswordRequest.java` | DTO |
| 新增 | `system/service/ApiKeyService.java` | API Key 业务逻辑 |
| 新增 | `system/controller/ApiKeyController.java` | API Key 管理接口 |
