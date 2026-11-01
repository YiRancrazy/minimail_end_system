package com.yirancrazy.minimall.merchant.controller.v1;

import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.merchant.dto.ShopCreateDto;
import com.yirancrazy.minimall.merchant.dto.ShopUpdateDto;
import com.yirancrazy.minimall.merchant.service.ShopService;
import com.yirancrazy.minimall.merchant.vo.ShopVo;
import jakarta.validation.Valid;
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

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家店铺 HTTP 接口；基于可信 X-Merchant-Id 提供店铺查询、游标列表、创建、更新和删除能力，不在此层处理登录鉴权。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/merchant/shops")
public class ShopControllerV1 {

    private final ShopService shopService;

    /**
     * 注入店铺领域服务。
     *
     * @param shopService 店铺领域服务
     */
    public ShopControllerV1(ShopService shopService) {
        this.shopService = shopService;
    }

    /**
     * 查询当前商家自有店铺详情。
     *
     * @param merchantId 可信商家 ID
     * @param id 店铺主键
     * @return 统一响应，数据为店铺视图
     * @throws BizException 店铺不存在或不属于该商家时抛出 SHOP_NOT_FOUND
     */
    @GetMapping("/{id}")
    public Result<ShopVo> get(
            @RequestHeader("X-Merchant-Id") Long merchantId, @PathVariable("id") Long id) {
        return Result.success(shopService.getVo(merchantId, id));
    }

    /**
     * 按 ID 倒序查询当前商家的店铺列表。
     *
     * @param merchantId 可信商家 ID
     * @param cursor 上一页末条店铺 ID，可为空
     * @param limit 期望返回条数，服务层默认 20 且最大 100
     * @return 统一响应，数据为店铺视图列表
     */
    @GetMapping
    public Result<java.util.List<ShopVo>> list(
            @RequestHeader("X-Merchant-Id") Long merchantId,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return Result.success(shopService.listVo(merchantId, cursor, limit));
    }

    /**
     * 为当前商家创建店铺。
     *
     * @param merchantId 可信商家身份，对应商家表的 userId
     * @param dto 已校验的店铺创建内容
     * @return 统一响应，数据为新店铺主键
     * @throws BizException 商家不存在或资质未审核通过时抛出 SHOP_MERCHANT_NOT_APPROVED
     */
    @PostMapping
    public Result<Long> create(
            @RequestHeader("X-Merchant-Id") Long merchantId, @Valid @RequestBody ShopCreateDto dto) {
        return Result.success(shopService.create(merchantId, dto));
    }

    /**
     * 更新当前商家的自有店铺。
     *
     * @param merchantId 可信商家 ID
     * @param id 店铺主键
     * @param dto 已校验的更新内容，status 可省略
     * @return 统一响应，数据表示更新是否成功
     * @throws BizException 店铺归属校验失败、设置冻结状态或状态别名非法时抛出
     */
    @PutMapping("/{id}")
    public Result<Boolean> update(
            @RequestHeader("X-Merchant-Id") Long merchantId,
            @PathVariable("id") Long id,
            @Valid @RequestBody ShopUpdateDto dto) {
        return Result.success(shopService.update(merchantId, id, dto));
    }

    /**
     * 逻辑删除当前商家的自有店铺。
     *
     * @param merchantId 可信商家 ID
     * @param id 店铺主键
     * @return 统一响应，数据表示删除是否成功
     * @throws BizException 店铺不存在或不属于该商家时抛出 SHOP_NOT_FOUND
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(
            @RequestHeader("X-Merchant-Id") Long merchantId, @PathVariable("id") Long id) {
        return Result.success(shopService.delete(merchantId, id));
    }
}
