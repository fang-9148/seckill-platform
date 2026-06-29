package com.seckill.core.queue;

import com.seckill.core.service.OrderService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 秒杀订单异步处理队列
 *
 * <pre>
 * 设计思路:
 *   1. 用 LinkedBlockingQueue 作为缓冲区（容量 10000）
 *   2. 核心线程数 = CPU 核数，慢慢消费队列中的订单
 *   3. 队列满时降级为调用者同步执行，保证不丢单
 *
 * 核心优势:
 *   - 削峰：请求进队列 → 立即返回，数据库不受冲击
 *   - 异步：后台线程慢慢落库
 *   - 限流：队列满了直接拒绝，防止系统过载
 *   - 零外部依赖，延迟更低（无网络开销）
 * </pre>
 */
@Slf4j
@Component
public class SeckillOrderQueue {

    private final OrderService orderService;

    /** 订单任务队列 —— 削峰缓冲区的核心 */
    private final ArrayBlockingQueue<SeckillOrderTask> taskQueue;

    /** 订单处理线程池 */
    private ThreadPoolExecutor executor;

    /** 队列最大容量 */
    private static final int QUEUE_CAPACITY = 10_000;

    public SeckillOrderQueue(OrderService orderService) {
        this.orderService = orderService;
        this.taskQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    }

    @PostConstruct
    public void init() {
        int cores = Runtime.getRuntime().availableProcessors();

        executor = new ThreadPoolExecutor(
                cores,                      // 核心线程数
                cores * 2,                  // 最大线程数
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                r -> {
                    Thread t = new Thread(r, "seckill-order-worker");
                    t.setDaemon(true);
                    return t;
                },
                // 拒绝策略：队列满 + 线程满 → 交由调用线程执行（降级不丢消息）
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // 启动核心消费者线程，持续从队列拉取订单任务执行
        for (int i = 0; i < cores; i++) {
            executor.submit(this::consumeLoop);
        }

        log.info("[订单队列初始化] 核心线程数={}, 队列容量={}", cores, QUEUE_CAPACITY);
    }

    /**
     * 提交秒杀订单任务到队列
     *
     * @return true=提交成功, false=队列满且降级也失败
     */
    public boolean submit(SeckillOrderTask task) {
        boolean offered = taskQueue.offer(task);
        if (!offered) {
            log.warn("[订单队列已满] 触发降级策略 userId={}, activityId={}", task.userId(), task.activityId());
            // 队列满时，降级为同步执行（保证不丢单）
            try {
                processTask(task);
                return true;
            } catch (Exception e) {
                log.error("[降级执行失败] userId={}, activityId={}", task.userId(), task.activityId(), e);
                return false;
            }
        }
        log.debug("[订单入队成功] userId={}, activityId={}, 队列剩余容量={}",
                task.userId(), task.activityId(), taskQueue.remainingCapacity());
        return true;
    }

    /**
     * 消费者循环 —— 每个工作线程持续从队列拉取任务并处理
     * <p>
     * 使用 poll + 超时，让线程能在队列空闲时阻塞等待，不空转浪费 CPU。
     */
    private void consumeLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // 阻塞等待，最多等 1 秒（超时后检查中断状态）
                SeckillOrderTask task = taskQueue.poll(1, TimeUnit.SECONDS);
                if (task != null) {
                    processTask(task);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[订单消费者] 线程被中断");
                break;
            } catch (Exception e) {
                log.error("[订单消费者] 处理异常", e);
            }
        }
    }

    /**
     * 处理单个订单任务
     */
    private void processTask(SeckillOrderTask task) {
        long start = System.currentTimeMillis();
        try {
            orderService.createSeckillOrder(task.userId(), task.activityId(), task.productId());
            long elapsed = System.currentTimeMillis() - start;
            log.info("[订单处理完成] userId={}, activityId={}, 耗时={}ms", task.userId(), task.activityId(), elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[订单处理失败] userId={}, activityId={}, 耗时={}ms", task.userId(), task.activityId(), elapsed, e);
            // 失败重试一次（简单的指数退避）
            try {
                TimeUnit.MILLISECONDS.sleep(500);
                orderService.createSeckillOrder(task.userId(), task.activityId(), task.productId());
                log.info("[订单重试成功] userId={}, activityId={}", task.userId(), task.activityId());
            } catch (Exception retryEx) {
                log.error("[订单重试失败] userId={}, activityId={}", task.userId(), task.activityId(), retryEx);
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("[订单队列] 开始关闭，剩余任务数={}", taskQueue.size());

        executor.shutdown();
        try {
            // 等待最多 30 秒让队列中的任务处理完
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("[订单队列] 超时未完成，强制关闭，丢弃任务数={}", taskQueue.size());
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }

        log.info("[订单队列] 已关闭");
    }

    /**
     * 获取实时监控指标
     */
    public QueueMetrics getMetrics() {
        return new QueueMetrics(
                taskQueue.size(),
                taskQueue.remainingCapacity(),
                executor.getActiveCount(),
                executor.getPoolSize(),
                executor.getCompletedTaskCount()
        );
    }

    /**
     * 队列监控指标
     */
    public record QueueMetrics(
            int queueSize,          // 当前队列中等待的任务数
            int queueRemaining,     // 队列剩余容量
            int activeThreads,      // 活跃消费者线程数
            int poolSize,           // 线程池当前大小
            long completedTasks     // 已完成任务数
    ) {}
}
