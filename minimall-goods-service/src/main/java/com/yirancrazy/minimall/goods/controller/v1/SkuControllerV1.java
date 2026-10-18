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
 * @Description: SkuControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/merchant/goods/skus")
public class SkuControllerV1 {

    private final SkuService skuService;

    /**
     * 构造 SkuControllerV1 实例。
     * @param skuService skuService 参数
     */
    public SkuControllerV1(SkuService skuService) {
        this.skuService = skuService;
    }


    /**
     * 获取相关数据。
     * @param merchantId merchantId 参数
     * @param id id 参数
     * @return 统一响应结果
     */
    @GetMapping("/{id}")
    public Result<SkuVO> get(@RequestHeader("X-Merchant-Id") Long merchantId,
                             @PathVariable("id") Long id) {
        return Result.success(skuService.getVO(id, merchantId));
    }


    /**
     * 创建业务数据。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping
    public Result<Long> create(@RequestHeader("X-Merchant-Id") Long merchantId,
                               @Valid @RequestBody SkuCreateDTO dto) {
        return Result.success(skuService.create(merchantId, dto));
    }


    /**
     * 分页查询数据。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping
    public Result<CursorPageVO<SkuVO>> page(@RequestHeader("X-Merchant-Id") Long merchantId,
                                            @Valid SkuPageDTO dto) {
        dto.setMerchantId(merchantId);
        return Result.success(skuService.pageVO(dto));
    }


    /**
     * 更新业务数据。
     * @param merchantId merchantId 参数
     * @param id id 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PutMapping("/{id}")
    public Result<Boolean> update(@RequestHeader("X-Merchant-Id") Long merchantId,
                                  @PathVariable("id") Long id,
                                  @Valid @RequestBody SkuUpdateDTO dto) {
        return Result.success(skuService.update(id, merchantId, dto));
    }


    /**
     * 删除业务数据。
     * @param merchantId merchantId 参数
     * @param id id 参数
     * @return 统一响应结果
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@RequestHeader("X-Merchant-Id") Long merchantId,
                                  @PathVariable("id") Long id) {
        return Result.success(skuService.delete(id, merchantId));
    }
}
