package com.yirancrazy.minimall.order.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.order.service.OrderService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单内部控制器，提供Order相关内部接口
 * @Version: 1.1
 * @DateTime: 2026/08/02
 */
@RestController
@RequestMapping("/internal/order")
public class InternalOrderControllerV1 {

    private final OrderService orderService;

    public InternalOrderControllerV1(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 查询指定订单的当前业务状态。
     *
     * @param id 订单标识
     * @return 订单状态；订单不存在时返回 UNKNOWN
     */
    @GetMapping("/{id}")
    public Result<Integer> status(@PathVariable Long id) {
        return Result.success(orderService.getStatus(id));
    }

    /**
     * 推进指定订单为已支付状态，并广播 OrderPaidDTO 事件。
     *
     * @param id 订单标识
     * @return 空响应体
     */
    @PostMapping("/pay/{id}")
    public Result<Void> pay(@PathVariable Long id) {
        orderService.pay(id);
        return Result.success(null);
    }
}