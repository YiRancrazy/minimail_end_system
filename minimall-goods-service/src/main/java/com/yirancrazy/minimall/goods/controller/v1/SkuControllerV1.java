package com.yirancrazy.minimall.goods.controller.v1;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.goods.dto.SkuCreateDTO;
import com.yirancrazy.minimall.goods.dto.SkuPageDTO;
import com.yirancrazy.minimall.goods.dto.SkuUpdateDTO;
import com.yirancrazy.minimall.goods.entity.SkuPO;
import com.yirancrazy.minimall.goods.service.SkuService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品控制器，提供Sku RESTful API
 * @Version: 1.1
 * @DateTime: 2026/08/02
 */
@RestController
@RequestMapping("/api/v1/skus")
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

    /**
     * 分页查询SKU列表，支持按名称模糊搜索。
     * @param dto 分页查询入参
     * @return SKU 分页结果
     */
    @GetMapping
    public Result<IPage<SkuPO>> page(@Valid SkuPageDTO dto) {
        return Result.success(skuService.page(dto));
    }

    /**
     * 根据ID更新SKU信息。
     * @param id SKU ID
     * @param dto SKU修改DTO
     * @return 更新是否成功
     */
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable("id") Long id, @Valid @RequestBody SkuUpdateDTO dto) {
        return Result.success(skuService.update(id, dto));
    }

    /**
     * 根据ID删除SKU。
     * @param id SKU ID
     * @return 删除是否成功
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable("id") Long id) {
        return Result.success(skuService.delete(id));
    }
}
