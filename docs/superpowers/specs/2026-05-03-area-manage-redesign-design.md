# 行政区维护页面优化设计

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构行政区维护页面为左侧树+右侧列表的主从布局，提升数据管理员批量维护场景下的易用性

**Architecture:** 左侧异步树（省>市>县三级可展开）驱动右侧子级列表，列表支持查看/编辑/删除/启用禁用，树节点支持新增子级

**Tech Stack:** Vue 3 + Element Plus, Spring Boot + MyBatis-Plus

---

### 整体布局

```
┌────────────────────────────────────────────────────┐
│  [搜索: 名称] [层级筛选 ▾]     [+ 新增]  [导入]     │
├──────────────────┬─────────────────────────────────┤
│  左侧：行政区树    │  右侧：行政区列表               │
│  (30% 宽度)      │  (70% 宽度)                     │
│                  │                                 │
│  ── 全部行政区   │  ID  │ 名称 │ 简称 │ 行政编码   │
│  ├ 广东省         │  1   │ 深圳 │ 南山 │ 440300   │
│  ├ 江苏省         │      │      │      │ 查看 编辑 │
│  │  ├ 深圳市      │                               │
│  │  │  ├ 南山区   │  [el-switch 启用/禁用]         │
│  │  │  └ 福田区   │                               │
│  └ 浙江省         │  (+) 节点hover显示"新增子级"    │
│                  │                                 │
└──────────────────┴─────────────────────────────────┘
```

### Task 1: 后端新增 children API

**Files:**
- Modify: `food-manage-web/src/main/java/com/jihao/food/area/controller/AdminAreaController.java`
- Modify: `food-manage-web/src/main/java/com/jihao/food/area/service/AreaQueryService.java`
- Modify: `food-manage-web/src/main/java/com/jihao/food/area/mapper/AreaMapper.java`

- [ ] **Step 1: AreaMapper 添加 selectByPidAndState 方法**

```java
@Select("SELECT * FROM area WHERE pid = #{pid} AND state = #{state} ORDER BY id")
List<Area> selectByPidAndState(@Param("pid") Long pid, @Param("state") Integer state);
```

- [ ] **Step 2: AreaQueryService 添加 getChildren 方法**

```java
public List<Area> getChildren(Long parentId) {
    if (parentId == null || parentId == 0) {
        // 查询所有省级
        return areaMapper.selectByLevel(1, 1);
    }
    return areaMapper.selectByPidAndState(parentId, 1);
}
```

- [ ] **Step 3: AdminAreaController 添加 /children 接口**

```java
@GetMapping("/children")
public Result<List<Area>> children(
        @RequestParam(required = false) Long parentId) {
    return Result.success(areaQueryService.getChildren(parentId));
}
```

### Task 2: 前端新增 getAreaChildren API

**Files:**
- Modify: `food-web-fe/src/api/area/manage.js`

- [ ] **Step: 添加 getAreaChildren 方法**

```javascript
// 查询子级行政区列表
export function getAreaChildren(parentId) {
  return request({
    url: '/api/admin/area/children',
    method: 'get',
    params: { parentId: parentId || undefined }
  })
}
```

### Task 3: 重写行政区维护页面

**Files:**
- Rewrite: `food-web-fe/src/views/area/manage/index.vue`

- [ ] **Step 1: 模板部分 - 左右分栏布局**

