package com.yirancrazy.minimall.order.controller.v1;

import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.order.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/order")
public class InternalOrderControllerV1 {

    private final OrderService orderService;

    public InternalOrderControllerV1(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{id}")
    public Result<String> status(@PathVariable Long id) {
        return Result.success(orderService.status(id));
    }
}