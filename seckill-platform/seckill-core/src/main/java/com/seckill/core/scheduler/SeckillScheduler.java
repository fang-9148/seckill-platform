package com.seckill.core.scheduler;

import com.seckill.core.model.entity.SeckillActivity;
import com.seckill.core.mapper.SeckillActivityMapper;
import com.seckill.core.service.SeckillService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 秒杀定时任务
 * <p>
 * 负责库存预热、活动状态更新等定时操作。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillScheduler {

    private final SeckillActivityMapper activityMapper;
    private final SeckillService seckillService;

    /**
     * 每分钟检查即将开始的秒杀活动，提前预热库存到 Redis
     */
    @Scheduled(cron = "0 * * * * ?")
    public void warmUpStock() {
        log.debug("[定时任务] 检查需要预热的秒杀活动...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fiveMinutesLater = now.plusMinutes(5);

        // 查找 5 分钟内即将开始的活动
        List<SeckillActivity> upcomingActivities =
                activityMapper.selectList(new LambdaQueryWrapper<SeckillActivity>()
                        .eq(SeckillActivity::getStatus, 0)); // 0-未开始

        for (SeckillActivity activity : upcomingActivities) {
            if (activity.getStartTime().isBefore(fiveMinutesLater)
                    && activity.getStartTime().isAfter(now)) {
                log.info("[定时任务] 预热秒杀库存 activityId={}", activity.getId());
                seckillService.warmUpSeckillStock(activity.getId());
            }
        }
    }

    /**
     * 每分钟检查已过期的活动，更新状态
     */
    @Scheduled(cron = "0 * * * * ?")
    public void updateExpiredActivities() {
        List<SeckillActivity> activeActivities = activityMapper.selectList(
                new LambdaQueryWrapper<SeckillActivity>()
                        .eq(SeckillActivity::getStatus, 1)); // 1-进行中

        LocalDateTime now = LocalDateTime.now();
        for (SeckillActivity activity : activeActivities) {
            if (activity.getEndTime().isBefore(now)) {
                activity.setStatus(2); // 2-已结束
                activityMapper.updateById(activity);
                log.info("[定时任务] 秒杀活动已结束 activityId={}", activity.getId());
            }
        }
    }
}
