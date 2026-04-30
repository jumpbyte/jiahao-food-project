import request from '@/utils/request'

// 查询办事处列表
export function listOffice(query) {
  return request({
    url: '/api/admin/office/list',
    method: 'get',
    params: query
  })
}

// 新增办事处
export function createOffice(data) {
  return request({
    url: '/api/admin/office/create',
    method: 'post',
    data: data
  })
}

// 修改办事处
export function updateOffice(data) {
  return request({
    url: '/api/admin/office/update',
    method: 'post',
    data: data
  })
}

// 删除办事处
export function deleteOffice(id) {
  return request({
    url: '/api/admin/office/delete',
    method: 'post',
    params: { id }
  })
}

// 获取可选行政区树
export function getSelectableAreaTree(orgId) {
  return request({
    url: '/api/admin/office/selectable-area-tree',
    method: 'get',
    params: { orgId }
  })
}

// 绑定街道
export function bindStreets(data) {
  return request({
    url: '/api/admin/office/bind-streets',
    method: 'post',
    data: data
  })
}

// 解绑街道
export function unbindStreet(orgId, streetId) {
  return request({
    url: '/api/admin/office/unbind-street',
    method: 'post',
    params: { orgId, streetId }
  })
}
