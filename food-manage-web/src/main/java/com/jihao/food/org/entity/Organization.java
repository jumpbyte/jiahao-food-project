package com.jihao.food.org.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("organization")
public class Organization {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    /** 组织类型: 1-大区 2-办事处 3-片区 */
    private Integer type;

    private Long parentId;

    private Integer level;

    private String path;

    private Integer sortIndex;

    private Integer state;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static final int TYPE_REGION = 1;
    public static final int TYPE_OFFICE = 2;
    public static final int TYPE_DISTRICT = 3;
}
