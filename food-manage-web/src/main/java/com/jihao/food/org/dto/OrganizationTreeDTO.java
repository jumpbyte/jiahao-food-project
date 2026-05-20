package com.jihao.food.org.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationTreeDTO {

    private Long id;

    private Long parentId;

    private String name;

    private String code;

    private Integer type;

    private Integer state;

    private List<OrganizationTreeDTO> children;

    public void addChild(OrganizationTreeDTO child) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(child);
    }
}
