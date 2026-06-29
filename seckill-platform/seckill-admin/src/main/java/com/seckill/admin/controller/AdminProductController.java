package com.seckill.admin.controller;

import com.seckill.common.result.Result;
import com.seckill.core.model.entity.Product;
import com.seckill.core.mapper.ProductMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理后台 — 商品管理
 */
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@Tag(name = "管理后台-商品", description = "商品增删改查")
public class AdminProductController {

    private final ProductMapper productMapper;

    @GetMapping
    @Operation(summary = "商品列表")
    public Result<List<Product>> list() {
        return Result.success(productMapper.selectList(null));
    }

    @GetMapping("/{id}")
    @Operation(summary = "商品详情")
    public Result<Product> detail(@PathVariable Long id) {
        return Result.success(productMapper.selectById(id));
    }

    @PostMapping
    @Operation(summary = "新增商品")
    public Result<Product> create(@Valid @RequestBody Product product) {
        productMapper.insert(product);
        return Result.success(product);
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑商品")
    public Result<Product> update(@PathVariable Long id, @Valid @RequestBody Product product) {
        product.setId(id);
        productMapper.updateById(product);
        return Result.success(product);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除商品")
    public Result<Void> delete(@PathVariable Long id) {
        productMapper.deleteById(id);
        return Result.success();
    }
}
