package com.yirancrazy.minimall.merchant.controller.v1;

import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.merchant.entity.ShopPO;
import com.yirancrazy.minimall.merchant.service.ShopService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant/shop")
public class ShopControllerV1 {

    private final ShopService shopService;

    public ShopControllerV1(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping("/{id}")
    public Result<ShopPO> get(@PathVariable Long id) {
        return Result.success(shopService.getById(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody ShopPO shop) {
        return Result.success(shopService.create(shop));
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody ShopPO shop) {
        return Result.success(shopService.update(id, shop));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(shopService.delete(id));
    }
}