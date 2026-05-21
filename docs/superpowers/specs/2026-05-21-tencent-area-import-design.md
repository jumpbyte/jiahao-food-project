# 腾讯地图行政区划数据导入设计

**日期**: 2026-05-21
**状态**: 待审批

## 目标

将腾讯地图Excel文件（`腾讯地图_行政区划编码表_20260427.xlsx`）中的全国行政区划数据导入到 `area` 表，全量覆盖现有数据。

## 数据源

| Sheet | 记录数 | 层级 |
|-------|--------|------|
| 省市区 | 3625 | 省/市/区县（1-3级） |
| 乡镇街道 | 39251 | 乡镇/街道（4级） |
| **合计** | **~42876** | |

Excel结构：每行 `(code, name)`，name为逗号分隔的全路径，如 `"中国,,北京市,东城区,东华门街道"`

## 字段映射

| area表字段 | 来源/计算方式 | 示例 |
|------------|--------------|------|
| id | Excel的code | 110101001 |
| pid | 通过code自动推导（见下表） | 110101 |
| level | 解析name中非空段数 | 1-4 |
| name | name最后一个非空段 | "东华门街道" |
| short_name | name去掉"省/市/区/街道/镇/乡"等后缀 | "东华门" |
| full_name | name中所有非空段拼接（去掉"中国"） | "北京市东城区东华门街道" |
| pin_yin | pypinyin(short_name)，无空格无音调 | "dongcheng" |
| adcode | Excel的code（字符串） | "110101001" |
| zip_code | 空字符串 | "" |
| lng | 0 | 0.0000000 |
| lat | 0 | 0.0000000 |
| path | 父级adcode序列 + 自身adcode，用/连接 | "110000/110100/110101/110101001" |
| version | 固定值 | "tencent-map-20260427" |
| state | 固定值 | 1 |
| create_time/update_time | 固定时间戳 | "2026-05-21 00:00:00" |

### pid推导规则

| level | code示例 | pid计算 | 说明 |
|-------|----------|---------|------|
| 1 (省) | 110000 | 0 | 顶级 |
| 2 (市) | 110100 | 前2位+0000 = 110000 | 归属省份 |
| 3 (区县) | 110101 | 前4位+00 = 110100 | 归属城市 |
| 4 (乡镇) | 110101001 | 前6位 = 110101 | 归属区县 |

## 实现方式

### Python脚本

脚本位于 `scripts/import_tencent_area.py`，功能：

1. 读取Excel两个sheet
2. 解析每条记录的层级、name、short_name等
3. 用pypinyin生成拼音
4. 生成SQL文件 `food-manage-web/src/main/resources/db/area-tencent.sql`

### 输出SQL

- 文件开头包含 `TRUNCATE TABLE area;` 实现全量覆盖
- 批量INSERT（每500条一组）
- 兼容utf8mb4

### 执行方式

```bash
# 生成SQL
python3 scripts/import_tencent_area.py

# 导入数据库
mysql -u root -p jiahao_food_db < food-manage-web/src/main/resources/db/area-tencent.sql
```

## 风险与注意事项

1. **全量覆盖**：执行后现有area数据全部丢失，导入前建议备份
2. **编码差异**：腾讯地图与现有modood数据的adcode可能不完全一致，关联此数据的area_relation表可能失效
3. **性能**：约4.3万条数据，生成和导入均很快（秒级）