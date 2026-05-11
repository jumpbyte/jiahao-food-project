package com.jiahao.food.sdk.model;

/**
 * 乡镇组织信息 DTO。
 */
public class TownshipOrgDTO {
    private Long townshipId;
    private String townshipName;
    private Long regionId;
    private String regionName;
    private Long officeId;
    private String officeName;
    private Long districtId;
    private String districtName;

    public Long getTownshipId() { return townshipId; }
    public void setTownshipId(Long townshipId) { this.townshipId = townshipId; }
    public String getTownshipName() { return townshipName; }
    public void setTownshipName(String townshipName) { this.townshipName = townshipName; }
    public Long getRegionId() { return regionId; }
    public void setRegionId(Long regionId) { this.regionId = regionId; }
    public String getRegionName() { return regionName; }
    public void setRegionName(String regionName) { this.regionName = regionName; }
    public Long getOfficeId() { return officeId; }
    public void setOfficeId(Long officeId) { this.officeId = officeId; }
    public String getOfficeName() { return officeName; }
    public void setOfficeName(String officeName) { this.officeName = officeName; }
    public Long getDistrictId() { return districtId; }
    public void setDistrictId(Long districtId) { this.districtId = districtId; }
    public String getDistrictName() { return districtName; }
    public void setDistrictName(String districtName) { this.districtName = districtName; }
}
