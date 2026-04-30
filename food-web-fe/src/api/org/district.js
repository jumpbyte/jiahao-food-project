import request from '@/utils/request'

// 查询片区列表
export function listDistrict(query) {
  return request({
    url: '/api/admin/district/list',
    method: 'get',
    params: query
  })
}

// 新增片区
export function createDistrict(data) {
  return request({
    url: '/api/admin/district/create',
    method: 'post',
    data: data
  })
}

// 修改片区
export function updateDistrict(data) {
  return request({
    url: '/api/admin/district/update',
    method: 'post',
    data: data
  })
}

// 删除片区
export function deleteDistrict(id) {
  return request({
    url: '/api/admin/district/delete',
    method: 'post',
    params: { id }
  })
}

// 获取父级信息
export function getParent(areaId) {
  return request({
    url: '/api/admin/district/parent',
    method: 'get',
    params: { areaId }
  })
}

// 绑定街道
export function bindStreets(data) {
  return request({
    url: '/api/admin/district/bind-streets',
    method: 'post',
    data: data
  })
}

// 获取可选行政区树
export function getSelectableAreaTree(orgId) {
  return request({
    url: '/api/admin/district/selectable-area-tree',
    method: 'get',
    params: { orgId }
  })
}

// 解绑街道
export function unbindStreet(areaId, streetId) {
  return request({
    url: '/api/admin/district/unbind-street',
    method: 'post',
    params: { areaId, streetId }
  })
}
