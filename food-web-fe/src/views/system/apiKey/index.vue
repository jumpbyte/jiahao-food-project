<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="应用名称" prop="appName">
        <el-input v-model="queryParams.appName" placeholder="请输入应用名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
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

    <el-table v-loading="loading" :data="apiKeyList">
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="App Key" align="center" prop="appKey" :show-overflow-tooltip="true" />
      <el-table-column label="App Secret" align="center" prop="appSecret" :show-overflow-tooltip="true" width="280" />
      <el-table-column label="应用名称" align="center" prop="appName" :show-overflow-tooltip="true" />
      <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
      <el-table-column label="状态" align="center" prop="state" width="100">
        <template #default="scope">
          <el-switch v-model="scope.row.state" :active-value="1" :inactive-value="0" @change="handleStatusChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180" />
      <el-table-column label="操作" align="center" width="200">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)">编辑</el-button>
          <el-button link type="warning" icon="Refresh" @click="handleRegenerate(scope.row)">重置Secret</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.page" v-model:limit="queryParams.size" @pagination="getList" />

    <!-- 添加或编辑对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="应用名称" prop="appName">
          <el-input v-model="form.appName" placeholder="请输入应用名称" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ApiKey">
import { listApiKey, createApiKey, updateApiKey, updateStatus, regenerateSecret } from '@/api/system/apiKey'

const { proxy } = getCurrentInstance()

const apiKeyList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const title = ref('')
const open = ref(false)

const queryParams = ref({
  page: 1,
  size: 10,
  appName: '',
  state: null
})

const form = ref({})

const rules = {
  appName: [{ required: true, message: '应用名称不能为空', trigger: 'blur' }]
}

function getList() {
  loading.value = true
  listApiKey(queryParams.value).then(res => {
    const page = res.data || res
    apiKeyList.value = page.records || []
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
  queryParams.value = { page: 1, size: 10, appName: '', state: null }
  handleQuery()
}

function handleAdd() {
  form.value = {}
  open.value = true
  title.value = '新增 API Key'
}

function handleUpdate(row) {
  form.value = { ...row }
  open.value = true
  title.value = '编辑 API Key'
}

function handleStatusChange(row) {
  updateStatus({ id: row.id, state: row.state }).then(() => {
    proxy.$modal.msgSuccess('状态修改成功')
  }).catch(() => {
    row.state = row.state === 1 ? 0 : 1
  })
}

function handleRegenerate(row) {
  proxy.$modal.confirm('确认要重新生成该 API Key 的 Secret 吗？').then(() => {
    regenerateSecret({ id: row.id }).then(res => {
      proxy.$modal.msgSuccess('Secret 已重置')
      getList()
    })
  }).catch(() => {})
}

function submitForm() {
  proxy.$refs.formRef.validate(valid => {
    if (valid) {
      if (form.value.id) {
        updateApiKey(form.value).then(() => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        })
      } else {
        createApiKey(form.value).then(() => {
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
