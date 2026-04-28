package com.jihao.food.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_api_key")
public class SysApiKey {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String appKey;

    private String appSecret;

    private String appName;

    private Integer state;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
