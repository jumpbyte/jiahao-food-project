package com.jihao.food.common.annotation;

import java.lang.annotation.*;

/**
 * 标注跳过 JWT 认证校验的接口
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IgnoreAuth {
}
