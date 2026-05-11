package com.jiahao.food.sdk.model;

import java.math.BigDecimal;

/**
 * 行政区基础信息 DTO。
 */
public class AreaDTO {
    private Long id;
    private String name;
    private String shortName;
    private String adcode;
    private Integer level;
    private BigDecimal lng;
    private BigDecimal lat;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }
    public String getAdcode() { return adcode; }
    public void setAdcode(String adcode) { this.adcode = adcode; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public BigDecimal getLng() { return lng; }
    public void setLng(BigDecimal lng) { this.lng = lng; }
    public BigDecimal getLat() { return lat; }
    public void setLat(BigDecimal lat) { this.lat = lat; }
}
