package com.seckill.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.seckill.common.enums.ResultCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一响应体
 * <p>
 * 前端接收格式统一为：
 * <pre>
 * {
 *   "code": 200,
 *   "message": "操作成功",
 *   "data": { ... }
 * }
 * </pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;
    private String message;
    private T data;
    private Long timestamp;

    // ==================== 成功响应 ====================

    public static <T> Result<T> success() {
        return build(ResultCode.SUCCESS, null);
    }

    public static <T> Result<T> success(T data) {
        return build(ResultCode.SUCCESS, data);
    }

    public static <T> Result<T> success(String message, T data) {
        Result<T> result = build(ResultCode.SUCCESS, data);
        result.setMessage(message);
        return result;
    }

    // ==================== 失败响应 ====================

    public static <T> Result<T> error() {
        return build(ResultCode.INTERNAL_ERROR, null);
    }

    public static <T> Result<T> error(String message) {
        Result<T> result = build(ResultCode.INTERNAL_ERROR, null);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> error(ResultCode resultCode) {
        return build(resultCode, null);
    }

    public static <T> Result<T> error(ResultCode resultCode, String message) {
        Result<T> result = build(resultCode, null);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> error(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }

    // ==================== 快捷判断 ====================

    public boolean isSuccess() {
        return this.code == ResultCode.SUCCESS.getCode();
    }

    // ==================== 私有构建方法 ====================

    private static <T> Result<T> build(ResultCode resultCode, T data) {
        Result<T> result = new Result<>();
        result.setCode(resultCode.getCode());
        result.setMessage(resultCode.getMessage());
        result.setData(data);
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }
}
