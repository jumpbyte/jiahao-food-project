import request from '@/utils/request'

// 组织树
export function getOrgTree(params) {
  return request({ url: '/api/admin/org/tree', method: 'get', params })
}

// ===== 大区 =====
export function listRegion(params) { return request({ url: '/api/admin/region/list', method: 'get', params }) }
export function createRegion(data) { return request({ url: '/api/admin/region/create', method: 'post', data }) }
export function updateRegion(data) { return request({ url: '/api/admin/region/update', method: 'post', data }) }
export function deleteRegion(id) { return request({ url: '/api/admin/region/delete', method: 'post', params: { id } }) }
export function bindRegionStreets(data) { return request({ url: '/api/admin/region/bind-streets', method: 'post', data }) }
export function getRegionStreets(orgId) { return request({ url: '/api/admin/region/streets', method: 'get', params: { orgId } }) }
export function getRegionSelectableAreaTree(orgId) { return request({ url: '/api/admin/region/selectable-area-tree', method: 'get', params: { orgId } }) }

// ===== 办事处 =====
export function listOffice(params) { return request({ url: '/api/admin/office/list', method: 'get', params }) }
export function createOffice(data) { return request({ url: '/api/admin/office/create', method: 'post', data }) }
export function updateOffice(data) { return request({ url: '/api/admin/office/update', method: 'post', data }) }
export function deleteOffice(id) { return request({ url: '/api/admin/office/delete', method: 'post', params: { id } }) }
export function bindOfficeStreets(data) { return request({ url: '/api/admin/office/bind-streets', method: 'post', data }) }
export function getOfficeStreets(orgId) { return request({ url: '/api/admin/office/streets', method: 'get', params: { orgId } }) }
export function getOfficeSelectableAreaTree(orgId) { return request({ url: '/api/admin/office/selectable-area-tree', method: 'get', params: { orgId } }) }

// ===== 片区 =====
export function listDistrict(params) { return request({ url: '/api/admin/district/list', method: 'get', params }) }
export function createDistrict(data) { return request({ url: '/api/admin/district/create', method: 'post', data }) }
export function updateDistrict(data) { return request({ url: '/api/admin/district/update', method: 'post', data }) }
export function deleteDistrict(id) { return request({ url: '/api/admin/district/delete', method: 'post', params: { id } }) }
export function bindDistrictStreets(data) { return request({ url: '/api/admin/district/bind-streets', method: 'post', data }) }
export function getDistrictStreets(areaId) { return request({ url: '/api/admin/district/streets', method: 'get', params: { areaId } }) }
export function getDistrictSelectableAreaTree(orgId) { return request({ url: '/api/admin/district/selectable-area-tree', method: 'get', params: { orgId } }) }
export function getDistrictParent(areaId) { return request({ url: '/api/admin/district/parent', method: 'get', params: { areaId } }) }

// ===== 工具函数 =====

/** 根据组织类型获取对应的 CRUD API */
export function getCrudApis(type) {
  const map = {
    1: { list: listRegion, create: createRegion, update: updateRegion, del: deleteRegion, bindStreets: bindRegionStreets, getStreets: getRegionStreets, getSelectableTree: getRegionSelectableAreaTree },
    2: { list: listOffice, create: createOffice, update: updateOffice, del: deleteOffice, bindStreets: bindOfficeStreets, getStreets: getOfficeStreets, getSelectableTree: getOfficeSelectableAreaTree },
    3: { list: listDistrict, create: createDistrict, update: updateDistrict, del: deleteDistrict, bindStreets: bindDistrictStreets, getStreets: getDistrictStreets, getSelectableTree: getDistrictSelectableAreaTree }
  }
  return map[type] || map[1]
}

/** 组织类型标签 */
export function typeLabel(type) {
  return { 1: '大区', 2: '办事处', 3: '片区' }[type] || ''
}
