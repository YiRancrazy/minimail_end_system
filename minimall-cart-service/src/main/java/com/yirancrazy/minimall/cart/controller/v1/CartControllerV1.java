package com.yirancrazy.minimall.cart.controller.v1;

import java.util.List;
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
import com.yirancrazy.minimall.cart.dto.CartClearDTO;
import com.yirancrazy.minimall.cart.dto.CartItemAddDTO;
import com.yirancrazy.minimall.cart.dto.CartItemListDTO;
import com.yirancrazy.minimall.cart.dto.CartSelectAllDTO;
import com.yirancrazy.minimall.cart.dto.CartSelectDTO;
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
@RequestMapping("/api/v1/cart")
public class CartControllerV1 {

    private final CartService cartService;

    public CartControllerV1(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * 按用户 ID 查询其购物车全部条目。
     *
     * @param dto 查询条件
     * @return 该用户购物车条目列表
     */
    @GetMapping
    public Result<List<CartItemVO>> list(@Valid CartItemListDTO dto) {
        return Result.success(cartService.listByUser(dto).stream().map(CartItemVO::from).toList());
    }

    /**
     * 添加购物车项。
     * @param dto 购物车添加DTO
     * @return 购物车项ID
     */
    @PostMapping
    public Result<Long> add(@Valid @RequestBody CartItemAddDTO dto) {
        return Result.success(cartService.add(dto));
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
     * 修改购物车项数量。
     * @param id 购物车项ID
     * @param dto 数量修改入参
     * @return 更新是否成功
     */
    @PutMapping("/{id}/quantity")
    public Result<Boolean> updateQuantity(@PathVariable("id") Long id, @Valid @RequestBody CartUpdateDTO dto) {
        return Result.success(cartService.updateQuantity(id, dto));
    }

    /**
     * 修改购物车项勾选状态。
     * @param id 购物车项ID
     * @param dto 勾选状态入参
     * @return 更新是否成功
     */
    @PutMapping("/{id}/select")
    public Result<Boolean> select(@PathVariable("id") Long id, @Valid @RequestBody CartSelectDTO dto) {
        return Result.success(cartService.select(id, dto));
    }

    /**
     * 全选或取消全选指定用户的购物车项。
     * @param dto 全选入参
     * @return 更新是否成功
     */
    @PutMapping("/select-all")
    public Result<Boolean> selectAll(@Valid @RequestBody CartSelectAllDTO dto) {
        return Result.success(cartService.selectAll(dto));
    }

    /**
     * 清空指定用户的购物车。
     * @param userId 用户ID
     * @return 清空是否成功
     */
    @DeleteMapping
    public Result<Boolean> clear(@RequestBody CartClearDTO dto) {
        return Result.success(cartService.clear(dto.getUserId()));
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
