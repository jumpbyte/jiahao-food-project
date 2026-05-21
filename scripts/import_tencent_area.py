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
    return "".join(item[0] for item in result)


def escape_sql_string(s: str) -> str:
    """转义单引号和反斜杠。"""
    return str(s).replace("\\", "\\\\").replace("'", "\\'")


def derive_pid(code_str: str, level: int) -> int:
    """根据adcode编码规则推导父级ID。"""
    if level == 1:
        return 0
    elif level == 2:
        return int(code_str[:2] + "0000")
    elif level == 3:
        return int(code_str[:4] + "00")
    elif level == 4:
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


def parse_excel_row(code, name: str):
    """解析Excel一行数据，返回area记录或None。"""
    code_str = str(code)
    parts = [p for p in name.split(",") if p and p != "中国"]
    level = len(parts)

    if level < 1 or level > 4:
        return None

    area_name = parts[-1]
    short_name = derive_short_name(area_name, level)
    full_name = "".join(parts)
    pin_yin = derive_pin_yin(short_name)
    adcode = code_str
    pid = derive_pid(code_str, level)
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
            if count == 0:
                count += 1
                continue
            code, name = row
            rec = parse_excel_row(code, name)
            if rec:
                records.append(rec)
                sheet_count += 1
        print(f"  {sheet_name}: 解析 {sheet_count} 条")

    wb.close()

    records.sort(key=lambda r: r["id"])

    print(f"\n总记录数: {len(records)}")

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
