import request from '@/utils/request'

// 查询用户列表
export function listUser(query) {
  return request({
    url: '/api/admin/user/list',
    method: 'get',
    params: query
  })
}

// 新增用户
export function createUser(data) {
  return request({
    url: '/api/admin/user/create',
    method: 'post',
    data: data
  })
}

// 修改用户
export function updateUser(data) {
  return request({
    url: '/api/admin/user/update',
    method: 'post',
    data: data
  })
}

// 用户状态修改
export function updateStatus(data) {
  return request({
    url: '/api/admin/user/status',
    method: 'post',
    data: data
  })
}

// 管理员重置密码
export function resetPassword(data) {
  return request({
    url: '/api/admin/user/reset-password',
    method: 'post',
    data: data
  })
}

// 修改自己的密码
export function changePassword(data) {
  return request({
    url: '/api/admin/user/change-password',
    method: 'post',
    data: data
  })
}
