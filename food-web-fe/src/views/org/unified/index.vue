<template>
  <div class="app-container">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <el-button type="primary" icon="Plus" @click="handleAddRoot">新增大区</el-button>
      <el-button icon="Refresh" @click="refreshTree">刷新</el-button>
    </div>

    <el-row :gutter="16">
      <!-- 左侧：组织树 -->
      <el-col :span="7">
        <el-card shadow="never" class="tree-card">
          <template #header><span>区域组织</span></template>
          <el-tree
            ref="treeRef"
            :data="treeData"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            highlight-current
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <span class="custom-tree-node">
                <span class="node-label">
                  <el-tag v-if="data.type === 1" size="small" type="success">大区</el-tag>
                  <el-tag v-else-if="data.type === 2" size="small">办事处</el-tag>
                  <el-tag v-else-if="data.type === 3" size="small" type="warning">片区</el-tag>
                  {{ node.label }}
                </span>
                <span class="node-actions">
                  <el-button link type="primary" size="small" @click.stop="handleEditNode(data)">
                    <el-icon><Edit /></el-icon>
                  </el-button>
                  <el-button link type="primary" size="small" @click.stop="handleAddChild(data)" v-if="data.type < 3">
                    <el-icon><Plus /></el-icon>
                  </el-button>
                  <el-popconfirm title="确认删除？" @confirm="handleDelete(data)" v-if="data.type < 3">
                    <template #reference>
                      <el-button link type="danger" size="small" @click.stop>
                        <el-icon><Delete /></el-icon>
                      </el-button>
                    </template>
                  </el-popconfirm>
                </span>
              </span>
            </template>
          </el-tree>
        </el-card>
      </el-col>

      <!-- 右侧：动态视图 -->
      <el-col :span="17">
        <el-card shadow="never" class="content-card">
          <template #header>
            <div class="card-header">
              <span>{{ headerTitle }}</span>
              <el-button v-if="viewMode !== 'list'" link type="primary" @click="backToList">← 返回列表</el-button>
            </div>
          </template>

          <!-- ========== 子级列表视图 ========== -->
          <div v-if="viewMode === 'list'">
            <div v-if="!currentNode" class="empty-tip">请在左侧选择组织节点</div>
            <template v-else>
              <!-- 当前节点详情 -->
              <el-descriptions :column="3" border class="node-detail">
                <el-descriptions-item label="ID">{{ currentNode.id }}</el-descriptions-item>
                <el-descriptions-item label="名称">{{ currentNode.name }}</el-descriptions-item>
                <el-descriptions-item label="类型">{{ typeLabel(currentNode.type) }}</el-descriptions-item>
                <el-descriptions-item label="状态">
                  <el-tag :type="currentNode.state === 1 ? 'success' : 'danger'">
                    {{ currentNode.state === 1 ? '启用' : '禁用' }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ currentNode.createTime || '-' }}</el-descriptions-item>
              </el-descriptions>

              <!-- 已关联街道 -->
              <div class="section-title">已关联街道 (共 {{ boundStreets.length }} 个)</div>
              <el-table v-if="boundStreetsGrouped.length > 0" :data="boundStreetsGrouped" size="small" class="street-table">
                <el-table-column label="省/市/县" min-width="200">
                  <template #default="scope">{{ scope.row.countyPath || '-' }}</template>
                </el-table-column>
                <el-table-column label="乡镇/街道名称" min-width="200">
                  <template #default="scope">{{ scope.row.streets.join('、') }}</template>
                </el-table-column>
              </el-table>
              <el-empty v-else description="暂无关联街道" :image-size="60" />

              <!-- 子级列表 -->
              <div class="section-title">
                <span>子级列表 (共 {{ childList.length }} 个)</span>
                <el-button v-if="currentNode.type < 3" type="primary" size="small" icon="Plus" @click="handleAddChild(currentNode)">新增子级</el-button>
              </div>
              <el-table v-loading="childListLoading" :data="childList" size="default">
                <el-table-column label="ID" prop="id" min-width="70" />
                <el-table-column label="名称" prop="name" min-width="150" :show-overflow-tooltip="true" />
                <el-table-column label="状态" min-width="80" align="center">
                  <template #default="scope">
                    <el-tag :type="scope.row.state === 1 ? 'success' : 'danger'" size="small">
                      {{ scope.row.state === 1 ? '启用' : '禁用' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="已绑定街道" min-width="250">
                  <template #default="scope">
                    <span v-if="scope.row.streetCount > 0">
                      {{ scope.row.streetPreview }}
                      <el-link v-if="scope.row.streetCount > 3" type="primary" @click="showStreetsDialog(scope.row)" style="margin-left: 4px;">
                        查看{{ scope.row.streetCount - 3 }}个更多
                      </el-link>
                    </span>
                    <span v-else class="no-data">—</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" min-width="180" align="center">
                  <template #default="scope">
                    <el-button link type="primary" size="small" @click="handleView(scope.row)">查看</el-button>
                    <el-button link type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
                    <el-button link type="danger" size="small" @click="handleDeleteChild(scope.row)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-if="!childListLoading && childList.length === 0" description="暂无子级" :image-size="60" />
            </template>
          </div>

          <!-- ========== 详情视图 ========== -->
          <div v-else-if="viewMode === 'detail'">
            <el-descriptions :column="3" border>
              <el-descriptions-item label="ID">{{ detailData.id }}</el-descriptions-item>
              <el-descriptions-item label="名称">{{ detailData.name }}</el-descriptions-item>
              <el-descriptions-item label="类型">{{ typeLabel(detailData.type) }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="detailData.state === 1 ? 'success' : 'danger'">
                  {{ detailData.state === 1 ? '启用' : '禁用' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ detailData.createTime || '-' }}</el-descriptions-item>
            </el-descriptions>

            <div class="section-title">已关联街道 (共 {{ detailStreets.length }} 个)</div>
            <el-table v-if="detailStreetsGrouped.length > 0" :data="detailStreetsGrouped" size="small" class="street-table">
              <el-table-column label="省/市/县" min-width="200">
                <template #default="scope">{{ scope.row.countyPath || '-' }}</template>
              </el-table-column>
              <el-table-column label="乡镇/街道名称" min-width="200">
                <template #default="scope">{{ scope.row.streets.join('、') }}</template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="暂无关联街道" :image-size="60" />

            <div style="margin-top: 16px;">
              <el-button type="primary" @click="handleEdit(detailData)">编辑</el-button>
            </div>
          </div>

          <!-- ========== 新建/编辑表单视图 ========== -->
          <div v-else-if="viewMode === 'new' || viewMode === 'edit'">
            <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
              <el-form-item label="名称" prop="name">
                <el-input v-model="form.name" :placeholder="'请输入' + typeLabel(formType)" style="max-width: 400px;" />
              </el-form-item>
              <el-form-item label="状态" prop="state" v-if="viewMode === 'edit'">
                <el-radio-group v-model="form.state">
                  <el-radio :value="1">启用</el-radio>
                  <el-radio :value="0">禁用</el-radio>
                </el-radio-group>
              </el-form-item>

              <el-form-item label="关联街道">
                <div class="street-picker">
                  <el-tree
                    ref="streetTreeRef"
                    :data="streetTreeData"
                    :props="{ label: 'name', children: 'children' }"
                    node-key="id"
                    show-checkbox
                    :check-strictly="false"
                    :default-checked-keys="checkedStreetIds"
                    @check="onStreetCheck"
                    style="max-height: 350px; overflow-y: auto; border: 1px solid #dcdfe6; border-radius: 4px; padding: 8px;"
                  />
                </div>
                <div class="form-tip">仅街道/乡镇级别可选择，勾选后自动包含其上级</div>
              </el-form-item>

              <!-- 冲突提示 -->
              <div v-if="streetConflict" class="street-conflict-text">{{ streetConflict.message }}</div>
            </el-form>

            <div style="margin-top: 16px;">
              <el-button @click="backToList">取消</el-button>
              <el-button type="primary" :loading="formLoading" @click="submitForm">保存</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 街道详情对话框 -->
    <el-dialog :title="streetsDialogTitle" v-model="streetsDialogOpen" width="650px" append-to-body>
      <el-table :data="groupStreetsByCounty(allStreetsForDialog)" size="small">
        <el-table-column label="省/市/县" min-width="200">
          <template #default="scope">{{ scope.row.countyPath || '-' }}</template>
        </el-table-column>
        <el-table-column label="乡镇/街道名称" min-width="200">
          <template #default="scope">{{ scope.row.streets.join('、') }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup name="OrgUnified">
import { ref, computed, nextTick } from 'vue'
import { Plus, Delete, Refresh, Edit } from '@element-plus/icons-vue'
import {
  getOrgTree,
  getCrudApis,
  typeLabel,
  bindRegionStreets,
  bindOfficeStreets,
  bindDistrictStreets,
  getRegionStreets,
  getOfficeStreets,
  getDistrictStreets,
  getRegionSelectableAreaTree,
  getOfficeSelectableAreaTree,
  getDistrictSelectableAreaTree
} from '@/api/org/unified'

const { proxy } = getCurrentInstance()

// ===== 状态 =====
const treeData = ref([])
const currentNode = ref(null)
const childList = ref([])
const childListLoading = ref(false)
const boundStreets = ref([])
const detailData = ref({})
const detailStreets = ref([])
const viewMode = ref('list') // list | detail | new | edit
const form = ref({})
const formLoading = ref(false)
const formType = ref(1) // 1=大区, 2=办事处, 3=片区
const streetTreeData = ref([])
const checkedStreetIds = ref([])
const selectedStreetIds = ref([])
const streetConflict = ref(null)
const treeRef = ref(null)
const formRef = ref(null)
const streetTreeRef = ref(null)

// ===== 街道对话框
const streetsDialogOpen = ref(false)
const allStreetsForDialog = ref([])
const streetsDialogTitle = ref('')

// ===== 聚合计算 =====
const boundStreetsGrouped = computed(() => groupStreetsByCounty(boundStreets.value))
const detailStreetsGrouped = computed(() => groupStreetsByCounty(detailStreets.value))

// ===== 计算属性 =====
const headerTitle = computed(() => {
  if (!currentNode.value) return '展示区域'
  if (viewMode.value === 'list') return `${typeLabel(currentNode.value.type)} - ${currentNode.value.name}`
  if (viewMode.value === 'detail') return `详情 - ${detailData.value.name}`
  if (viewMode.value === 'new') return `新建${typeLabel(formType.value)}`
  if (viewMode.value === 'edit') return `编辑 - ${detailData.value.name}`
  return ''
})

const rules = {
  name: [{ required: true, message: '名称不能为空', trigger: 'blur' }]
}

// ===== 树加载 =====
async function loadTree() {
  try {
    const res = await getOrgTree({})
    treeData.value = res.data || res || []
  } catch (e) {
    treeData.value = []
  }
}

function refreshTree() {
  loadTree()
  if (currentNode.value) {
    handleNodeClick(currentNode.value)
  }
}

// ===== 节点点击 =====
async function handleNodeClick(data) {
  currentNode.value = data
  viewMode.value = 'list'
  await Promise.all([
    loadChildList(data),
    loadBoundStreets(data)
  ])
}

// ===== 加载子级列表 =====
async function loadChildList(node) {
  if (node.type >= 3) {
    childList.value = []
    return
  }
  childListLoading.value = true
  try {
    // 查询子级：大区→查办事处，办事处→查片区
    const childType = node.type + 1
    const apis = getCrudApis(childType)
    const filterParam = childType === 2 ? { regionId: node.id, page: 1, size: 100 } : { officeId: node.id, page: 1, size: 100 }
    const res = await apis.list(filterParam)
    const records = res.data?.records || res.data || []
    // 为每条记录加载已绑定街道数量
    childList.value = await Promise.all(records.map(async (item) => {
      const streets = await fetchBoundStreetsForOrg(item.id, childType)
      return {
        ...item,
        streetCount: streets.length,
        streetPreview: formatStreetsPreview(streets.slice(0, 3)),
        _allStreets: streets
      }
    }))
  } catch (e) {
    childList.value = []
  } finally {
    childListLoading.value = false
  }
}

// ===== 加载已绑定街道 =====
async function loadBoundStreets(node) {
  try {
    const streets = await fetchBoundStreetsForOrg(node.id, node.type)
    boundStreets.value = streets
  } catch (e) {
    boundStreets.value = []
  }
}

// ===== 获取组织已绑定街道 =====
async function fetchBoundStreetsForOrg(orgId, type) {
  try {
    let res
    if (type === 1) res = await getRegionStreets(orgId)
    else if (type === 2) res = await getOfficeStreets(orgId)
    else res = await getDistrictStreets(orgId)

    const raw = res.data || res || []
    return raw.map(item => ({
      id: item.id,
      areaId: item.areaId,
      name: item.name || '',
      pathText: item.pathText || buildPathText(item)
    }))
  } catch {
    return []
  }
}

/** 将街道列表按县区聚合 */
function groupStreetsByCounty(streets) {
  const map = new Map()
  for (const s of streets) {
    // pathText = 省/市/县, 提取县区路径
    const parts = (s.pathText || '').split('/')
    const countyKey = parts.length >= 3 ? parts.slice(0, 3).join('/') : parts.length >= 1 ? parts.join('/') : '未知'
    if (!map.has(countyKey)) {
      map.set(countyKey, { countyPath: countyKey, streets: [] })
    }
    map.get(countyKey).streets.push(s.name)
  }
  return Array.from(map.values())
}

/** 将街道列表聚合为紧凑文本（用于子级列表预览） */
function formatStreetsPreview(streets) {
  const groups = groupStreetsByCounty(streets)
  const parts = groups.map(g => `${g.countyPath} — ${g.streets.join('、')}`)
  return parts.join('；')
}

function buildPathText(item) {
  const parts = []
  if (item.provinceName) parts.push(item.provinceName)
  if (item.cityName) parts.push(item.cityName)
  if (item.countyName) parts.push(item.countyName)
  return parts.join('/')
}

// ===== 查看详情 =====
async function handleView(row) {
  detailData.value = { ...row }
  try {
    const streets = await fetchBoundStreetsForOrg(row.id, currentNode.value.type + 1)
    detailStreets.value = streets
    detailData.value._allStreets = streets
  } catch {
    detailStreets.value = []
  }
  viewMode.value = 'detail'
}

// ===== 树节点编辑 =====
async function handleEditNode(data) {
  currentNode.value = data
  // 构建行数据用于 handleEdit
  const row = { id: data.id, name: data.name, state: data.state ?? 1 }
  await handleEdit(row)
}

// ===== 编辑 =====
async function handleEdit(row) {
  detailData.value = { ...row }
  // 如果编辑的是当前选中节点本身（从树节点点击编辑），type 直接用节点 type
  // 如果编辑的是子级列表中的行，type 是 currentNode.type + 1
  const isEditingCurrentNode = currentNode.value && currentNode.value.id === row.id
  formType.value = isEditingCurrentNode ? currentNode.value.type : currentNode.value.type + 1
  form.value = {
    id: row.id,
    name: row.name,
    state: row.state ?? 1
  }
  streetConflict.value = null

  // 加载已绑定街道和可选街道树
  await loadFormStreetTree(row.id, currentNode.value)

  viewMode.value = 'edit'
}

// ===== 新建子级 =====
async function handleAddChild(parentNode) {
  formType.value = parentNode.type + 1
  form.value = { name: '', state: 1 }
  checkedStreetIds.value = []
  selectedStreetIds.value = []
  streetConflict.value = null
  currentNode.value = parentNode

  // 加载可选街道树
  await loadFormStreetTree(null, parentNode)

  viewMode.value = 'new'
}

// ===== 新建大区 =====
async function handleAddRoot() {
  formType.value = 1
  form.value = { name: '', state: 1 }
  checkedStreetIds.value = []
  selectedStreetIds.value = []
  streetConflict.value = null
  currentNode.value = null

  // 大区：加载全部街道
  try {
    const res = await getRegionSelectableAreaTree(0)
    const raw = res.data || res || []
    streetTreeData.value = raw
    checkedStreetIds.value = []
  } catch (e) {
    proxy.$modal.msgError('加载街道树失败')
  }

  viewMode.value = 'new'
}

// ===== 加载表单街道树 =====
async function loadFormStreetTree(editOrgId, parentNode) {
  try {
    let res
    // 使用表单目标类型（子级类型）获取正确的 API，而非父节点类型
    const apis = getCrudApis(formType.value)
    if (editOrgId) {
      // 编辑模式：传被编辑组织的 ID
      res = await apis.getSelectableTree(editOrgId)
    } else if (parentNode) {
      // 新建子级：传父组织 ID
      if (formType.value === 2) {
        // 新增办事处：传 parentOrgId 限制在大区管辖范围内
        res = await apis.getSelectableTree(parentNode.id, parentNode.id)
      } else if (formType.value === 3) {
        // 新增片区：传 parentOrgId 限制在办事处管辖范围内
        res = await apis.getSelectableTree(parentNode.id, parentNode.id)
      } else {
        res = await apis.getSelectableTree(parentNode.id)
      }
    }
    const raw = res.data || res || []
    streetTreeData.value = raw

    // 加载已绑定街道（编辑模式）
    if (editOrgId) {
      const streets = await fetchBoundStreetsForOrg(editOrgId, formType.value)
      checkedStreetIds.value = streets.map(s => s.areaId)
      selectedStreetIds.value = [...checkedStreetIds.value]
    } else {
      checkedStreetIds.value = []
      selectedStreetIds.value = []
    }

    await nextTick()
    if (streetTreeRef.value) {
      streetTreeRef.value.setCheckedKeys(checkedStreetIds.value)
    }
  } catch (e) {
    proxy.$modal.msgError('加载街道树失败')
    streetTreeData.value = []
  }
}

// ===== 街道树选择变化 =====
function onStreetCheck(data, info) {
  selectedStreetIds.value = info.checkedKeys
}

// ===== 提交表单 =====
async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // 获取选中的街道 ID（仅叶子节点）
  const streetIds = getLeafStreetIds()
  if (streetIds.length === 0) {
    proxy.$modal.msgWarning('请至少选择一个街道')
    return
  }

  formLoading.value = true
  try {
    const apis = getCrudApis(formType.value)

    if (viewMode.value === 'new') {
      // 新建
      const createData = { name: form.value.name }
      if (formType.value === 2) createData.regionId = currentNode.value.id
      if (formType.value === 3) createData.officeId = currentNode.value.id
      if (streetIds.length > 0) createData.streetIds = streetIds

      const res = await apis.create(createData)
      const newOrg = res.data || res
      proxy.$modal.msgSuccess('新建成功')

      // 刷新树和列表，跳到新组织详情
      await loadTree()
      await handleNodeClick(currentNode.value)
      detailData.value = newOrg
      detailStreets.value = streetIds.map(id => ({ id, name: getStreetNameById(id) }))
      viewMode.value = 'detail'
    } else {
      // 编辑
      await apis.update({ id: form.value.id, name: form.value.name, state: form.value.state })

      // 绑定街道
      await apis.bindStreets({ orgId: form.value.id, streetIds })
      proxy.$modal.msgSuccess('修改成功')

      // 刷新树，跳到详情
      await loadTree()
      // 找到更新后的节点
      const updatedNode = findNodeInTree(treeData.value, form.value.id)
      if (updatedNode) {
        detailData.value = { ...updatedNode }
        const streets = await fetchBoundStreetsForOrg(form.value.id, formType.value)
        detailStreets.value = streets
        viewMode.value = 'detail'
      }
    }
  } catch (e) {
    // 冲突错误显示在表单内（红色提示）
    const errMsg = e.response?.data?.message || e.response?.data?.msg || e.message
    if (errMsg && errMsg.includes('被') && errMsg.includes('占用')) {
      streetConflict.value = { message: errMsg }
    } else {
      proxy.$modal.msgError(errMsg || '操作失败')
    }
  } finally {
    formLoading.value = false
  }
}

// ===== 删除 =====
async function handleDelete(node) {
  try {
    const apis = getCrudApis(node.type)
    await apis.del(node.id)
    proxy.$modal.msgSuccess('删除成功')
    await loadTree()
    if (currentNode.value?.id === node.id) {
      currentNode.value = null
      childList.value = []
      boundStreets.value = []
    }
  } catch (e) {
    const msg = e.response?.data?.msg || e.message || '删除失败'
    proxy.$modal.msgError(msg)
  }
}

async function handleDeleteChild(row) {
  proxy.$modal.confirm(`确认删除"${row.name}"吗？`).then(async () => {
    try {
      const childType = currentNode.value.type + 1
      const apis = getCrudApis(childType)
      await apis.del(row.id)
      proxy.$modal.msgSuccess('删除成功')
      await loadChildList(currentNode.value)
    } catch (e) {
      const msg = e.response?.data?.msg || e.message || '删除失败'
      proxy.$modal.msgError(msg)
    }
  }).catch(() => {})
}

// ===== 返回列表 =====
function backToList() {
  viewMode.value = 'list'
  streetConflict.value = null
  if (currentNode.value) {
    handleNodeClick(currentNode.value)
  }
}

// ===== 街道对话框 =====
function showStreetsDialog(row) {
  streetsDialogTitle.value = `${row.name} - 已绑定街道`
  allStreetsForDialog.value = row._allStreets || []
  streetsDialogOpen.value = true
}

// ===== 工具函数 =====
function getLeafStreetIds() {
  // 从街道树中获取所有被勾选的叶子节点 ID
  function collectLeaves(nodes) {
    let ids = []
    for (const node of (nodes || [])) {
      if (!node.children || node.children.length === 0) {
        if (selectedStreetIds.value.includes(node.id)) {
          ids.push(node.id)
        }
      } else {
        ids = ids.concat(collectLeaves(node.children))
      }
    }
    return ids
  }
  return collectLeaves(streetTreeData.value)
}

function getStreetNameById(id) {
  function find(nodes) {
    for (const n of (nodes || [])) {
      if (n.id === id) return n.name
      if (n.children) {
        const result = find(n.children)
        if (result) return result
      }
    }
    return ''
  }
  return find(streetTreeData.value)
}

function findNodeInTree(nodes, id) {
  for (const node of (nodes || [])) {
    if (node.id === id) return node
    if (node.children) {
      const result = findNodeInTree(node.children, id)
      if (result) return result
    }
  }
  return null
}

// ===== 初始化 =====
loadTree()
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-bottom: 12px;
}

.tree-card {
  height: calc(100vh - 180px);
  overflow-y: auto;
}

.content-card {
  min-height: calc(100vh - 180px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.custom-tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 8px;
  font-size: 14px;
}

.node-label {
  display: flex;
  align-items: center;
  gap: 6px;
}

.node-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.2s;
}

.el-tree-node__content:hover .node-actions {
  opacity: 1;
}

.empty-tip {
  text-align: center;
  color: #909399;
  padding: 60px 0;
  font-size: 14px;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  margin-bottom: 12px;
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}

.node-detail {
  margin-bottom: 8px;
}

.street-table {
  margin-bottom: 8px;
}

.street-picker {
  width: 100%;
  max-width: 500px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.no-data {
  color: #c0c4cc;
}

.street-conflict-text {
  color: #f56c6c;
  font-size: 13px;
  margin-top: 8px;
  line-height: 1.6;
}
</style>
