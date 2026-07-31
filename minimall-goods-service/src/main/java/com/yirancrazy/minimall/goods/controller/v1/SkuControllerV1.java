package com.yirancrazy.minimall.goods.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.goods.dto.SkuCreateDTO;
import com.yirancrazy.minimall.goods.entity.SkuPO;
import com.yirancrazy.minimall.goods.service.SkuService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品控制器，提供Sku RESTful API
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class SkuControllerV1 {

    private final SkuService skuService;

    public SkuControllerV1(SkuService skuService) {
        this.skuService = skuService;
    }

    /**
     * 根据主键查询 SKU 详情。
     *
     * @param id SKU 主键 ID
     * @return SKU 实体，不存在时由 Service 层抛出业务异常
     */
    @GetMapping("/{id}")
    public Result<SkuPO> get(@PathVariable("id") Long id) {
        return Result.success(skuService.getById(id));
    }

    /**
     * 创建SKU。
     * @param dto SKU创建DTO
     * @return SKU ID
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SkuCreateDTO dto) {
        return Result.success(skuService.create(dto));
    }
}