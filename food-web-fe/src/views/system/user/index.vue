<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="queryParams.username" placeholder="请输入用户名" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="真实姓名" prop="realName">
        <el-input v-model="queryParams.realName" placeholder="请输入真实姓名" clearable @keyup.enter="handleQuery" />
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

    <el-table v-loading="loading" :data="userList">
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="用户名" align="center" prop="username" :show-overflow-tooltip="true" />
      <el-table-column label="真实姓名" align="center" prop="realName" :show-overflow-tooltip="true" />
      <el-table-column label="手机号" align="center" prop="phone" :show-overflow-tooltip="true" />
      <el-table-column label="邮箱" align="center" prop="email" :show-overflow-tooltip="true" />
      <el-table-column label="角色" align="center" prop="role" width="120">
        <template #default="scope">
          <el-tag v-if="scope.row.role === 'admin'" type="danger">管理员</el-tag>
          <el-tag v-else-if="scope.row.role === 'operator'">运营人员</el-tag>
          <el-tag v-else type="info">只读人员</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="state" width="100">
        <template #default="scope">
          <el-switch v-model="scope.row.state" :active-value="1" :inactive-value="0" @change="handleStatusChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180" />
      <el-table-column label="操作" align="center" width="200">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)">编辑</el-button>
          <el-button link type="primary" icon="Key" @click="handleResetPwd(scope.row)">重置密码</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.page" v-model:limit="queryParams.size" @pagination="getList" />

    <!-- 添加或编辑对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="用户名" prop="username" v-if="!form.id">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!form.id">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色">
            <el-option label="管理员" value="admin" />
            <el-option label="运营人员" value="operator" />
            <el-option label="只读人员" value="viewer" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog title="重置密码" v-model="openPwd" width="500px" append-to-body>
      <el-form ref="pwdRef" :model="pwdForm" :rules="pwdRules" label-width="100px">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="openPwd = false">取消</el-button>
        <el-button type="primary" @click="submitPwd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="User">
import { listUser, createUser, updateUser, updateStatus, resetPassword } from '@/api/system/user'

const { proxy } = getCurrentInstance()

const userList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const title = ref('')
const open = ref(false)
const openPwd = ref(false)

const queryParams = ref({
  page: 1,
  size: 10,
  username: '',
  realName: '',
  state: null
})

const form = ref({})
const pwdForm = ref({ id: null, newPassword: '' })

const rules = {
  username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
  password: [{ required: true, message: '密码不能为空', trigger: 'blur' }],
  realName: [{ required: true, message: '真实姓名不能为空', trigger: 'blur' }],
  role: [{ required: true, message: '角色不能为空', trigger: 'change' }]
}

const pwdRules = {
  newPassword: [{ required: true, message: '新密码不能为空', trigger: 'blur' }]
}

function getList() {
  loading.value = true
  listUser(queryParams.value).then(res => {
    const page = res.data || res
    userList.value = page.records || []
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
  queryParams.value = { page: 1, size: 10, username: '', realName: '', state: null }
  handleQuery()
}

function handleAdd() {
  form.value = { role: 'operator' }
  open.value = true
  title.value = '新增用户'
}

function handleUpdate(row) {
  form.value = { ...row }
  open.value = true
  title.value = '编辑用户'
}

function handleStatusChange(row) {
  updateStatus({ id: row.id, state: row.state }).then(() => {
    proxy.$modal.msgSuccess('状态修改成功')
  }).catch(() => {
    row.state = row.state === 1 ? 0 : 1
  })
}

function handleResetPwd(row) {
  pwdForm.value = { id: row.id, newPassword: '' }
  openPwd.value = true
}

function submitForm() {
  proxy.$refs.formRef.validate(valid => {
    if (valid) {
      if (form.value.id) {
        updateUser(form.value).then(() => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        })
      } else {
        createUser(form.value).then(() => {
          proxy.$modal.msgSuccess('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

function submitPwd() {
  proxy.$refs.pwdRef.validate(valid => {
    if (valid) {
      resetPassword(pwdForm.value).then(() => {
        proxy.$modal.msgSuccess('密码重置成功')
        openPwd.value = false
      })
    }
  })
}

getList()
</script>
