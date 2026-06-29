package com.seckill.core.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

/**
 * Web 请求日志切面
 * <p>
 * 记录所有 Controller 方法的请求参数、响应结果和耗时，
 * 方便排查问题。
 */
@Slf4j
@Aspect
@Component
public class WebLogAspect {

    @Pointcut("execution(* com.seckill.core.controller..*(..))")
    public void controllerLog() {}

    @Around("controllerLog()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        String uri = "unknown";
        String method = "unknown";
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            uri = request.getRequestURI();
            method = request.getMethod();
        }

        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long elapsed = System.currentTimeMillis() - startTime;

        log.info("[WEB] {} {} | {}.{} | {}ms",
                method, uri, className, methodName, elapsed);

        if (elapsed > 500) {
            log.warn("[慢请求] {} {} | {}.{} | {}ms | args={}",
                    method, uri, className, methodName, elapsed,
                    Arrays.toString(joinPoint.getArgs()));
        }

        return result;
    }
}
