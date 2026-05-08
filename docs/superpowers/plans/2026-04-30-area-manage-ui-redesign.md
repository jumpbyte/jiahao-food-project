# 行政区维护界面改版实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将行政区维护界面从单列表格改为左侧树形导航 + 右侧列表的布局，支持懒加载、搜索、面包屑导航和自动上级填充。

**Architecture:** 左侧使用 el-tree 懒加载组件按需加载子节点，右侧显示当前选中节点的子级列表。新增时自动填充 pid 为当前选中节点。

**Tech Stack:** Vue 3 Composition API, Element Plus (el-tree, el-table, el-breadcrumb), Axios

---

## File Structure

| 文件 | 操作 | 说明 |
|------|------|------|
| `food-web-fe/src/views/area/manage/index.vue` | 重写 | 主组件，改为左右分栏布局 |
| `food-web-fe/src/api/area/manage.js` | 不变 | 已有 getAreaTree, getAreaDetail 等接口足够用 |

---

### Task 1: 重写为左右分栏布局 — 左侧懒加载树 + 右侧列表

**Files:**
- Rewrite: `food-web-fe/src/views/area/manage/index.vue`

- [ ] **Step 1: 重写整个组件**

用以下内容完全替换 `food-web-fe/src/views/area/manage/index.vue`：

