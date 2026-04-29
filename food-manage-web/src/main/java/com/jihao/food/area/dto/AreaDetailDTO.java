package com.jihao.food.area.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AreaDetailDTO {

    private Long id;

    private String name;

    private String fullName;

    private Integer level;

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
}