```vue
<template>
  <div class="app-container">
    <!-- 顶部操作栏 -->
    <div style="display: flex; justify-content: flex-end; gap: 8px; margin-bottom: 12px;">
      <el-button type="primary" icon="Plus" @click="handleAdd">新增</el-button>
      <el-button type="warning" icon="Upload" @click="handleImport">导入</el-button>
    </div>

    <el-row :gutter="16">
      <!-- 左侧：行政区树 -->
      <el-col :span="7">
        <el-card shadow="never" style="height: calc(100vh - 180px); overflow-y: auto;">
          <template #header>
            <span style="font-weight: bold;">行政区</span>
          </template>
          <el-tree
            ref="treeRef"
            :data="treeData"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            :load="loadNode"
            lazy
            highlight-current
            @node-click="handleNodeClick"
            @node-expand="handleNodeExpand"
          >
            <template #default="{ node, data }">
              <span class="custom-tree-node">
                <span>
                  <el-tag v-if="data.level === 1" size="small" type="success">省</el-tag>
                  <el-tag v-else-if="data.level === 2" size="small" type="">市</el-tag>
                  <el-tag v-else-if="data.level === 3" size="small" type="warning">县</el-tag>
                  {{ node.label }}
                </span>
                <el-button
                  link
                  type="primary"
                  icon="Plus"
                  size="small"
                  @click.stop="handleAddChild(data)"
                  v-if="data.level < 3"
                  class="add-child-btn"
                />
              </span>
            </template>
          </el-tree>
        </el-card>
      </el-col>

      <!-- 右侧：行政区列表 -->
      <el-col :span="17">
        <el-card shadow="never" style="height: calc(100vh - 180px); overflow-y: auto;">
          <template #header>
            <span style="font-weight: bold;">{{ currentParentName || '请选择行政区' }}</span>
          </template>
          <el-table v-loading="tableLoading" :data="areaList" size="small">
            <el-table-column label="ID" prop="id" width="80" />
            <el-table-column label="名称" prop="name" :show-overflow-tooltip="true" />
            <el-table-column label="简称" prop="shortName" width="120" />
            <el-table-column label="行政编码" prop="adcode" width="120" />
            <el-table-column label="状态" width="80" align="center">
              <template #default="scope">
                <el-switch
                  v-model="scope.row.state"
                  :active-value="1"
                  :inactive-value="0"
                  @change="handleToggleStatus(scope.row)"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" align="center">
              <template #default="scope">
                <el-button link type="primary" size="small" @click="handleView(scope.row)">查看</el-button>
                <el-button link type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
                <el-button link type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 查看详情对话框 -->
    <el-dialog title="行政区详情" v-model="openView" width="600px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="ID">{{ viewData.id }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ viewData.name }}</el-descriptions-item>
        <el-descriptions-item label="简称">{{ viewData.shortName }}</el-descriptions-item>
        <el-descriptions-item label="全称">{{ viewData.fullName }}</el-descriptions-item>
        <el-descriptions-item label="行政编码">{{ viewData.adcode }}</el-descriptions-item>
        <el-descriptions-item label="层级">{{ levelLabel(viewData.level) }}</el-descriptions-item>
        <el-descriptions-item label="经度">{{ viewData.lng }}</el-descriptions-item>
        <el-descriptions-item label="纬度">{{ viewData.lat }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="viewData.state === 1 ? 'success' : 'danger'">
            {{ viewData.state === 1 ? '启用' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="路径">{{ viewData.path }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="openView = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑对话框 -->
    <el-dialog :title="dialogTitle" v-model="openForm" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="上级行政区" prop="pid">
          <el-tree-select
            v-model="form.pid"
            :data="areaTreeOptions"
            :props="{ value: 'id', label: 'name', children: 'children' }"
            value-key="id"
            placeholder="选择上级行政区"
            check-strictly
            clearable
            filterable
            style="width: 100%"
          />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="简称" prop="shortName">
              <el-input v-model="form.shortName" placeholder="请输入简称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="全称" prop="fullName">
              <el-input v-model="form.fullName" placeholder="请输入全称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="行政编码" prop="adcode">
              <el-input v-model="form.adcode" placeholder="请输入行政编码" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="经度" prop="lng">
              <el-input-number v-model="form.lng" :precision="6" :step="0.000001" placeholder="经度" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纬度" prop="lat">
              <el-input-number v-model="form.lat" :precision="6" :step="0.000001" placeholder="纬度" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="openForm = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 导入对话框 -->
    <el-dialog title="导入行政区" v-model="openImport" width="600px" append-to-body>
      <el-upload
        :before-upload="beforeUpload"
        :limit="1"
        accept=".json"
        drag
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">将 JSON 文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">仅支持 .json 格式文件</div>
        </template>
      </el-upload>
      <div v-if="previewData.length > 0" style="margin-top: 16px;">
        <el-alert title="导入预览" type="info" :closable="false" style="margin-bottom: 8px;">
          共 {{ previewData.length }} 条数据
        </el-alert>
        <el-table :data="previewData" max-height="300" size="small">
          <el-table-column label="名称" prop="name" width="150" />
          <el-table-column label="简称" prop="shortName" width="100" />
          <el-table-column label="层级" width="80">
            <template #default="scope">{{ levelLabel(scope.row.level) }}</template>
          </el-table-column>
          <el-table-column label="行政编码" prop="adcode" width="100" />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="openImport = false">取消</el-button>
        <el-button type="primary" :disabled="previewData.length === 0" @click="confirmImport">确认导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>
```

- [ ] **Step 2: Script 部分**

