<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="状态" prop="state">
        <el-select v-model="queryParams.state" placeholder="状态" clearable>
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="regionList">
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="大区名称" align="center" prop="name" :show-overflow-tooltip="true" />
      <el-table-column label="状态" align="center" prop="state" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.state === 1 ? 'success' : 'danger'">
            {{ scope.row.state === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180" />
      <el-table-column label="操作" align="center" width="180">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)">编辑</el-button>
          <el-button link type="primary" icon="Link" @click="handleBindStreets(scope.row)">绑定街道</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.page" v-model:limit="queryParams.size" @pagination="getList" />

    <!-- 添加或编辑对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="大区名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入大区名称" />
        </el-form-item>
        <el-form-item label="绑定街道" prop="streetIds" v-if="!form.id">
          <el-cascader
            v-model="form.streetIds"
            :options="areaTreeOptions"
            :props="{ multiple: true, checkStrictly: true, emitPath: false, value: 'id', label: 'name' }"
            placeholder="选择要绑定的街道（可选）"
            clearable
            filterable
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 绑定街道对话框 -->
    <el-dialog title="绑定街道" v-model="openBind" width="800px" append-to-body>
      <div v-loading="streetLoading" style="display: flex; gap: 16px; align-items: flex-start; min-height: 300px;">
        <!-- 左侧：可选街道树 -->
        <div style="flex: 1; border: 1px solid #dcdfe6; border-radius: 4px; padding: 12px;">
          <div style="font-weight: bold; margin-bottom: 8px; font-size: 14px;">可选街道</div>
          <el-input v-model="leftSearch" placeholder="搜索..." clearable size="small" style="margin-bottom: 8px;" @input="onLeftSearch" />
          <el-tree
            ref="leftTreeRef"
            :data="areaTreeData"
            show-checkbox
            :check-strictly="false"
            :default-checked-keys="checkedKeys"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            :filter-node-method="filterNode"
            style="max-height: 350px; overflow-y: auto;"
          >
            <template #default="{ node, data }">
              <span :style="{ color: data.level === 4 ? '#333' : '#999', fontSize: data.level === 4 ? '13px' : '12px' }">
                {{ node.label }}
              </span>
            </template>
          </el-tree>
        </div>
        <!-- 中间：操作按钮 -->
        <div style="display: flex; flex-direction: column; align-items: center; gap: 12px; padding-top: 40px;">
          <el-button size="small" @click="moveToRight" :disabled="areaTreeData.length === 0">→</el-button>
          <el-button size="small" @click="moveToLeft" :disabled="areaTreeData.length === 0">←</el-button>
        </div>
        <!-- 右侧：已选街道树 -->
        <div style="flex: 1; border: 1px solid #dcdfe6; border-radius: 4px; padding: 12px;">
          <div style="font-weight: bold; margin-bottom: 8px; font-size: 14px;">已选街道</div>
          <el-tree
            :data="selectedTreeData"
            show-checkbox
            :default-checked-keys="selectedTreeKeys"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            :check-strictly="true"
            style="max-height: 350px; overflow-y: auto;"
          >
            <template #default="{ node, data }">
              <span style="font-size: 13px;">{{ node.label }}</span>
            </template>
          </el-tree>
          <el-empty v-if="selectedTreeKeys.length === 0" description="暂无已选街道" :image-size="60" />
        </div>
      </div>
      <template #footer>
        <el-button @click="openBind = false">取消</el-button>
        <el-button type="primary" @click="submitBindStreets">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Region">
import { listRegion, createRegion, updateRegion, bindStreets, getSelectableAreaTree } from '@/api/org/region'

const { proxy } = getCurrentInstance()

const regionList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const title = ref('')
const open = ref(false)
const openBind = ref(false)

const queryParams = ref({
  page: 1,
  size: 10,
  state: null
})

const form = ref({})
const areaTreeOptions = ref([])
const streetOptions = ref([])
const selectedStreets = ref([])
const currentRegionId = ref(null)
const streetLoading = ref(false)
const streetLoaded = ref(false)
const areaTreeData = ref([])
const checkedKeys = ref([])
const leftTreeRef = ref(null)
const selectedTreeData = ref([])
const selectedTreeKeys = ref([])
const leftSearch = ref('')

const rules = {
  name: [{ required: true, message: '大区名称不能为空', trigger: 'blur' }]
}

function getList() {
  loading.value = true
  listRegion(queryParams.value).then(res => {
    const page = res.data || res
    regionList.value = page.records || []
    total.value = page.total || 0
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.page = 1
  getList()
}

function resetQuery() {
  queryParams.value = { page: 1, size: 10, state: null }
  handleQuery()
}

function handleAdd() {
  form.value = {}
  areaTreeOptions.value = []
  getSelectableAreaTree(0).then(res => {
    areaTreeOptions.value = res.data || res || []
  })
  open.value = true
  title.value = '新增大区'
}

function handleUpdate(row) {
  form.value = { ...row }
  open.value = true
  title.value = '编辑大区'
}

function handleBindStreets(row) {
  currentRegionId.value = row.id
  areaTreeData.value = []
  checkedKeys.value = []
  selectedTreeData.value = []
  selectedTreeKeys.value = []
  streetLoading.value = true
  streetLoaded.value = false
  openBind.value = true
  getSelectableAreaTree(row.id).then(res => {
    const data = res.data || res
    if (!Array.isArray(data) || data.length === 0) {
      streetLoading.value = false
      streetLoaded.value = true
      return
    }
    // 克隆树数据，保留正常可勾选状态
    const cloneTree = (nodes) => {
      if (!Array.isArray(nodes)) return []
      return nodes.map(node => {
        const cloned = { ...node }
        // 不禁用父节点，保留 el-tree 的级联勾选能力
        if (node.children) {
          cloned.children = cloneTree(node.children)
        }
        return cloned
      })
    }
    areaTreeData.value = cloneTree(data)
    // 提取已选中的街道ID
    const extractChecked = (nodes) => {
      const ids = []
      if (!Array.isArray(nodes)) return ids
      nodes.forEach(node => {
        if (node.level === 4 && node.selected) {
          ids.push(node.id)
        }
        if (node.children) {
          ids.push(...extractChecked(node.children))
        }
      })
      return ids
    }
    checkedKeys.value = extractChecked(data)
    // 构建右侧已选树
    buildSelectedTree(data)
    streetLoading.value = false
    streetLoaded.value = true
  }).catch(err => {
    console.error('获取可选街道失败:', err)
    streetLoading.value = false
    streetLoaded.value = true
  })
}

function buildSelectedTree(data) {
  const build = (nodes) => {
    if (!Array.isArray(nodes)) return []
    return nodes
      .map(node => {
        if (node.level === 4 && node.selected) {
          return { id: node.id, name: node.name, level: node.level }
        }
        if (node.children) {
          const childResults = build(node.children)
          if (childResults.length > 0) {
            return { id: node.id, name: node.name, level: node.level, children: childResults }
          }
        }
        return null
      })
      .filter(Boolean)
  }
  selectedTreeData.value = build(data)
  // 收集右侧所有街道ID
  const collectIds = (nodes) => {
    const ids = []
    if (!Array.isArray(nodes)) return ids
    nodes.forEach(node => {
      if (node.level === 4) ids.push(node.id)
      if (node.children) ids.push(...collectIds(node.children))
    })
    return ids
  }
  selectedTreeKeys.value = collectIds(selectedTreeData.value)
}

function filterNode(value, data) {
  if (!value) return true
  return data.name.includes(value)
}

function onLeftSearch() {
  leftTreeRef.value.filter(leftSearch.value)
}

function moveToRight() {
  const checked = leftTreeRef.value.getCheckedKeys()
  const halfChecked = leftTreeRef.value.getHalfCheckedKeys() || []
  const allChecked = [...checked, ...halfChecked]
  // 重新构建树，将选中的节点标记为selected
  const markSelected = (nodes) => {
    if (!Array.isArray(nodes)) return []
    return nodes.map(node => {
      const cloned = { ...node }
      if (node.level === 4 && allChecked.includes(node.id)) {
        cloned.selected = true
      }
      if (node.children) {
        cloned.children = markSelected(node.children)
      }
      return cloned
    })
  }
  buildSelectedTree(markSelected(areaTreeData.value))
}

function moveToLeft() {
  const currentKeys = [...selectedTreeKeys.value]
  // 从左侧树中取消勾选
  leftTreeRef.value.setCheckedKeys(currentKeys.filter(k => !selectedTreeKeys.value.includes(k)))
  // 重建右侧树
  const removeFromTree = (nodes, toRemove) => {
    if (!Array.isArray(nodes)) return []
    return nodes
      .map(node => {
        if (node.level === 4 && toRemove.includes(node.id)) {
          return null
        }
        if (node.children) {
          const filtered = removeFromTree(node.children, toRemove)
          if (filtered.length > 0) {
            return { ...node, children: filtered }
          }
        }
        return null
      })
      .filter(Boolean)
  }
  selectedTreeData.value = removeFromTree(selectedTreeData.value, currentKeys)
  selectedTreeKeys.value = collectIds(selectedTreeData.value)
}

function collectIds(nodes) {
  const ids = []
  if (!Array.isArray(nodes)) return ids
  nodes.forEach(node => {
    if (node.level === 4) ids.push(node.id)
    if (node.children) ids.push(...collectIds(node.children))
  })
  return ids
}

function submitBindStreets() {
  bindStreets({ orgId: currentRegionId.value, streetIds: selectedTreeKeys.value }).then(() => {
    proxy.$modal.msgSuccess('绑定成功')
    openBind.value = false
    getList()
  })
}

function submitForm() {
  proxy.$refs.formRef.validate(valid => {
    if (valid) {
      if (form.value.id) {
        updateRegion(form.value).then(() => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        })
      } else {
        createRegion(form.value).then(() => {
          proxy.$modal.msgSuccess('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

getList()
</script>
