package com.yirancrazy.minimall.goods.controller.v1;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.goods.dto.SkuCreateDTO;
import com.yirancrazy.minimall.goods.dto.SkuPageDTO;
import com.yirancrazy.minimall.goods.dto.SkuUpdateDTO;
import com.yirancrazy.minimall.goods.service.SkuService;
import com.yirancrazy.minimall.goods.vo.SkuVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品控制器，提供Sku RESTful API
 * @Version: 1.1
 * @DateTime: 2026/08/02
 */
@RestController
@RequestMapping("/api/v1/merchant/goods/skus")
public class SkuControllerV1 {

    private final SkuService skuService;

    public SkuControllerV1(SkuService skuService) {
        this.skuService = skuService;
    }

    /**
     * 根据主键查询 SKU 详情。
     *
     * @param merchantId 商家ID，来自可信Header
     * @param id SKU 主键 ID
     * @return SKU 视图，不存在或非本人商品时由 Service 层抛出业务异常
     */
    @GetMapping("/{id}")
    public Result<SkuVO> get(@RequestHeader("X-Merchant-Id") Long merchantId,
                             @PathVariable("id") Long id) {
        return Result.success(SkuVO.from(skuService.getById(id, merchantId)));
    }

    /**
     * 创建SKU，归属商家ID来自可信Header。
     * @param merchantId 商家ID，来自可信Header
     * @param dto SKU创建DTO
     * @return SKU ID
     */
    @PostMapping
    public Result<Long> create(@RequestHeader("X-Merchant-Id") Long merchantId,
                               @Valid @RequestBody SkuCreateDTO dto) {
        return Result.success(skuService.create(merchantId, dto));
    }

    /**
     * 游标分页查询SKU列表，支持按名称模糊搜索，按归属商家过滤。
     * @param merchantId 商家ID，来自可信Header
     * @param dto 游标分页查询入参
     * @return SKU 游标分页结果
     */
    @GetMapping
    public Result<CursorPageVO<SkuVO>> page(@RequestHeader("X-Merchant-Id") Long merchantId,
                                            @Valid SkuPageDTO dto) {
        dto.setMerchantId(merchantId);
        return Result.success(skuService.page(dto).map(SkuVO::from));
    }

    /**
     * 根据ID更新SKU信息。
     * @param merchantId 商家ID，来自可信Header
     * @param id SKU ID
     * @param dto SKU修改DTO
     * @return 更新是否成功
     */
    @PutMapping("/{id}")
    public Result<Boolean> update(@RequestHeader("X-Merchant-Id") Long merchantId,
                                  @PathVariable("id") Long id,
                                  @Valid @RequestBody SkuUpdateDTO dto) {
        return Result.success(skuService.update(id, merchantId, dto));
    }

    /**
     * 根据ID删除SKU。
     * @param merchantId 商家ID，来自可信Header
     * @param id SKU ID
     * @return 删除是否成功
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@RequestHeader("X-Merchant-Id") Long merchantId,
                                  @PathVariable("id") Long id) {
        return Result.success(skuService.delete(id, merchantId));
    }
}
