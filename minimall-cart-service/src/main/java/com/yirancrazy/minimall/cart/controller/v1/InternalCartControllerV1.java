package com.yirancrazy.minimall.cart.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.cart.dto.CartCountDTO;
import com.yirancrazy.minimall.cart.service.CartService;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车内部控制器，提供Cart相关内部接口
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class InternalCartControllerV1 {

    private final CartService cartService;

    public InternalCartControllerV1(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * 统计指定用户的购物车条目数量。
     *
     * @param dto 查询条件
     * @return 该用户购物车条目数量
     */
    @GetMapping("/count")
    public Result<Long> count(@Valid CartCountDTO dto) {
        return Result.success(cartService.countByUser(dto.getUserId()));
    }
}