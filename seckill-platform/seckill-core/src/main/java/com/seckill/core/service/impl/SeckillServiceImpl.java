package com.seckill.core.service.impl;

import com.seckill.common.constant.RedisKeyConstant;
import com.seckill.common.enums.ResultCode;
import com.seckill.common.exception.BizException;
import com.seckill.common.util.MD5Util;
import com.seckill.common.util.RedisUtil;
import com.seckill.core.model.dto.SeckillOrderDTO;
import com.seckill.core.model.entity.Order;
import com.seckill.core.model.entity.SeckillActivity;
import com.seckill.core.model.vo.SeckillProductVO;
import com.seckill.core.queue.SeckillOrderQueue;
import com.seckill.core.queue.SeckillOrderTask;
import com.seckill.core.mapper.OrderMapper;
import com.seckill.core.mapper.SeckillActivityMapper;
import com.seckill.core.service.SeckillService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 秒杀服务实现 — 秒杀核心引擎
 *
 * <pre>
 * 核心链路:
 *   1. 用户请求 → 校验秒杀路径（防刷）
 *   2. Redis Lua 脚本原子扣库存
 *   3. 订单任务进内存队列 LinkedBlockingQueue（削峰）
 *   4. 线程池异步消费队列，创建订单落库
 *   5. 用户轮询秒杀结果
 *
 * 防超卖策略:
 *   - 第一层: Redis Lua 脚本原子扣减
 *   - 第二层: MySQL 乐观锁扣减（防 Redis 挂了）
 *   - 第三层: 数据库唯一索引防重复下单
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private final RedisUtil redisUtil;
    private final RedissonClient redissonClient;
    private final SeckillActivityMapper activityMapper;
    private final OrderMapper orderMapper;
    private final SeckillOrderQueue orderQueue;

    /** 秒杀路径盐值 */
    private static final String PATH_SALT = "Seckill2024!@#";

    @Override
    public String generateSeckillPath(Long userId, Long productId, Long activityId) {
        // 校验活动是否在进行
        SeckillActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BizException(ResultCode.PRODUCT_NOT_EXIST);
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) {
            throw new BizException(ResultCode.SECKILL_NOT_STARTED);
        }
        if (now.isAfter(activity.getEndTime())) {
            throw new BizException(ResultCode.SECKILL_ENDED);
        }

        // 生成动态秒杀路径
        String path = MD5Util.generateSeckillPath(productId, PATH_SALT);

        // 存入 Redis，有效期到活动结束
        String pathKey = "seckill:path:" + userId + ":" + activityId;
        redisUtil.set(pathKey, path, 60, TimeUnit.SECONDS);

        log.info("[生成秒杀路径] userId={}, activityId={}, path={}", userId, activityId, path);
        return path;
    }

    @Override
    public boolean verifySeckillPath(Long userId, Long activityId, String path) {
        String pathKey = "seckill:path:" + userId + ":" + activityId;
        String cachedPath = redisUtil.get(pathKey, String.class);
        return path != null && path.equals(cachedPath);
    }

    /**
     * 执行秒杀 — 核心方法
     */
    @Override
    public Long executeSeckill(Long userId, SeckillOrderDTO dto) {
        Long activityId = dto.getActivityId();
        Long productId = dto.getProductId();

        // ===== 第一步: 校验秒杀路径（防刷） =====
        if (!verifySeckillPath(userId, activityId, dto.getSeckillPath())) {
            throw new BizException(ResultCode.FORBIDDEN, "秒杀路径无效");
        }

        // ===== 第二步: 检查是否已下过单（防重复） =====
        String orderKey = RedisKeyConstant.orderKey(userId, productId);
        if (redisUtil.hasKey(orderKey)) {
            throw new BizException(ResultCode.SECKILL_DUPLICATE);
        }

        // 也查一下数据库（兜底）
        if (orderMapper.exists(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getActivityId, activityId))) {
            redisUtil.set(orderKey, "1", 3600, TimeUnit.SECONDS);
            throw new BizException(ResultCode.SECKILL_DUPLICATE);
        }

        // ===== 第三步: Redis Lua 原子扣库存 =====
        String stockKey = RedisKeyConstant.stockKey(activityId);
        boolean success = redisUtil.decreaseStockAtomic(stockKey);
        if (!success) {
            // 标记已售罄
            redisUtil.set(RedisKeyConstant.overKey(activityId), "1", 3600, TimeUnit.SECONDS);
            throw new BizException(ResultCode.STOCK_NOT_ENOUGH);
        }

        // ===== 第四步: 标记用户已下单 =====
        redisUtil.set(orderKey, "1", 3600, TimeUnit.SECONDS);

        // ===== 第五步: 入内存队列，异步创建订单（削峰） =====
        boolean submitted = orderQueue.submit(new SeckillOrderTask(userId, activityId, productId));
        if (!submitted) {
            // 队列满 + 降级执行也失败 → 回滚 Redis 库存
            String key = RedisKeyConstant.stockKey(activityId);
            redisUtil.increment(key);
            redisUtil.delete(orderKey);
            throw new BizException(ResultCode.SECKILL_FAILED, "系统繁忙，秒杀失败");
        }

        log.info("[秒杀请求已入队] userId={}, activityId={}, productId={}", userId, activityId, productId);
        return activityId;
    }

    @Override
    public String getSeckillResult(Long userId, Long activityId) {
        // 检查是否下单成功
        String orderKey = RedisKeyConstant.orderKey(userId, activityId);
        if (!redisUtil.hasKey(orderKey)) {
            return "NOT_STARTED";  // 未参与秒杀
        }

        // 检查订单是否已创建
        String resultKey = "seckill:result:" + userId + ":" + activityId;
        String orderNo = redisUtil.get(resultKey, String.class);
        if (orderNo != null) {
            return orderNo;  // 秒杀成功，返回订单号
        }

        // 检查是否卖完了
        String overKey = RedisKeyConstant.overKey(activityId);
        if (redisUtil.hasKey(overKey)) {
            // 判断用户是否在库存扣减成功的名单中
            return "FAILED_SOLD_OUT";
        }

        return "QUEUING";  // 排队中
    }

    @Override
    public List<SeckillProductVO> getActiveSeckillProducts() {
        LocalDateTime now = LocalDateTime.now();
        List<SeckillActivity> activities = activityMapper.selectList(
                new LambdaQueryWrapper<SeckillActivity>()
                        .le(SeckillActivity::getStartTime, now)
                        .ge(SeckillActivity::getEndTime, now));

        return activities.stream().map(activity -> {
            SeckillProductVO vo = SeckillProductVO.builder()
                    .activityId(activity.getId())
                    .productId(activity.getProductId())
                    .seckillPrice(activity.getSeckillPrice())
                    .startTime(activity.getStartTime())
                    .endTime(activity.getEndTime())
                    .status(activity.getStatus())
                    .build();

            // 从 Redis 读取实时库存
            String stockKey = RedisKeyConstant.stockKey(activity.getId());
            Object stock = redisUtil.get(stockKey);
            if (stock != null) {
                int remaining = Integer.parseInt(stock.toString());
                vo.setStockStatus(remaining > 0 ? 1 : 0); // 1-有货 0-售罄
            }

            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public SeckillProductVO getSeckillProductDetail(Long activityId) {
        SeckillActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BizException(ResultCode.PRODUCT_NOT_EXIST);
        }

        SeckillProductVO vo = SeckillProductVO.builder()
                .activityId(activity.getId())
                .productId(activity.getProductId())
                .seckillPrice(activity.getSeckillPrice())
                .startTime(activity.getStartTime())
                .endTime(activity.getEndTime())
                .status(activity.getStatus())
                .build();

        // 从 Redis 获取实时库存
        String stockKey = RedisKeyConstant.stockKey(activityId);
        Object stock = redisUtil.get(stockKey);
        vo.setStockStatus(stock != null && Integer.parseInt(stock.toString()) > 0 ? 1 : 0);

        return vo;
    }

    /**
     * 预热秒杀库存到 Redis
     * <p>
     * 在秒杀活动开始前，将库存加载到 Redis 缓存，避免第一次请求击穿到数据库。
     * 使用分布式锁保证只预热一次。
     */
    @Override
    @Transactional
    public void warmUpSeckillStock(Long activityId) {
        String lockKey = RedisKeyConstant.lockKey(activityId);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试加锁，最多等待 5 秒，锁持有时间 30 秒
            if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                try {
                    SeckillActivity activity = activityMapper.selectById(activityId);
                    if (activity == null) {
                        throw new BizException(ResultCode.PRODUCT_NOT_EXIST);
                    }

                    String stockKey = RedisKeyConstant.stockKey(activityId);

                    // 只有 Redis 中没有库存数据时才预热（防止重复预热）
                    if (!redisUtil.hasKey(stockKey)) {
                        redisUtil.set(stockKey, activity.getSeckillStock());
                        log.info("[库存预热成功] activityId={}, stock={}", activityId, activity.getSeckillStock());
                    } else {
                        log.info("[库存已存在，跳过预热] activityId={}", activityId);
                    }
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("[获取分布式锁失败，跳过预热] activityId={}", activityId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[库存预热被中断] activityId={}", activityId, e);
        }
    }
}
