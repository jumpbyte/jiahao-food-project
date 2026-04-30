# 项目约定

## 用户偏好

- **后续所有操作不用再询问用户，直接执行** — 不要提确认问题，不要问"是否继续"，直接做
- **语言**：尽量使用中文回复

## 项目结构

- 后端项目在 `food-manage-web/` 目录下
- Spring Boot 3.2.5 + MyBatis-Plus 3.5.7 + Java 17+
- Maven 编译需要设置 `JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home"`

## 已完成模块

| 模块 | 包路径 | 说明 |
|------|--------|------|
| 公共组件 | `common/` | Result 统一响应、异常处理、JWT/签名/树工具、注解 |
| 配置 | `config/` | MyBatis-Plus、WebMvc、Jackson、请求体缓存过滤器 |
| 拦截器 | `interceptor/` | JWT 登录拦截、API MD5 签名校验 |
| 认证 | `auth/` | 用户登录 + JWT 生成 |
| 组织架构 | `org/` | 大区/办事处/片区 CRUD + 组织树查询 |
| 行政区 | `area/` | 行政区 CRUD、导入、查询（省市县街道） |
| 映射关系 | `relation/` | 组织-街道绑定（大区/办事处/片区）、行政区归属查询、可选行政区树 |
| 系统 | `system/` | SysUser、SysApiKey 实体、Mapper、Service、Controller（用户管理 + API Key 管理） |

## API 路由

### 后台管理接口（需 JWT 登录，免签名）

| 模块 | 路径前缀 |
|------|----------|
| 大区 | `/api/admin/region/*` (list, create, update, delete, bind-streets, streets, selectable-area-tree) |
| 办事处 | `/api/admin/office/*` (list, create, update, delete, bind-streets, streets, selectable-area-tree) |
| 片区 | `/api/admin/district/*` (list, create, update, delete, bind-streets, unbind-street, parent, selectable-area-tree) |
| 行政区管理 | `/api/admin/area/*` |
| 街道 | `/api/admin/street/list` |
| 组织树 | `/api/admin/org/tree` |
| 用户管理 | `/api/admin/user/*` (list, create, update, status, reset-password, change-password) |
| API Key | `/api/admin/api-key/*` (list, create, update, status, regenerate-secret) |

### 对外开放接口（需 MD5 签名，免 JWT）

| 模块 | 路径前缀 |
|------|----------|
| 行政区查询 | `/api/open/area/*` (provinces, cities, counties, townships, detail, tree, province-list 等) |
| 归属查询 | `/api/open/geo/org` `/api/open/geo/org/by-township` |
