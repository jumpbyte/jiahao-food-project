package com.jihao.food.relation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("area_relation")
public class AreaRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long areaId;

    private Long orgId;

    private Integer delFlag;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
