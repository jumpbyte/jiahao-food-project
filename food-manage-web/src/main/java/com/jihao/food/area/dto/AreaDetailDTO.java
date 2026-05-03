package com.jihao.food.area.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AreaDetailDTO {

    private Long id;

    private String name;

    private String shortName;

    private String fullName;

    private String adcode;

    private Integer level;

    private Integer state;

    private Long provinceId;

    private String provinceName;

    private Long cityId;

    private String cityName;

    private Long countyId;

    private String countyName;

    private Long townshipId;

    private String townshipName;

    private BigDecimal lng;

    private BigDecimal lat;

    private String path;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
