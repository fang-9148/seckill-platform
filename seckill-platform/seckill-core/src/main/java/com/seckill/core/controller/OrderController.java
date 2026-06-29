package com.seckill.core.controller;

import com.seckill.common.result.Result;
import com.seckill.core.model.entity.Order;
import com.seckill.core.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Tag(name = "订单接口", description = "订单查询")
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/{orderNo}")
    @Operation(summary = "根据订单号查询订单")
    public Result<Order> getOrder(@PathVariable String orderNo) {
        return Result.success(orderService.getByOrderNo(orderNo));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "查询用户所有订单")
    public Result<List<Order>> getUserOrders(@PathVariable Long userId) {
        return Result.success(orderService.getUserOrders(userId));
    }
}
