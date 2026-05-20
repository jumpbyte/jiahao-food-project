#!/usr/bin/env python3
"""
从民政部行政区划信息服务平台 (MCA) 抓取全国四级行政区划数据并导入 area 表。

数据来源: https://dmfw.mca.gov.cn/9095/xzqh/getList
目标表: area (jiahao_food_db)
"""

import json
import random
import time

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
