package com.yirancrazy.minimall.goods.controller.v1;

import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.goods.dto.SkuCreateDTO;
import com.yirancrazy.minimall.goods.entity.SkuPO;
import com.yirancrazy.minimall.goods.service.SkuService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
* 商品 C 端接口控制器，提供 SKU 查询与创建能力，供前台商品维护与展示使用。
 */
@RestController
@RequestMapping("/api/v1/goods/sku")
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

    @PostMapping
    public Result<Long> create(@Valid @RequestBody SkuCreateDTO dto) {
        return Result.success(skuService.create(dto));
    }
}