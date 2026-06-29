package com.seckill.common.exception;

import com.seckill.common.enums.ResultCode;
import lombok.Getter;

/**
 * 业务异常基类
 * <p>
 * 全局异常处理器根据此异常类型返回对应的错误码和消息给前端。
 * 项目中所有业务异常均应抛出此类或其子类。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message) {
        super(message);
        this.code = ResultCode.INTERNAL_ERROR.getCode();
    }

    public BizException(String message, Throwable cause) {
        super(message, cause);
        this.code = ResultCode.INTERNAL_ERROR.getCode();
    }
}
