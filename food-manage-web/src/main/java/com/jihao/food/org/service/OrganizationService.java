package com.jihao.food.org.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jihao.food.common.exception.BusinessException;
import com.jihao.food.common.util.TreeUtil;
import com.jihao.food.org.dto.OrganizationDTO;
import com.jihao.food.org.dto.OrganizationTreeDTO;
import com.jihao.food.org.entity.Organization;
import com.jihao.food.org.mapper.OrganizationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationMapper organizationMapper;

    public Organization getById(Long id) {
        return organizationMapper.selectById(id);
    }

    /**
     * 分页查询指定类型的组织
     */
    public Page<OrganizationDTO> listByType(Integer type, Long parentId, Integer state, int page, int size) {
        LambdaQueryWrapper<Organization> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Organization::getType, type);
        if (parentId != null) {
            wrapper.eq(Organization::getParentId, parentId);
        }
        if (state != null) {
            wrapper.eq(Organization::getState, state);
        }
        wrapper.orderByAsc(Organization::getSortIndex);

        Page<Organization> result = organizationMapper.selectPage(new Page<>(page, size), wrapper);

        Page<OrganizationDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        dtoPage.setRecords(result.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList()));
        return dtoPage;
    }

    /**
     * 创建组织
     */
    @Transactional
    public Organization create(Organization org) {
        validateNameUnique(org);
        org.setLevel(1);
        org.setPath("");
        org.setState(1);
        org.setSortIndex(0);
        organizationMapper.insert(org);

        // 更新 path 为包含自身 ID 的路径
        String newPath = (org.getParentId() == null || org.getParentId() == 0)
                ? String.valueOf(org.getId())
                : buildParentPath(org.getParentId()) + "/" + org.getId();
        org.setPath(newPath);
        organizationMapper.updateById(org);
        return org;
    }

    /**
     * 更新组织
     */
    @Transactional
    public Organization update(Long id, String name, Integer state) {
        Organization org = organizationMapper.selectById(id);
        if (org == null) {
            throw new BusinessException(404, "组织不存在");
        }

        if (name != null && !name.equals(org.getName())) {
            org.setName(name);
            validateNameUnique(org);
        }
        if (state != null) {
            org.setState(state);
        }

        organizationMapper.updateById(org);
        return org;
    }

    /**
     * 删除组织（有子节点或绑定关系时不可删除）
     */
    @Transactional
    public boolean delete(Long id) {
        int childCount = organizationMapper.countChildren(id);
        if (childCount > 0) {
            throw new BusinessException(400, "存在子节点，不可删除");
        }

        Organization org = organizationMapper.selectById(id);
        if (org == null) {
            throw new BusinessException(404, "组织不存在");
        }

        org.setState(0);
        organizationMapper.updateById(org);
        return true;
    }

    /**
     * 查询组织树
     */
    public List<OrganizationTreeDTO> getTree(Long regionId, Integer state) {
        LambdaQueryWrapper<Organization> wrapper = new LambdaQueryWrapper<>();
        if (regionId != null) {
            Organization region = organizationMapper.selectById(regionId);
            if (region != null) {
                wrapper.and(w -> w.eq(Organization::getId, regionId)
                        .or().apply("path LIKE CONCAT({0}, '/%')", region.getPath()));
            }
        }
        if (state != null) {
            wrapper.eq(Organization::getState, state);
        }
        wrapper.orderByAsc(Organization::getSortIndex);

        List<Organization> list = organizationMapper.selectList(wrapper);
        List<OrganizationTreeDTO> treeNodes = list.stream().map(this::toTreeDTO).collect(Collectors.toList());

        return TreeUtil.buildTree(
                treeNodes,
                OrganizationTreeDTO::getId,
                OrganizationTreeDTO::getParentId,
                OrganizationTreeDTO::getChildren,
                OrganizationTreeDTO::setChildren,
                node -> node.getParentId() == null || node.getParentId() == 0
        );
    }

    private String buildParentPath(Long parentId) {
        if (parentId == null || parentId == 0) {
            return "0";
        }
        Organization parent = organizationMapper.selectById(parentId);
        if (parent == null) {
            return "0";
        }
        return parent.getPath();
    }

    private OrganizationDTO toDTO(Organization org) {
        return new OrganizationDTO(
                org.getId(),
                org.getName(),
                org.getType(),
                org.getState(),
                org.getCreateTime() != null ? org.getCreateTime().toString() : null
        );
    }

    private OrganizationTreeDTO toTreeDTO(Organization org) {
        return new OrganizationTreeDTO(org.getId(), org.getParentId(), org.getName(), org.getType(), new ArrayList<>());
    }

    private void validateNameUnique(Organization org) {
        LambdaQueryWrapper<Organization> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Organization::getName, org.getName())
                .eq(Organization::getType, org.getType())
                .eq(Organization::getParentId, org.getParentId());
        if (org.getId() != null) {
            wrapper.ne(Organization::getId, org.getId());
        }
        Long count = organizationMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(400, "同一层级下名称已存在");
        }
    }
}
