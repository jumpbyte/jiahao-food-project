# 腾讯地图行政区划数据导入实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 编写Python脚本将腾讯地图Excel中的42876条行政区划数据转换为SQL文件，全量覆盖area表

**Architecture:** 参照现有 `scripts/fetch-area-data.py` 的代码风格，编写单文件脚本读取Excel两个sheet，解析name字段推导层级/pid/short_name等，用pypinyin生成拼音，批量生成INSERT SQL

**Tech Stack:** Python 3, openpyxl, pypinyin, 纯文件输出（无数据库直连）

---

### Task 1: 编写核心解析逻辑

**Files:**
- Create: `scripts/import_tencent_area.py`
- Test: 脚本内嵌的验证输出 + 生成SQL后手动检查

这个脚本参照 `fetch-area-data.py` 的模式：
- `SUFFIX_MAP` 去除行政后缀
- `derive_short_name` 生成简称
- `escape_sql_string` 转义SQL字符
- `generate_sql` 批量输出INSERT

关键差异：数据源是Excel（非JSON树），name是逗号分隔全路径（非嵌套结构）

- [ ] **Step 1: 创建脚本文件**

```python
#!/usr/bin/env python3
"""
从腾讯地图Excel文件导入全国行政区划数据到 area 表。

数据源: 腾讯地图_行政区划编码表_20260427.xlsx
  - Sheet "省市区": 3625条（省/市/区县，1-3级）
  - Sheet "乡镇街道": 39251条（乡镇/街道，4级）
目标: 生成 SQL 文件全量覆盖 area 表
"""

import sys
import os
from datetime import datetime

import openpyxl
from pypinyin import pinyin, Style

# ─── 常量 ───────────────────────────────────────────────────
EXCEL_FILE = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "腾讯地图_行政区划编码表_20260427.xlsx"
)
OUTPUT_SQL = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "food-manage-web/src/main/resources/db/area-tencent.sql"
)
BATCH_SIZE = 500
VERSION = "tencent-map-20260427"
TIMESTAMP = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

# 行政后缀映射（按长度降序，优先匹配长后缀）
SUFFIX_MAP = {
    1: ["维吾尔自治区", "壮族自治区", "回族自治区", "自治区", "特别行政区", "省", "市"],
    2: ["自治州", "地区", "盟", "市"],
    3: ["自治旗", "自治县", "林区", "特区", "旗", "县", "区", "市"],
    4: ["街道办事处", "街道", "办事处", "民族乡", "民族苏木", "苏木", "镇", "乡"],
}


# ─── 工具函数 ──────────────────────────────────────────────
def derive_short_name(name: str, level: int) -> str:
    """去除行政后缀生成 short_name。"""
    if not name:
        return name
    suffixes = SUFFIX_MAP.get(level, [])
    for suffix in suffixes:
        if name.endswith(suffix):
            result = name[: -len(suffix)]
            return result if result else name
    return name


def derive_pin_yin(short_name: str) -> str:
    """用 pypinyin 生成拼音，无空格无音调。"""
    if not short_name:
        return ""
    result = pinyin(short_name, style=Style.NORMAL)
    # pinyin("北京") -> [["bei"], ["jing"]]
    return "".join(item[0] for item in result)


def escape_sql_string(s: str) -> str:
    """转义单引号和反斜杠。"""
    return str(s).replace("\\", "\\\\").replace("'", "\\'")


def parse_excel_row(code, name: str):
    """解析Excel一行数据，返回area记录或None。

    Args:
        code: Excel的code（int）
        name: 逗号分隔的全路径，如 "中国,,北京市,东城区,东华门街道"

    Returns:
        dict with keys: id, pid, level, name, short_name, full_name, pin_yin, adcode, path
        or None if invalid
    """
    code_str = str(code)
    # 分割name，过滤空值和"中国"
    parts = [p for p in name.split(",") if p and p != "中国"]
    level = len(parts)

    if level < 1 or level > 4:
        return None

    # 从name中取最后一段作为name
    area_name = parts[-1]
    # short_name
    short_name = derive_short_name(area_name, level)
    # full_name：所有非空段拼接
    full_name = "".join(parts)
    # pin_yin：基于short_name
    pin_yin = derive_pin_yin(short_name)
    # adcode
    adcode = code_str
    # pid推导
    pid = derive_pid(code_str, level)
    # path：从根到自身的adcode序列
    path = build_path(code_str, level)

    return {
        "id": code,
        "pid": pid,
        "level": level,
        "name": area_name,
        "short_name": short_name,
        "full_name": full_name,
        "pin_yin": pin_yin,
        "adcode": adcode,
        "path": path,
    }


def derive_pid(code_str: str, level: int) -> int:
    """根据adcode编码规则推导父级ID。"""
    if level == 1:
        return 0
    elif level == 2:
        # 市级：前2位 + 0000
        return int(code_str[:2] + "0000")
    elif level == 3:
        # 区县级：前4位 + 00
        return int(code_str[:4] + "00")
    elif level == 4:
        # 乡镇级：前6位
        return int(code_str[:6])
    return 0


def build_path(code_str: str, level: int) -> str:
    """构建path字段：父级adcode序列 + 自身adcode。"""
    if level == 1:
        return code_str
    elif level == 2:
        parent = code_str[:2] + "0000"
        return f"{parent}/{code_str}"
    elif level == 3:
        grandparent = code_str[:2] + "0000"
        parent = code_str[:4] + "00"
        return f"{grandparent}/{parent}/{code_str}"
    elif level == 4:
        gp = code_str[:2] + "0000"
        p = code_str[:4] + "00"
        pp = code_str[:6]
        return f"{gp}/{p}/{pp}/{code_str}"
    return code_str


# ─── SQL生成 ──────────────────────────────────────────────
def generate_sql(records: list, output_path: str):
    """生成批量INSERT SQL文件。"""
    with open(output_path, "w", encoding="utf-8") as f:
        f.write("-- 中国行政区数据自动导入（腾讯地图数据源）\n")
        f.write(f"-- 数据来源: 腾讯地图行政区划编码表\n")
        f.write(f"-- 生成时间: {TIMESTAMP}\n")
        f.write(f"-- 总记录数: {len(records)}\n\n")
        f.write("SET NAMES utf8mb4;\n")
        f.write("SET FOREIGN_KEY_CHECKS = 0;\n\n")
        f.write("-- 清空现有数据（全量覆盖）\n")
        f.write("TRUNCATE TABLE `area`;\n\n")

        # 按层级统计
        level_counts = {}
        for r in records:
            level_counts[r["level"]] = level_counts.get(r["level"], 0) + 1
        f.write(f"-- 省级: {level_counts.get(1, 0)}\n")
        f.write(f"-- 市级: {level_counts.get(2, 0)}\n")
        f.write(f"-- 区县级: {level_counts.get(3, 0)}\n")
        f.write(f"-- 乡镇级: {level_counts.get(4, 0)}\n\n")

        batch_count = 0
        for i in range(0, len(records), BATCH_SIZE):
            batch = records[i:i + BATCH_SIZE]
            batch_count += 1
            start_idx = i + 1
            end_idx = i + len(batch)

            f.write(f"-- Batch {batch_count} (records {start_idx}-{end_idx})\n")
            f.write("INSERT INTO `area` (`id`, `pid`, `level`, `name`, `short_name`, `full_name`, `pin_yin`, `adcode`, `zip_code`, `lng`, `lat`, `path`, `version`, `state`, `create_time`, `update_time`) VALUES\n")

            for j, rec in enumerate(batch):
                values = [
                    str(rec["id"]),
                    str(rec["pid"]),
                    str(rec["level"]),
                    f"'{escape_sql_string(rec['name'])}'",
                    f"'{escape_sql_string(rec['short_name'])}'",
                    f"'{escape_sql_string(rec['full_name'])}'",
                    f"'{escape_sql_string(rec['pin_yin'])}'",
                    f"'{rec['adcode']}'",
                    "''",
                    "0.0000000",
                    "0.0000000",
                    f"'{rec['path']}'",
                    f"'{VERSION}'",
                    "1",
                    f"'{TIMESTAMP}'",
                    f"'{TIMESTAMP}'",
                ]
                line = "  (" + ", ".join(values) + ")"
                if j < len(batch) - 1:
                    line += ","
                f.write(line + "\n")

            f.write(";\n\n")

        f.write("SET FOREIGN_KEY_CHECKS = 1;\n")


# ─── 主流程 ────────────────────────────────────────────────
def main():
    excel_path = sys.argv[1] if len(sys.argv) > 1 else EXCEL_FILE
    output_path = sys.argv[2] if len(sys.argv) > 2 else OUTPUT_SQL

    if not os.path.exists(excel_path):
        print(f"错误: Excel文件不存在: {excel_path}", file=sys.stderr)
        sys.exit(1)

    print(f"读取Excel: {excel_path}")
    wb = openpyxl.load_workbook(excel_path, read_only=True, data_only=True)

    records = []
    for sheet_name in ["省市区", "乡镇街道"]:
        if sheet_name not in wb.sheetnames:
            print(f"警告: 工作表 '{sheet_name}' 不存在", file=sys.stderr)
            continue
        ws = wb[sheet_name]
        sheet_count = 0
        count = 0
        for row in ws.iter_rows(values_only=True):
            if count == 0:  # skip header
                count += 1
                continue
            code, name = row
            rec = parse_excel_row(code, name)
            if rec:
                records.append(rec)
                sheet_count += 1
        print(f"  {sheet_name}: 解析 {sheet_count} 条")

    wb.close()

    # 按id排序确保层级顺序（父级在前）
    records.sort(key=lambda r: r["id"])

    print(f"\n总记录数: {len(records)}")

    # 统计
    level_counts = {}
    for r in records:
        level_counts[r["level"]] = level_counts.get(r["level"], 0) + 1
    for lv in sorted(level_counts.keys()):
        names = {1: "省级", 2: "市级", 3: "区县级", 4: "乡镇级"}
        print(f"  {names.get(lv, f'{lv}级')}: {level_counts[lv]}")

    print(f"\n生成SQL文件: {output_path}")
    generate_sql(records, output_path)
    print(f"文件大小: {os.path.getsize(output_path) / 1024 / 1024:.1f} MB")
    print("完成!")


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: 运行脚本生成SQL**

```bash
cd /Users/yuanjinan/codespace/jiahao-food-project
python3 scripts/import_tencent_area.py
```

Expected output:
```
读取Excel: ...腾讯地图_行政区划编码表_20260427.xlsx
  省市区: 解析 ~3625 条
  乡镇街道: 解析 ~39251 条

