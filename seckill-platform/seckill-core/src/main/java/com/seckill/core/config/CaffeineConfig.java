package com.seckill.core.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.seckill.core.model.vo.SeckillProductVO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine 本地缓存配置
 * <p>
 * 三级缓存策略: Caffeine(本地) → Redis(分布式) → MySQL
 */
@Configuration
public class CaffeineConfig {

    /**
     * 秒杀商品列表本地缓存
     * 过期策略: 写入后 10 秒自动刷新
     * 最大容量: 100 条
     */
    @Bean
    public Cache<String, List<SeckillProductVO>> seckillProductCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.SECONDS)
                .maximumSize(100)
                .recordStats()
                .build();
    }
}
