package com.yirancrazy.minimall.cart.controller.v1;

import com.yirancrazy.minimall.cart.dto.CartItemAddDto;
import com.yirancrazy.minimall.cart.dto.CartMoveToFavoriteDto;
import com.yirancrazy.minimall.cart.dto.CartSelectAllDto;
import com.yirancrazy.minimall.cart.dto.CartUpdateDto;
import com.yirancrazy.minimall.cart.service.CartService;
import com.yirancrazy.minimall.cart.vo.CartItemVo;
import com.yirancrazy.minimall.common.result.Result;
import jakarta.validation.Valid;
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

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供用户购物车查询、添加、更新、删除、勾选、清空和移入收藏接口。控制器信任网关
 *     边界写入的 X-User-Id 请求头，将归属校验和业务规则全部委托给 CartService，并用统一 Result
 *     包装成功响应。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/user/cart")
public class CartControllerV1 {

    private final CartService cartService;

    /**
     * 创建用户购物车控制器.
     *
     * @param cartService 负责购物车业务规则的应用服务
     */
    public CartControllerV1(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * 查询指定用户的全部购物车条目.
     *
     * @param userId 网关可信请求头中的用户标识
     * @return 按数据层顺序返回购物车视图的统一响应；购物车为空时返回空列表
     */
    @GetMapping
    public Result<List<CartItemVo>> list(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(cartService.listByUser(userId));
    }

    /**
     * 通过服务层原子 upsert 向用户购物车添加指定 SKU 数量.
     *
     * @param userId 网关可信请求头中的用户标识
     * @param dto 已校验的 SKU、数量及可选勾选状态
     * @return 包含持久化购物车条目主键的统一响应
     */
    @PostMapping
    public Result<Long> add(
            @RequestHeader("X-User-Id") Long userId, @Valid @RequestBody CartItemAddDto dto) {
        return Result.success(cartService.add(userId, dto));
    }

    /**
     * 删除指定用户拥有的一个购物车条目.
     *
     * @param id 路径中的购物车条目主键
     * @param userId 由 CartService 用于归属校验的用户标识
     * @return 表示删除是否影响数据行的统一响应
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(
            @PathVariable("id") Long id, @RequestHeader("X-User-Id") Long userId) {
        return Result.success(cartService.delete(id, userId));
    }

    /**
     * 部分更新指定用户拥有的购物车条目数量或勾选状态.
     *
     * @param id 路径中的购物车条目主键
     * @param userId 由 CartService 用于归属校验的用户标识
     * @param dto 可选数量与勾选字段；空字段保持原值
     * @return 表示数据层更新是否成功的统一响应
     */
    @PatchMapping("/{id}")
    public Result<Boolean> update(
            @PathVariable("id") Long id,
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CartUpdateDto dto) {
        return Result.success(cartService.update(id, userId, dto));
    }

    /**
     * 将同一勾选状态应用到用户拥有的全部购物车条目.
     *
     * @param userId 网关可信请求头中的用户标识
     * @param dto 已校验的布尔勾选状态
     * @return 表示批量更新是否成功的统一响应
     */
    @PutMapping("/select-all")
    public Result<Boolean> selectAll(
            @RequestHeader("X-User-Id") Long userId, @Valid @RequestBody CartSelectAllDto dto) {
        return Result.success(cartService.selectAll(userId, dto.getIsSelected() ? 1 : 0));
    }

    /**
     * 删除指定用户拥有的全部购物车条目.
     *
     * @param userId 网关可信请求头中的用户标识
     * @return 表示批量删除是否成功的统一响应
     */
    @DeleteMapping
    public Result<Boolean> clear(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(cartService.clear(userId));
    }

    /**
     * 将指定且属于当前用户的购物车条目移入收藏夹.
     *
     * @param userId 网关可信请求头中的用户标识
     * @param dto 非空的待转换购物车条目主键
     * @return 服务完成远程收藏和本地删除后的空成功响应
     */
    @PostMapping("/move-to-favorite")
    public Result<Void> moveToFavorite(
            @RequestHeader("X-User-Id") Long userId, @Valid @RequestBody CartMoveToFavoriteDto dto) {
        cartService.moveToFavorite(userId, dto.getItemIds());
        return Result.success(null);
    }
}
