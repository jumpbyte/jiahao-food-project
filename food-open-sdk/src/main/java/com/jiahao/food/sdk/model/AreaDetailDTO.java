package com.jiahao.food.sdk.model;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 行政区详情 DTO。
 */
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
    private Date createTime;
    private Date updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getAdcode() { return adcode; }
    public void setAdcode(String adcode) { this.adcode = adcode; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public Integer getState() { return state; }
    public void setState(Integer state) { this.state = state; }
    public Long getProvinceId() { return provinceId; }
    public void setProvinceId(Long provinceId) { this.provinceId = provinceId; }
    public String getProvinceName() { return provinceName; }
    public void setProvinceName(String provinceName) { this.provinceName = provinceName; }
    public Long getCityId() { return cityId; }
    public void setCityId(Long cityId) { this.cityId = cityId; }
    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }
    public Long getCountyId() { return countyId; }
    public void setCountyId(Long countyId) { this.countyId = countyId; }
    public String getCountyName() { return countyName; }
    public void setCountyName(String countyName) { this.countyName = countyName; }
    public Long getTownshipId() { return townshipId; }
    public void setTownshipId(Long townshipId) { this.townshipId = townshipId; }
    public String getTownshipName() { return townshipName; }
    public void setTownshipName(String townshipName) { this.townshipName = townshipName; }
    public BigDecimal getLng() { return lng; }
    public void setLng(BigDecimal lng) { this.lng = lng; }
    public BigDecimal getLat() { return lat; }
    public void setLat(BigDecimal lat) { this.lat = lat; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
