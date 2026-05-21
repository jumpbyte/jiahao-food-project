# 断点续传功能设计

**日期**: 2026-05-21
**状态**: 待审批

## 目标

为 `scripts/import_area_from_mca.py` 增加断点续传能力，中断后可从上次位置继续，无需清空重来。

## 方案

**进度文件** — `.import_progress.json` 记录已完成的市级/县级 code。

**格式**:
```json
{
  "completed_cities": ["110100000000", "110200000000", ...],
  "completed_counties": ["110101000000", "110102000000", ...]
}
```

**执行逻辑**:
1. 脚本启动时读取 `.import_progress.json`（不存在则新建空记录）
2. Step 2 遍历市级时，跳过 `completed_cities` 中已有的 code
3. Step 3 遍历县级时，跳过 `completed_counties` 中已有的 code
4. 每成功获取一个市/县的数据，立即追加 code 到文件并写入磁盘
5. 全部完成后删除 `.import_progress.json`

**新增代码**:
- `ProgressTracker` 类 — 管理进度文件的读取、写入、删除
- 修改 `main()` 中 Step 2/Step 3 的循环逻辑，接入 `ProgressTracker`

**命令行**:
- `--resume` — 启用断点续传（默认开启）
- `--fresh` — 忽略已有进度，从头开始

## 受影响的文件

| 操作 | 文件 | 说明 |
|------|------|------|
| 修改 | `scripts/import_area_from_mca.py` | 添加 ProgressTracker，修改 main 循环 |
| 新增（运行时） | `.import_progress.json` | 进度记录文件（不提交到 git） |
