package com.jihao.food.area.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AreaDTO {

    private Long id;

    private String name;

    private String shortName;

    private String adcode;

    private Integer level;

    private BigDecimal lng;

    private BigDecimal lat;
}
