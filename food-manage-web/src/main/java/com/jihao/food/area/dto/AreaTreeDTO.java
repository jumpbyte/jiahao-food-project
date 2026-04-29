package com.jihao.food.area.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AreaTreeDTO {

    private Long id;

    private Long parentId;

    private String name;

    private Integer level;

    private List<AreaTreeDTO> children;

    public void addChild(AreaTreeDTO child) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(child);
    }
}
