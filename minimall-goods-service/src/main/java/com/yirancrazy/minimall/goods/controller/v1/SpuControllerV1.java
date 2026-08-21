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
import com.yirancrazy.minimall.goods.dto.SpuCreateDTO;
import com.yirancrazy.minimall.goods.dto.SpuPageDTO;
import com.yirancrazy.minimall.goods.dto.SpuUpdateDTO;
import com.yirancrazy.minimall.goods.service.SpuService;
import com.yirancrazy.minimall.goods.vo.SpuVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品控制器，提供Spu RESTful API，商家身份由 X-Merchant-Id 透传
 * @Version: 1.0
 * @DateTime: 2026/08/02
 */
@RestController
@RequestMapping("/api/v1/merchant/goods/spus")
public class SpuControllerV1 {

    private final SpuService spuService;

    public SpuControllerV1(SpuService spuService) {
        this.spuService = spuService;
    }

    /**
     * 根据主键查询 SPU 详情（含 SKU 列表，供编辑页回显）。
     * @param merchantId 商家ID，来自可信Header
     * @param id SPU 主键 ID
     * @return SPU 视图，不存在或非本人商品时由 Service 层抛出业务异常
     */
    @GetMapping("/{id}")
    public Result<SpuVO> get(@RequestHeader("X-Merchant-Id") Long merchantId,
                             @PathVariable("id") Long id) {
        return Result.success(spuService.getDetail(id, merchantId));
    }

    /**
     * 创建 SPU，商家ID来自可信 Header。
     * @param merchantId 商家ID
     * @param dto SPU创建DTO
     * @return SPU ID
     */
    @PostMapping
    public Result<Long> create(@RequestHeader("X-Merchant-Id") Long merchantId,
                               @Valid @RequestBody SpuCreateDTO dto) {
        return Result.success(spuService.create(merchantId, dto));
    }

    /**
     * 游标分页查询 SPU 列表，按商家、状态、标题过滤。
     * @param merchantId 商家ID
     * @param dto 游标分页查询入参
     * @return SPU 游标分页结果
     */
    @GetMapping
    public Result<CursorPageVO<SpuVO>> page(@RequestHeader("X-Merchant-Id") Long merchantId,
                                            @Valid SpuPageDTO dto) {
        dto.setMerchantId(merchantId);
        return Result.success(spuService.page(dto));
    }

    /**
     * 根据ID更新 SPU 信息。
     * @param merchantId 商家ID，来自可信Header
     * @param id SPU ID
     * @param dto SPU修改DTO
     * @return 更新是否成功
     */
    @PutMapping("/{id}")
    public Result<Boolean> update(@RequestHeader("X-Merchant-Id") Long merchantId,
                                  @PathVariable("id") Long id,
                                  @Valid @RequestBody SpuUpdateDTO dto) {
        return Result.success(spuService.update(id, merchantId, dto));
    }

    /**
     * 根据ID删除 SPU。
     * @param merchantId 商家ID，来自可信Header
     * @param id SPU ID
     * @return 删除是否成功
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@RequestHeader("X-Merchant-Id") Long merchantId,
                                  @PathVariable("id") Long id) {
        return Result.success(spuService.delete(id, merchantId));
    }

    /**
     * 上架 SPU。
     * @param merchantId 商家ID，来自可信Header
     * @param id SPU ID
     * @return 上架是否成功
     */
    @PutMapping("/{id}/on-shelf")
    public Result<Boolean> onShelf(@RequestHeader("X-Merchant-Id") Long merchantId,
                                   @PathVariable("id") Long id) {
        return Result.success(spuService.onShelf(id, merchantId));
    }

    /**
     * 下架 SPU。
     * @param merchantId 商家ID，来自可信Header
     * @param id SPU ID
     * @return 下架是否成功
     */
    @PutMapping("/{id}/off-shelf")
    public Result<Boolean> offShelf(@RequestHeader("X-Merchant-Id") Long merchantId,
                                    @PathVariable("id") Long id) {
        return Result.success(spuService.offShelf(id, merchantId));
    }
}
