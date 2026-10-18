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
 * @Description: SpuControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/merchant/goods/spus")
public class SpuControllerV1 {

    private final SpuService spuService;

    /**
     * 构造 SpuControllerV1 实例。
     * @param spuService spuService 参数
     */
    public SpuControllerV1(SpuService spuService) {
        this.spuService = spuService;
    }


    /**
     * 获取相关数据。
     * @param merchantId merchantId 参数
     * @param id id 参数
     * @return 统一响应结果
     */
    @GetMapping("/{id}")
    public Result<SpuVO> get(@RequestHeader("X-Merchant-Id") Long merchantId,
                             @PathVariable("id") Long id) {
        return Result.success(spuService.getDetail(id, merchantId));
    }


    /**
     * 创建业务数据。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping
    public Result<Long> create(@RequestHeader("X-Merchant-Id") Long merchantId,
                               @Valid @RequestBody SpuCreateDTO dto) {
        return Result.success(spuService.create(merchantId, dto));
    }


    /**
     * 分页查询数据。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping
    public Result<CursorPageVO<SpuVO>> page(@RequestHeader("X-Merchant-Id") Long merchantId,
                                            @Valid SpuPageDTO dto) {
        dto.setMerchantId(merchantId);
        return Result.success(spuService.page(dto));
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
                                  @Valid @RequestBody SpuUpdateDTO dto) {
        return Result.success(spuService.update(id, merchantId, dto));
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
        return Result.success(spuService.delete(id, merchantId));
    }


    /**
     * 执行 onShelf 方法。
     * @param merchantId merchantId 参数
     * @param id id 参数
     * @return 统一响应结果
     */
    @PutMapping("/{id}/on-shelf")
    public Result<Boolean> onShelf(@RequestHeader("X-Merchant-Id") Long merchantId,
                                   @PathVariable("id") Long id) {
        return Result.success(spuService.onShelf(id, merchantId));
    }


    /**
     * 执行 offShelf 方法。
     * @param merchantId merchantId 参数
     * @param id id 参数
     * @return 统一响应结果
     */
    @PutMapping("/{id}/off-shelf")
    public Result<Boolean> offShelf(@RequestHeader("X-Merchant-Id") Long merchantId,
                                    @PathVariable("id") Long id) {
        return Result.success(spuService.offShelf(id, merchantId));
    }
}
