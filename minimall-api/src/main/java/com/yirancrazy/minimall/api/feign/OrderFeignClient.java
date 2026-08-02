package com.yirancrazy.minimall.api.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.yirancrazy.minimall.api.fallback.OrderFeignFallbackFactory;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Order Feign 客户端，调用Order服务接口
 * @Version: 1.2
 * @DateTime: 2026/08/02
 */
@FeignClient(name = "minimall-order-service", fallbackFactory = OrderFeignFallbackFactory.class)
public interface OrderFeignClient {
    @GetMapping("/internal/order/{id}")
    String status(@PathVariable("id") Long id);

    /**
     * 推进指定订单为已支付状态。
     * @param id 订单ID
     */
    @PostMapping("/internal/order/pay/{id}")
    void pay(@PathVariable("id") Long id);

    /**
     * Notify the order service of a refund result so it can advance status.
     * @param id the order id
     * @param success whether the refund succeeded
     */
    @PostMapping("/internal/order/refund-callback/{id}")
    void refundCallback(@PathVariable("id") Long id, @RequestParam("success") boolean success);
}