package com.jihao.food.relation.service;

import com.jihao.food.common.exception.BusinessException;
import com.jihao.food.relation.entity.AreaRelation;
import com.jihao.food.relation.mapper.AreaRelationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AreaRelationService {

    private final AreaRelationMapper areaRelationMapper;

    public List<AreaRelation> listByDistrictId(Long districtId) {
        return areaRelationMapper.selectByOrgId(districtId);
    }

    @Transactional
    public int bindStreets(Long districtId, List<Long> streetIds) {
        int count = 0;
        for (Long streetId : streetIds) {
            AreaRelation existing = areaRelationMapper.selectByAreaIdAndOrgId(streetId, districtId);
            if (existing == null) {
                AreaRelation relation = new AreaRelation();
                relation.setAreaId(streetId);
                relation.setOrgId(districtId);
                relation.setDelFlag(0);
                areaRelationMapper.insert(relation);
                count++;
            }
        }
        return count;
    }

    @Transactional
    public boolean unbindStreet(Long districtId, Long streetId) {
        AreaRelation relation = areaRelationMapper.selectByAreaIdAndOrgId(streetId, districtId);
        if (relation == null) {
            throw new BusinessException(400, "绑定关系不存在");
        }
        relation.setDelFlag(1);
        areaRelationMapper.updateById(relation);
        return true;
    }

    public List<AreaRelation> getOrgByAreaId(Long areaId) {
        return areaRelationMapper.selectByAreaId(areaId);
    }
}
