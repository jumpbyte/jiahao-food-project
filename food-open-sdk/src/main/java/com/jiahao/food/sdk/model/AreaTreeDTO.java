package com.jiahao.food.sdk.model;

import java.util.List;

/**
 * 行政区树节点 DTO。
 */
public class AreaTreeDTO {
    private Long id;
    private Long parentId;
    private String name;
    private Integer level;
    private List<AreaTreeDTO> children;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public List<AreaTreeDTO> getChildren() { return children; }
    public void setChildren(List<AreaTreeDTO> children) { this.children = children; }
}
