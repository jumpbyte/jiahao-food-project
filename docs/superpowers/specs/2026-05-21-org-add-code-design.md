# organization 表新增 code 字段设计

**Goal:** 为 organization 表新增 code 字段，用于存储从其他系统导入的组织编码。

**Date:** 2026-05-21

---

## 设计决策

### parent_id 策略

**保持 parent_id 使用自增 id，不改为 code。**

理由：
1. 现有树形结构（path、level、parent_id）均基于自增 id 构建
2. code 是外部系统标识，作为引用字段独立存在
3. 外部系统的 code 可能变化，自增 id 作为内部关联更稳定

### code 字段定义

- 字段名：`code`
- 类型：`varchar(50)`
- 默认值：`''`
- 注释：`组织编码（外部系统导入）`
- 位置：`name` 字段之后

### 涉及变更

1. **数据库**：schema.sql 添加 code 字段
2. **实体类**：Organization.java 添加 code 属性
3. **DTO**：OrganizationDTO 添加 code 属性
4. **树查询**：OrganizationTreeDTO 如需要也添加

---

## 实施范围

- [ ] 数据库 schema.sql 添加 ALTER TABLE 语句
- [ ] Organization 实体类添加 code 字段
- [ ] OrganizationDTO 添加 code 字段
- [ ] OrganizationTreeDTO 添加 code 字段（如需）
- [ ] 前端相关页面显示 code 字段（如需）