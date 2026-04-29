package com.jihao.food.area.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jihao.food.area.dto.AreaImportDTO;
import com.jihao.food.area.entity.Area;
import com.jihao.food.area.mapper.AreaMapper;
import com.jihao.food.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AreaService {

    private final AreaMapper areaMapper;

    @Transactional
    public Area create(Area area) {
        Area existing = areaMapper.selectByAdcode(area.getAdcode());
        if (existing != null) {
            throw new BusinessException(400, "行政区划代码已存在");
        }

        if (area.getPid() != null && area.getPid() != 0) {
            Area parent = areaMapper.selectById(area.getPid());
            if (parent == null) {
                throw new BusinessException(400, "上级行政区不存在");
            }
            area.setLevel(parent.getLevel() + 1);
            area.setPath(parent.getPath() + "/" + area.getId());
        } else {
            area.setLevel(1);
            area.setPid(0L);
            area.setPath("0");
        }

        area.setState(1);
        areaMapper.insert(area);
        return area;
    }

    @Transactional
    public Area update(Long id, String name, String shortName, BigDecimal lng, BigDecimal lat) {
        Area area = areaMapper.selectById(id);
        if (area == null) {
            throw new BusinessException(404, "行政区不存在");
        }

        if (name != null) {
            area.setName(name);
        }
        if (shortName != null) {
            area.setShortName(shortName);
        }
        if (lng != null) {
            area.setLng(lng);
        }
        if (lat != null) {
            area.setLat(lat);
        }

        areaMapper.updateById(area);
        return area;
    }

    @Transactional
    public boolean delete(Long id) {
        Area area = areaMapper.selectById(id);
        if (area == null) {
            throw new BusinessException(404, "行政区不存在");
        }

        int childCount = areaMapper.countChildren(id);
        if (childCount > 0) {
            throw new BusinessException(400, "存在子级行政区，不可删除");
        }

        area.setState(0);
        areaMapper.updateById(area);
        return true;
    }

    @Transactional
    public void toggleStatus(Long id, Integer state) {
        Area area = areaMapper.selectById(id);
        if (area == null) {
            throw new BusinessException(404, "行政区不存在");
        }

        area.setState(state);
        areaMapper.updateById(area);

        if (state == 0) {
            disableChildren(id);
        }
    }

    private void disableChildren(Long parentId) {
        List<Area> children = areaMapper.selectByPid(parentId, 1);
        for (Area child : children) {
            child.setState(0);
            areaMapper.updateById(child);
            disableChildren(child.getId());
        }
    }

    public List<AreaImportDTO> previewImport(List<Area> importData) {
        List<AreaImportDTO> result = new ArrayList<>();

        for (Area item : importData) {
            Area existing = areaMapper.selectByAdcode(item.getAdcode());
            if (existing == null) {
                AreaImportDTO dto = new AreaImportDTO();
                dto.setAction("add");
                dto.setAdcode(item.getAdcode());
                dto.setName(item.getName());
                dto.setFullName(item.getFullName());
                dto.setLevel(item.getLevel());
                dto.setParentAdcode("");
                dto.setReason("新增行政区");
                result.add(dto);
            } else if (!existing.getName().equals(item.getName())) {
                AreaImportDTO dto = new AreaImportDTO();
                dto.setAction("update");
                dto.setAdcode(item.getAdcode());
                dto.setName(item.getName());
                dto.setFullName(item.getFullName());
                dto.setLevel(item.getLevel());
                dto.setParentAdcode(existing.getAdcode());
                dto.setReason("行政区名称变更: " + existing.getName() + " -> " + item.getName());
                result.add(dto);
            }
        }

        return result;
    }

    @Transactional
    public int doImport(List<Area> importData) {
        int count = 0;
        for (Area item : importData) {
            Area existing = areaMapper.selectByAdcode(item.getAdcode());
            if (existing == null) {
                item.setState(1);
                areaMapper.insert(item);
                count++;
            } else if (!existing.getName().equals(item.getName())) {
                existing.setName(item.getName());
                existing.setFullName(item.getFullName());
                existing.setShortName(item.getShortName());
                areaMapper.updateById(existing);
                count++;
            }
        }
        return count;
    }

    public Page<Area> listDisabled(int page, int size) {
        LambdaQueryWrapper<Area> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Area::getState, 0)
                .orderByDesc(Area::getUpdateTime);
        return areaMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
