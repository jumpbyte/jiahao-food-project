package com.jihao.food.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jihao.food.common.Result;
import com.jihao.food.common.annotation.IgnoreSign;
import com.jihao.food.common.util.SignUtil;
import com.jihao.food.system.entity.SysApiKey;
import com.jihao.food.system.mapper.SysApiKeyMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiSignInterceptor implements HandlerInterceptor {

    @Value("${api.sign.time-window}")
    private long timeWindowMs;

    private final SysApiKeyMapper sysApiKeyMapper;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        if (handlerMethod.hasMethodAnnotation(IgnoreSign.class)
                || handlerMethod.getBeanType().isAnnotationPresent(IgnoreSign.class)) {
            return true;
        }

        String appKey = request.getHeader("appKey");
        String timestamp = request.getHeader("timestamp");
        String nonce = request.getHeader("nonce");
        String sign = request.getHeader("sign");

        if (appKey == null || timestamp == null || nonce == null || sign == null) {
            sendError(response, "签名参数缺失", 401);
            return false;
        }

        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            sendError(response, "时间戳格式错误", 401);
            return false;
        }

        if (!SignUtil.isWithinTimeWindow(ts, timeWindowMs)) {
            sendError(response, "请求已过期", 401);
            return false;
        }

        SysApiKey apiKey = sysApiKeyMapper.findByAppKey(appKey);
        if (apiKey == null) {
            sendError(response, "appKey 无效", 401);
            return false;
        }

        if (apiKey.getState() != 1) {
            sendError(response, "appKey 已禁用", 403);
            return false;
        }

        String body = getRequestBody(request);
        if (!SignUtil.verifySign(appKey, ts, nonce, body, apiKey.getAppSecret(), sign)) {
            sendError(response, "签名验证失败", 401);
            return false;
        }

        return true;
    }

    private String getRequestBody(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper wrapper) {
            byte[] content = wrapper.getContentAsByteArray();
            if (content.length > 0) {
                try {
                    return new String(content, request.getCharacterEncoding());
                } catch (java.io.UnsupportedEncodingException e) {
                    return new String(content, java.nio.charset.StandardCharsets.UTF_8);
                }
            }
        }
        return "";
    }

    private void sendError(HttpServletResponse response, String message, int code) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(code, message)));
    }
}
