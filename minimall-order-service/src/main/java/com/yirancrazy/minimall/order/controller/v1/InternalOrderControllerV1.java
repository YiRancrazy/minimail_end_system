package com.yirancrazy.minimall.order.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.order.service.OrderService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: InternalOrderControllerV1 description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
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
}