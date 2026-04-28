# 区域管理后端框架搭建实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 从零搭建 Spring Boot 3 + MyBatis-Plus 后端框架，包含项目结构、公共组件、鉴权体系、以及组织架构/行政区/映射关系模块的基础骨架。

**Architecture:** 单模块 Maven 项目，按功能包划分模块（org/area/relation/auth/system），通过拦截器实现 JWT 登录和 MD5 签名双重鉴权，MyBatis-Plus 操作 MySQL。

**Tech Stack:** Java 17, Spring Boot 3.2.x, MyBatis-Plus 3.5.7, MySQL, JWT (jjwt 0.12.5), springdoc-openapi 2.3.0, Lombok, commons-codec 1.15

---

## 文件总览

### 新建文件

| 文件 | 说明 |
|------|------|
| `pom.xml` | Maven 项目配置 |
| `src/main/java/com/jihao/food/FoodProjectApplication.java` | Spring Boot 入口 |
| `src/main/resources/application.yml` | 主配置（占位数据库连接） |
| `src/main/resources/application-dev.yml` | 开发环境配置 |
| `src/main/java/com/jihao/food/common/Result.java` | 统一响应体 |
| `src/main/java/com/jihao/food/common/exception/BusinessException.java` | 业务异常 |
| `src/main/java/com/jihao/food/common/exception/GlobalExceptionHandler.java` | 全局异常处理 |
| `src/main/java/com/jihao/food/common/util/SignUtil.java` | MD5 签名工具 |
| `src/main/java/com/jihao/food/common/util/JwtUtil.java` | JWT 工具 |
| `src/main/java/com/jihao/food/common/util/TreeUtil.java` | 树形结构工具 |
| `src/main/java/com/jihao/food/common/annotation/ApiSign.java` | API 签名注解 |
| `src/main/java/com/jihao/food/common/annotation/IgnoreAuth.java` | 跳过鉴权注解 |
| `src/main/java/com/jihao/food/common/annotation/IgnoreSign.java` | 跳过签名校验注解 |
| `src/main/java/com/jihao/food/config/MybatisPlusConfig.java` | MyBatis-Plus 配置 |
| `src/main/java/com/jihao/food/config/WebMvcConfig.java` | Web MVC 配置 |
| `src/main/java/com/jihao/food/config/JacksonConfig.java` | Jackson 配置 |
| `src/main/java/com/jihao/food/interceptor/AuthInterceptor.java` | JWT 登录拦截器 |
| `src/main/java/com/jihao/food/interceptor/ApiSignInterceptor.java` | API 签名拦截器 |
| `src/main/java/com/jihao/food/system/entity/SysUser.java` | 系统用户实体 |
| `src/main/java/com/jihao/food/system/entity/SysApiKey.java` | API Key 实体 |
| `src/main/java/com/jihao/food/system/mapper/SysUserMapper.java` | 用户 Mapper |
| `src/main/java/com/jihao/food/system/mapper/SysApiKeyMapper.java` | API Key Mapper |
| `src/main/java/com/jihao/food/auth/dto/LoginRequest.java` | 登录请求 DTO |
| `src/main/java/com/jihao/food/auth/dto/LoginResponse.java` | 登录响应 DTO |
| `src/main/java/com/jihao/food/auth/dto/UserInfoDTO.java` | 用户信息 DTO |
| `src/main/java/com/jihao/food/auth/service/AuthService.java` | 认证服务 |
| `src/main/java/com/jihao/food/auth/controller/AuthController.java` | 认证控制器 |
| `src/main/java/com/jihao/food/org/entity/Organization.java` | 组织实体 |
| `src/main/java/com/jihao/food/org/mapper/OrganizationMapper.java` | 组织 Mapper |
| `src/main/java/com/jihao/food/org/dto/OrganizationDTO.java` | 组织 DTO |
| `src/main/java/com/jihao/food/org/dto/OrganizationTreeDTO.java` | 组织树 DTO |
| `src/main/java/com/jihao/food/org/service/OrganizationService.java` | 组织服务 |
| `src/main/java/com/jihao/food/org/controller/RegionController.java` | 大区控制器 |
| `src/main/java/com/jihao/food/org/controller/OfficeController.java` | 办事处控制器 |
| `src/main/java/com/jihao/food/org/controller/DistrictController.java` | 片区控制器 |
| `src/main/java/com/jihao/food/area/entity/Area.java` | 行政区实体 |
| `src/main/java/com/jihao/food/area/mapper/AreaMapper.java` | 行政区 Mapper |
| `src/main/java/com/jihao/food/area/dto/AreaDTO.java` | 行政区 DTO |
| `src/main/java/com/jihao/food/area/dto/AreaTreeDTO.java` | 行政区树 DTO |
| `src/main/java/com/jihao/food/area/dto/AreaDetailDTO.java` | 行政区详情 DTO |
| `src/main/java/com/jihao/food/area/dto/AreaImportDTO.java` | 行政区导入 DTO |
| `src/main/java/com/jihao/food/area/service/AreaService.java` | 行政区服务 |
| `src/main/java/com/jihao/food/area/service/AreaQueryService.java` | 行政区查询服务 |
| `src/main/java/com/jihao/food/area/controller/AdminAreaController.java` | 行政区管理控制器 |
| `src/main/java/com/jihao/food/area/controller/AreaQueryController.java` | 行政区查询控制器 |
| `src/main/java/com/jihao/food/relation/entity/AreaRelation.java` | 映射关系实体 |
| `src/main/java/com/jihao/food/relation/mapper/AreaRelationMapper.java` | 映射关系 Mapper |
| `src/main/java/com/jihao/food/relation/service/AreaRelationService.java` | 映射关系服务 |
| `src/main/java/com/jihao/food/relation/controller/StreetController.java` | 街道列表控制器 |
| `src/main/java/com/jihao/food/relation/controller/GeoQueryController.java` | 行政区归属查询控制器 |
| `src/main/java/com/jihao/food/org/controller/OrgTreeController.java` | 组织树控制器 |
| `src/main/java/com/jihao/food/config/RequestCachingFilter.java` | 请求体缓存过滤器 |

---

## 任务分解

### Task 1: Maven 项目骨架与 Spring Boot 入口

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/jihao/food/FoodProjectApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/application-dev.yml`

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>com.jihao</groupId>
    <artifactId>food-project</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>food-project</name>
    <description>区域管理后台</description>

    <properties>
        <java.version>17</java.version>
        <mybatis-plus.version>3.5.7</mybatis-plus.version>
        <jjwt.version>0.12.5</jjwt.version>
        <springdoc.version>2.5.0</springdoc.version>
        <commons-codec.version>1.15</commons-codec.version>
    </properties>

    <dependencies>
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- MyBatis-Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <!-- MySQL -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- springdoc-openapi -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>

        <!-- commons-codec for MD5 -->
        <dependency>
            <groupId>commons-codec</groupId>
            <artifactId>commons-codec</artifactId>
            <version>${commons-codec.version}</version>
        </dependency>

        <!-- spring-security-crypto (仅用 BCrypt) -->
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-crypto</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建 Spring Boot 入口类**

```java
package com.jihao.food;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FoodProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoodProjectApplication.class, args);
    }
}
```

- [ ] **Step 3: 创建 application.yml（主配置）**

```yaml
server:
  port: 8080

spring:
  profiles:
    active: dev
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DB:food_project}?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: ${MYSQL_USER:root}
    password: ${MYSQL_PASSWORD:}
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto

# JWT 配置
jwt:
  secret: ${JWT_SECRET:food-project-jwt-secret-key-must-be-at-least-256-bits-long}
  expiration: 7200000  # 2 hours in milliseconds

# 签名校验时间窗口（毫秒）
api:
  sign:
    time-window: 300000  # 5 minutes
```

- [ ] **Step 4: 创建 application-dev.yml（开发环境）**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/food_project?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password:
```

- [ ] **Step 5: 编译验证**

```bash
mvn compile -q
```

预期：编译成功，无错误。

- [ ] **Step 6: 提交**

```bash
git add pom.xml src/main/java/com/jihao/food/FoodProjectApplication.java src/main/resources/
git commit -m "feat: initialize Spring Boot 3 + MyBatis-Plus project skeleton"
```

