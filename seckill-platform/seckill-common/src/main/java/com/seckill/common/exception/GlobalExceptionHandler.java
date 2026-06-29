package com.seckill.common.exception;

import com.seckill.common.enums.ResultCode;
import com.seckill.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * <p>
 * 统一拦截所有异常，返回标准的 Result 格式响应，
 * 避免异常信息直接暴露给前端。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== 业务异常 ====================

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e, HttpServletRequest request) {
        log.warn("[业务异常] path={}, code={}, message={}", request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    // ==================== 参数校验异常 ====================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        log.warn("[参数校验失败] path={}, message={}", request.getRequestURI(), message);
        return Result.error(ResultCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e, HttpServletRequest request) {
        log.warn("[参数绑定失败] path={}, message={}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        log.warn("[约束校验失败] path={}, message={}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingParamException(MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("[缺少参数] path={}, param={}", request.getRequestURI(), e.getParameterName());
        return Result.error(ResultCode.BAD_REQUEST, "缺少必要参数: " + e.getParameterName());
    }

    // ==================== 请求方法不支持 ====================

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("[方法不支持] path={}, method={}", request.getRequestURI(), e.getMethod());
        return Result.error(ResultCode.METHOD_NOT_ALLOWED);
    }

    // ==================== 未知异常兜底 ====================

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleUnknownException(Exception e, HttpServletRequest request) {
        log.error("[系统异常] path={}, type={}, message={}", request.getRequestURI(),
                e.getClass().getSimpleName(), e.getMessage(), e);
        return Result.error(ResultCode.INTERNAL_ERROR);
    }
}
