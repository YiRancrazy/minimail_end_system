package com.yirancrazy.minimall.goods.controller.v1;

import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.goods.entity.SkuPO;
import com.yirancrazy.minimall.goods.service.SkuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/goods/sku")
public class SkuControllerV1 {

    private final SkuService skuService;

    public SkuControllerV1(SkuService skuService) {
        this.skuService = skuService;
    }

    @GetMapping("/{id}")
    public Result<SkuPO> get(@PathVariable Long id) {
        return Result.success(skuService.getById(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody SkuPO sku) {
        return Result.success(skuService.create(sku));
    }
}