---

### Task 2: 公共组件 — 统一响应体、异常处理、工具类、注解

**Files:**
- Create: `src/main/java/com/jihao/food/common/Result.java`
- Create: `src/main/java/com/jihao/food/common/exception/BusinessException.java`
- Create: `src/main/java/com/jihao/food/common/exception/GlobalExceptionHandler.java`
- Create: `src/main/java/com/jihao/food/common/util/SignUtil.java`
- Create: `src/main/java/com/jihao/food/common/util/JwtUtil.java`
- Create: `src/main/java/com/jihao/food/common/util/TreeUtil.java`
- Create: `src/main/java/com/jihao/food/common/annotation/ApiSign.java`
- Create: `src/main/java/com/jihao/food/common/annotation/IgnoreAuth.java`
- Create: `src/main/java/com/jihao/food/common/annotation/IgnoreSign.java`

- [ ] **Step 1: 创建统一响应体 Result.java**

```java
package com.jihao.food.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private int code;
    private String message;
    private T data;
    private long timestamp;
    private String traceId;

    public static <T> Result<T> success(T data) {
        return new Result<>(0, "success", data, System.currentTimeMillis(), null);
    }

    public static <T> Result<T> success(T data, String traceId) {
        return new Result<>(0, "success", data, System.currentTimeMillis(), traceId);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null, System.currentTimeMillis(), null);
    }

    public static <T> Result<T> error(int code, String message, String traceId) {
        return new Result<>(code, message, null, System.currentTimeMillis(), traceId);
    }
}
```

- [ ] **Step 2: 创建业务异常类 BusinessException.java**

```java
package com.jihao.food.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        this(500, message);
    }
}
```

- [ ] **Step 3: 创建全局异常处理器 GlobalExceptionHandler.java**

```java
package com.jihao.food.common.exception;

import com.jihao.food.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        String traceId = getTraceId(request);
        log.warn("BusinessException: code={}, message={}, traceId={}", e.getCode(), e.getMessage(), traceId);
        return Result.error(e.getCode(), e.getMessage(), traceId);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String traceId = getTraceId(request);
        String errors = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("ValidationException: {}, traceId={}", errors, traceId);
        return Result.error(400, errors, traceId);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        String traceId = getTraceId(request);
        log.error("Unexpected error, traceId={}", traceId, e);
        return Result.error(500, "服务器内部错误", traceId);
    }

    private String getTraceId(HttpServletRequest request) {
        String traceId = (String) request.getAttribute("traceId");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            request.setAttribute("traceId", traceId);
        }
        return traceId;
    }
}
```

- [ ] **Step 4: 创建签名工具 SignUtil.java**

```java
package com.jihao.food.common.util;

import org.apache.commons.codec.digest.DigestUtils;

public final class SignUtil {

    private SignUtil() {}

    /**
     * 生成 API 签名
     * 公式: sign = MD5(appKey + timestamp + nonce + body + secret)
     */
    public static String generateSign(String appKey, long timestamp, String nonce, String body, String secret) {
        String signStr = appKey + timestamp + nonce + body + secret;
        return DigestUtils.md5Hex(signStr).toUpperCase();
    }

    /**
     * 校验签名
     */
    public static boolean verifySign(String appKey, long timestamp, String nonce, String body, String secret, String sign) {
        String expected = generateSign(appKey, timestamp, nonce, body, secret);
        return expected.equals(sign);
    }

    /**
     * 校验时间戳是否在允许的时间窗口内
     */
    public static boolean isWithinTimeWindow(long timestamp, long timeWindowMs) {
        long now = System.currentTimeMillis();
        return Math.abs(now - timestamp) <= timeWindowMs;
    }
}
```

- [ ] **Step 5: 创建 JWT 工具 JwtUtil.java**

```java
package com.jihao.food.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

public final class JwtUtil {

    private JwtUtil() {}

    private static SecretKey getSigningKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT Token
     */
    public static String generateToken(String secret, long expirationMs, long userId, String username, Map<String, Object> claims) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claims(claims)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey(secret))
                .compact();
    }

    /**
     * 解析 JWT Token
     */
    public static Claims parseToken(String secret, String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey(secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 检查 Token 是否过期
     */
    public static boolean isTokenExpired(String secret, String token) {
        try {
            Claims claims = parseToken(secret, token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
```

- [ ] **Step 6: 创建树形结构工具 TreeUtil.java**

```java
package com.jihao.food.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class TreeUtil {

    private TreeUtil() {}

    /**
     * 将扁平列表转为树形结构
     *
     * @param list       原始列表
     * @param getId      获取 ID 的函数
     * @param getParentId 获取父 ID 的函数
     * @param getChildren 获取子列表的函数
     * @param setChildren 设置子列表的函数
     * @param rootFilter  判断是否为根节点的函数
     */
    public static <T> List<T> buildTree(
            List<T> list,
            Function<T, Long> getId,
            Function<T, Long> getParentId,
            Function<T, List<T>> getChildren,
            BiConsumer<T, List<T>> setChildren,
            Predicate<T> rootFilter) {

        List<T> roots = new ArrayList<>();
        for (T item : list) {
            if (rootFilter.test(item)) {
                roots.add(item);
                buildChildren(item, list, getId, getParentId, getChildren, setChildren);
            }
        }
        return roots;
    }

    private static <T> void buildChildren(
            T parent,
            List<T> all,
            Function<T, Long> getId,
            Function<T, Long> getParentId,
            Function<T, List<T>> getChildren,
            BiConsumer<T, List<T>> setChildren) {

        List<T> children = new ArrayList<>();
        Long parentId = getId.apply(parent);
        for (T item : all) {
            if (parentId.equals(getParentId.apply(item))) {
                children.add(item);
                buildChildren(item, all, getId, getParentId, getChildren, setChildren);
            }
        }
        setChildren.accept(parent, children);
    }
}
```

- [ ] **Step 7: 创建自定义注解**

```java
// ApiSign.java
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
```

```java
// IgnoreAuth.java
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
```

```java
// IgnoreSign.java
package com.jihao.food.common.annotation;

import java.lang.annotation.*;

/**
 * 标注跳过 API 签名校验的接口（内部接口默认不需要签名）
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IgnoreSign {
}
```

- [ ] **Step 8: 编译验证**

```bash
mvn compile -q
```

预期：编译成功。

- [ ] **Step 9: 提交**

```bash
git add src/main/java/com/jihao/food/common/
git commit -m "feat: add common components - Result, exceptions, utils, annotations"
```

---

### Task 3: 配置层与拦截器

**Files:**
- Create: `src/main/java/com/jihao/food/config/MybatisPlusConfig.java`
- Create: `src/main/java/com/jihao/food/config/WebMvcConfig.java`
- Create: `src/main/java/com/jihao/food/config/JacksonConfig.java`
- Create: `src/main/java/com/jihao/food/interceptor/AuthInterceptor.java`
- Create: `src/main/java/com/jihao/food/interceptor/ApiSignInterceptor.java`

- [ ] **Step 1: 创建 MybatisPlusConfig.java**

```java
package com.jihao.food.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

- [ ] **Step 2: 创建 WebMvcConfig.java**

```java
package com.jihao.food.config;

