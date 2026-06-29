package com.seckill.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.core.model.entity.SeckillActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SeckillMapper extends BaseMapper<SeckillActivity> {

    /**
     * 使用 MySQL 行锁 + 乐观锁扣减秒杀库存
     * SQL 层面保证原子性，防止超卖
     */
    @Update("UPDATE t_seckill_activity " +
            "SET seckill_stock = seckill_stock - 1, version = version + 1 " +
            "WHERE id = #{activityId} " +
            "AND seckill_stock > 0 " +
            "AND version = #{version}")
    int decreaseStock(@Param("activityId") Long activityId, @Param("version") Integer version);
}
