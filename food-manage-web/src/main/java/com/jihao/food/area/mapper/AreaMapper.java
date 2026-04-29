package com.jihao.food.area.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jihao.food.area.entity.Area;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AreaMapper extends BaseMapper<Area> {

    @Select("SELECT * FROM area WHERE pid = #{pid} AND state = #{state} ORDER BY id ASC")
    List<Area> selectByPid(@Param("pid") Long pid, @Param("state") Integer state);

    @Select("SELECT * FROM area WHERE level = #{level} AND state = #{state} ORDER BY id ASC")
    List<Area> selectByLevel(@Param("level") Integer level, @Param("state") Integer state);

    default List<Area> selectByLevel(Integer level) {
        return selectByLevel(level, 1);
    }

    @Select("SELECT COUNT(*) FROM area WHERE pid = #{id} AND state = 1")
    int countChildren(Long id);

    @Select("SELECT * FROM area WHERE adcode = #{adcode}")
    Area selectByAdcode(String adcode);
}
