package com.jihao.food.relation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jihao.food.relation.entity.AreaRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AreaRelationMapper extends BaseMapper<AreaRelation> {

    @Select("SELECT * FROM area_relation WHERE org_id = #{orgId} AND del_flag = 0")
    List<AreaRelation> selectByOrgId(Long orgId);

    @Select("SELECT COUNT(*) FROM area_relation WHERE org_id = #{orgId} AND del_flag = 0")
    int countByOrgId(Long orgId);

    @Select("SELECT * FROM area_relation WHERE area_id = #{areaId} AND org_id = #{orgId} AND del_flag = 0")
    AreaRelation selectByAreaIdAndOrgId(@Param("areaId") Long areaId, @Param("orgId") Long orgId);

    @Select("SELECT ar.* FROM area_relation ar " +
            "JOIN organization o ON ar.org_id = o.id " +
            "WHERE ar.area_id = #{areaId} AND o.type = 3 AND ar.del_flag = 0")
    List<AreaRelation> selectByAreaId(Long areaId);

    @Select("SELECT ar.area_id FROM area_relation ar " +
            "JOIN organization o ON ar.org_id = o.id " +
            "WHERE ar.org_id = #{orgId} AND o.type = #{orgType} AND ar.del_flag = 0")
    List<Long> selectAreaIdsByOrgIdAndType(@Param("orgId") Long orgId, @Param("orgType") Integer orgType);

    @Select("SELECT DISTINCT ar.area_id FROM area_relation ar " +
            "JOIN organization child ON ar.org_id = child.id " +
            "WHERE child.path LIKE CONCAT((SELECT path FROM organization WHERE id = #{parentOrgId}), '%') " +
            "AND child.del_flag = 0 AND ar.del_flag = 0")
    List<Long> selectAreaIdsByParentOrg(Long parentOrgId);

    @Select("SELECT a.id FROM area a WHERE a.level = 4 AND a.state = 1 AND a.id NOT IN " +
            "(SELECT DISTINCT ar.area_id FROM area_relation ar " +
            "JOIN organization o ON ar.org_id = o.id " +
            "WHERE o.type = #{orgType} AND ar.del_flag = 0 AND o.del_flag = 0)")
    List<Long> selectUnboundStreetsByOrgType(@Param("orgType") Integer orgType);
}
