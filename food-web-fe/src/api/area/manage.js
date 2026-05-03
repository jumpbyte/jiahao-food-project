import request from '@/utils/request'

// 查询行政区树
export function getAreaTree(query) {
  return request({
    url: '/api/admin/area/tree',
    method: 'get',
    params: query
  })
}

// 查询子级行政区列表
export function getAreaChildren(parentId) {
  return request({
    url: '/api/admin/area/children',
    method: 'get',
    params: { parentId: parentId || undefined }
  })
}

// 查询行政区详情
export function getAreaDetail(id) {
  return request({
    url: '/api/admin/area/detail',
    method: 'get',
    params: { id }
  })
}

// 新增行政区
export function createArea(data) {
  return request({
    url: '/api/admin/area/create',
    method: 'post',
    data: data
  })
}

// 修改行政区
export function updateArea(data) {
  return request({
    url: '/api/admin/area/update',
    method: 'post',
    data: data
  })
}

// 删除行政区
export function deleteArea(id) {
  return request({
    url: '/api/admin/area/delete',
    method: 'post',
    params: { id }
  })
}

// 启用/禁用
export function toggleStatus(id, state) {
  return request({
    url: '/api/admin/area/status',
    method: 'post',
    params: { id, state }
  })
}

// 导入预览
export function importPreview(data) {
  return request({
    url: '/api/admin/area/import-preview',
    method: 'post',
    data: data
  })
}

// 执行导入
export function doImport(data) {
  return request({
    url: '/api/admin/area/import',
    method: 'post',
    data: data
  })
}
