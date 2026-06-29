package com.seckill.core.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀活动实体
 * <p>
 * 每个商品可创建一次秒杀活动，活动有独立的秒杀库存和价格。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_seckill_activity")
public class SeckillActivity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联商品ID */
    private Long productId;

    /** 秒杀价格 */
    private BigDecimal seckillPrice;

    /** 秒杀库存（独立于商品总库存） */
    private Integer seckillStock;

    /** 活动开始时间 */
    private LocalDateTime startTime;

    /** 活动结束时间 */
    private LocalDateTime endTime;

    /** 活动状态: 0-未开始, 1-进行中, 2-已结束, 3-已取消 */
    @Builder.Default
    private Integer status = 0;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Version
    private Integer version;
}
