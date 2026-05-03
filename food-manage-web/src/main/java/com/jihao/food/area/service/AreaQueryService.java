package com.jihao.food.area.service;

import com.jihao.food.area.dto.AreaDTO;
import com.jihao.food.area.dto.AreaDetailDTO;
import com.jihao.food.area.dto.AreaTreeDTO;
import com.jihao.food.area.entity.Area;
import com.jihao.food.area.mapper.AreaMapper;
import com.jihao.food.common.util.TreeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AreaQueryService {

    private final AreaMapper areaMapper;

    public List<AreaDTO> listByLevel(Integer level) {
        List<Area> areas = areaMapper.selectByLevel(level, 1);
        return areas.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<AreaDTO> listByParentId(Long parentId) {
        List<Area> areas = areaMapper.selectByPid(parentId, 1);
        return areas.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public AreaDetailDTO getDetail(Long areaId) {
        Area area = areaMapper.selectById(areaId);
        if (area == null) {
            return null;
        }

        AreaDetailDTO dto = new AreaDetailDTO();
        dto.setId(area.getId());
        dto.setName(area.getName());
        dto.setFullName(area.getFullName());
        dto.setLevel(area.getLevel());
        dto.setLng(area.getLng());
        dto.setLat(area.getLat());

        buildPathInfo(dto, area);
        return dto;
    }

    public List<AreaTreeDTO> getTree(Integer type, Long areaId, Integer depth) {
        List<Area> areas;
        if (areaId != null) {
            areas = areaMapper.selectByPid(areaId, 1);
        } else {
            areas = areaMapper.selectByLevel(1, 1);
        }

        List<AreaTreeDTO> nodes = areas.stream().map(this::toTreeDTO).collect(Collectors.toList());
        return TreeUtil.buildTree(
                nodes,
                AreaTreeDTO::getId,
                AreaTreeDTO::getParentId,
                AreaTreeDTO::getChildren,
                AreaTreeDTO::setChildren,
                node -> node.getParentId() == null || node.getParentId() == 0
        );
    }

    private void buildPathInfo(AreaDetailDTO dto, Area area) {
        if (area.getLevel() >= 4) {
            dto.setTownshipId(area.getId());
            dto.setTownshipName(area.getName());
        }
        if (area.getLevel() >= 3) {
            Area county = findAncestor(area, Area.LEVEL_COUNTY);
            if (county != null) {
                dto.setCountyId(county.getId());
                dto.setCountyName(county.getName());
            }
        }
        if (area.getLevel() >= 2) {
            Area city = findAncestor(area, Area.LEVEL_CITY);
            if (city != null) {
                dto.setCityId(city.getId());
                dto.setCityName(city.getName());
            }
        }
        if (area.getLevel() >= 1) {
            Area province = findAncestor(area, Area.LEVEL_PROVINCE);
            if (province != null) {
                dto.setProvinceId(province.getId());
                dto.setProvinceName(province.getName());
            }
        }
    }

    private Area findAncestor(Area area, int targetLevel) {
        if (area.getLevel() == targetLevel) {
            return area;
        }
        if (area.getPid() == null || area.getPid() == 0) {
            return null;
        }
        Area parent = areaMapper.selectById(area.getPid());
        if (parent == null) {
            return null;
        }
        if (parent.getLevel() == targetLevel) {
            return parent;
        }
        return findAncestor(parent, targetLevel);
    }

    private AreaDTO toDTO(Area area) {
        return new AreaDTO(
                area.getId(),
                area.getName(),
                area.getShortName(),
                area.getAdcode(),
                area.getLevel(),
                area.getLng(),
                area.getLat()
        );
    }

    private AreaTreeDTO toTreeDTO(Area area) {
        return new AreaTreeDTO(area.getId(), area.getPid(), area.getName(), area.getLevel(), null);
    }

    public List<Area> getChildren(Long parentId) {
        if (parentId == null || parentId == 0) {
            return areaMapper.selectByLevel(1, 1);
        }
        return areaMapper.selectByPid(parentId, 1);
    }
}
