package com.jihao.food.auth.controller;

import com.jihao.food.auth.dto.LoginRequest;
import com.jihao.food.auth.dto.LoginResponse;
import com.jihao.food.auth.service.AuthService;
import com.jihao.food.common.Result;
import com.jihao.food.common.annotation.IgnoreAuth;
import com.jihao.food.common.annotation.IgnoreSign;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@IgnoreAuth
@IgnoreSign
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }
}
