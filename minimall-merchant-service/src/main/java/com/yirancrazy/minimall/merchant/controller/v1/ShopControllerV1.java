package com.yirancrazy.minimall.merchant.controller.v1;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.merchant.dto.ShopCreateDTO;
import com.yirancrazy.minimall.merchant.dto.ShopUpdateDTO;
import com.yirancrazy.minimall.merchant.entity.ShopPO;
import com.yirancrazy.minimall.merchant.service.ShopService;

/**
* 店铺 C 端 HTTP 接口控制器，提供店铺基础信息的查询、创建、更新与删除等 REST 能力。
 */
@RestController
@RequestMapping("/api/v1/merchant/shop")
public class ShopControllerV1 {

    private final ShopService shopService;

    public ShopControllerV1(ShopService shopService) {
        this.shopService = shopService;
    }

    /**
     * 根据主键 ID 查询店铺详情。
     *
     * @param id 店铺主键 ID
     * @return 店铺详情结果
     */
    @GetMapping("/{id}")
    public Result<ShopPO> get(@PathVariable("id") Long id) {
        return Result.success(shopService.getById(id));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody ShopCreateDTO dto) {
        return Result.success(shopService.create(dto));
    }

    /**
     * 根据主键 ID 更新店铺信息。
     *
     * @param id 店铺主键 ID
     * @param dto 待更新的店铺信息
     * @return 是否更新成功
     */
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable("id") Long id, @Valid @RequestBody ShopUpdateDTO dto) {
        return Result.success(shopService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable("id") Long id) {
        return Result.success(shopService.delete(id));
    }
}