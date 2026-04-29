package com.jihao.food.area.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AreaImportDTO {

    /** 操作类型: add-新增, update-更新, delete-删除, merge-合并 */
    private String action;

    private String adcode;

    private String name;

    private String fullName;

    private Integer level;

    private String parentAdcode;

    private String reason;
}
