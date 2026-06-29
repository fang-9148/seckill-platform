package com.seckill.core.queue;

import java.io.Serializable;

public record SeckillOrderTask(
        Long userId,
        Long activityId,
        Long productId,
        long timestamp
) implements Serializable {

    public SeckillOrderTask(Long userId, Long activityId, Long productId) {
        this(userId, activityId, productId, System.currentTimeMillis());
    }
}
