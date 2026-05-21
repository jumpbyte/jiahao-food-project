# 断点续传功能 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 MCA 区划导入脚本增加断点续传能力，中断后从上次位置继续

**Architecture:** 添加 ProgressTracker 类管理 `.import_progress.json` 的读写，修改 main 中 Step 2/Step 3 循环逻辑跳过已完成的 code

**Tech Stack:** Python 3 + json 标准库

---

### File Structure

| 操作 | 文件 | 说明 |
|------|------|------|
| 修改 | `scripts/import_area_from_mca.py` | 添加 ProgressTracker 类，修改 main 循环 |
| 创建 | `scripts/.gitignore` | 排除 `.import_progress.json` |

---

### Task 1: 添加 ProgressTracker 类

**Files:**
- Modify: `scripts/import_area_from_mca.py`

- [ ] **Step 1: 在 DbWriter 类之前添加 ProgressTracker 类**

```python
PROGRESS_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".import_progress.json")


class ProgressTracker:
    """管理导入进度的读取、写入和删除，支持断点续传。"""

    def __init__(self):
        self.completed_cities = set()
        self.completed_counties = set()
        self._load()

    def _load(self):
        if os.path.exists(PROGRESS_FILE):
            try:
                with open(PROGRESS_FILE, "r") as f:
                    data = json.load(f)
                self.completed_cities = set(data.get("completed_cities", []))
                self.completed_counties = set(data.get("completed_counties", []))
                print(f"加载进度: 已完成 {len(self.completed_cities)} 个市级, {len(self.completed_counties)} 个县级")
            except (json.JSONDecodeError, IOError):
                print("进度文件损坏，从头开始")

    def _save(self):
        data = {
            "completed_cities": sorted(self.completed_cities),
            "completed_counties": sorted(self.completed_counties),
        }
        with open(PROGRESS_FILE, "w") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)

    def mark_city_done(self, city_code: str):
        self.completed_cities.add(city_code)
        self._save()

    def mark_county_done(self, county_code: str):
        self.completed_counties.add(county_code)
        self._save()

    def cleanup(self):
        if os.path.exists(PROGRESS_FILE):
            os.remove(PROGRESS_FILE)
            print("导入完成，清除进度文件")
```

- [ ] **Step 2: 添加 `import os` 到导入部分**

- [ ] **Step 3: 提交**

```bash
git add scripts/import_area_from_mca.py
git commit -m "feat: add ProgressTracker for resume capability"
```

---

### Task 2: 修改 main 函数接入断点续传

**Files:**
- Modify: `scripts/import_area_from_mca.py`

- [ ] **Step 1: 修改 main 函数，添加 --fresh 参数，接入 ProgressTracker**

将 `main()` 函数修改为以下内容：

```python
def main():
    parser = argparse.ArgumentParser(description="从 MCA API 导入全国行政区划数据到 area 表")
    parser.add_argument("--host", default="localhost", help="数据库地址 (默认: localhost)")
    parser.add_argument("--port", type=int, default=3306, help="数据库端口 (默认: 3306)")
    parser.add_argument("--user", default="root", help="数据库用户 (默认: root)")
    parser.add_argument("--password", default="", help="数据库密码")
    parser.add_argument("--db", default="jiahao_food_db", help="数据库名 (默认: jiahao_food_db)")
    parser.add_argument("--dry-run", action="store_true", help="只打印不写入数据库")
    parser.add_argument("--fresh", action="store_true", help="忽略已有进度，从头开始")
    args = parser.parse_args()

    client = McaClient()

    # 进度追踪
    if args.fresh:
        progress = ProgressTracker()
        progress.cleanup()
        progress = ProgressTracker()
        print("从头开始导入...")
    else:
        progress = ProgressTracker()

    # Step 1: 获取省+市
    print("[1/3] 获取省级和市级数据...")
    base_tree = client.query(code=None, max_level=2)
    print(f"  获取到根节点下 {len(base_tree)} 个一级节点")

    # Step 2: 遍历每个市，获取市+县
    city_codes = extract_codes_by_level(base_tree, target_level=2)
    remaining_cities = [c for c in city_codes if c not in progress.completed_cities]
    skipped = len(city_codes) - len(remaining_cities)
    if skipped > 0:
        print(f"  跳过 {skipped} 个已完成的市级")

    print(f"[2/3] 遍历 {len(remaining_cities)} 个市级，获取区县级数据...")
    for idx, city_code in enumerate(remaining_cities, 1):
        if idx % 10 == 0 or idx == len(remaining_cities):
            print(f"  进度: {idx}/{len(remaining_cities)}")
        county_data = client.query(code=city_code, max_level=2)
        merge_tree(base_tree, county_data)
        progress.mark_city_done(city_code)

    # Step 3: 遍历每个县，获取县+乡镇
    county_codes = extract_codes_by_level(base_tree, target_level=3)
    remaining_counties = [c for c in county_codes if c not in progress.completed_counties]
    skipped = len(county_codes) - len(remaining_counties)
    if skipped > 0:
        print(f"  跳过 {skipped} 个已完成的县级")

    print(f"[3/3] 遍历 {len(remaining_counties)} 个县级，获取乡镇级数据...")
    for idx, county_code in enumerate(remaining_counties, 1):
        if idx % 100 == 0 or idx == len(remaining_counties):
            print(f"  进度: {idx}/{len(remaining_counties)}")
        township_data = client.query(code=county_code, max_level=1)
        merge_tree(base_tree, township_data)
        progress.mark_county_done(county_code)

    # 展平
    print("\n展平树形结构...")
    records = flatten_tree(base_tree)

    print_summary(records)

    # 写入数据库
    with DbWriter(args.host, args.port, args.user, args.password, args.db, args.dry_run) as writer:
        if not args.dry_run:
            writer.truncate_area()
        writer.batch_insert(records)

    # 清除进度文件
    progress.cleanup()
    print("完成!")
```

- [ ] **Step 2: 提交**

```bash
git add scripts/import_area_from_mca.py
git commit -m "feat: add --fresh flag and integrate ProgressTracker into main workflow"
```

---

### Task 3: 添加 .gitignore 并验证

**Files:**
- Create: `scripts/.gitignore`

- [ ] **Step 1: 创建 scripts/.gitignore**

```
.import_progress.json
```

- [ ] **Step 2: 提交**

```bash
git add scripts/.gitignore
git commit -m "chore: ignore .import_progress.json"
```

- [ ] **Step 3: 验证**

```bash
# 验证 --fresh 模式
python3 scripts/import_area_from_mca.py --dry-run --fresh

# 验证断点续传（中断后继续）
python3 scripts/import_area_from_mca.py --dry-run
# 中断后重新运行，应显示"加载进度: 已完成 X 个市级, Y 个县级"
python3 scripts/import_area_from_mca.py --dry-run
```
