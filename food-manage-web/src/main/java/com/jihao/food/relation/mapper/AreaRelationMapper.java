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
            "AND child.state = 1 AND ar.del_flag = 0")
    List<Long> selectAreaIdsByParentOrg(Long parentOrgId);

    @Select("SELECT a.id FROM area a " +
            "LEFT JOIN area_relation ar ON ar.area_id = a.id AND ar.del_flag = 0 " +
            "LEFT JOIN organization o ON o.id = ar.org_id AND o.type = #{orgType} AND o.state = 1 " +
            "WHERE a.level = 4 AND a.state = 1 AND o.id IS NULL")
    List<Long> selectUnboundStreetsByOrgType(@Param("orgType") Integer orgType);

    /** 查询指定街道中被同级组织（排除当前组织）绑定的：streetId -> orgId 映射 */
    @Select("<script>" +
            "SELECT ar.area_id, ar.org_id FROM area_relation ar " +
            "JOIN organization o ON ar.org_id = o.id " +
            "WHERE ar.area_id IN " +
            "<foreach collection='streetIds' item='sid' open='(' separator=',' close=')'>#{sid}</foreach> " +
            "AND o.type = #{orgType} AND ar.del_flag = 0 AND o.state = 1" +
            "<if test='excludeOrgId != null'> AND ar.org_id != #{excludeOrgId}</if>" +
            "</script>")
    List<java.util.Map<String, Object>> selectStreetOrgConflicts(@Param("streetIds") List<Long> streetIds,
                                                                   @Param("orgType") Integer orgType,
                                                                   @Param("excludeOrgId") Long excludeOrgId);
}
