package com.jihao.food.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jihao.food.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    SysUser findByUsername(String username);
}
