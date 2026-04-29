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
}
