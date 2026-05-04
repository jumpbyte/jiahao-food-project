package com.jihao.food.relation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreetInfoDTO {

    private Long id;

    private Long areaId;

    private String name;

    private String provinceName;

    private String cityName;

    private String countyName;

    /** 省/市/县路径文本 */
    private String pathText;
}
