# Playwright 自动化联调测试实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 自动化执行前后端联调测试，根据测试反馈自动修复问题，循环迭代直到全部通过。

**Architecture:** 使用 Playwright 启动浏览器，模拟用户操作（登录、访问各管理页面、检查页面是否正常渲染），捕获失败和截图。测试失败后分析原因并自动修复代码，然后重新测试。

**Tech Stack:** Playwright (Node.js) + 现有前端项目 (Vue3) + 后端 (Spring Boot)

**Success Criteria:**
1. 后端服务正常启动（localhost:8080）
2. 前端服务正常启动（localhost:80）
3. Playwright 自动化测试全部通过
4. 每个页面都能正常渲染，无 JS 报错

## 流程设计

### 步骤 1：环境准备

- [ ] 检查后端编译是否通过
- [ ] 检查前端依赖是否安装（yarn install）
- [ ] 安装 Playwright 及浏览器依赖

### 步骤 2：Playwright 测试脚本编写

创建 `food-web-fe/tests/e2e/` 目录，包含以下测试文件：

**tests/e2e/login.spec.js** — 登录测试
- 访问首页，自动跳转到登录页
- 输入 admin/admin123
- 点击登录
- 验证是否成功跳转到首页
- 截图

**tests/e2e/system.spec.js** — 系统管理模块测试
- 用户管理页面：访问 `/system/user`，检查页面元素
- API Key 管理页面：访问 `/system/api-key`，检查页面元素
- 截图

**tests/e2e/org.spec.js** — 组织架构模块测试
- 大区管理页面：访问 `/org/region`
- 办事处管理页面：访问 `/org/office`
- 片区管理页面：访问 `/org/district`
- 截图

**tests/e2e/area.spec.js** — 行政区管理测试
- 行政区维护页面：访问 `/area/manage`
- 截图

### 步骤 3：自动化测试执行

```bash
# 1. 启动后端
cd food-manage-web && JAVA_HOME="..." mvn spring-boot:run &

# 2. 等待后端就绪
curl -s http://localhost:8080/api/auth/getInfo -H "Authorization: Bearer test" || wait

# 3. 启动前端
cd food-web-fe && yarn dev &

# 4. 等待前端就绪
curl -s http://localhost:80 || wait

# 5. 运行 Playwright 测试
cd food-web-fe && npx playwright test --reporter=list,screenshot
```

### 步骤 4：问题自动修复

根据测试输出日志：

| 失败类型 | 自动修复策略 |
|---------|------------|
| API 路径错误 | 修改前端 API 文件中的 URL |
| 响应格式不匹配 | 修改 request.js 拦截器或后端 Result |
| 字段名不匹配 | 修改前端页面字段名或后端 DTO |
| 页面元素不存在 | 修改 Vue 模板 |
| JS 运行时错误 | 分析错误栈，修复对应代码 |
| 后端 500 错误 | 查看后端日志，修复 Java 代码 |
| 数据库问题 | 检查/修复 schema.sql |

修复后重新执行步骤 3，直到全部通过。

### 步骤 5：生成测试报告

输出测试结果汇总：
- 通过的测试用例
- 失败的测试用例及原因
- 修复记录
- 最终截图文件

## 文件变更清单

| 操作 | 文件 | 说明 |
|------|------|------|
| 创建 | `food-web-fe/package.json` (新增 playwright 依赖) | 添加测试依赖 |
| 创建 | `food-web-fe/playwright.config.js` | Playwright 配置 |
| 创建 | `food-web-fe/tests/e2e/*.spec.js` | 测试用例 |
