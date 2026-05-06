# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# 项目约定

## 用户偏好

- **不涉及铭感的操作操作就不用再询问用户，直接执行** — 不要提确认问题，不要问"是否继续"，直接做
- **语言**：使用中文回复

## 项目概览

区域管理后台系统，包含组织架构管理（大区→办事处→片区三级）、行政区划管理、组织-街道映射关系、系统用户和 API Key 管理。

### 核心业务规则

1. 组织层级固定为 3 级：大区 → 办事处 → 片区
2. 大区/办事处/片区名称在各自层级内必须唯一
3. 大区/办事处/片区的新建或修改都必须关联街道/乡镇级别行政区（通过 area_relation 表）
4. **层级约束规则**：
   - 办事处可关联的街道/乡镇 ⊆ 所属大区已关联的街道/乡镇
   - 片区可关联的街道/乡镇 ⊆ 所属办事处已关联的街道/乡镇
   - 创建/修改绑定关系时自动校验，超出父级管辖范围则拒绝
5. **一个街道/乡镇只能归属一个片区**（通过 area_relation 唯一约束保证）
6. 删除规则：有子节点或有绑定街道关系则不可删除

## 项目结构

| 目录 | 说明 |
|------|------|
| `food-manage-web/` | 后端 Spring Boot 项目 |
| `food-web-fe/` | 前端 Vue 3 + Element Plus + Vite（基于若依框架） |
| `scripts/` | 运维脚本 |
| `docs/` | 设计文档 |
| `详细设计/` | 详细设计文档 |

### 后端

- 路径：`food-manage-web/`
- 技术栈：Spring Boot 3.2.5 + MyBatis-Plus 3.5.7 + Java 17
- 数据库：MySQL 8.0+，数据库名 `food_project`（application.yml 中配置为 `jiahao_food_db`）
- Maven 编译需要设置 `JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home"`

#### 常用命令

```bash
cd food-manage-web

# 编译
export JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home"
mvn clean compile

# 打包
mvn clean package -DskipTests

# 运行
mvn spring-boot:run

# 运行测试
mvn test

# 运行单个测试类
mvn test -Dtest=TestClassName
```

#### 数据库初始化

```bash
# schema.sql 包含表结构
mysql -u root -p < food-manage-web/src/main/resources/db/schema.sql

# area-data.sql 包含行政区基础数据
mysql -u root -p < food-manage-web/src/main/resources/db/area-data.sql
```

#### 包结构

| 模块 | 包路径 | 说明 |
|------|--------|------|
| 公共组件 | `common/` | Result 统一响应、异常处理、JWT/签名/树工具类、注解 |
| 配置 | `config/` | MyBatis-Plus、WebMvc、Jackson、请求体缓存过滤器 |
| 拦截器 | `interceptor/` | JWT 登录拦截、API MD5 签名校验 |
| 认证 | `auth/` | 用户登录 + JWT 生成 |
| 组织架构 | `org/` | 大区/办事处/片区 CRUD + 组织树查询 |
| 行政区 | `area/` | 行政区 CRUD、导入、查询（省市县街道） |
| 映射关系 | `relation/` | 组织-街道绑定、行政区归属查询、可选行政区树 |
| 系统 | `system/` | SysUser、SysApiKey 实体、Mapper、Service、Controller |

#### 关键依赖

- **ORM**：MyBatis-Plus 3.5.7
- **JWT**：jjwt 0.12.5
- **密码**：Spring Security Crypto (BCrypt)
- **签名**：commons-codec 1.15 (MD5)
- **缓存**：Spring Cache + Caffeine
- **文档**：springdoc-openapi 2.5.0（Swagger UI 访问 `http://localhost:8080/swagger-ui.html`）

### 前端

- 路径：`food-web-fe/`
- 技术栈：Vue 3.5 + Element Plus + Pinia + Vue Router + Vite
- 基于若依（RuoYi）管理系统二次开发

#### 常用命令

```bash
cd food-web-fe

# 安装依赖
npm install

# 开发服务器
npm run dev

# 生产构建
npm run build:prod

# 预构建
npm run build:stage
```

## API 路由

### 后台管理接口（需 JWT 登录，免签名）

| 模块 | 路径前缀 |
|------|----------|
| 大区 | `/api/admin/region/*` |
| 办事处 | `/api/admin/office/*` |
| 片区 | `/api/admin/district/*` |
| 行政区管理 | `/api/admin/area/*` |
| 街道 | `/api/admin/street/list` |
| 组织树 | `/api/admin/org/tree` |
| 用户管理 | `/api/admin/user/*` |
| API Key | `/api/admin/api-key/*` |

### 对外开放接口（需 MD5 签名，免 JWT）

| 模块 | 路径前缀 |
|------|----------|
| 行政区查询 | `/api/open/area/*` |
| 归属查询 | `/api/open/geo/org`、`/api/open/geo/org/by-township` |

## 数据库表

| 表名 | 说明 |
|------|------|
| `area` | 行政区表（省市县街道，四级层级） |
| `organization` | 组织架构表（大区/办事处/片区统一） |
| `area_relation` | 行政区与组织映射关系表 |
| `sys_user` | 系统用户表 |
| `sys_api_key` | API Key 存储表 |

## 测试

- 后端使用 Spring Boot Test，测试文件位于 `food-manage-web/src/test/`
- 前端使用 Playwright E2E 测试，测试文件位于 `food-web-fe/tests/`
- 运行前端 E2E：`cd food-web-fe && npx playwright test`