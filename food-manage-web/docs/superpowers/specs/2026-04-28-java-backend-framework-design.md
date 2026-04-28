# 区域管理后台 Java 后端框架设计

## 概述

搭建销售区域管理中台的 Java 后端框架，基于 Spring Boot 3 + MyBatis-Plus，实现组织架构管理、行政区管理、映射关系管理、用户登录鉴权、外部 API 签名验证等功能。

## 技术选型

| 项目 | 选择 |
|------|------|
| Java 版本 | 17 |
| 框架 | Spring Boot 3.x |
| ORM | MyBatis-Plus 3.5.x |
| 数据库 | MySQL 8.x（占位配置） |
| 模块结构 | 单模块 + 包分层 |
| 内部鉴权 | JWT (jjwt) |
| 外部鉴权 | MD5 签名校验 (appKey + timestamp + nonce + body + secret) |
| API 文档 | springdoc-openapi (Swagger) |
| 构建工具 | Maven |
| 其他 | Lombok, commons-codec, validation |

## 项目结构

```
src/main/java/com/jihao/food/
├── common/              # 公共组件
│   ├── Result.java           # 统一响应体 {code, message, data, timestamp, traceId}
│   ├── exception/            # 全局异常处理
│   │   ├── BusinessException.java  # 业务异常
│   │   └── GlobalExceptionHandler.java  # @RestControllerAdvice
│   ├── util/                 # 工具类
│   │   ├── SignUtil.java     # 签名生成/校验 (MD5)
│   │   ├── JwtUtil.java      # JWT 生成/解析
│   │   └── TreeUtil.java     # 列表转树形结构
│   └── annotation/           # 自定义注解
│       └── ApiSign.java      # API 签名校验注解
│
├── config/                # 配置类
│   ├── MybatisPlusConfig.java   # MyBatis-Plus 配置 + 分页插件
│   ├── WebMvcConfig.java        # 拦截器注册、CORS
│   └── JacksonConfig.java       # JSON 序列化配置
│
├── interceptor/             # 拦截器
│   ├── AuthInterceptor.java      # JWT 登录校验（内部用户）
│   └── ApiSignInterceptor.java   # 外部 API 签名校验
│
├── auth/                  # 认证模块
│   ├── controller/AuthController.java
│   ├── service/AuthService.java
│   └── dto/
│       ├── LoginRequest.java
│       └── LoginResponse.java
│
├── org/                   # 组织架构模块（大区/办事处/片区）
│   ├── controller/
│   │   ├── RegionController.java    # 大区 CRUD
│   │   ├── OfficeController.java    # 办事处 CRUD
│   │   └── DistrictController.java  # 片区 CRUD + 上级查询
│   ├── service/
│   │   ├── OrganizationService.java     # 通用组织操作
│   │   ├── OrganizationTreeService.java # 组织树查询
│   │   └── dto/
│   ├── mapper/
│   │   └── OrganizationMapper.java
│   └── entity/
│       └── Organization.java
│
├── area/                  # 行政区模块
│   ├── controller/
│   │   ├── AdminAreaController.java     # 管理接口：CRUD + 导入 + 状态
│   │   └── AreaQueryController.java     # 查询接口：省市县街道列表 + 详情 + 树
│   ├── service/
│   │   ├── AreaService.java             # 行政区 CRUD + 导入
│   │   └── AreaQueryService.java        # 行政区查询
│   ├── mapper/
│   │   └── AreaMapper.java
│   └── entity/
│       └── Area.java
│
├── relation/              # 映射关系模块
│   ├── controller/
│   │   └── AreaRelationController.java  # 街道绑定/解除/查询
│   ├── service/
│   │   └── AreaRelationService.java
│   ├── mapper/
│   │   └── AreaRelationMapper.java
│   └── entity/
│       └── AreaRelation.java
│
├── system/                # 系统模块
│   ├── entity/
│   │   ├── SysUser.java
│   │   └── SysApiKey.java
│   ├── mapper/
│   │   ├── SysUserMapper.java
│   │   └── SysApiKeyMapper.java
│   └── dto/
│       └── UserInfoDTO.java
│
└── FoodProjectApplication.java

src/main/resources/
├── application.yml          # 主配置（占位数据库连接）
└── application-dev.yml      # 开发环境配置
```

## 核心组件设计

### 1. 统一响应体

```java
public class Result<T> {
    private int code;
    private String message;
    private T data;
    private long timestamp;
    private String traceId;
}
```

所有接口统一返回此结构。成功 code=0，错误按设计文档定义的错误码返回。

### 2. 签名校验机制

外部 API 请求需携带 `appKey`、`timestamp`、`nonce`、`sign` 四个请求头。

校验逻辑：
1. `ApiSignInterceptor` 拦截带有 `@ApiSign` 注解的 Controller
2. 根据 appKey 查询 `sys_api_key` 表获取 secret
3. 按 `MD5(appKey + timestamp + nonce + body + secret)` 计算签名并比对
4. 校验 timestamp 与服务器时间差 ≤ 5 分钟

### 3. JWT 认证

内部用户登录流程：
1. `POST /api/auth/login` 验证用户名密码（BCrypt）
2. 生成 JWT（包含 userId、username、roles）
3. 后续请求在请求头携带 `Authorization: Bearer <token>`
4. `AuthInterceptor` 解析 JWT 并注入用户信息到 ThreadLocal

### 4. MyBatis-Plus 配置

- 逻辑删除字段：不使用（设计文档中删除为软删除，通过业务层控制）
- 自动填充：createTime、updateTime
- 分页插件：PageInterceptor
- 乐观锁：不使用（当前场景不需要）

## API 路由汇总

| 模块 | 路径前缀 | 鉴权方式 |
|------|----------|----------|
| 认证 | `/api/auth/*` | 无（仅 login 公开） |
| 大区 | `/api/region/*` | JWT + 签名 |
| 办事处 | `/api/office/*` | JWT + 签名 |
| 片区 | `/api/area/*` + `/api/area/*-streets` | JWT + 签名 |
| 行政区管理 | `/api/admin-area/*` | JWT + 签名 |
| 行政区查询 | `/api/area/*` (查询类) | 签名（仅外部 API） |
| 映射关系 | `/api/geo/*` | 签名 |
| 组织树 | `/api/org/tree` | JWT + 签名 |

## 错误处理

`GlobalExceptionHandler` 统一处理：
- `BusinessException` → code 来自异常，message 来自异常
- `MethodArgumentNotValidException` → 400 + 字段错误信息
- `Exception` → 500 + "服务器内部错误"
- 每个异常记录日志，返回 traceId

## 依赖版本

```xml
<properties>
    <java.version>17</java.version>
    <spring-boot.version>3.2.x</spring-boot.version>
    <mybatis-plus.version>3.5.7</mybatis-plus.version>
    <jjwt.version>0.12.5</jjwt.version>
    <springdoc.version>2.3.0</springdoc.version>
    <commons-codec.version>1.15</commons-codec.version>
    <lombok.version>1.18.30</lombok.version>
</properties>
```
