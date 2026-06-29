package com.seckill.core.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀商品视图对象（返回给前端）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillProductVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long activityId;
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal originalPrice;
    private BigDecimal seckillPrice;
    private Integer seckillStock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /** 秒杀状态: 0-未开始, 1-进行中, 2-已结束 */
    private Integer status;

    /** 剩余库存（脱敏后，防止恶意爬虫） */
    private Integer stockStatus;
}
