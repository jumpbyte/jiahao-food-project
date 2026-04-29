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
| 映射关系 | `relation/` | 片区-街道绑定、行政区归属查询 |
| 系统 | `system/` | SysUser、SysApiKey 实体及 Mapper |

## API 路由

| 模块 | 路径前缀 |
|------|----------|
| 认证 | `/api/auth/login` |
| 大区 | `/api/region/*` |
| 办事处 | `/api/office/*` |
| 片区 | `/api/area/*` (CRUD) + `/api/area/bind-streets` + `/api/area/unbind-street` + `/api/area/parent` |
| 行政区管理 | `/api/admin-area/*` |
| 行政区查询 | `/api/area/provinces` `/api/area/cities` 等 |
| 街道 | `/api/street/list` |
| 归属查询 | `/api/geo/org` `/api/geo/org/by-township` |
| 组织树 | `/api/org/tree` |
