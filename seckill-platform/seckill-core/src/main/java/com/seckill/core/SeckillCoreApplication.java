package com.seckill.core;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 秒杀核心服务 — 启动类
 *
 * <pre>
 * 技术栈：
 *   - Spring Boot 3.2         基础框架
 *   - MyBatis-Plus            持久层
 *   - Spring Data JPA         基础 CRUD
 *   - Redis                   缓存 + 原子库存扣减 + 接口限流
 *   - Redisson                分布式锁
 *   - LinkedBlockingQueue     内存队列削峰
 *   - Caffeine                本地缓存（三级缓存）
 *   - Knife4j                 API 文档
 * </pre>
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.seckill"})
@MapperScan(basePackages = {"com.seckill.core.mapper"})
@EnableCaching
@EnableAsync
@EnableScheduling
public class SeckillCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeckillCoreApplication.class, args);
        System.out.println("""

                ╔══════════════════════════════════════════════╗
                ║  ⚡ Seckill Core — 秒杀核心服务启动成功 ⚡  ║
                ║  API 文档: http://localhost:9091/doc.html   ║
                ╚══════════════════════════════════════════════╝
                """);
    }
}