总记录数: ~42876
  省级: ~31
  市级: ~340
  区县级: ~3127
  乡镇级: ~39251

生成SQL文件: ...area-tencent.sql
文件大小: ~XX MB
完成!
```

- [ ] **Step 3: 验证生成的SQL文件**

检查SQL文件内容是否正确：
```bash
head -40 food-manage-web/src/main/resources/db/area-tencent.sql
```

确认包含：
- `TRUNCATE TABLE area;` 语句
- `SET NAMES utf8mb4;`
- 按层级的统计注释
- 第一批INSERT语句格式正确

- [ ] **Step 4: 抽样验证数据准确性**

选取几个典型区域验证字段是否正确：
```bash
# 检查北京的几条记录
grep "110000\|110100\|110101\|110101001" food-manage-web/src/main/resources/db/area-tencent.sql | head -10
```

预期结果：
```
(110000, 0, 1, '北京市', '北京', '北京市', 'beijing', '110000', ..., '110000', ...)
(110100, 110000, 2, '市辖区', '市辖区', '北京市市辖区', ..., '110000/110100', ...)
(110101, 110100, 3, '东城区', '东城', '北京市东城区', 'dongcheng', '110101', ..., '110000/110100/110101', ...)
(110101001, 110101, 4, '东华门街道', '东华门', '北京市东城区东华门街道', 'donghuamen', ..., '110000/110100/110101/110101001', ...)
```

- [ ] **Step 5: 检查拼音生成**

```bash
# 检查几个拼音值
grep "pin_yin" food-manage-web/src/main/resources/db/area-tencent.sql | head -5
# 实际检查SQL值中的拼音部分
grep "beijing\|dongcheng\|donghuamen" food-manage-web/src/main/resources/db/area-tencent.sql | head -5
```

- [ ] **Step 6: 提交**

```bash
git add scripts/import_tencent_area.py food-manage-web/src/main/resources/db/area-tencent.sql
git commit -m "feat: 添加腾讯地图行政区划数据导入脚本和生成的SQL文件"
```

---

## 执行命令参考

```bash
# 生成SQL（默认路径）
python3 scripts/import_tencent_area.py

# 自定义Excel路径和输出路径
python3 scripts/import_tencent_area.py /path/to/excel.xlsx /path/to/output.sql

# 导入数据库（执行前请确认已备份）
mysql -u root -p jiahao_food_db < food-manage-web/src/main/resources/db/area-tencent.sql
```
