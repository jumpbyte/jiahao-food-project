#!/usr/bin/env python3
"""
从民政部行政区划信息服务平台 (MCA) 抓取全国四级行政区划数据并导入 area 表。

数据来源: https://dmfw.mca.gov.cn/9095/xzqh/getList
目标表: area (jiahao_food_db)
"""

import json
import random
import time

import pymysql
import requests

MCA_BASE_URL = "https://dmfw.mca.gov.cn/9095/xzqh/getList"
REQUEST_DELAY = 0.1       # 基础间隔 (秒)
REQUEST_JITTER = 0.05     # 随机抖动 (秒)
MAX_RETRIES = 3           # 失败重试次数


class McaClient:
    """封装 MCA API 调用，包含频率控制和重试逻辑。"""

    def __init__(self):
        self.session = requests.Session()
        self.session.headers.update({
            "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) FoodProject/1.0",
            "Accept": "application/json",
        })

    def _delay(self):
        time.sleep(REQUEST_DELAY + random.uniform(0, REQUEST_JITTER))

    def query(self, code=None, max_level=1):
        """调用 MCA API，返回 data 列表。

        Args:
            code: 行政区划代码，None 表示从根节点查询
            max_level: 查询深度 1 或 2
        """
        params = {"maxLevel": max_level}
        if code is not None:
            params["code"] = code

        for attempt in range(1, MAX_RETRIES + 1):
            if attempt > 1:
                self._delay()
            try:
                resp = self.session.get(MCA_BASE_URL, params=params, timeout=30)
                resp.raise_for_status()
                result = resp.json()
                if result.get("status") != 0:
                    raise ValueError(f"MCA API error: {result.get('message', 'unknown')}")
                return result["data"]
            except (requests.RequestException, ValueError, json.JSONDecodeError) as e:
                if attempt == MAX_RETRIES:
                    raise RuntimeError(f"MCA query failed after {MAX_RETRIES} retries: {e}") from e
                wait = 2 ** attempt
                print(f"  [重试 {attempt}/{MAX_RETRIES}] {e}，{wait}s 后重试...")
                time.sleep(wait)


# 行政后缀映射：level -> 需要去除的后缀列表（按优先级排序，长的在前）
SUFFIX_MAP = {
    1: ["维吾尔自治区", "壮族自治区", "回族自治区", "自治区", "特别行政区", "省", "市"],
    2: ["自治州", "地区", "盟", "市"],
    3: ["自治旗", "自治县", "林区", "特区", "旗", "县", "区", "市"],
    4: ["民族乡", "民族苏木", "苏木", "街道办事处", "街道", "办事处", "镇", "乡"],
}


def truncate_code(code_str: str, level: int) -> str:
    """统计局 12 位编码截取为民政编码。

    level 1-3: 取前 6 位
    level 4:   取前 9 位
    """
    if level <= 3:
        return code_str[:6]
    return code_str[:9]


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


def merge_tree(base_nodes, new_nodes):
    """将新 API 响应的节点合并到已有树中，通过匹配 code 补充 children。

    核心问题：Step 1 获取省→市时，市级节点的 children 为空。
    Step 2 遍历每个市获取市→县时，需要将县级 children 合并回
    Step 1 中对应的市级节点下，这样 flatten_tree 才能正确遍历。
    """
    # 建立 code -> node 的映射（递归遍历 base 树）
    code_map = {}

    def build_map(nodes):
        for node in nodes:
            code = str(node["code"])
            code_map[code] = node
            for child in node.get("children", []) or []:
                build_map([child])

    build_map(base_nodes)

    # 独立的 children 去重集合，按 code 索引，不污染节点 dict
    children_sets = {}

    # 递归合并：将 new_nodes 的 children 合并到 base 中对应的节点
    def merge_into(new_list):
        for new_node in new_list:
            code = str(new_node["code"])
            if code in code_map and new_node.get("children"):
                base_node = code_map[code]
                if not base_node.get("children"):
                    base_node["children"] = []
                if code not in children_sets:
                    children_sets[code] = {
                        str(c["code"]) for c in base_node["children"]
                    }
                existing = children_sets[code]
                for child in new_node["children"]:
                    child_code = str(child["code"])
                    if child_code not in existing:
                        base_node["children"].append(child)
                        existing.add(child_code)
            # 递归处理更深层
            for child in new_node.get("children", []) or []:
                merge_into([child])

    merge_into(new_nodes)
    return base_nodes


