package com.seckill.core.service;

import com.seckill.common.constant.RedisKeyConstant;
import com.seckill.common.enums.ResultCode;
import com.seckill.common.exception.BizException;
import com.seckill.core.mapper.SeckillMapper;
import com.seckill.core.model.entity.Order;
import com.seckill.core.model.entity.SeckillActivity;
import com.seckill.core.mapper.OrderMapper;
import com.seckill.core.mapper.SeckillActivityMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.common.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 订单服务
 * <p>
 * 处理 MQ 消费者回调创建订单，以及订单查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final SeckillActivityMapper activityMapper;
    private final SeckillMapper seckillMapper;
    private final RedisUtil redisUtil;

    /**
     * 异步创建秒杀订单（由 MQ 消费者调用）
     * <p>
     * 使用 MySQL 乐观锁做最终扣减，防止 Redis 挂了后的少卖/超卖。
     */
    @Transactional
    public Order createSeckillOrder(Long userId, Long activityId, Long productId) {
        SeckillActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BizException(ResultCode.PRODUCT_NOT_EXIST);
        }

        // MySQL 乐观锁扣库存（最终防线）
        int rows = seckillMapper.decreaseStock(activityId, activity.getVersion());
        if (rows <= 0) {
            log.warn("[数据库库存扣减失败] activityId={}, userId={}", activityId, userId);
            throw new BizException(ResultCode.STOCK_NOT_ENOUGH);
        }

        // 创建订单
        Order order = Order.builder()
                .orderNo(generateOrderNo())
                .userId(userId)
                .productId(productId)
                .activityId(activityId)
                .orderAmount(activity.getSeckillPrice())
                .status(0)
                .build();

        orderMapper.insert(order);

        // 写入 Redis 通知用户秒杀成功
        String resultKey = "seckill:result:" + userId + ":" + activityId;
        redisUtil.set(resultKey, order.getOrderNo(), 30, TimeUnit.MINUTES);

        log.info("[秒杀订单创建成功] orderNo={}, userId={}, activityId={}", order.getOrderNo(), userId, activityId);
        return order;
    }

    public Order getByOrderNo(String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_EXIST);
        }
        return order;
    }

    public List<Order> getUserOrders(Long userId) {
        return orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime));
    }

    private String generateOrderNo() {
        return "SK" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
