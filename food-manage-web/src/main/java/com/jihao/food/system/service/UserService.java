package com.jihao.food.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jihao.food.common.exception.BusinessException;
import com.jihao.food.system.entity.SysUser;
import com.jihao.food.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper sysUserMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 分页查询用户列表
     */
    public Page<SysUser> list(String username, String realName, Integer state, int page, int size) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isBlank()) {
            wrapper.like(SysUser::getUsername, username);
        }
        if (realName != null && !realName.isBlank()) {
            wrapper.like(SysUser::getRealName, realName);
        }
        if (state != null) {
            wrapper.eq(SysUser::getState, state);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        return sysUserMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 创建用户
     */
    @Transactional
    public SysUser create(String username, String password, String realName, String phone, String email, String role) {
        SysUser existing = sysUserMapper.findByUsername(username);
        if (existing != null) {
            throw new BusinessException(400, "用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setPhone(phone != null ? phone : "");
        user.setEmail(email != null ? email : "");
        user.setRole(role != null ? role : "operator");
        user.setState(1);
        sysUserMapper.insert(user);
        return user;
    }

    /**
     * 更新用户
     */
    @Transactional
    public SysUser update(Long id, String realName, String phone, String email, String role) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setRealName(realName);
        user.setPhone(phone != null ? phone : "");
        user.setEmail(email != null ? email : "");
        user.setRole(role != null ? role : "operator");
        sysUserMapper.updateById(user);
        return user;
    }

    /**
     * 启用/禁用
     */
    @Transactional
    public SysUser updateStatus(Long id, Integer state) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setState(state);
        sysUserMapper.updateById(user);
        return user;
    }

    /**
     * 重置密码
     */
    @Transactional
    public boolean resetPassword(Long id, String newPassword) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(user);
        return true;
    }

    /**
     * 修改自己的密码
     */
    @Transactional
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(400, "旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(user);
        return true;
    }
}
