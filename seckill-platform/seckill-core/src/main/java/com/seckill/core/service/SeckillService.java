package com.seckill.core.service;

import com.seckill.core.model.dto.SeckillOrderDTO;
import com.seckill.core.model.vo.SeckillProductVO;

import java.util.List;

/**
 * 秒杀服务接口
 */
public interface SeckillService {

    /**
     * 生成秒杀路径（防刷）
     * 每次都动态生成唯一路径，防止提前构造请求
     */
    String generateSeckillPath(Long userId, Long productId, Long activityId);

    /**
     * 验证秒杀路径是否合法
     */
    boolean verifySeckillPath(Long userId, Long activityId, String path);

    /**
     * 执行秒杀（核心方法）
     * <p>
     * 流程: 校验路径 → Redis 原子扣库存 → 发 MQ 异步下单 → 返回排队中
     */
    Long executeSeckill(Long userId, SeckillOrderDTO dto);

    /**
     * 查询秒杀结果
     */
    String getSeckillResult(Long userId, Long activityId);

    /**
     * 获取所有正在进行的秒杀商品列表
     */
    List<SeckillProductVO> getActiveSeckillProducts();

    /**
     * 获取单个秒杀商品详情
     */
    SeckillProductVO getSeckillProductDetail(Long activityId);

    /**
     * 预热秒杀库存到 Redis（活动开始前调用）
     */
    void warmUpSeckillStock(Long activityId);
}
