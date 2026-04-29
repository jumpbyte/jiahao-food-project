package com.jihao.food.org.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDTO {

    private Long id;

    private String name;

    private Integer type;

    private Integer state;

    private String createTime;
}