import com.jihao.food.interceptor.ApiSignInterceptor;
import com.jihao.food.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final ApiSignInterceptor apiSignInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login");

        registry.addInterceptor(apiSignInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

- [ ] **Step 3: 创建 JacksonConfig.java**

```java
package com.jihao.food.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.TimeZone;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder.json()
                .modules(new JavaTimeModule())
                .propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
                .serializationFeature(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .timeZone(TimeZone.getTimeZone("Asia/Shanghai"))
                .build();
    }
}
```

- [ ] **Step 4: 创建 AuthInterceptor.java（JWT 登录拦截）**

```java
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
```

- [ ] **Step 5: 创建 ApiSignInterceptor.java（外部 API 签名校验）**

```java
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
                return new String(content, request.getCharacterEncoding());
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
```

- [ ] **Step 6: 注册 ContentCachingRequestWrapper 过滤器**

签名拦截器需要读取请求体，需要添加一个过滤器来缓存请求体。创建：

```java
package com.jihao.food.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

@Component
public class RequestCachingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest) {
            request = new ContentCachingRequestWrapper(httpRequest);
        }
        chain.doFilter(request, response);
    }
}
```

- [ ] **Step 7: 编译验证**

```bash
mvn compile -q
```

预期：编译成功（拦截器引用了 SysApiKeyMapper，下一步创建）。

- [ ] **Step 8: 提交**

```bash
git add src/main/java/com/jihao/food/config/ src/main/java/com/jihao/food/interceptor/
git commit -m "feat: add config classes and interceptors (Auth + ApiSign)"
```

---

### Task 4: 系统模块 — 用户与 API Key 实体及 Mapper

**Files:**
- Create: `src/main/java/com/jihao/food/system/entity/SysUser.java`
- Create: `src/main/java/com/jihao/food/system/entity/SysApiKey.java`
- Create: `src/main/java/com/jihao/food/system/mapper/SysUserMapper.java`
- Create: `src/main/java/com/jihao/food/system/mapper/SysApiKeyMapper.java`

- [ ] **Step 1: 创建 SysUser.java**

```java
package com.jihao.food.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String realName;

    private String phone;

    private String email;

    private Integer state;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

- [ ] **Step 2: 创建 SysApiKey.java**

```java
package com.jihao.food.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_api_key")
public class SysApiKey {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String appKey;

    private String appSecret;

    private String appName;

    private Integer state;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

- [ ] **Step 3: 创建 SysUserMapper.java**

```java
package com.jihao.food.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jihao.food.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    SysUser findByUsername(String username);
}
```

- [ ] **Step 4: 创建 SysApiKeyMapper.java**

```java
package com.jihao.food.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jihao.food.system.entity.SysApiKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysApiKeyMapper extends BaseMapper<SysApiKey> {

    @Select("SELECT * FROM sys_api_key WHERE app_key = #{appKey}")
    SysApiKey findByAppKey(String appKey);
}
```

- [ ] **Step 5: 编译验证**

```bash
mvn compile -q
```

预期：编译成功。

- [ ] **Step 6: 提交**

```bash
git add src/main/java/com/jihao/food/system/
git commit -m "feat: add system module - SysUser and SysApiKey entities with mappers"
```

---

### Task 5: 认证模块 — 登录功能

**Files:**
- Create: `src/main/java/com/jihao/food/auth/dto/LoginRequest.java`
- Create: `src/main/java/com/jihao/food/auth/dto/LoginResponse.java`
- Create: `src/main/java/com/jihao/food/auth/dto/UserInfoDTO.java`
- Create: `src/main/java/com/jihao/food/auth/service/AuthService.java`
- Create: `src/main/java/com/jihao/food/auth/controller/AuthController.java`

- [ ] **Step 1: 创建 LoginRequest.java**

```java
package com.jihao.food.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
```

- [ ] **Step 2: 创建 UserInfoDTO.java**

```java
package com.jihao.food.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDTO {

    private Long userId;

    private String username;

    private String realName;

    private List<String> roles;
}
```

- [ ] **Step 3: 创建 LoginResponse.java**

```java
package com.jihao.food.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;

    private Long expiresIn;

    private UserInfoDTO userInfo;
}
```

- [ ] **Step 4: 创建 AuthService.java**

```java
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
```

注意：`BCryptPasswordEncoder` 来自 `spring-security-crypto`。需要在 pom.xml 中添加：

```xml
<!-- spring-security-crypto (仅用 BCrypt) -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>
```

- [ ] **Step 5: 创建 AuthController.java**

```java
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
```

- [ ] **Step 6: 编译验证**

```bash
mvn compile -q
```

预期：编译成功。

- [ ] **Step 7: 提交**

```bash
git add src/main/java/com/jihao/food/auth/
git commit -m "feat: add auth module - login with JWT"
```

---

### Task 6: 组织架构模块 — 实体、Mapper、Service、Controller

**Files:**
- Create: `src/main/java/com/jihao/food/org/entity/Organization.java`
- Create: `src/main/java/com/jihao/food/org/mapper/OrganizationMapper.java`
- Create: `src/main/java/com/jihao/food/org/dto/OrganizationDTO.java`
- Create: `src/main/java/com/jihao/food/org/dto/OrganizationTreeDTO.java`
- Create: `src/main/java/com/jihao/food/org/service/OrganizationService.java`
- Create: `src/main/java/com/jihao/food/org/controller/RegionController.java`
- Create: `src/main/java/com/jihao/food/org/controller/OfficeController.java`
- Create: `src/main/java/com/jihao/food/org/controller/DistrictController.java`
- Create: `src/main/java/com/jihao/food/org/controller/OrgTreeController.java`

- [ ] **Step 1: 创建 Organization.java**

```java
package com.jihao.food.org.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("organization")
public class Organization {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 组织类型: 1-大区 2-办事处 3-片区 */
    private Integer type;

    private Long parentId;

    private Integer level;

    private String path;

    private Integer sortIndex;

    private Integer state;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static final int TYPE_REGION = 1;
    public static final int TYPE_OFFICE = 2;
    public static final int TYPE_DISTRICT = 3;
}
```

- [ ] **Step 2: 创建 OrganizationMapper.java**

```java
package com.jihao.food.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jihao.food.org.entity.Organization;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrganizationMapper extends BaseMapper<Organization> {

    @Select("SELECT * FROM organization WHERE parent_id = #{parentId} AND type = #{type}")
    List<Organization> selectByParentIdAndType(@Param("parentId") Long parentId, @Param("type") Integer type);

    @Select("SELECT COUNT(*) FROM organization WHERE parent_id = #{id}")
    int countChildren(Long id);

    @Select("SELECT * FROM organization WHERE id = #{id}")
    Organization selectById(Long id);
}
```

- [ ] **Step 3: 创建 OrganizationDTO.java**

```java
package com.jihao.food.org.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDTO {

    private Long id;

    private String name;

    private Integer type;

    private Integer state;

    private String createTime;
}
```

- [ ] **Step 4: 创建 OrganizationTreeDTO.java**

```java
package com.jihao.food.org.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationTreeDTO {

    private Long id;

    private String name;

    private Integer type;

    private List<OrganizationTreeDTO> children;

    public void addChild(OrganizationTreeDTO child) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(child);
    }
}
```

- [ ] **Step 5: 创建 OrganizationService.java**

```java
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

    /**
     * 根据 ID 查询组织
     */
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
        buildPathAndLevel(org);
        org.setState(1);
        org.setSortIndex(0);
        organizationMapper.insert(org);

        // 更新 path 为包含自身 ID 的路径
        String newPath = (org.getParentId() == null || org.getParentId() == 0)
                ? String.valueOf(org.getId())
                : org.getPath().replace("/0", "/" + org.getId());
        org.setPath(newPath);
        organizationMapper.updateById(org);

        return org;
    }

    /**
     * 更新组织
     */
    @Transactional
    public Organization update(Long id, String name, Integer state, String remark) {
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

        // TODO: 检查 area_relation 表中是否有绑定关系
        Organization org = organizationMapper.selectById(id);
        if (org == null) {
            throw new BusinessException(404, "组织不存在");
        }

        // 软删除：设置 state=0
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

    private void buildPathAndLevel(Organization org) {
        if (org.getParentId() == null || org.getParentId() == 0) {
            org.setLevel(1);
            org.setPath(""); // will be updated after insert
        } else {
            Organization parent = organizationMapper.selectById(org.getParentId());
            if (parent == null) {
                throw new BusinessException(400, "父级组织不存在");
            }
            org.setLevel(parent.getLevel() + 1);
            org.setPath(parent.getPath() + "/0"); // placeholder, updated after insert
        }
    }
}
```

注意：上面的 `OrganizationTreeDTO` 需要加一个 `parentId` 字段，并且 `toTreeDTO` 方法需要传入父 ID。让我修正 DTO 和 Service：

**修正 OrganizationTreeDTO.java：**

```java
package com.jihao.food.org.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationTreeDTO {

    private Long id;

    private Long parentId;

    private String name;

    private Integer type;

    private List<OrganizationTreeDTO> children;

    public void addChild(OrganizationTreeDTO child) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(child);
    }
}
```

**修正 OrganizationService 中的相关方法：**

```java
    private OrganizationTreeDTO toTreeDTO(Organization org) {
        return new OrganizationTreeDTO(org.getId(), org.getParentId(), org.getName(), org.getType(), new ArrayList<>());
    }

    private Long getParentIdFromList(OrganizationTreeDTO node) {
        return node.getParentId();
    }

    private Long getParentIdFromTree(OrganizationTreeDTO node) {
        return node.getParentId();
    }

    private boolean isRoot(OrganizationTreeDTO node, List<OrganizationTreeDTO> all) {
        Long parentId = node.getParentId();
        if (parentId == null || parentId == 0) {
            return true;
        }
        return all.stream().noneMatch(n -> n.getId().equals(parentId));
    }
```

- [ ] **Step 6: 创建 RegionController.java（大区管理）**

```java
package com.jihao.food.org.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jihao.food.common.Result;
import com.jihao.food.org.dto.OrganizationDTO;
import com.jihao.food.org.entity.Organization;
import com.jihao.food.org.service.OrganizationService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/region")
@RequiredArgsConstructor
public class RegionController {

    private final OrganizationService organizationService;

    @GetMapping("/list")
    public Result<Page<OrganizationDTO>> list(
            @RequestParam(required = false) Integer state,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(organizationService.listByType(Organization.TYPE_REGION, null, state, page, size));
    }

    @PostMapping("/create")
    public Result<OrganizationDTO> create(@Validated @RequestBody CreateRegionRequest request) {
        Organization org = new Organization();
        org.setName(request.getName());
        org.setType(Organization.TYPE_REGION);
        org.setParentId(0L);
        organizationService.create(org);
        return Result.success(organizationService.listByType(Organization.TYPE_REGION, null, null, 1, 1)
                .getRecords().stream()
                .filter(dto -> dto.getName().equals(request.getName()))
                .findFirst()
                .orElse(null));
    }

    @PostMapping("/update")
    public Result<OrganizationDTO> update(@Validated @RequestBody UpdateRegionRequest request) {
        organizationService.update(request.getId(), request.getName(), request.getState(), null);
        // 返回更新后的信息
        return Result.success(null);
    }

    @PostMapping("/delete")
    public Result<Void> delete(@RequestParam Long id) {
        organizationService.delete(id);
        return Result.success(null);
    }

    @Data
    static class CreateRegionRequest {
        @NotBlank(message = "大区名称不能为空")
        private String name;
        private String remark;
    }

    @Data
    static class UpdateRegionRequest {
        private Long id;
        @NotBlank(message = "大区名称不能为空")
        private String name;
        private Integer state;
        private String remark;
    }
}
```

RegionController 的 create/update 返回值需要优化。让我重新设计返回更清晰：

```java
package com.jihao.food.org.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jihao.food.common.Result;
import com.jihao.food.org.dto.OrganizationDTO;
import com.jihao.food.org.entity.Organization;
import com.jihao.food.org.service.OrganizationService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/region")
@RequiredArgsConstructor
public class RegionController {

    private final OrganizationService organizationService;

    @GetMapping("/list")
    public Result<Page<OrganizationDTO>> list(
            @RequestParam(required = false) Integer state,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(organizationService.listByType(Organization.TYPE_REGION, null, state, page, size));
    }

    @PostMapping("/create")
    public Result<Organization> create(@Validated @RequestBody CreateRegionRequest request) {
        Organization org = new Organization();
        org.setName(request.getName());
        org.setType(Organization.TYPE_REGION);
        org.setParentId(0L);
        Organization created = organizationService.create(org);
        return Result.success(created);
    }

    @PostMapping("/update")
    public Result<Organization> update(@Validated @RequestBody UpdateRegionRequest request) {
        Organization updated = organizationService.update(request.getId(), request.getName(), request.getState(), null);
        return Result.success(updated);
    }

    @PostMapping("/delete")
    public Result<Void> delete(@RequestParam Long id) {
        organizationService.delete(id);
        return Result.success(null);
    }

    @Data
    static class CreateRegionRequest {
        @NotBlank(message = "大区名称不能为空")
        private String name;
        private String remark;
    }

    @Data
    static class UpdateRegionRequest {
        private Long id;
        @NotBlank(message = "大区名称不能为空")
        private String name;
        private Integer state;
        private String remark;
    }
}
```

- [ ] **Step 7: 创建 OfficeController.java（办事处管理）**

```java
package com.jihao.food.org.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jihao.food.common.Result;
import com.jihao.food.org.dto.OrganizationDTO;
import com.jihao.food.org.entity.Organization;
import com.jihao.food.org.service.OrganizationService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/office")
@RequiredArgsConstructor
public class OfficeController {

    private final OrganizationService organizationService;

    @GetMapping("/list")
    public Result<Page<OrganizationDTO>> list(
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Integer state,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(organizationService.listByType(Organization.TYPE_OFFICE, regionId, state, page, size));
    }

    @PostMapping("/create")
    public Result<Organization> create(@Validated @RequestBody CreateOfficeRequest request) {
        Organization org = new Organization();
        org.setName(request.getName());
        org.setType(Organization.TYPE_OFFICE);
        org.setParentId(request.getRegionId());
        Organization created = organizationService.create(org);
        return Result.success(created);
    }

    @PostMapping("/update")
    public Result<Organization> update(@Validated @RequestBody UpdateOfficeRequest request) {
        Organization updated = organizationService.update(request.getId(), request.getName(), request.getState(), null);
        return Result.success(updated);
    }

    @PostMapping("/delete")
    public Result<Void> delete(@RequestParam Long id) {
        organizationService.delete(id);
        return Result.success(null);
    }

    @Data
    static class CreateOfficeRequest {
        @NotBlank(message = "办事处名称不能为空")
        private String name;
        @NotNull(message = "所属大区不能为空")
        private Long regionId;
        private String remark;
    }

    @Data
    static class UpdateOfficeRequest {
        private Long id;
        @NotBlank(message = "办事处名称不能为空")
        private String name;
        private Integer state;
        private String remark;
    }
}
```

- [ ] **Step 8: 创建 DistrictController.java（片区管理 + 街道绑定）**

```java
package com.jihao.food.org.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jihao.food.common.Result;
import com.jihao.food.org.dto.OrganizationDTO;
import com.jihao.food.org.entity.Organization;
import com.jihao.food.org.service.OrganizationService;
import com.jihao.food.relation.service.AreaRelationService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/area")
@RequiredArgsConstructor
public class DistrictController {

    private final OrganizationService organizationService;
    private final AreaRelationService areaRelationService;

    @GetMapping("/list")
    public Result<Page<OrganizationDTO>> list(
            @RequestParam(required = false) Long officeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(organizationService.listByType(Organization.TYPE_DISTRICT, officeId, null, page, size));
    }

    @PostMapping("/create")
    public Result<Organization> create(@Validated @RequestBody CreateDistrictRequest request) {
        Organization org = new Organization();
        org.setName(request.getName());
        org.setType(Organization.TYPE_DISTRICT);
        org.setParentId(request.getOfficeId());
        Organization created = organizationService.create(org);
        return Result.success(created);
    }

    @PostMapping("/update")
    public Result<Organization> update(@Validated @RequestBody UpdateDistrictRequest request) {
        Organization updated = organizationService.update(request.getId(), request.getName(), request.getState(), null);
        return Result.success(updated);
    }

    @PostMapping("/delete")
    public Result<Void> delete(@RequestParam Long id) {
        organizationService.delete(id);
        return Result.success(null);
    }

    /**
     * 为片区绑定街道
     */
    @PostMapping("/bind-streets")
    public Result<Integer> bindStreets(@Validated @RequestBody BindStreetsRequest request) {
        int count = areaRelationService.bindStreets(request.getAreaId(), request.getStreetIds());
        return Result.success(count);
    }

    /**
     * 解除片区与街道的绑定
     */
    @PostMapping("/unbind-street")
    public Result<Void> unbindStreet(@Validated @RequestBody UnbindStreetRequest request) {
        areaRelationService.unbindStreet(request.getAreaId(), request.getStreetId());
        return Result.success(null);
    }

    /**
     * 查询片区所属上级
     */
    @GetMapping("/parent")
    public Result<ParentInfoDTO> getParent(@RequestParam Long areaId) {
        Organization district = organizationService.getById(areaId);
        if (district == null) {
            return Result.success(null);
        }
        Organization office = organizationService.getById(district.getParentId());
        Organization region = office != null ? organizationService.getById(office.getParentId()) : null;

        ParentInfoDTO dto = new ParentInfoDTO();
        dto.setAreaId(district.getId());
        dto.setAreaName(district.getName());
        if (office != null) {
            dto.setOfficeId(office.getId());
            dto.setOfficeName(office.getName());
        }
        if (region != null) {
            dto.setRegionId(region.getId());
            dto.setRegionName(region.getName());
        }
        return Result.success(dto);
    }

    @Data
    static class CreateDistrictRequest {
        @NotBlank(message = "片区名称不能为空")
        private String name;
        @NotNull(message = "所属办事处不能为空")
        private Long officeId;
        private String remark;
    }

    @Data
    static class UpdateDistrictRequest {
        private Long id;
        @NotBlank(message = "片区名称不能为空")
        private String name;
        private Integer state;
        private String remark;
    }

    @Data
    static class BindStreetsRequest {
        @NotNull(message = "片区 ID 不能为空")
        private Long areaId;
        @NotEmpty(message = "街道 ID 列表不能为空")
        private List<Long> streetIds;
    }

    @Data
    static class UnbindStreetRequest {
        @NotNull(message = "片区 ID 不能为空")
        private Long areaId;
        @NotNull(message = "街道 ID 不能为空")
        private Long streetId;
    }

    @Data
    static class ParentInfoDTO {
        private Long areaId;
        private String areaName;
        private Long officeId;
        private String officeName;
        private Long regionId;
        private String regionName;
    }
}
```

- [ ] **Step 9: 编译验证**

```bash
mvn compile -q
```

预期：编译成功。

- [ ] **Step 10: 提交**

```bash
git add src/main/java/com/jihao/food/org/
git commit -m "feat: add organization module - region/office/district CRUD"
```

---

### Task 7: 行政区模块 — 实体、Mapper、Service、Controller

**Files:**
- Create: `src/main/java/com/jihao/food/area/entity/Area.java`
- Create: `src/main/java/com/jihao/food/area/mapper/AreaMapper.java`
- Create: `src/main/java/com/jihao/food/area/dto/AreaDTO.java`
- Create: `src/main/java/com/jihao/food/area/dto/AreaTreeDTO.java`
- Create: `src/main/java/com/jihao/food/area/dto/AreaDetailDTO.java`
- Create: `src/main/java/com/jihao/food/area/dto/AreaImportDTO.java`
- Create: `src/main/java/com/jihao/food/area/service/AreaService.java`
- Create: `src/main/java/com/jihao/food/area/service/AreaQueryService.java`
- Create: `src/main/java/com/jihao/food/area/controller/AdminAreaController.java`
- Create: `src/main/java/com/jihao/food/area/controller/AreaQueryController.java`

- [ ] **Step 1: 创建 Area.java**

```java
package com.jihao.food.area.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("area")
public class Area {

    @TableId
    private Long id;

    private Long pid;

    private Integer level;

    private String name;

    private String shortName;

    private String fullName;

    private String pinYin;

    private String adcode;

    private String zipCode;

    private BigDecimal lng;

    private BigDecimal lat;

    private String path;

    private String version;

    private Integer state;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 行政级别: 1-省 2-市 3-区县 4-乡镇街道 */
    public static final int LEVEL_PROVINCE = 1;
    public static final int LEVEL_CITY = 2;
    public static final int LEVEL_COUNTY = 3;
    public static final int LEVEL_TOWNSHIP = 4;
}
```

- [ ] **Step 2: 创建 AreaMapper.java**

```java
package com.jihao.food.area.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jihao.food.area.entity.Area;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AreaMapper extends BaseMapper<Area> {

    @Select("SELECT * FROM area WHERE pid = #{pid} AND state = #{state} ORDER BY id ASC")
    List<Area> selectByPid(@Param("pid") Long pid, @Param("state") Integer state);

    @Select("SELECT * FROM area WHERE level = #{level} AND state = #{state} ORDER BY id ASC")
    List<Area> selectByLevel(@Param("level") Integer level, @Param("state") Integer state);

    @Select("SELECT COUNT(*) FROM area WHERE pid = #{id} AND state = 1")
    int countChildren(Long id);

    @Select("SELECT * FROM area WHERE adcode = #{adcode}")
    Area selectByAdcode(String adcode);
}
```

- [ ] **Step 3: 创建 DTO 类**

```java
// AreaDTO.java
package com.jihao.food.area.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AreaDTO {

    private Long id;

    private String name;

    private String shortName;

    private String adcode;

    private Integer level;

    private BigDecimal lng;

    private BigDecimal lat;
}
```

```java
// AreaTreeDTO.java
package com.jihao.food.area.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AreaTreeDTO {

    private Long id;

    private Long parentId;

    private String name;

    private Integer level;

    private List<AreaTreeDTO> children;

    public void addChild(AreaTreeDTO child) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(child);
    }
}
```

```java
// AreaDetailDTO.java
package com.jihao.food.area.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AreaDetailDTO {

    private Long id;

    private String name;

    private String fullName;

    private Integer level;

    private Long provinceId;

    private String provinceName;

    private Long cityId;

    private String cityName;

    private Long countyId;

    private String countyName;

    private Long townshipId;

    private String townshipName;

    private BigDecimal lng;

    private BigDecimal lat;
}
```

```java
// AreaImportDTO.java
package com.jihao.food.area.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AreaImportDTO {

    /** 操作类型: add-新增, update-更新, delete-删除, merge-合并 */
    private String action;

    private String adcode;

    private String name;

    private String fullName;

    private Integer level;

    private String parentAdcode;

    private String reason;
}
```

- [ ] **Step 4: 创建 AreaService.java**

```java
package com.jihao.food.area.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jihao.food.area.dto.AreaImportDTO;
import com.jihao.food.area.entity.Area;
import com.jihao.food.area.mapper.AreaMapper;
import com.jihao.food.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AreaService {

    private final AreaMapper areaMapper;

    /**
     * 新增行政区
     */
    @Transactional
    public Area create(Area area) {
        Area existing = areaMapper.selectByAdcode(area.getAdcode());
        if (existing != null) {
            throw new BusinessException(400, "行政区划代码已存在");
        }

        if (area.getPid() != null && area.getPid() != 0) {
            Area parent = areaMapper.selectById(area.getPid());
            if (parent == null) {
                throw new BusinessException(400, "上级行政区不存在");
            }
            area.setLevel(parent.getLevel() + 1);
            area.setPath(parent.getPath() + "/" + area.getId());
        } else {
            area.setLevel(1);
            area.setPid(0L);
            area.setPath("0");
        }

        area.setState(1);
        areaMapper.insert(area);
        return area;
    }

    /**
     * 更新行政区
     */
    @Transactional
    public Area update(Long id, String name, String shortName, BigDecimal lng, BigDecimal lat) {
        Area area = areaMapper.selectById(id);
        if (area == null) {
            throw new BusinessException(404, "行政区不存在");
        }

        if (name != null) {
            area.setName(name);
        }
        if (shortName != null) {
            area.setShortName(shortName);
        }
        if (lng != null) {
            area.setLng(lng);
        }
        if (lat != null) {
            area.setLat(lat);
        }

        areaMapper.updateById(area);
        return area;
    }

    /**
     * 删除行政区
     */
    @Transactional
    public boolean delete(Long id) {
        Area area = areaMapper.selectById(id);
        if (area == null) {
            throw new BusinessException(404, "行政区不存在");
        }

        int childCount = areaMapper.countChildren(id);
        if (childCount > 0) {
            throw new BusinessException(400, "存在子级行政区，不可删除");
        }

        // TODO: 检查 area_relation 表中是否有绑定关系
        area.setState(0);
        areaMapper.updateById(area);
        return true;
    }

    /**
     * 启用/禁用行政区（联动子级）
     */
    @Transactional
    public void toggleStatus(Long id, Integer state) {
        Area area = areaMapper.selectById(id);
        if (area == null) {
            throw new BusinessException(404, "行政区不存在");
        }

        area.setState(state);
        areaMapper.updateById(area);

        // 禁用时联动子级
        if (state == 0) {
            disableChildren(id);
        }
    }

    private void disableChildren(Long parentId) {
        List<Area> children = areaMapper.selectByPid(parentId, 1);
        for (Area child : children) {
            child.setState(0);
            areaMapper.updateById(child);
            disableChildren(child.getId());
        }
    }

    /**
     * 导入预览（检测增量变更）
     */
    public List<AreaImportDTO> previewImport(List<Area> importData) {
        List<AreaImportDTO> result = new ArrayList<>();

        for (Area item : importData) {
            Area existing = areaMapper.selectByAdcode(item.getAdcode());
            if (existing == null) {
                AreaImportDTO dto = new AreaImportDTO();
                dto.setAction("add");
                dto.setAdcode(item.getAdcode());
                dto.setName(item.getName());
                dto.setFullName(item.getFullName());
                dto.setLevel(item.getLevel());
                dto.setParentAdcode("");
                dto.setReason("新增行政区");
                result.add(dto);
            } else {
                if (!existing.getName().equals(item.getName())) {
                    AreaImportDTO dto = new AreaImportDTO();
                    dto.setAction("update");
                    dto.setAdcode(item.getAdcode());
                    dto.setName(item.getName());
                    dto.setFullName(item.getFullName());
                    dto.setLevel(item.getLevel());
                    dto.setParentAdcode(existing.getAdcode());
                    dto.setReason("行政区名称变更: " + existing.getName() + " -> " + item.getName());
                    result.add(dto);
                }
            }
        }

        return result;
    }

    /**
     * 执行导入
     */
    @Transactional
    public int doImport(List<Area> importData) {
        int count = 0;
        for (Area item : importData) {
            Area existing = areaMapper.selectByAdcode(item.getAdcode());
            if (existing == null) {
                item.setState(1);
                areaMapper.insert(item);
                count++;
            } else if (!existing.getName().equals(item.getName())) {
                existing.setName(item.getName());
                existing.setFullName(item.getFullName());
                existing.setShortName(item.getShortName());
                areaMapper.updateById(existing);
                count++;
            }
        }
        return count;
    }

    /**
     * 查询已禁用的行政区
     */
    public Page<Area> listDisabled(int page, int size) {
        LambdaQueryWrapper<Area> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Area::getState, 0)
                .orderByDesc(Area::getUpdateTime);
        return areaMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
```

- [ ] **Step 5: 创建 AreaQueryService.java**

```java
package com.jihao.food.area.service;

import com.jihao.food.area.dto.AreaDTO;
import com.jihao.food.area.dto.AreaDetailDTO;
import com.jihao.food.area.dto.AreaTreeDTO;
import com.jihao.food.area.entity.Area;
import com.jihao.food.area.mapper.AreaMapper;
import com.jihao.food.common.util.TreeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AreaQueryService {

    private final AreaMapper areaMapper;

    /**
     * 按层级查询行政区列表
     */
    public List<AreaDTO> listByLevel(Integer level) {
        List<Area> areas = areaMapper.selectByLevel(level, 1);
        return areas.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 按父级 ID 查询下级行政区
     */
    public List<AreaDTO> listByParentId(Long parentId) {
        List<Area> areas = areaMapper.selectByPid(parentId, 1);
        return areas.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 查询详情（含完整上级路径）
     */
    public AreaDetailDTO getDetail(Long areaId) {
        Area area = areaMapper.selectById(areaId);
        if (area == null) {
            return null;
        }

        AreaDetailDTO dto = new AreaDetailDTO();
        dto.setId(area.getId());
        dto.setName(area.getName());
        dto.setFullName(area.getFullName());
        dto.setLevel(area.getLevel());
        dto.setLng(area.getLng());
        dto.setLat(area.getLat());

        buildPathInfo(dto, area);
        return dto;
    }

    /**
     * 查询行政区树
     */
    public List<AreaTreeDTO> getTree(Integer type, Long areaId, Integer depth) {
        List<Area> areas;
        if (areaId != null) {
            areas = areaMapper.selectByPid(areaId, 1);
        } else {
            areas = areaMapper.selectByLevel(1, 1);
        }

        List<AreaTreeDTO> nodes = areas.stream().map(this::toTreeDTO).collect(Collectors.toList());
        return TreeUtil.buildTree(
                nodes,
                AreaTreeDTO::getId,
                AreaTreeDTO::getParentId,
                AreaTreeDTO::getChildren,
                AreaTreeDTO::setChildren,
                node -> node.getParentId() == null || node.getParentId() == 0
        );
    }

    private void buildPathInfo(AreaDetailDTO dto, Area area) {
        // 递归查找完整路径
        Area current = area;
        if (current.getLevel() >= 1) {
            Area province = findAncestor(current, Area.LEVEL_PROVINCE);
            if (province != null) {
                dto.setProvinceId(province.getId());
                dto.setProvinceName(province.getName());
            }
        }
        if (current.getLevel() >= 2) {
            Area city = findAncestor(current, Area.LEVEL_CITY);
            if (city != null) {
                dto.setCityId(city.getId());
                dto.setCityName(city.getName());
            }
        }
        if (current.getLevel() >= 3) {
            Area county = findAncestor(current, Area.LEVEL_COUNTY);
            if (county != null) {
                dto.setCountyId(county.getId());
                dto.setCountyName(county.getName());
            }
        }
        if (current.getLevel() >= 4) {
            dto.setTownshipId(current.getId());
            dto.setTownshipName(current.getName());
        }
    }

    private Area findAncestor(Area area, int targetLevel) {
        if (area.getLevel() == targetLevel) {
            return area;
        }
        if (area.getPid() == null || area.getPid() == 0) {
            return null;
        }
        Area parent = areaMapper.selectById(area.getPid());
        if (parent == null) {
            return null;
        }
        if (parent.getLevel() == targetLevel) {
            return parent;
        }
        return findAncestor(parent, targetLevel);
    }

    private AreaDTO toDTO(Area area) {
        return new AreaDTO(
                area.getId(),
                area.getName(),
                area.getShortName(),
                area.getAdcode(),
                area.getLevel(),
                area.getLng(),
                area.getLat()
        );
    }

    private AreaTreeDTO toTreeDTO(Area area) {
        return new AreaTreeDTO(area.getId(), area.getPid(), area.getName(), area.getLevel(), null);
    }
}
```

- [ ] **Step 6: 创建 AdminAreaController.java**

```java
package com.jihao.food.area.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jihao.food.area.dto.AreaImportDTO;
import com.jihao.food.area.entity.Area;
import com.jihao.food.area.service.AreaService;
import com.jihao.food.common.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/admin-area")
@RequiredArgsConstructor
public class AdminAreaController {

    private final AreaService areaService;

    @GetMapping("/tree")
    public Result<List<com.jihao.food.area.dto.AreaTreeDTO>> tree(
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false, defaultValue = "1") Integer depth) {
        return Result.success(areaQueryService.getTree(type, areaId, depth));
    }

    @GetMapping("/detail")
    public Result<com.jihao.food.area.dto.AreaDetailDTO> detail(@RequestParam Long id) {
        return Result.success(areaQueryService.getDetail(id));
    }

    @PostMapping("/create")
    public Result<Area> create(@Validated @RequestBody CreateAreaRequest request) {
        Area area = new Area();
        area.setId(request.getId());
        area.setPid(request.getPid());
        area.setName(request.getName());
        area.setShortName(request.getShortName());
        area.setFullName(request.getFullName());
        area.setAdcode(request.getAdcode());
        area.setZipCode(request.getZipCode());
        area.setLng(request.getLng());
        area.setLat(request.getLat());
        Area created = areaService.create(area);
        return Result.success(created);
    }

    @PostMapping("/update")
    public Result<Area> update(@Validated @RequestBody UpdateAreaRequest request) {
        Area updated = areaService.update(request.getId(), request.getName(),
                request.getShortName(), request.getLng(), request.getLat());
        return Result.success(updated);
    }

    @PostMapping("/delete")
    public Result<Void> delete(@RequestParam Long id) {
        areaService.delete(id);
        return Result.success(null);
    }

    @PostMapping("/status")
    public Result<Void> toggleStatus(@RequestParam Long id, @RequestParam Integer state) {
        areaService.toggleStatus(id, state);
        return Result.success(null);
    }

    @PostMapping("/import")
    public Result<Integer> doImport(@Valid @RequestBody List<Area> importData) {
        int count = areaService.doImport(importData);
        return Result.success(count);
    }

    @PostMapping("/import-preview")
    public Result<List<AreaImportDTO>> importPreview(@Valid @RequestBody List<Area> importData) {
        List<AreaImportDTO> preview = areaService.previewImport(importData);
        return Result.success(preview);
    }

    @GetMapping("/disabled")
    public Result<Page<Area>> listDisabled(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(areaService.listDisabled(page, size));
    }

    @Data
    static class CreateAreaRequest {
        private Long id;
        private Long pid;
        @NotNull(message = "行政区名称不能为空")
        private String name;
        private String shortName;
        private String fullName;
        private String adcode;
        private String zipCode;
        private BigDecimal lng;
        private BigDecimal lat;
    }

    @Data
    static class UpdateAreaRequest {
        private Long id;
        private String name;
        private String shortName;
        private BigDecimal lng;
        private BigDecimal lat;
    }
}
```

- [ ] **Step 7: 创建 AreaQueryController.java**

```java
package com.jihao.food.area.controller;

import com.jihao.food.area.dto.AreaDTO;
import com.jihao.food.area.dto.AreaDetailDTO;
import com.jihao.food.area.dto.AreaTreeDTO;
import com.jihao.food.area.service.AreaQueryService;
import com.jihao.food.common.Result;
import com.jihao.food.common.annotation.IgnoreAuth;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/area")
@RequiredArgsConstructor
@IgnoreAuth
public class AreaQueryController {

    private final AreaQueryService areaQueryService;

    @GetMapping("/provinces")
    public Result<List<AreaDTO>> provinces() {
        return Result.success(areaQueryService.listByLevel(1));
    }

    @GetMapping("/cities")
    public Result<List<AreaDTO>> cities(@RequestParam Long provinceId) {
        return Result.success(areaQueryService.listByParentId(provinceId));
    }

    @GetMapping("/counties")
    public Result<List<AreaDTO>> counties(@RequestParam Long cityId) {
        return Result.success(areaQueryService.listByParentId(cityId));
    }

    @GetMapping("/townships")
    public Result<List<AreaDTO>> townships(@RequestParam Long countyId) {
        return Result.success(areaQueryService.listByParentId(countyId));
    }

    @GetMapping("/province-list")
    public Result<List<AreaDTO>> provinceList() {
        return Result.success(areaQueryService.listByLevel(1));
    }

    @GetMapping("/city-list")
    public Result<List<AreaDTO>> cityList() {
        return Result.success(areaQueryService.listByLevel(2));
    }

    @GetMapping("/county-list")
    public Result<List<AreaDTO>> countyList() {
        return Result.success(areaQueryService.listByLevel(3));
    }

    @GetMapping("/township-list")
    public Result<List<AreaDTO>> townshipList() {
        return Result.success(areaQueryService.listByLevel(4));
    }

    @GetMapping("/detail")
    public Result<AreaDetailDTO> detail(@RequestParam Long areaId) {
        return Result.success(areaQueryService.getDetail(areaId));
    }

    @GetMapping("/tree")
    public Result<List<AreaTreeDTO>> tree(
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false, defaultValue = "1") Integer depth) {
        return Result.success(areaQueryService.getTree(type, areaId, depth));
    }
}
```

- [ ] **Step 8: 编译验证**

```bash
mvn compile -q
```

预期：编译成功。

- [ ] **Step 9: 提交**

```bash
git add src/main/java/com/jihao/food/area/
git commit -m "feat: add area module - admin CRUD + public query APIs"
```

---

### Task 8: 映射关系模块 — 实体、Mapper、Service、Controller

**Files:**
- Create: `src/main/java/com/jihao/food/relation/entity/AreaRelation.java`
- Create: `src/main/java/com/jihao/food/relation/mapper/AreaRelationMapper.java`
- Create: `src/main/java/com/jihao/food/relation/service/AreaRelationService.java`
- Create: `src/main/java/com/jihao/food/relation/controller/StreetController.java`
- Create: `src/main/java/com/jihao/food/relation/controller/GeoQueryController.java`

- [ ] **Step 1: 创建 AreaRelation.java**

```java
package com.jihao.food.relation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("area_relation")
public class AreaRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long areaId;

    private Long orgId;

    private Integer delFlag;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

- [ ] **Step 2: 创建 AreaRelationMapper.java**

```java
package com.jihao.food.relation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jihao.food.relation.entity.AreaRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AreaRelationMapper extends BaseMapper<AreaRelation> {

    @Select("SELECT * FROM area_relation WHERE org_id = #{orgId} AND del_flag = 0")
    List<AreaRelation> selectByOrgId(Long orgId);

    @Select("SELECT COUNT(*) FROM area_relation WHERE org_id = #{orgId} AND del_flag = 0")
    int countByOrgId(Long orgId);

    @Select("SELECT * FROM area_relation WHERE area_id = #{areaId} AND org_id = #{orgId} AND del_flag = 0")
    AreaRelation selectByAreaIdAndOrgId(@Param("areaId") Long areaId, @Param("orgId") Long orgId);

    @Select("SELECT ar.* FROM area_relation ar " +
            "JOIN organization o ON ar.org_id = o.id " +
            "WHERE ar.area_id = #{areaId} AND o.type = 3 AND ar.del_flag = 0")
    List<AreaRelation> selectByAreaId(Long areaId);
}
```

- [ ] **Step 3: 创建 AreaRelationService.java**

```java
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

    /**
     * 查询片区管辖街道
     */
    public List<AreaRelation> listByDistrictId(Long districtId) {
        return areaRelationMapper.selectByOrgId(districtId);
    }

    /**
     * 为片区批量绑定街道
     */
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

    /**
     * 解除片区与街道的绑定
     */
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

    /**
     * 根据行政区 ID 查询所属组织
     */
    public List<AreaRelation> getOrgByAreaId(Long areaId) {
        return areaRelationMapper.selectByAreaId(areaId);
    }
}
```

- [ ] **Step 4: 创建 StreetController.java（/api/street/list）**

```java
package com.jihao.food.relation.controller;

import com.jihao.food.common.Result;
import com.jihao.food.relation.entity.AreaRelation;
import com.jihao.food.relation.service.AreaRelationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/street")
@RequiredArgsConstructor
public class StreetController {

    private final AreaRelationService areaRelationService;

    @GetMapping("/list")
    public Result<List<AreaRelation>> list(@RequestParam(required = false) Long areaId) {
        if (areaId == null) {
            return Result.success(List.of());
        }
        return Result.success(areaRelationService.listByDistrictId(areaId));
    }
}
```

- [ ] **Step 5: 创建 GeoQueryController.java（/api/geo/*）**

```java
package com.jihao.food.relation.controller;

import com.jihao.food.area.entity.Area;
import com.jihao.food.area.mapper.AreaMapper;
import com.jihao.food.common.Result;
import com.jihao.food.common.annotation.IgnoreAuth;
import com.jihao.food.org.entity.Organization;
import com.jihao.food.org.mapper.OrganizationMapper;
import com.jihao.food.relation.entity.AreaRelation;
import com.jihao.food.relation.service.AreaRelationService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/geo")
@RequiredArgsConstructor
@IgnoreAuth
public class GeoQueryController {

    private final AreaRelationService areaRelationService;
    private final OrganizationMapper organizationMapper;
    private final AreaMapper areaMapper;

    /**
     * 根据行政区 ID 查询所属组织
     */
    @GetMapping("/org")
    public Result<OrgInfoDTO> getOrgByAreaId(@RequestParam Long areaId) {
        List<AreaRelation> relations = areaRelationService.getOrgByAreaId(areaId);
        if (relations == null || relations.isEmpty()) {
            return Result.success(null);
        }

        AreaRelation relation = relations.get(0);
        Organization district = organizationMapper.selectById(relation.getOrgId());
        if (district == null) {
            return Result.success(null);
        }

        OfficeInfo office = findParent(district.getParentId(), Organization.TYPE_OFFICE);
        RegionInfo region = findParent(office != null ? office.getOfficeParentId() : district.getParentId(), Organization.TYPE_REGION);

        Area area = areaMapper.selectById(areaId);

        OrgInfoDTO dto = new OrgInfoDTO();
        dto.setAreaId(areaId);
        dto.setAreaName(area != null ? area.getName() : "");
        dto.setOrgId(district.getId());
        dto.setOrgName(district.getName());
        if (office != null) {
            dto.setOfficeId(office.getId());
            dto.setOfficeName(office.getName());
        }
        if (region != null) {
            dto.setRegionId(region.getId());
            dto.setRegionName(region.getName());
        }
        return Result.success(dto);
    }

    /**
     * 根据街道行政区 ID 查询所属片区
     */
    @GetMapping("/org/by-township")
    public Result<TownshipOrgDTO> getOrgByTownshipId(@RequestParam Long townshipId) {
        List<AreaRelation> relations = areaRelationService.getOrgByAreaId(townshipId);
        if (relations == null || relations.isEmpty()) {
            return Result.success(null);
        }

        AreaRelation relation = relations.get(0);
        Organization district = organizationMapper.selectById(relation.getOrgId());
        if (district == null) {
            return Result.success(null);
        }

        Area area = areaMapper.selectById(townshipId);
        OfficeInfo office = findParent(district.getParentId(), Organization.TYPE_OFFICE);
        RegionInfo region = findParent(office != null ? office.getOfficeParentId() : district.getParentId(), Organization.TYPE_REGION);

        TownshipOrgDTO dto = new TownshipOrgDTO();
        dto.setTownshipId(townshipId);
        dto.setTownshipName(area != null ? area.getName() : "");
        if (region != null) {
            dto.setRegionId(region.getId());
            dto.setRegionName(region.getName());
        }
        if (office != null) {
            dto.setOfficeId(office.getId());
            dto.setOfficeName(office.getName());
        }
        dto.setDistrictId(district.getId());
        dto.setDistrictName(district.getName());
        return Result.success(dto);
    }

    private Organization findParent(Long id, int type) {
        if (id == null || id == 0) return null;
        Organization org = organizationMapper.selectById(id);
        if (org != null && org.getType() == type) {
            return org;
        }
        if (org != null) {
            return findParent(org.getParentId(), type);
        }
        return null;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class OrgInfoDTO {
        private Long areaId;
        private String areaName;
        private Long orgId;
        private String orgName;
        private Long officeId;
        private String officeName;
        private Long regionId;
        private String regionName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TownshipOrgDTO {
        private Long townshipId;
        private String townshipName;
        private Long regionId;
        private String regionName;
        private Long officeId;
        private String officeName;
        private Long districtId;
        private String districtName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class OfficeInfo {
        private Long id;
        private String name;
        private Long officeParentId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class RegionInfo {
        private Long id;
        private String name;
    }
}
```

- [ ] **Step 6: 编译验证**

```bash
mvn compile -q
```

预期：编译成功。

- [ ] **Step 7: 提交**

```bash
git add src/main/java/com/jihao/food/relation/
git commit -m "feat: add relation module - street binding + geo query APIs"
```

---

### Task 9: 组织树查询接口

**Files:**
- Create: `src/main/java/com/jihao/food/org/controller/OrgTreeController.java`

- [ ] **Step 1: 创建 OrgTreeController.java**

```java
package com.jihao.food.org.controller;

import com.jihao.food.common.Result;
import com.jihao.food.org.dto.OrganizationTreeDTO;
import com.jihao.food.org.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/org")
@RequiredArgsConstructor
public class OrgTreeController {

    private final OrganizationService organizationService;

    @GetMapping("/tree")
    public Result<List<OrganizationTreeDTO>> tree(
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Integer state) {
        return Result.success(organizationService.getTree(regionId, state));
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn compile -q
```

预期：编译成功。

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/jihao/food/org/controller/OrgTreeController.java
git commit -m "feat: add org tree API"
```

---

### Task 10: 启动测试与文档

- [ ] **Step 1: 完整编译**

```bash
mvn clean compile -q
```

- [ ] **Step 2: 确认所有模块文件存在**

```bash
find src/main/java -name "*.java" | sort
```

预期输出：

```
src/main/java/com/jihao/food/FoodProjectApplication.java
src/main/java/com/jihao/food/area/controller/AdminAreaController.java
src/main/java/com/jihao/food/area/controller/AreaQueryController.java
src/main/java/com/jihao/food/area/dto/AreaDTO.java
src/main/java/com/jihao/food/area/dto/AreaDetailDTO.java
src/main/java/com/jihao/food/area/dto/AreaImportDTO.java
src/main/java/com/jihao/food/area/dto/AreaTreeDTO.java
src/main/java/com/jihao/food/area/entity/Area.java
src/main/java/com/jihao/food/area/mapper/AreaMapper.java
src/main/java/com/jihao/food/area/service/AreaQueryService.java
src/main/java/com/jihao/food/area/service/AreaService.java
src/main/java/com/jihao/food/auth/controller/AuthController.java
src/main/java/com/jihao/food/auth/dto/LoginRequest.java
src/main/java/com/jihao/food/auth/dto/LoginResponse.java
src/main/java/com/jihao/food/auth/dto/UserInfoDTO.java
src/main/java/com/jihao/food/auth/service/AuthService.java
src/main/java/com/jihao/food/common/Result.java
src/main/java/com/jihao/food/common/annotation/ApiSign.java
src/main/java/com/jihao/food/common/annotation/IgnoreAuth.java
src/main/java/com/jihao/food/common/annotation/IgnoreSign.java
src/main/java/com/jihao/food/common/exception/BusinessException.java
src/main/java/com/jihao/food/common/exception/GlobalExceptionHandler.java
src/main/java/com/jihao/food/common/util/JwtUtil.java
src/main/java/com/jihao/food/common/util/SignUtil.java
src/main/java/com/jihao/food/common/util/TreeUtil.java
src/main/java/com/jihao/food/config/MybatisPlusConfig.java
src/main/java/com/jihao/food/config/RequestCachingFilter.java
src/main/java/com/jihao/food/config/WebMvcConfig.java
src/main/java/com/jihao/food/config/JacksonConfig.java
src/main/java/com/jihao/food/interceptor/ApiSignInterceptor.java
src/main/java/com/jihao/food/interceptor/AuthInterceptor.java
src/main/java/com/jihao/food/org/controller/DistrictController.java
src/main/java/com/jihao/food/org/controller/OfficeController.java
src/main/java/com/jihao/food/org/controller/OrgTreeController.java
src/main/java/com/jihao/food/org/controller/RegionController.java
src/main/java/com/jihao/food/org/dto/OrganizationDTO.java
src/main/java/com/jihao/food/org/dto/OrganizationTreeDTO.java
src/main/java/com/jihao/food/org/entity/Organization.java
src/main/java/com/jihao/food/org/mapper/OrganizationMapper.java
src/main/java/com/jihao/food/org/service/OrganizationService.java
src/main/java/com/jihao/food/relation/controller/AreaRelationController.java
src/main/java/com/jihao/food/relation/entity/AreaRelation.java
src/main/java/com/jihao/food/relation/mapper/AreaRelationMapper.java
src/main/java/com/jihao/food/relation/service/AreaRelationService.java
src/main/java/com/jihao/food/system/entity/SysApiKey.java
src/main/java/com/jihao/food/system/entity/SysUser.java
src/main/java/com/jihao/food/system/mapper/SysApiKeyMapper.java
src/main/java/com/jihao/food/system/mapper/SysUserMapper.java
```

- [ ] **Step 3: 最终提交**

```bash
git add -A
git commit -m "feat: complete Java backend framework scaffold

- Spring Boot 3 + MyBatis-Plus project scaffold
- Common components: Result, exceptions, utils (Sign/JWT/Tree), annotations
- Config: MyBatis-Plus, WebMvc, Jackson, RequestCachingFilter
- Interceptors: Auth (JWT) + ApiSign (MD5)
- Modules: auth, org (region/office/district), area (admin+query), relation
- System entities: SysUser, SysApiKey
- All API routes per design spec
- Swagger/OpenAPI documentation enabled"
```
