package com.seckill.admin.controller;

import com.seckill.common.result.Result;
import com.seckill.core.model.entity.SeckillActivity;
import com.seckill.core.mapper.SeckillActivityMapper;
import com.seckill.core.service.SeckillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理后台 — 秒杀活动管理
 */
@RestController
@RequestMapping("/api/admin/seckill")
@RequiredArgsConstructor
@Tag(name = "管理后台-秒杀", description = "秒杀活动配置与库存预热")
public class AdminSeckillController {

    private final SeckillActivityMapper activityMapper;
    private final SeckillService seckillService;

    @GetMapping
    @Operation(summary = "秒杀活动列表")
    public Result<List<SeckillActivity>> list() {
        return Result.success(activityMapper.selectList(null));
    }

    @GetMapping("/{id}")
    @Operation(summary = "秒杀活动详情")
    public Result<SeckillActivity> detail(@PathVariable Long id) {
        return Result.success(activityMapper.selectById(id));
    }

    @PostMapping
    @Operation(summary = "创建秒杀活动")
    public Result<SeckillActivity> create(@Valid @RequestBody SeckillActivity activity) {
        activityMapper.insert(activity);
        return Result.success(activity);
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑秒杀活动")
    public Result<SeckillActivity> update(@PathVariable Long id,
                                           @Valid @RequestBody SeckillActivity activity) {
        activity.setId(id);
        activityMapper.updateById(activity);
        return Result.success(activity);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除秒杀活动")
    public Result<Void> delete(@PathVariable Long id) {
        activityMapper.deleteById(id);
        return Result.success();
    }

    @PostMapping("/{id}/warmup")
    @Operation(summary = "手动预热秒杀库存到 Redis")
    public Result<Void> warmUp(@PathVariable Long id) {
        seckillService.warmUpSeckillStock(id);
        return Result.success();
    }
}
