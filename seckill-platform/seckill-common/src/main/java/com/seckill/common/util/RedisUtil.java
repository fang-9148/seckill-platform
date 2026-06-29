package com.seckill.common.util;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 * <p>
 * 封装常用的 Redis 操作，包括分布式锁、原子扣减等。
 */
@Component
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisUtil(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ==================== 基础操作 ====================

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public <T> T get(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        return clazz.cast(value);
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    public Long getExpire(String key, TimeUnit unit) {
        return redisTemplate.getExpire(key, unit);
    }

    // ==================== 计数器操作 ====================

    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    public Long decrement(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    public Long decrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    // ==================== Lua 脚本执行 ====================

    /**
     * 执行 Lua 脚本
     * <p>
     * 典型用法：原子性判断库存并扣减，防止超卖。
     */
    public <T> T executeLuaScript(String script, List<String> keys, Object... args) {
        DefaultRedisScript<T> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        return redisTemplate.execute(redisScript, keys, args);
    }

    // ==================== 原子扣库存 Lua 脚本 ====================

    /**
     * 秒杀扣库存 Lua 脚本
     * KEYS[1] — 商品库存 key
     * 返回 1 表示扣减成功，0 表示库存不足
     */
    public static final String SECKILL_DECREASE_STOCK_LUA =
            "local stock = redis.call('get', KEYS[1]) " +
            "if stock and tonumber(stock) > 0 then " +
            "    redis.call('decr', KEYS[1]) " +
            "    return 1 " +
            "else " +
            "    return 0 " +
            "end";

    /**
     * 原子扣减秒杀库存
     *
     * @param stockKey Redis 中存储库存的 key
     * @return true=扣减成功, false=库存不足
     */
    public boolean decreaseStockAtomic(String stockKey) {
        Long result = executeLuaScript(
                SECKILL_DECREASE_STOCK_LUA,
                Collections.singletonList(stockKey)
        );
        return result != null && result == 1;
    }
}
