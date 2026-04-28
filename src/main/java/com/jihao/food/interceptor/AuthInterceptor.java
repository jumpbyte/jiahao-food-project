package com.jihao.food.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jihao.food.common.Result;
import com.jihao.food.common.annotation.IgnoreAuth;
import com.jihao.food.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final ObjectMapper objectMapper;

    public static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    public static final ThreadLocal<String> USERNAME = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        if (handlerMethod.hasMethodAnnotation(IgnoreAuth.class)
                || handlerMethod.getBeanType().isAnnotationPresent(IgnoreAuth.class)) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(response, "未登录");
            return false;
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = JwtUtil.parseToken(jwtSecret, token);
            if (JwtUtil.isTokenExpired(jwtSecret, token)) {
                sendUnauthorized(response, "登录已过期");
                return false;
            }

            USER_ID.set(Long.parseLong(claims.getSubject()));
            USERNAME.set((String) claims.get("username"));
            return true;
        } catch (Exception e) {
            sendUnauthorized(response, "Token 无效");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        USER_ID.remove();
        USERNAME.remove();
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(401, message)));
    }
}
