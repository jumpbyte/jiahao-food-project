package com.jihao.food.auth.service;

import com.jihao.food.auth.dto.LoginRequest;
import com.jihao.food.auth.dto.LoginResponse;
import com.jihao.food.auth.dto.UserInfoDTO;
import com.jihao.food.common.exception.BusinessException;
import com.jihao.food.common.util.JwtUtil;
import com.jihao.food.system.entity.SysUser;
import com.jihao.food.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest request) {
        SysUser user = sysUserMapper.findByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException(400, "用户名或密码错误");
        }

        if (user.getState() != 1) {
            throw new BusinessException(403, "账号已被禁用");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(400, "用户名或密码错误");
        }

        String token = JwtUtil.generateToken(
                jwtSecret,
                jwtExpiration,
                user.getId(),
                user.getUsername(),
                Collections.emptyMap()
        );

        UserInfoDTO userInfo = new UserInfoDTO(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                determineRoles(user)
        );

        return new LoginResponse(token, jwtExpiration / 1000, userInfo);
    }

    private List<String> determineRoles(SysUser user) {
        // TODO: 后续接入 RBAC 角色表
        return Collections.singletonList("ADMIN");
    }
}
