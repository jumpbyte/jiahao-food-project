package com.jihao.food.area.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("area")
public class Area {

    @TableId
    private Long id;

    private Long pid;

    private Integer level;

    private String name;

    private String shortName;

    private String fullName;

    private String pinYin;

    private String adcode;

    private String zipCode;

    private BigDecimal lng;

    private BigDecimal lat;

    private String path;

    private String version;

    private Integer state;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static final int LEVEL_PROVINCE = 1;
    public static final int LEVEL_CITY = 2;
    public static final int LEVEL_COUNTY = 3;
    public static final int LEVEL_TOWNSHIP = 4;
}
