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
import com.yirancrazy.minimall.merchant.entity.ShopPO;
import com.yirancrazy.minimall.merchant.service.ShopService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商户控制器，提供Shop RESTful API
 * @Version: 1.1
 * @DateTime: 2026/08/05
 **/
@RestController
@RequestMapping("/api/v1/merchant/shops")
public class ShopControllerV1 {

    private final ShopService shopService;

    public ShopControllerV1(ShopService shopService) {
        this.shopService = shopService;
    }

    /**
     * 根据主键 ID 查询店铺详情。
     *
     * @param merchantId 商家账号 ID（来自网关 X-Merchant-Id）
     * @param id 店铺主键 ID
     * @return 店铺详情结果
     */
    @GetMapping("/{id}")
    public Result<ShopPO> get(
            @RequestHeader("X-Merchant-Id") Long merchantId,
            @PathVariable("id") Long id) {
        return Result.success(shopService.getById(merchantId, id));
    }

    /**
     * 分页查询当前商家名下的店铺列表。
     *
     * @param merchantId 商家账号 ID（来自网关 X-Merchant-Id）
     * @param cursor 上一页最后一条记录的 ID；为空表示首页
     * @param limit 每页大小，默认 20，最大 100
     */
    @GetMapping
    public Result<java.util.List<ShopPO>> list(
            @RequestHeader("X-Merchant-Id") Long merchantId,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return Result.success(shopService.list(merchantId, cursor, limit));
    }

    /**
     * 创建店铺，归属绑定到当前商家。
     * @param merchantId 商家账号 ID（来自网关 X-Merchant-Id）
     * @param dto 店铺创建DTO
     * @return 店铺ID
     */
    @PostMapping
    public Result<Long> create(
            @RequestHeader("X-Merchant-Id") Long merchantId,
            @Valid @RequestBody ShopCreateDTO dto) {
        return Result.success(shopService.create(merchantId, dto));
    }

    /**
     * 根据主键 ID 更新店铺信息，仅允许更新本人名下店铺。
     *
     * @param merchantId 商家账号 ID（来自网关 X-Merchant-Id）
     * @param id 店铺主键 ID
     * @param dto 待更新的店铺信息
     * @return 是否更新成功
     */
    @PutMapping("/{id}")
    public Result<Boolean> update(
            @RequestHeader("X-Merchant-Id") Long merchantId,
            @PathVariable("id") Long id,
            @Valid @RequestBody ShopUpdateDTO dto) {
        return Result.success(shopService.update(merchantId, id, dto));
    }

    /**
     * 删除店铺，仅允许删除本人名下店铺。
     * @param merchantId 商家账号 ID（来自网关 X-Merchant-Id）
     * @param id 店铺ID
     * @return 删除是否成功
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(
            @RequestHeader("X-Merchant-Id") Long merchantId,
            @PathVariable("id") Long id) {
        return Result.success(shopService.delete(merchantId, id));
    }
}