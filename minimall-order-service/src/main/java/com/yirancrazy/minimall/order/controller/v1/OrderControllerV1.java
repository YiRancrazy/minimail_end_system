package com.yirancrazy.minimall.order.controller.v1;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.order.dto.OrderCreateDTO;
import com.yirancrazy.minimall.order.dto.OrderPageDTO;
import com.yirancrazy.minimall.order.entity.OrderPO;
import com.yirancrazy.minimall.order.service.OrderService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单控制器，提供Order RESTful API
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderControllerV1 {

    private final OrderService orderService;

    public OrderControllerV1(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 分页查询订单列表，支持按用户/商家/状态过滤。
     * @param dto 分页查询入参
     * @return 订单分页结果
     */
    @GetMapping
    public Result<IPage<OrderPO>> page(@Valid OrderPageDTO dto) {
        return Result.success(orderService.page(dto));
    }

    /**
     * 商家端分页查询订单列表，merchantId 由可信 Header 注入。
     * @param merchantId 商家ID
     * @param dto 分页查询入参
     * @return 订单分页结果
     */
    @GetMapping("/merchant")
    public Result<IPage<OrderPO>> merchantPage(@RequestHeader("X-Merchant-Id") Long merchantId,
                                               @Valid OrderPageDTO dto) {
        dto.setMerchantId(merchantId);
        return Result.success(orderService.page(dto));
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
    public Result<Void> pay(@PathVariable("id") Long id) {
        orderService.pay(id);
        return Result.success(null);
    }

    /**
     * 查询订单状态。
     * @param id 订单ID
     * @return 订单状态
     */
    @GetMapping("/{id}")
    public Result<Integer> status(@PathVariable("id") Long id) {
        return Result.success(orderService.getStatus(id));
    }

    /**
     * 查询订单详情。
     * @param id 订单ID
     * @return 订单实体
     */
    @GetMapping("/{id}/detail")
    public Result<OrderPO> detail(@PathVariable("id") Long id) {
        return Result.success(orderService.getDetail(id));
    }

    /**
     * 取消订单。
     * @param orderId 订单ID
     * @param userId  当前用户ID
     */
    @PostMapping("/{orderId}/cancel")
    public Result<Void> cancel(@PathVariable Long orderId,
                               @RequestHeader("X-User-Id") Long userId) {
        orderService.cancel(orderId, userId);
        return Result.success(null);
    }

    /**
     * 发货。
     * @param orderId    订单ID
     * @param merchantId 商户ID
     */
    @PostMapping("/{orderId}/ship")
    public Result<Void> ship(@PathVariable Long orderId,
                             @RequestHeader("X-User-Id") Long merchantId) {
        orderService.ship(orderId, merchantId);
        return Result.success(null);
    }

    /**
     * 确认收货。
     * @param orderId 订单ID
     * @param userId  当前用户ID
     */
    @PostMapping("/{orderId}/confirm")
    public Result<Void> confirm(@PathVariable Long orderId,
                                @RequestHeader("X-User-Id") Long userId) {
        orderService.confirm(orderId, userId);
        return Result.success(null);
    }

    /**
     * 申请退款。
     * @param orderId 订单ID
     */
    @PostMapping("/{orderId}/refund")
    public Result<Void> refund(@PathVariable Long orderId) {
        orderService.refund(orderId);
        return Result.success(null);
    }

    /**
     * 商家关闭订单，仅允许待支付订单关闭并释放库存。
     * @param orderId 订单ID
     * @param merchantId 商家ID
     */
    @PostMapping("/{orderId}/merchant-close")
    public Result<Void> merchantClose(@PathVariable Long orderId,
                                      @RequestHeader("X-Merchant-Id") Long merchantId) {
        orderService.merchantClose(orderId, merchantId);
        return Result.success(null);
    }

    /**
     * 用户删除订单，仅允许终态订单软删除。
     * @param orderId 订单ID
     * @param userId 用户ID
     */
    @DeleteMapping("/{orderId}")
    public Result<Void> delete(@PathVariable Long orderId,
                               @RequestHeader("X-User-Id") Long userId) {
        orderService.delete(orderId, userId);
        return Result.success(null);
    }
}