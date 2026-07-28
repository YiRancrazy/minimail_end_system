package com.yirancrazy.minimall.order.controller.v1;

import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.order.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/order")
public class OrderControllerV1 {

    private final OrderService orderService;

    public OrderControllerV1(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Result<Long> create(@RequestParam("userId") Long userId,
                               @RequestParam("skuId") Long skuId,
                               @RequestParam("quantity") Integer quantity) {
        return Result.success(orderService.create(userId, skuId, quantity));
    }

    @PostMapping("/{id}/pay")
    public Result<Boolean> pay(@PathVariable("id") Long id) {
        return Result.success(orderService.pay(id));
    }

    @GetMapping("/{id}")
    public Result<String> status(@PathVariable("id") Long id) {
        return Result.success(orderService.status(id));
    }
}