package com.jihao.food.common.annotation;

import java.lang.annotation.*;

/**
 * 标注需要 API 签名校验的接口
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiSign {
}
