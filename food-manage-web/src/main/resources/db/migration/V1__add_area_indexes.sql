-- 可选行政区树接口性能优化：添加复合索引
-- 日期: 2026-05-04

-- area 表：加速 selectByIdsAndLevel 和 selectByLevel 查询
ALTER TABLE area ADD INDEX idx_level_state (level, state);

-- area_relation 表：加速 selectByOrgId、selectAreaIdsByOrgIdAndType 等查询
ALTER TABLE area_relation ADD INDEX idx_org_del (org_id, del_flag);

-- area_relation 表：加速 selectByAreaIdAndOrgId、selectByAreaId 等查询
ALTER TABLE area_relation ADD INDEX idx_area_org_del (area_id, org_id, del_flag);
