package com.yirancrazy.minimall.cart.controller.v1;

import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.cart.dto.CartItemAddDTO;
import com.yirancrazy.minimall.cart.dto.CartSelectAllDTO;
import com.yirancrazy.minimall.cart.dto.CartUpdateDTO;
import com.yirancrazy.minimall.cart.service.CartService;
import com.yirancrazy.minimall.cart.vo.CartItemVO;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车控制器，提供Cart RESTful API
 * @Version: 1.2
 * @DateTime: 2026/08/03
 */
@RestController
@RequestMapping("/api/v1/user/cart")
public class CartControllerV1 {

    private final CartService cartService;

    public CartControllerV1(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * 按当前登录用户查询其购物车全部条目，用户ID来自网关X-User-Id可信头。
     * @param userId 用户ID，来自网关X-User-Id头
     * @return 该用户购物车条目列表
     */
    @GetMapping
    public Result<List<CartItemVO>> list(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(cartService.listByUser(userId));
    }

    /**
     * 添加购物车项，用户ID来自网关X-User-Id可信头。
     * @param userId 用户ID，来自网关X-User-Id头
     * @param dto 购物车添加DTO
     * @return 购物车项ID
     */
    @PostMapping
    public Result<Long> add(@RequestHeader("X-User-Id") Long userId, @Valid @RequestBody CartItemAddDTO dto) {
        return Result.success(cartService.add(userId, dto));
    }

    /**
     * 根据购物车项 ID 逻辑删除该条目。
     *
     * @param id 购物车项 ID
     * @return 是否删除成功
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable("id") Long id) {
        return Result.success(cartService.delete(id));
    }

    /**
     * 部分更新购物车项，支持同时修改数量和勾选状态。
     * @param id 购物车项ID
     * @param dto 更新入参，仅非空字段生效
     * @return 更新是否成功
     */
    @PatchMapping("/{id}")
    public Result<Boolean> update(@PathVariable("id") Long id, @Valid @RequestBody CartUpdateDTO dto) {
        return Result.success(cartService.update(id, dto));
    }

    /**
     * 全选或取消全选当前登录用户的购物车项，用户ID来自网关X-User-Id可信头。
     * @param userId 用户ID，来自网关X-User-Id头
     * @param dto 全选入参
     * @return 更新是否成功
     */
    @PutMapping("/select-all")
    public Result<Boolean> selectAll(@RequestHeader("X-User-Id") Long userId,
                                     @Valid @RequestBody CartSelectAllDTO dto) {
        return Result.success(cartService.selectAll(
            userId, dto.getIsSelected() ? 1 : 0));
    }

    /**
     * 清空当前登录用户的购物车，用户ID来自网关X-User-Id可信头。
     * @param userId 用户ID，来自网关X-User-Id头
     * @return 清空是否成功
     */
    @DeleteMapping
    public Result<Boolean> clear(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(cartService.clear(userId));
    }

    /**
     * 将指定购物车商品移入收藏夹。
     * @param userId 用户ID，来自网关X-User-Id头
     * @param skuId 商品SKU ID
     * @return 无业务数据的成功响应
     */
    @PostMapping("/{skuId}/move-to-favorite")
    public Result<Void> moveToFavorite(@RequestHeader("X-User-Id") Long userId, @PathVariable("skuId") Long skuId) {
        cartService.moveToFavorite(userId, skuId);
        return Result.success(null);
    }
}
