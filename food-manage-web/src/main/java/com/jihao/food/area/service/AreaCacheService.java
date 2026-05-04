package com.jihao.food.area.service;

import com.jihao.food.area.entity.Area;
import com.jihao.food.area.mapper.AreaMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 全国行政区全量内存缓存，启动时加载，数据变更时刷新。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AreaCacheService {

    private final AreaMapper areaMapper;

    private volatile List<Area> allAreas = Collections.emptyList();
    private volatile Map<Integer, List<Area>> areasByLevel = Map.of();
    private volatile Map<Long, List<Area>> areasByPid = Map.of();

    @PostConstruct
    public void load() {
        refresh();
    }

    public synchronized void refresh() {
        log.info("正在刷新行政区全量内存缓存...");
        List<Area> areas = areaMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Area>()
                        .eq(Area::getState, 1)
        );
        this.allAreas = areas;
        this.areasByLevel = areas.stream().collect(Collectors.groupingBy(Area::getLevel));
        this.areasByPid = areas.stream().collect(Collectors.groupingBy(Area::getPid));
        log.info("行政区缓存刷新完成，共 {} 条", areas.size());
    }

    public List<Area> getByLevel(int level) {
        return areasByLevel.getOrDefault(level, Collections.emptyList());
    }

    public List<Area> getByPids(List<Long> pids) {
        if (pids == null || pids.isEmpty()) return Collections.emptyList();
        Set<Long> pidSet = new HashSet<>(pids);
        List<Area> result = new ArrayList<>();
        for (Long pid : pidSet) {
            result.addAll(areasByPid.getOrDefault(pid, Collections.emptyList()));
        }
        return result;
    }

    public List<Area> getByIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        return allAreas.stream().filter(a -> ids.contains(a.getId())).toList();
    }

    public Area getById(Long id) {
        return allAreas.stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null);
    }

    public List<Area> getAllAreas() {
        return allAreas;
    }
}