def flatten_tree(nodes, parent_id=0, parent_path="", parent_full_name=""):
    """将 MCA API 返回的树形结构展平为 area 记录列表。

    直接读取 node["level"] 判断层级，跳过 level=0 的根节点。
    """
    records = []
    seen_ids = set()

    for node in nodes:
        raw_code = str(node["code"])
        name = node["name"]
        mca_level = node["level"]

        # 根节点，跳过但递归处理 children
        if mca_level == 0:
            children = node.get("children", [])
            if children:
                records.extend(flatten_tree(children, parent_id=0, parent_path="", parent_full_name=""))
            continue

        # 截取编码
        truncated = truncate_code(raw_code, mca_level)
        record_id = int(truncated)

        # 去重：同一层级可能出现重复节点
        if record_id in seen_ids:
            continue
        seen_ids.add(record_id)

        # 构建路径和全名
        path = f"{parent_path}/{truncated}" if parent_path else truncated
        full_name = f"{parent_full_name}/{name}" if parent_full_name else name

        record = {
            "id": record_id,
            "pid": parent_id,
            "level": mca_level,
            "name": name,
            "short_name": derive_short_name(name, mca_level),
            "full_name": full_name,
            "adcode": truncated,
            "path": path,
        }
        records.append(record)

        # 递归处理 children（最多到 level 4）
        children = node.get("children", [])
        if children and mca_level < 4:
            child_records = flatten_tree(
                children,
                parent_id=record_id,
                parent_path=path,
                parent_full_name=full_name,
            )
            records.extend(child_records)

    return records


class DbWriter:
    """数据库连接和批量写入。"""

    VERSION = "mca-official"
    BATCH_SIZE = 1000
    INSERT_SQL = """INSERT INTO `area` (`id`, `pid`, `level`, `name`, `short_name`, `full_name`, `pin_yin`, `adcode`, `zip_code`, `lng`, `lat`, `path`, `version`, `state`, `create_time`, `update_time`)
VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW())"""

    def __init__(self, host, port, user, password, db, dry_run=False):
        self.dry_run = dry_run
        if not dry_run:
            self.conn = pymysql.connect(
                host=host, port=port, user=user, password=password,
                database=db, charset="utf8mb4", autocommit=False
            )
            self.cursor = self.conn.cursor()
        else:
            self.conn = None
            self.cursor = None

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        if exc_type and self.conn:
            self.conn.rollback()
        self.close()
        return False

    def truncate_area(self):
        if self.dry_run:
            print("[dry-run] TRUNCATE TABLE `area`;")
            return
        print("清空 area 表...")
        self.cursor.execute("TRUNCATE TABLE `area`")
        self.conn.commit()

    def batch_insert(self, records):
        """批量插入 records，每 BATCH_SIZE 条 commit 一次。"""
        total = len(records)
        inserted = 0

        for i in range(0, total, self.BATCH_SIZE):
            batch = records[i:i + self.BATCH_SIZE]
            values = []
            for rec in batch:
                values.append((
                    rec["id"], rec["pid"], rec["level"], rec["name"],
                    rec["short_name"], rec["full_name"], "", rec["adcode"],
                    "", 0, 0, rec["path"], self.VERSION, 1,
                ))

            if self.dry_run:
                print(f"[dry-run] INSERT {len(values)} records (batch starting at id={batch[0]['id']})")
            else:
                self.cursor.executemany(self.INSERT_SQL, values)
                self.conn.commit()

            inserted += len(values)
            print(f"  已写入 {inserted}/{total} 条")

        print(f"写入完成: {inserted} 条")

    def close(self):
        if self.cursor:
            self.cursor.close()
        if self.conn:
            self.conn.close()
