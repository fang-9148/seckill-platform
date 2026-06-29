package com.seckill.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * 线程池配置
 * <p>
 * 自定义线程池用于异步任务，如库存预热、日志记录等。
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * 秒杀业务线程池
     * 核心线程 = CPU 核数 * 2
     * 最大线程 = CPU 核数 * 4
     * 队列容量 = 1000
     * 拒绝策略: CallerRunsPolicy（交由调用者线程执行）
     */
    @Bean("seckillThreadPool")
    public ThreadPoolExecutor seckillThreadPool() {
        int cores = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolExecutor(
                cores * 2,
                cores * 4,
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1000),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
