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
 * @Description: InternalCartControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/internal/cart")
public class InternalCartControllerV1 {

    private final CartService cartService;

    /**
     * 构造 InternalCartControllerV1 实例。
     * @param cartService cartService 参数
     */
    public InternalCartControllerV1(CartService cartService) {
        this.cartService = cartService;
    }


    /**
     * 统计业务数据。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping("/count")
    public Result<Long> count(@Valid CartCountDTO dto) {
        return Result.success(cartService.countByUser(dto.getUserId()));
    }
}