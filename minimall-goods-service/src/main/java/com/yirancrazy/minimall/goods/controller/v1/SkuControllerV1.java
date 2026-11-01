package com.yirancrazy.minimall.goods.controller.v1;

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.goods.dto.SkuCreateDto;
import com.yirancrazy.minimall.goods.dto.SkuPageDto;
import com.yirancrazy.minimall.goods.dto.SkuUpdateDto;
import com.yirancrazy.minimall.goods.service.SkuService;
import com.yirancrazy.minimall.goods.vo.SkuVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 面向商家端提供 SKU 创建、查询、分页、更新和删除接口，并强制商家归属校验。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/merchant/goods/skus")
public class SkuControllerV1 {

    private final SkuService skuService;

    /**
     * 注入SKU 业务服务，构造 SkuControllerV1 实例。
     *
     * @param skuService SKU 业务服务
     */
    public SkuControllerV1(SkuService skuService) {
        this.skuService = skuService;
    }

    /**
     * 商家按 SKU 主键读取记录，并使用请求头中的商家 ID 校验归属。
     *
     * @param merchantId 当前商家 ID，用于商品归属校验
     * @param id SKU 主键
     * @return 统一响应包装；data 为转换后的 SKU 视图对象
     */
    @GetMapping("/{id}")
    public Result<SkuVo> get(
            @RequestHeader("X-Merchant-Id") Long merchantId, @PathVariable("id") Long id) {
        return Result.success(skuService.getVo(id, merchantId));
    }

    /**
     * 商家创建 SKU，父 SPU 必须属于请求头指定的商家。
     *
     * @param merchantId 当前商家 ID，用于商品归属校验
     * @param dto SKU 创建参数
     * @return 统一响应包装；data 为新建记录的主键
     */
    @PostMapping
    public Result<Long> create(
            @RequestHeader("X-Merchant-Id") Long merchantId, @Valid @RequestBody SkuCreateDto dto) {
        return Result.success(skuService.create(merchantId, dto));
    }

    /**
     * 商家分页查询 SKU，服务层强制以请求头中的商家 ID 过滤。
     *
     * @param merchantId 当前商家 ID，用于商品归属校验
     * @param dto SKU 游标分页参数
     * @return 统一响应包装；data 为按主键游标分页的结果；记录项为转换后的 SKU 视图对象
     */
    @GetMapping
    public Result<CursorPageVo<SkuVo>> page(
            @RequestHeader("X-Merchant-Id") Long merchantId, @Valid SkuPageDto dto) {
        dto.setMerchantId(merchantId);
        return Result.success(skuService.pageVo(dto));
    }

    /**
     * 商家更新 SKU 名称、价格或库存，并执行归属校验。
     *
     * @param merchantId 当前商家 ID，用于商品归属校验
     * @param id SKU 主键
     * @param dto SKU 增量更新参数
     * @return 统一响应包装；data 为操作实际影响记录时为 true，否则为 false
     */
    @PutMapping("/{id}")
    public Result<Boolean> update(
            @RequestHeader("X-Merchant-Id") Long merchantId,
            @PathVariable("id") Long id,
            @Valid @RequestBody SkuUpdateDto dto) {
        return Result.success(skuService.update(id, merchantId, dto));
    }

    /**
     * 商家删除 SKU，并执行归属校验。
     *
     * @param merchantId 当前商家 ID，用于商品归属校验
     * @param id SKU 主键
     * @return 统一响应包装；data 为操作实际影响记录时为 true，否则为 false
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(
            @RequestHeader("X-Merchant-Id") Long merchantId, @PathVariable("id") Long id) {
        return Result.success(skuService.delete(id, merchantId));
    }
}
