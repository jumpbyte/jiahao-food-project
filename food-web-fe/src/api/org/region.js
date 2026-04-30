import request from '@/utils/request'

// 查询大区列表
export function listRegion(query) {
  return request({
    url: '/api/admin/region/list',
    method: 'get',
    params: query
  })
}

// 新增大区
export function createRegion(data) {
  return request({
    url: '/api/admin/region/create',
    method: 'post',
    data: data
  })
}

// 修改大区
export function updateRegion(data) {
  return request({
    url: '/api/admin/region/update',
    method: 'post',
    data: data
  })
}

// 删除大区
export function deleteRegion(id) {
  return request({
    url: '/api/admin/region/delete',
    method: 'post',
    params: { id }
  })
}

// 绑定街道
export function bindStreets(data) {
  return request({
    url: '/api/admin/region/bind-streets',
    method: 'post',
    data: data
  })
}

// 获取可选行政区树
export function getSelectableAreaTree(orgId) {
  return request({
    url: '/api/admin/region/selectable-area-tree',
    method: 'get',
    params: { orgId }
  })
}

// 解绑街道
export function unbindStreet(orgId, streetId) {
  return request({
    url: '/api/admin/region/unbind-street',
    method: 'post',
    params: { orgId, streetId }
  })
}
