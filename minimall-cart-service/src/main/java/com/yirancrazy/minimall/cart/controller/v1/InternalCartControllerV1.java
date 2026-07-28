package com.yirancrazy.minimall.cart.controller.v1;

import com.yirancrazy.minimall.cart.service.CartService;
import com.yirancrazy.minimall.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/cart")
public class InternalCartControllerV1 {

    private final CartService cartService;

    public InternalCartControllerV1(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/count")
    public Result<Long> count(@RequestParam Long userId) {
        return Result.success(cartService.countByUser(userId));
    }
}