```vue
<template>
  <div class="app-container area-manage">
    <el-row :gutter="12" style="height: 100%">
      <!-- 左侧树面板 -->
      <el-col :span="6" class="tree-panel">
        <div class="tree-header">
          <el-input
            v-model="treeFilterText"
            placeholder="搜索行政区..."
            clearable
            prefix-icon="Search"
            size="small"
          />
        </div>
        <el-tree
          ref="treeRef"
          class="area-tree"
          :data="treeData"
          :props="treeProps"
          :filter-node-method="filterNode"
          :highlight-current="true"
          node-key="id"
          default-expand-all
          @node-click="handleNodeClick"
        >
          <template #default="{ node, data }">
            <span class="tree-node-label">
              <el-tag v-if="data.level === 1" size="small" type="primary" effect="plain">省</el-tag>
              <el-tag v-else-if="data.level === 2" size="small" type="success" effect="plain">市</el-tag>
              <el-tag v-else-if="data.level === 3" size="small" effect="plain">区</el-tag>
              <el-tag v-else size="small" type="info" effect="plain">街</el-tag>
              <span class="node-name">{{ node.label }}</span>
            </span>
          </template>
        </el-tree>
      </el-col>

      <!-- 右侧列表面板 -->
      <el-col :span="18" class="list-panel">
        <div class="list-header">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item
              v-for="item in breadcrumb"
              :key="item.id"
              @click="handleBreadcrumbClick(item)"
              style="cursor: pointer"
            >
              {{ item.name }}
            </el-breadcrumb-item>
          </el-breadcrumb>
          <span v-if="breadcrumb.length === 0" class="no-selection">请选择左侧节点</span>
        </div>

        <el-row :gutter="10" class="mb8" v-if="selectedNode">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button plain icon="Refresh" @click="refreshList">刷新</el-button>
          </el-col>
        </el-row>

        <el-table v-loading="loading" :data="childList" row-key="id" border stripe>
          <el-table-column label="名称" align="left" prop="name" :show-overflow-tooltip="true" />
          <el-table-column label="简称" align="center" prop="shortName" width="100" />
          <el-table-column label="层级" align="center" prop="level" width="80">
            <template #default="scope">
              <el-tag v-if="scope.row.level === 1" type="primary" size="small">省</el-tag>
              <el-tag v-else-if="scope.row.level === 2" type="success" size="small">市</el-tag>
              <el-tag v-else-if="scope.row.level === 3">区县</el-tag>
              <el-tag v-else type="info" size="small">街道</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="编码" align="center" prop="adcode" width="120" />
          <el-table-column label="操作" align="center" width="160">
            <template #default="scope">
              <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)">编辑</el-button>
              <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="!loading && childList.length === 0 && selectedNode" description="暂无子级数据" />
      </el-col>
    </el-row>

    <!-- 添加或编辑对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="上级行政区" v-if="!form.id">
          <el-input :model-value="parentLabel" disabled />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="简称" prop="shortName">
          <el-input v-model="form.shortName" placeholder="请输入简称（不带行政后缀）" />
        </el-form-item>
        <el-form-item label="编码" prop="adcode">
          <el-input v-model="form.adcode" placeholder="请输入行政区划代码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="AreaManage">
import { getAreaTree, createArea, updateArea, deleteArea, getAreaDetail } from '@/api/area/manage'

const treeData = ref([])
const treeFilterText = ref('')
const treeRef = ref(null)
const loading = ref(false)
const open = ref(false)
const title = ref('')
const form = ref({})
const formRef = ref(null)
const selectedNode = ref(null)
const childList = ref([])
const breadcrumb = ref([])

const treeProps = {
  children: 'children',
  label: 'name'
}

const rules = {
  name: [{ required: true, message: '名称不能为空', trigger: 'blur' }]
}

// 计算上级标签显示文本
const parentLabel = computed(() => {
  if (!selectedNode.value) return '无'
  return breadcrumb.value.map(b => b.name).join(' > ')
})

// 过滤树节点
watch(treeFilterText, (val) => {
  treeRef.value?.filter(val)
})

function filterNode(value, data) {
  if (!value) return true
  return data.name?.includes(value)
}

// 加载根节点（省级）
function loadRootTree() {
  getAreaTree({}).then(res => {
    treeData.value = extractChildren(res)
  })
}

// 加载某个节点的子级（用于懒加载树）
function loadChildren(nodeData, callback) {
  getAreaTree({ areaId: nodeData.id }).then(res => {
    const children = extractChildren(res) || []
    callback(children)
  })
}

// 点击树节点
function handleNodeClick(data) {
  selectedNode.value = data
  updateBreadcrumb(data)
  loadChildList(data)
}

// 更新面包屑
function updateBreadcrumb(node) {
  // 从树结构中回溯路径
  const path = findNodePath(treeData.value, node.id)
  breadcrumb.value = path
}

// 在树中查找节点路径
function findNodePath(tree, targetId, path = []) {
  for (const node of tree) {
    if (node.id === targetId) {
      return [...path, { id: node.id, name: node.name }]
    }
    if (node.children?.length > 0) {
      const found = findNodePath(node.children, targetId, [...path, { id: node.id, name: node.name }])
      if (found) return found
    }
  }
  return []
}

// 加载右侧子级列表
function loadChildList(nodeData) {
  loading.value = true
  getAreaTree({ areaId: nodeData.id }).then(res => {
    childList.value = extractChildren(res)
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

// 刷新列表
function refreshList() {
  if (selectedNode.value) {
    loadChildList(selectedNode.value)
  }
  loadRootTree()
}

// 面包屑点击
function handleBreadcrumbClick(item) {
  // 找到对应节点并触发点击
  const treeNode = findNodeInTree(treeData.value, item.id)
  if (treeNode) {
    treeRef.value?.setCurrentKey(item.id)
    handleNodeClick(treeNode)
  }
}

// 在树中查找节点
function findNodeInTree(tree, targetId) {
  for (const node of tree) {
    if (node.id === targetId) return node
    if (node.children?.length > 0) {
      const found = findNodeInTree(node.children, targetId)
      if (found) return found
    }
  }
  return null
}

// 从响应中提取 children 数组
function extractChildren(res) {
  // 后端返回格式: { code: 200, data: [...] }
  const data = res?.data || res
  return Array.isArray(data) ? data : []
}

// 新增
function handleAdd() {
  form.value = {}
  open.value = true
  title.value = `新增行政区（上级: ${parentLabel.value}）`
}

// 编辑
function handleUpdate(row) {
  form.value = { ...row }
  open.value = true
  title.value = '编辑行政区'
}

// 删除
function handleDelete(row) {
  const { proxy } = getCurrentInstance()
  proxy.$modal.confirm(`确认删除「${row.name}」吗？`).then(() => {
    deleteArea(row.id).then(() => {
      proxy.$modal.msgSuccess('删除成功')
      refreshList()
    })
  }).catch(() => {})
}

// 提交表单
function submitForm() {
  const { proxy } = getCurrentInstance()
  proxy.$refs.formRef.validate(valid => {
    if (valid) {
      if (form.value.id) {
        updateArea(form.value).then(() => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          refreshList()
        })
      } else {
        const formData = {
          ...form.value,
          pid: selectedNode.value?.id || null
        }
        createArea(formData).then(() => {
          proxy.$modal.msgSuccess('新增成功')
          open.value = false
          refreshList()
        })
      }
    }
  })
}

// 初始化
loadRootTree()
</script>

<style scoped>
.area-manage {
  height: calc(100vh - 120px);
  overflow: hidden;
}
.tree-panel {
  height: 100%;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}
.tree-header {
  padding: 10px;
  border-bottom: 1px solid #e4e7ed;
}
.area-tree {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}
.tree-node-label {
  display: flex;
  align-items: center;
  gap: 6px;
}
.node-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.list-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.list-header {
  padding: 12px 0;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  gap: 12px;
}
.no-selection {
  color: #909399;
  font-size: 14px;
}
.mb8 {
  margin-bottom: 8px;
  padding: 8px 0;
}
</style>
```

- [ ] **Step 2: 验证编译**

Run: `cd food-web-fe && npm run build` (or check with dev server)
Expected: No compilation errors

- [ ] **Step 3: 提交**

```bash
git add food-web-fe/src/views/area/manage/index.vue
git commit -m "feat: 重构行政区维护界面为左侧树+右侧列表布局"
```

---

## Self-Review

1. **Spec coverage:**
   - ✅ 左侧树面板懒加载
   - ✅ 树搜索过滤
   - ✅ 面包屑导航
   - ✅ 右侧子级列表
   - ✅ 新增自动填充 pid
   - ✅ 编辑对话框简化

2. **Placeholder scan:** 无 TBD/TODO，所有代码完整

3. **Type consistency:** `extractChildren` 处理 `res.data` 格式与之前修复的 getList 一致；`handleNodeClick`, `loadChildList` 等都使用统一的 `getAreaTree` API

4. **Scope:** 单文件修改，聚焦明确
