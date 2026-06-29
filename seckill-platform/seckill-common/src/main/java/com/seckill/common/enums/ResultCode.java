package com.seckill.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一响应状态码枚举
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // 2xx — 成功
    SUCCESS(200, "操作成功"),

    // 4xx — 客户端错误
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或Token已失效"),
    FORBIDDEN(403, "无访问权限"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),

    // 5xx — 服务端错误
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),

    // 1xxx — 用户模块
    USER_NOT_EXIST(1001, "用户不存在"),
    PASSWORD_ERROR(1002, "密码错误"),
    USER_ALREADY_EXISTS(1003, "用户已存在"),
    USER_BANNED(1004, "账号已被禁用"),

    // 2xxx — 秒杀模块
    SECKILL_NOT_STARTED(2001, "秒杀活动尚未开始"),
    SECKILL_ENDED(2002, "秒杀活动已结束"),
    STOCK_NOT_ENOUGH(2003, "库存不足"),
    SECKILL_DUPLICATE(2004, "您已参与过本轮秒杀，请勿重复下单"),
    SECKILL_IN_QUEUE(2005, "秒杀请求已入队，请等待结果"),
    SECKILL_FAILED(2006, "秒杀失败"),

    // 3xxx — 订单模块
    ORDER_NOT_EXIST(3001, "订单不存在"),
    ORDER_STATUS_ERROR(3002, "订单状态异常"),
    ORDER_PAY_TIMEOUT(3003, "订单支付超时"),

    // 4xxx — 商品模块
    PRODUCT_NOT_EXIST(4001, "商品不存在"),
    PRODUCT_OFFLINE(4002, "商品已下架");

    private final int code;
    private final String message;
}
