package com.seckill.core.aop;

import com.seckill.common.enums.ResultCode;
import com.seckill.common.exception.BizException;
import com.seckill.common.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collections;

/**
 * 限流切面实现
 * <p>
 * 基于 Redis Lua 脚本的固定时间窗口计数器限流。
 * 每个方法独立一个限流 key，利用 Redis 单线程原子性保证计数精确。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimiterAspect {

    private final RedisUtil redisUtil;

    /** Redis 限流 key 前缀 */
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    /**
     * 固定窗口计数器限流 Lua 脚本
     * <pre>
     * KEYS[1] — 限流 key（格式: rate_limit:{method}:{timestamp_seconds}）
     * ARGV[1] — 窗口内最大请求数
     * ARGV[2] — 窗口大小（秒）
     *
     * 返回 1 表示通过，0 表示被限流
     * </pre>
     */
    private static final String RATE_LIMIT_LUA =
            "local key = KEYS[1] " +
            "local limit = tonumber(ARGV[1]) " +
            "local window = tonumber(ARGV[2]) " +
            "local current = redis.call('INCR', key) " +
            "if current == 1 then " +
            "    redis.call('EXPIRE', key, window) " +
            "end " +
            "if current > limit then " +
            "    return 0 " +
            "end " +
            "return 1";

    @Pointcut("@annotation(com.seckill.core.aop.RateLimiter)")
    public void rateLimiterPointcut() {}

    @Around("rateLimiterPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimiter annotation = method.getAnnotation(RateLimiter.class);

        String methodKey = method.getDeclaringClass().getSimpleName() + "." + method.getName();

        // 以秒级时间戳构造限流 key，每个时间窗口内独立计数
        long currentWindow = System.currentTimeMillis() / 1000 / annotation.windowSeconds();
        String rateLimitKey = RATE_LIMIT_KEY_PREFIX + methodKey + ":" + currentWindow;

        // 执行 Redis Lua 脚本原子判断
        Long result = redisUtil.executeLuaScript(
                RATE_LIMIT_LUA,
                Collections.singletonList(rateLimitKey),
                annotation.permitsPerWindow(),
                annotation.windowSeconds()
        );

        if (result == null || result == 0) {
            log.warn("[限流触发] method={}, permitsPerWindow={}, windowSeconds={}",
                    methodKey, annotation.permitsPerWindow(), annotation.windowSeconds());
            throw new BizException(ResultCode.TOO_MANY_REQUESTS, annotation.message());
        }

        return joinPoint.proceed();
    }
}
