package com.yirancrazy.minimall.cart.controller.v1;

import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.cart.dto.CartItemAddDTO;
import com.yirancrazy.minimall.cart.dto.CartItemListDTO;
import com.yirancrazy.minimall.cart.entity.CartItemPO;
import com.yirancrazy.minimall.cart.service.CartService;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车控制器，提供Cart RESTful API
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
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
    public Result<List<CartItemPO>> list(@Valid CartItemListDTO dto) {
        return Result.success(cartService.listByUser(dto));
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
}