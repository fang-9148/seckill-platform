package com.seckill.core.aop;

import java.lang.annotation.*;

/**
 * 接口限流注解
 * <p>
 * 基于 Redis Lua 脚本的固定时间窗口计数器算法实现。
 * 标注在 Controller 方法上即可自动限流。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiter {

    /** 每个时间窗口内允许的最大请求数 */
    int permitsPerWindow() default 100;

    /** 时间窗口大小（秒） */
    int windowSeconds() default 1;

    /** 限流提示消息 */
    String message() default "请求过于频繁，请稍后再试";
}
