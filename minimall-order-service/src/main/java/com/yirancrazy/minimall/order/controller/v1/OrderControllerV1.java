package com.yirancrazy.minimall.order.controller.v1;

import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.order.dto.OrderCreateDTO;
import com.yirancrazy.minimall.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单 C 端接口控制器，提供创建订单、发起支付及订单状态查询等对外业务接口。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@RestController
@RequestMapping("/api/v1/order")
public class OrderControllerV1 {

    private final OrderService orderService;

    public OrderControllerV1(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 创建订单并完成库存锁定及支付流水初始化。
     *
     * @param dto 订单创建请求参数
     * @return 新创建的订单标识
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody OrderCreateDTO dto) {
        return Result.success(orderService.create(dto.getUserId(), dto.getSkuId(), dto.getQuantity()));
    }

    /**
     * 推进指定待支付订单的支付流程并返回处理结果。
     *
     * @param id 订单标识
     * @return 支付成功返回 true，订单状态不允许支付时返回 false
     */
    @PostMapping("/{id}/pay")
    public Result<Boolean> pay(@PathVariable("id") Long id) {
        return Result.success(orderService.pay(id));
    }

    @GetMapping("/{id}")
    public Result<String> status(@PathVariable("id") Long id) {
        return Result.success(orderService.status(id));
    }
}