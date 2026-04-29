package com.jihao.food.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jihao.food.org.entity.Organization;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrganizationMapper extends BaseMapper<Organization> {

    @Select("SELECT * FROM organization WHERE parent_id = #{parentId} AND type = #{type}")
    List<Organization> selectByParentIdAndType(@Param("parentId") Long parentId, @Param("type") Integer type);

    @Select("SELECT COUNT(*) FROM organization WHERE parent_id = #{id}")
    int countChildren(Long id);
}
