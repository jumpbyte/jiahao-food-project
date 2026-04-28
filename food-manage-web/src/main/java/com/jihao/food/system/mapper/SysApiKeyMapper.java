package com.jihao.food.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jihao.food.system.entity.SysApiKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysApiKeyMapper extends BaseMapper<SysApiKey> {

    @Select("SELECT * FROM sys_api_key WHERE app_key = #{appKey}")
    SysApiKey findByAppKey(String appKey);
}
