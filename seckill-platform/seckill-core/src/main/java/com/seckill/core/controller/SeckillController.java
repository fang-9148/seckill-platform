package com.seckill.core.controller;

import com.seckill.common.result.Result;
import com.seckill.core.model.dto.SeckillOrderDTO;
import com.seckill.core.model.vo.SeckillProductVO;
import com.seckill.core.queue.SeckillOrderQueue;
import com.seckill.core.service.SeckillService;
import com.seckill.core.aop.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 秒杀接口控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
@Tag(name = "秒杀接口", description = "用户端秒杀相关接口")
public class SeckillController {

    private final SeckillService seckillService;
    private final SeckillOrderQueue orderQueue;

    @GetMapping("/products")
    @Operation(summary = "获取正在进行秒杀的商品列表")
    public Result<List<SeckillProductVO>> getActiveProducts() {
        return Result.success(seckillService.getActiveSeckillProducts());
    }

    @GetMapping("/products/{activityId}")
    @Operation(summary = "获取秒杀商品详情")
    public Result<SeckillProductVO> getProductDetail(@PathVariable Long activityId) {
        return Result.success(seckillService.getSeckillProductDetail(activityId));
    }

    @GetMapping("/path")
    @Operation(summary = "获取秒杀路径（防刷）")
    public Result<String> getSeckillPath(
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam Long activityId) {
        String path = seckillService.generateSeckillPath(userId, productId, activityId);
        return Result.success(path);
    }

    @PostMapping("/execute")
    @Operation(summary = "执行秒杀")
    @RateLimiter(permitsPerWindow = 500, windowSeconds = 1, message = "秒杀太火爆，请稍后再试")
    public Result<String> executeSeckill(
            @RequestParam Long userId,
            @Valid @RequestBody SeckillOrderDTO dto) {
        Long activityId = seckillService.executeSeckill(userId, dto);
        return Result.success("秒杀请求已提交，请轮询结果", String.valueOf(activityId));
    }

    @GetMapping("/result")
    @Operation(summary = "查询秒杀结果")
    public Result<String> getSeckillResult(
            @RequestParam Long userId,
            @RequestParam Long activityId) {
        String result = seckillService.getSeckillResult(userId, activityId);
        return Result.success(result);
    }

    @GetMapping("/queue/metrics")
    @Operation(summary = "查看订单队列实时指标（监控用）")
    public Result<SeckillOrderQueue.QueueMetrics> getQueueMetrics() {
        return Result.success(orderQueue.getMetrics());
    }
}