```javascript
<script setup name="AreaManage">
import { getAreaTree, getAreaDetail, getAreaChildren, createArea, updateArea, deleteArea, toggleStatus, importPreview, doImport } from '@/api/area/manage'
import { getToken } from '@/utils/auth'

const { proxy } = getCurrentInstance()

// 左侧树
const treeData = ref([])
const currentNode = ref(null)
const currentParentName = ref('')

// 右侧列表
const areaList = ref([])
const tableLoading = ref(false)

// 对话框
const openView = ref(false)
const openForm = ref(false)
const openImport = ref(false)
const dialogTitle = ref('新增行政区')
const viewData = ref({})
const form = ref({})
const areaTreeOptions = ref([])
const previewData = ref([])
const importData = ref([])

// 表单校验
const rules = {
  name: [{ required: true, message: '名称不能为空', trigger: 'blur' }],
  pid: [{ required: true, message: '请选择上级行政区', trigger: 'change' }]
}

function levelLabel(level) {
  const map = { 1: '省', 2: '市', 3: '县', 4: '街道' }
  return map[level] || level
}

// 左侧树：懒加载节点
function loadNode(node, resolve) {
  if (node.level === 0) {
    // 根节点：加载省级
    getAreaChildren(null).then(res => {
      resolve((res.data || res || []).map(item => ({ ...item, level: 1 })))
    }).catch(() => resolve([]))
  } else {
    const parentId = node.data.id
    const level = node.data.level + 1
    if (level > 3) {
      // 最多到县级（level 3）
      resolve([])
      return
    }
    getAreaChildren(parentId).then(res => {
      resolve((res.data || res || []).map(item => ({ ...item, level })))
    }).catch(() => resolve([]))
  }
}

// 左侧树：点击节点
function handleNodeClick(data) {
  currentNode.value = data
  currentParentName.value = data.name + ' - 下级行政区'
  loadTableData(data.id)
}

// 加载右侧表格数据
function loadTableData(parentId) {
  tableLoading.value = true
  getAreaChildren(parentId).then(res => {
    areaList.value = res.data || res || []
    tableLoading.value = false
  }).catch(() => {
    areaList.value = []
    tableLoading.value = false
  })
}

// 左侧树：节点展开（预加载子级用于判断是否还有下级）
function handleNodeExpand(data) {}

// 查看
function handleView(row) {
  getAreaDetail(row.id).then(res => {
    viewData.value = res.data || res || row
    openView.value = true
  })
}

// 编辑
function handleEdit(row) {
  form.value = { ...row }
  dialogTitle.value = '编辑行政区'
  openForm.value = true
}

// 新增子级（从树节点）
function handleAddChild(data) {
  form.value = { pid: data.id, name: '', shortName: '', fullName: '', adcode: '', lng: undefined, lat: undefined }
  dialogTitle.value = '新增子级行政区（上级：' + data.name + '）'
  openForm.value = true
}

// 新增（从顶部按钮）
function handleAdd() {
  form.value = { pid: undefined, name: '', shortName: '', fullName: '', adcode: '', lng: undefined, lat: undefined }
  loadAreaTreeOptions()
  dialogTitle.value = '新增行政区'
  openForm.value = true
}

// 加载树选择选项
function loadAreaTreeOptions() {
  getAreaTree({ depth: 3 }).then(res => {
    areaTreeOptions.value = res.data || res || []
  })
}

// 删除
function handleDelete(row) {
  proxy.$modal.confirm('确认删除行政区"' + row.name + '"吗？').then(() => {
    deleteArea(row.id).then(() => {
      proxy.$modal.msgSuccess('删除成功')
      // 刷新当前列表
      if (currentNode.value) {
        loadTableData(currentNode.value.id)
      }
    })
  }).catch(() => {})
}

// 启用/禁用
function handleToggleStatus(row) {
  toggleStatus(row.id, row.state).then(() => {
    proxy.$modal.msgSuccess(row.state === 1 ? '已启用' : '已禁用')
  }).catch(() => {
    // 回滚
    row.state = row.state === 1 ? 0 : 1
  })
}

// 提交表单
function submitForm() {
  proxy.$refs.formRef.validate(valid => {
    if (valid) {
      if (form.value.id) {
        updateArea(form.value).then(() => {
          proxy.$modal.msgSuccess('修改成功')
          openForm.value = false
          if (currentNode.value) {
            loadTableData(currentNode.value.id)
          }
        })
      } else {
        createArea(form.value).then(() => {
          proxy.$modal.msgSuccess('新增成功')
          openForm.value = false
          if (currentNode.value) {
            loadTableData(currentNode.value.id)
          }
        })
      }
    }
  })
}

// 导入
function handleImport() {
  previewData.value = []
  importData.value = []
  openImport.value = true
}

function beforeUpload(file) {
  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const data = JSON.parse(e.target.result)
      importData.value = Array.isArray(data) ? data : [data]
      previewData.value = importData.value
    } catch {
      proxy.$modal.msgError('JSON 文件格式错误')
    }
  }
  reader.readAsText(file)
  return false
}

function confirmImport() {
  doImport(importData.value).then(res => {
    proxy.$modal.msgSuccess('成功导入 ' + (res.data || res || 0) + ' 条数据')
    openImport.value = false
    if (currentNode.value) {
      loadTableData(currentNode.value.id)
    }
  }).catch(() => {})
}

// 初始化：默认选中"全部行政区"，加载省级列表
getAreaChildren(null).then(res => {
  areaList.value = res.data || res || []
  currentParentName.value = '全部行政区 - 省级'
})
</script>
```

- [ ] **Step 3: 样式部分**

```vue
<style scoped>
.custom-tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 8px;
}
.add-child-btn {
  opacity: 0;
  transition: opacity 0.2s;
}
.custom-tree-node:hover .add-child-btn {
  opacity: 1;
}
</style>
```
