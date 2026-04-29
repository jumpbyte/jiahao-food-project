package com.jihao.food.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jihao.food.common.exception.BusinessException;
import com.jihao.food.system.entity.SysApiKey;
import com.jihao.food.system.mapper.SysApiKeyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final SysApiKeyMapper sysApiKeyMapper;

    /**
     * 分页查询 API Key 列表
     */
    public Page<SysApiKey> list(String appName, Integer state, int page, int size) {
        LambdaQueryWrapper<SysApiKey> wrapper = new LambdaQueryWrapper<>();
        if (appName != null && !appName.isBlank()) {
            wrapper.like(SysApiKey::getAppName, appName);
        }
        if (state != null) {
            wrapper.eq(SysApiKey::getState, state);
        }
        wrapper.orderByDesc(SysApiKey::getCreateTime);
        return sysApiKeyMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 创建 API Key
     */
    @Transactional
    public SysApiKey create(String appName, String remark) {
        SysApiKey apiKey = new SysApiKey();
        apiKey.setAppKey("APP_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        apiKey.setAppSecret(UUID.randomUUID().toString().replace("-", ""));
        apiKey.setAppName(appName);
        apiKey.setRemark(remark != null ? remark : "");
        apiKey.setState(1);
        sysApiKeyMapper.insert(apiKey);
        return apiKey;
    }

    /**
     * 更新 API Key
     */
    @Transactional
    public SysApiKey update(Long id, String appName, String remark) {
        SysApiKey apiKey = sysApiKeyMapper.selectById(id);
        if (apiKey == null) {
            throw new BusinessException(404, "API Key 不存在");
        }
        apiKey.setAppName(appName);
        apiKey.setRemark(remark != null ? remark : "");
        sysApiKeyMapper.updateById(apiKey);
        return apiKey;
    }

    /**
     * 启用/禁用
     */
    @Transactional
    public SysApiKey updateStatus(Long id, Integer state) {
        SysApiKey apiKey = sysApiKeyMapper.selectById(id);
        if (apiKey == null) {
            throw new BusinessException(404, "API Key 不存在");
        }
        apiKey.setState(state);
        sysApiKeyMapper.updateById(apiKey);
        return apiKey;
    }

    /**
     * 重新生成 Secret
     */
    @Transactional
    public SysApiKey regenerateSecret(Long id) {
        SysApiKey apiKey = sysApiKeyMapper.selectById(id);
        if (apiKey == null) {
            throw new BusinessException(404, "API Key 不存在");
        }
        apiKey.setAppSecret(UUID.randomUUID().toString().replace("-", ""));
        sysApiKeyMapper.updateById(apiKey);
        return apiKey;
    }
}
