package com.yirancrazy.minimall.merchant.controller.v1;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.merchant.dto.ShopCreateDTO;
import com.yirancrazy.minimall.merchant.dto.ShopUpdateDTO;
import com.yirancrazy.minimall.merchant.service.ShopService;
import com.yirancrazy.minimall.merchant.vo.ShopVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: ShopControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/merchant/shops")
public class ShopControllerV1 {

    private final ShopService shopService;

    /**
     * 构造 ShopControllerV1 实例。
     * @param shopService shopService 参数
     */
    public ShopControllerV1(ShopService shopService) {
        this.shopService = shopService;
    }


    /**
     * 获取相关数据。
     * @param merchantId merchantId 参数
     * @param id id 参数
     * @return 统一响应结果
     */
    @GetMapping("/{id}")
    public Result<ShopVO> get(
            @RequestHeader("X-Merchant-Id") Long merchantId,
            @PathVariable("id") Long id) {
        return Result.success(shopService.getVO(merchantId, id));
    }


    /**
     * 查询列表数据。
     * @param merchantId merchantId 参数
     * @param cursor cursor 参数
     * @param limit limit 参数
     * @return 统一响应结果
     */
    @GetMapping
    public Result<java.util.List<ShopVO>> list(
            @RequestHeader("X-Merchant-Id") Long merchantId,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return Result.success(shopService.listVO(merchantId, cursor, limit));
    }


    /**
     * 创建业务数据。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping
    public Result<Long> create(
            @RequestHeader("X-Merchant-Id") Long merchantId,
            @Valid @RequestBody ShopCreateDTO dto) {
        return Result.success(shopService.create(merchantId, dto));
    }


    /**
     * 更新业务数据。
     * @param merchantId merchantId 参数
     * @param id id 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PutMapping("/{id}")
    public Result<Boolean> update(
            @RequestHeader("X-Merchant-Id") Long merchantId,
            @PathVariable("id") Long id,
            @Valid @RequestBody ShopUpdateDTO dto) {
        return Result.success(shopService.update(merchantId, id, dto));
    }


    /**
     * 删除业务数据。
     * @param merchantId merchantId 参数
     * @param id id 参数
     * @return 统一响应结果
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(
            @RequestHeader("X-Merchant-Id") Long merchantId,
            @PathVariable("id") Long id) {
        return Result.success(shopService.delete(merchantId, id));
    }
}
