import request from '@/utils/request'

// 查询 API Key 列表
export function listApiKey(query) {
  return request({
    url: '/api/admin/api-key/list',
    method: 'get',
    params: query
  })
}

// 新增 API Key
export function createApiKey(data) {
  return request({
    url: '/api/admin/api-key/create',
    method: 'post',
    data: data
  })
}

// 修改 API Key
export function updateApiKey(data) {
  return request({
    url: '/api/admin/api-key/update',
    method: 'post',
    data: data
  })
}

// 状态修改
export function updateStatus(data) {
  return request({
    url: '/api/admin/api-key/status',
    method: 'post',
    data: data
  })
}

// 重新生成 Secret
export function regenerateSecret(data) {
  return request({
    url: '/api/admin/api-key/regenerate-secret',
    method: 'post',
    data: data
  })
}
