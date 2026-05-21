# MCA 区划数据导入设计

**日期**: 2026-05-21
**状态**: 待审批

## 目标

将民政部行政区划信息服务平台（MCA）的官方区划数据导入到 `area` 表中，替换现有的 `modood-pcas` 来源数据。

## 数据来源

- **接口**: `https://dmfw.mca.gov.cn/9095/xzqh/getList`
- **方式**: GET 请求
- **参数**: `code`（可选，行政区划代码）、`maxLevel`（1-2，查询深度）
- **响应**: 树形结构 `data[].{code, name, level, type, children[]}`

## API 调用策略

| 步骤 | 调用方式 | 返回层级 | 调用次数 |
|------|----------|----------|----------|
| 1 | `code=`(空), `maxLevel=2` | 省 + 市 | 1 次 |
| 2 | 遍历每个市 code, `maxLevel=2` | 市 + 县 | ~340 次 |
| 3 | 遍历每个县 code, `maxLevel=1` | 县 + 乡镇 | ~3000 次 |
| **合计** | | | **~3341 次** |

每次请求间隔 `2.0s + random(0~0.5s)`，预计总耗时约 **2~3 小时**。

## 编码转换

统计局 12 位编码 → 民政编码截取规则：

| 层级 | 统计局 12 位 | 截取规则 | 结果 |
|------|-------------|----------|------|
| 省/市/县 | `XXXXXXXXXXXX` | 前 6 位 | 6 位民政编码 |
| 乡镇 | `XXXXXXYYYZZZ` | 前 9 位 | 9 位民政编码 |
| 村/居委会 | 全部 | 跳过 | 不导入 |

乡镇级去重：同一县级下多条记录截取前 9 位相同则只保留第一条。

## 字段映射

| area 字段 | 来源/计算规则 |
|---|---|
| `id` | `int(truncated_code)` |
| `pid` | 父节点 id，根节点为 0 |
| `level` | 1→省, 2→市, 3→县, 4→乡镇 |
| `name` | MCA `name` 直接使用 |
| `short_name` | 去除行政后缀（省/市/区/县/自治州/州/盟/旗/自治县/林区/特区/街道办事处/街道/镇/乡） |
| `full_name` | 从根到当前节点 name 用 `/` 拼接 |
| `adcode` | 截取后的 code 字符串 |
| `path` | 从根到当前节点 code 用 `/` 拼接 |
| `pin_yin` | 空字符串 |
| `zip_code` | 空字符串 |
| `lng` | 0 |
| `lat` | 0 |
| `version` | `"mca-official"` |
| `state` | 1 |
| `create_time` | 当前时间 |
| `update_time` | 当前时间 |

## 脚本设计

**文件**: `scripts/import_area_from_mca.py`

**依赖**: `requests` + `pymysql`

**核心类/函数**:
- `McaClient` — 封装 MCA API 调用，包含频率控制和重试逻辑
- `flatten_tree()` — 将树形结构展平为 `(id, pid, level, name, path, full_name, ...)` 列表
- `truncate_code(code, level)` — 编码截取逻辑
- `derive_short_name(name, level)` — 行政后缀去除逻辑
- `DbWriter` — 数据库连接、TRUNCATE、批量 INSERT（每 1000 条 commit）

**频率控制**: `time.sleep(0.1 + random.uniform(0, 0.05))`，失败重试 3 次指数退避

**命令行参数**:
```
--host   数据库地址 (默认: localhost)
--port   数据库端口 (默认: 3306)
--user   数据库用户 (默认: root)
--password 数据库密码
--db     数据库名 (默认: jiahao_food_db)
--dry-run  只打印不写入数据库
```

## 执行流程

```
1. 连接数据库
2. 调 MCA API 获取省+市 (1次)
3. 遍历每个市 → 获取市+县 (~340次)
4. 遍历每个县 → 获取县+乡镇 (~3000次)
5. 展平整棵树，截取编码，计算 pid/path/fullName/shortName
6. 乡镇级去重
7. 打印统计摘要
8. --dry-run 模式下打印 SQL，否则 TRUNCATE + INSERT
```