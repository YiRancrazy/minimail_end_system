package com.yirancrazy.minimall.goods.controller.v1;

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.goods.dto.SpuCreateDto;
import com.yirancrazy.minimall.goods.dto.SpuPageDto;
import com.yirancrazy.minimall.goods.dto.SpuUpdateDto;
import com.yirancrazy.minimall.goods.service.SpuService;
import com.yirancrazy.minimall.goods.vo.SpuVo;
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
 * @Description: 面向商家端提供 SPU 创建、查询、分页、更新、删除及上下架申请接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/merchant/goods/spus")
public class SpuControllerV1 {

    private final SpuService spuService;

    /**
     * 注入SPU 聚合服务，构造 SpuControllerV1 实例。
     *
     * @param spuService SPU 聚合服务
     */
    public SpuControllerV1(SpuService spuService) {
        this.spuService = spuService;
    }

    /**
     * 商家按 SPU 主键读取商品和 SKU，并校验商家归属。
     *
     * @param merchantId 当前商家 ID，用于商品归属校验
     * @param id SPU 主键
     * @return 统一响应包装；data 为转换后的 SPU 视图对象
     */
    @GetMapping("/{id}")
    public Result<SpuVo> get(
            @RequestHeader("X-Merchant-Id") Long merchantId, @PathVariable("id") Long id) {
        return Result.success(spuService.getDetail(id, merchantId));
    }

    /**
     * 商家创建 SPU 及初始 SKU；创建前校验店铺归属与营业状态。
     *
     * @param merchantId 当前商家 ID，用于商品归属校验
     * @param dto SPU 创建参数
     * @return 统一响应包装；data 为新建记录的主键
     */
    @PostMapping
    public Result<Long> create(
            @RequestHeader("X-Merchant-Id") Long merchantId, @Valid @RequestBody SpuCreateDto dto) {
        return Result.success(spuService.create(merchantId, dto));
    }

    /**
     * 商家分页查询 SPU，服务层按请求头中的商家 ID 过滤。
     *
     * @param merchantId 当前商家 ID，用于商品归属校验
     * @param dto SPU 游标分页或状态过滤参数
     * @return 统一响应包装；data 为按主键游标分页的结果；记录项为转换后的 SPU 视图对象
     */
    @GetMapping
    public Result<CursorPageVo<SpuVo>> page(
            @RequestHeader("X-Merchant-Id") Long merchantId, @Valid SpuPageDto dto) {
        dto.setMerchantId(merchantId);
        return Result.success(spuService.page(dto));
    }

    /**
     * 商家增量更新 SPU 和 SKU 明细；非草稿商品会重新进入待审。
     *
     * @param merchantId 当前商家 ID，用于商品归属校验
     * @param id SPU 主键
     * @param dto SPU 增量更新参数
     * @return 统一响应包装；data 为操作实际影响记录时为 true，否则为 false
     */
    @PutMapping("/{id}")
    public Result<Boolean> update(
            @RequestHeader("X-Merchant-Id") Long merchantId,
            @PathVariable("id") Long id,
            @Valid @RequestBody SpuUpdateDto dto) {
        return Result.success(spuService.update(id, merchantId, dto));
    }

    /**
     * 商家删除 SPU，并刷新 Elasticsearch 镜像。
     *
     * @param merchantId 当前商家 ID，用于商品归属校验
     * @param id SPU 主键
     * @return 统一响应包装；data 为操作实际影响记录时为 true，否则为 false
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(
            @RequestHeader("X-Merchant-Id") Long merchantId, @PathVariable("id") Long id) {
        return Result.success(spuService.delete(id, merchantId));
    }

    /**
     * 商家提交 SPU 送审；仅草稿、下架或驳回状态允许操作。
     *
     * @param merchantId 当前商家 ID，用于商品归属校验
     * @param id SPU 主键
     * @return 统一响应包装；data 为操作实际影响记录时为 true，否则为 false
     */
    @PutMapping("/{id}/on-shelf")
    public Result<Boolean> onShelf(
            @RequestHeader("X-Merchant-Id") Long merchantId, @PathVariable("id") Long id) {
        return Result.success(spuService.onShelf(id, merchantId));
    }

    /**
     * 商家下架在售 SPU，并同步更新搜索镜像。
     *
     * @param merchantId 当前商家 ID，用于商品归属校验
     * @param id SPU 主键
     * @return 统一响应包装；data 为操作实际影响记录时为 true，否则为 false
     */
    @PutMapping("/{id}/off-shelf")
    public Result<Boolean> offShelf(
            @RequestHeader("X-Merchant-Id") Long merchantId, @PathVariable("id") Long id) {
        return Result.success(spuService.offShelf(id, merchantId));
    }
}
