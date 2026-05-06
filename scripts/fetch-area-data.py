#!/usr/bin/env python3
"""
Fetch China administrative area data (4 levels: province/city/county/township)
from modood/Administrative-divisions-of-China GitHub repository and generate SQL
for insertion into the `area` table.

Data source: https://github.com/modood/Administrative-divisions-of-China
File: dist/pcas-code.json (nested tree with 4 levels)
"""

import json
import urllib.request
import sys
from datetime import datetime

DATA_URL = "https://raw.githubusercontent.com/modood/Administrative-divisions-of-China/master/dist/pcas-code.json"
PROXY = "http://127.0.0.1:2022"
BATCH_SIZE = 500
VERSION = "modood-pcas-2024"


def fetch_json(url):
    """Fetch JSON from URL using proxy."""
    print(f"Fetching {url} via proxy...")
    proxy_handler = urllib.request.ProxyHandler({
        "https": PROXY,
        "http": PROXY,
    })
    opener = urllib.request.build_opener(proxy_handler)
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with opener.open(req, timeout=60) as resp:
        return json.loads(resp.read().decode("utf-8"))


# Suffixes to strip from short_name by level
SUFFIX_MAP = {
    1: ["省", "市", "自治区", "壮族自治区", "回族自治区", "维吾尔自治区", "自治区", "特别行政区"],
    2: ["市", "地区", "自治州", "盟"],
    3: ["区", "市", "县", "旗", "自治县", "自治旗", "特区", "林区"],
    4: ["街道", "镇", "乡", "民族乡", "苏木", "民族苏木", "办事处"],
}


def strip_short_name(name, level):
    """Remove administrative suffixes from name to create short_name."""
    if not name:
        return name
    suffixes = SUFFIX_MAP.get(level, [])
    for suffix in suffixes:
        if name.endswith(suffix):
            return name[: -len(suffix)]
    return name


def build_records(provinces):
    """Flatten the nested tree into flat records.

    Handles special cases where city and county share the same code
    (直筒子市: 东莞市, 中山市, 儋州市). For these, county id = rcode + '1'.
    """
    records = []
    seen_ids = set()

    for province in provinces:
        pcode = province["code"]
        pname = province["name"]
        province_id = int(pcode + "0000")
        seen_ids.add(province_id)

        # Province (level 1)
        records.append({
            "id": province_id,
            "pid": 0,
            "level": 1,
            "name": pname,
            "short_name": strip_short_name(pname, 1),
            "full_name": pname,
            "adcode": pcode + "0000",
            "path": pcode + "0000",
        })

        for city in province.get("children", []):
            ccode = city["code"]
            cname = city["name"]
            city_id = int(ccode + "00")
            seen_ids.add(city_id)
            city_full = f"{pname}{cname}"
            city_path = f"{pcode}0000/{ccode}00"

            # City (level 2)
            records.append({
                "id": city_id,
                "pid": int(pcode + "0000"),
                "level": 2,
                "name": cname,
                "short_name": strip_short_name(cname, 2),
                "full_name": city_full,
                "adcode": ccode + "00",
                "path": city_path,
            })

            for county in city.get("children", []):
                rcode = county["code"]
                rname = county["name"]
                county_full = f"{city_full}{rname}"

                # Handle duplicate IDs (直筒子市)
                county_id_candidate = int(rcode)
                if county_id_candidate in seen_ids:
                    county_id = int(rcode + "1")
                    county_path = f"{pcode}0000/{ccode}00/{rcode}1"
                else:
                    county_id = county_id_candidate
                    county_path = f"{pcode}0000/{ccode}00/{rcode}"
                seen_ids.add(county_id)

                # County (level 3)
                records.append({
                    "id": county_id,
                    "pid": city_id,
                    "level": 3,
                    "name": rname,
                    "short_name": strip_short_name(rname, 3),
                    "full_name": county_full,
                    "adcode": rcode,
                    "path": county_path,
                })

                for township in county.get("children", []):
                    tcode = township["code"]
                    tname = township["name"]
                    township_full = f"{county_full}{tname}"
                    township_path = f"{pcode}0000/{ccode}00/{rcode}/{tcode}"

                    # Township (level 4)
                    records.append({
                        "id": int(tcode),
                        "pid": county_id,
                        "level": 4,
                        "name": tname,
                        "short_name": strip_short_name(tname, 4),
                        "full_name": township_full,
                        "adcode": tcode,
                        "path": township_path,
                    })

    return records


def escape_sql_string(s):
    """Escape single quotes and backslashes for SQL."""
    return str(s).replace("\\", "\\\\").replace("'", "\\'")


def generate_sql(records, output_path):
    """Generate INSERT SQL statements."""
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    timestamp = f"'{now}'"

    with open(output_path, "w", encoding="utf-8") as f:
        f.write("-- 中国行政区四级数据自动导入\n")
        f.write(f"-- 数据来源: modood/Administrative-divisions-of-China (pcas-code.json)\n")
        f.write(f"-- 生成时间: {now}\n")
        f.write(f"-- 总记录数: {len(records)}\n\n")
        f.write("SET NAMES utf8mb4;\n")
        f.write("SET FOREIGN_KEY_CHECKS = 0;\n\n")

        # Count by level
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

            f.write(f"-- Batch {batch_count} (records {i+1}-{i+len(batch)})\n")
            f.write("INSERT INTO `area` (`id`, `pid`, `level`, `name`, `short_name`, `full_name`, `pin_yin`, `adcode`, `zip_code`, `lng`, `lat`, `path`, `version`, `state`, `create_time`, `update_time`) VALUES\n")

            for j, rec in enumerate(batch):
                values = [
                    str(rec["id"]),
                    str(rec["pid"]),
                    str(rec["level"]),
                    f"'{escape_sql_string(rec['name'])}'",
                    f"'{escape_sql_string(rec['short_name'])}'",
                    f"'{escape_sql_string(rec['full_name'])}'",
                    "''",
                    f"'{rec['adcode']}'",
                    "''",
                    "0.0000000",
                    "0.0000000",
                    f"'{rec['path']}'",
                    f"'{VERSION}'",
                    "1",
                    timestamp,
                    timestamp,
                ]
                line = "  (" + ", ".join(values) + ")"
                if j < len(batch) - 1:
                    line += ","
                f.write(line + "\n")

            f.write(";\n\n")

        f.write("SET FOREIGN_KEY_CHECKS = 1;\n")

    print(f"\nSQL file generated: {output_path}")
    print(f"Total records: {len(records)}")
    print(f"  Level 1 (Province): {level_counts.get(1, 0)}")
    print(f"  Level 2 (City): {level_counts.get(2, 0)}")
    print(f"  Level 3 (County): {level_counts.get(3, 0)}")
    print(f"  Level 4 (Township): {level_counts.get(4, 0)}")
    print(f"Total batches: {batch_count}")


def main():
    output_path = sys.argv[1] if len(sys.argv) > 1 else "area-data.sql"

    print("Fetching administrative area data from GitHub...\n")

    try:
        data = fetch_json(DATA_URL)
    except Exception as e:
        print(f"\nError fetching data: {e}", file=sys.stderr)
        sys.exit(1)

    print(f"Fetched {len(data)} provinces\n")
    print("Building area records...")

    records = build_records(data)
    print(f"Total records built: {len(records)}\n")

    print(f"Generating SQL to {output_path}...")
    generate_sql(records, output_path)
    print("Done!")


if __name__ == "__main__":
    main()