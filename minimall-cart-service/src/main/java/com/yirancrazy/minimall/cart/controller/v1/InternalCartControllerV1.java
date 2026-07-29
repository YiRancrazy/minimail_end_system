package com.yirancrazy.minimall.cart.controller.v1;

import com.yirancrazy.minimall.cart.service.CartService;
import com.yirancrazy.minimall.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车内部接口控制器，提供购物车条目数量统计等内部命令调用。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@RestController
@RequestMapping("/internal/cart")
public class InternalCartControllerV1 {

    private final CartService cartService;

    public InternalCartControllerV1(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * 统计指定用户的购物车条目数量。
     *
     * @param userId 用户 ID
     * @return 该用户购物车条目数量
     */
    @GetMapping("/count")
    public Result<Long> count(@RequestParam Long userId) {
        return Result.success(cartService.countByUser(userId));
    }
}