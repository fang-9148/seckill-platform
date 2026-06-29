package com.seckill.common.constant;

/**
 * Redis Key 统一常量
 * <p>
 * 所有 Redis Key 集中管理，避免散落在代码中的硬编码。
 */
public final class RedisKeyConstant {

    private RedisKeyConstant() {}

    /** 秒杀商品库存前缀，格式：seckill:stock:{productId} */
    public static final String SECKILL_STOCK_PREFIX = "seckill:stock:";

    /** 秒杀商品标记位（是否已抢完），格式：seckill:over:{productId} */
    public static final String SECKILL_OVER_PREFIX = "seckill:over:";

    /** 用户已下单标记（防重复下单），格式：seckill:order:{userId}:{productId} */
    public static final String SECKILL_ORDER_PREFIX = "seckill:order:";

    /** 秒杀商品预热缓存，格式：seckill:product:{productId} */
    public static final String SECKILL_PRODUCT_PREFIX = "seckill:product:";

    /** 分布式锁前缀，格式：lock:seckill:{productId} */
    public static final String SECKILL_LOCK_PREFIX = "lock:seckill:";

    /** 用户 Token 黑名单前缀（登出后失效） */
    public static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    /** 接口限流前缀，格式：ratelimit:{userId}:{uri} */
    public static final String RATE_LIMIT_PREFIX = "ratelimit:";

    // ==================== 工具方法 ====================

    public static String stockKey(long productId) {
        return SECKILL_STOCK_PREFIX + productId;
    }

    public static String overKey(long productId) {
        return SECKILL_OVER_PREFIX + productId;
    }

    public static String orderKey(long userId, long productId) {
        return SECKILL_ORDER_PREFIX + userId + ":" + productId;
    }

    public static String productKey(long productId) {
        return SECKILL_PRODUCT_PREFIX + productId;
    }

    public static String lockKey(long productId) {
        return SECKILL_LOCK_PREFIX + productId;
    }
}
