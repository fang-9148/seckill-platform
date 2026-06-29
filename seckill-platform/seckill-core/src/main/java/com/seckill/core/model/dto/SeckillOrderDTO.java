package com.seckill.core.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 秒杀下单请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrderDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "活动ID不能为空")
    private Long activityId;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /** 秒杀路径 MD5（防刷） */
    @NotNull(message = "秒杀路径不能为空")
    private String seckillPath;
}
