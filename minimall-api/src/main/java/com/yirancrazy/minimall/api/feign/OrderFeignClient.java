package com.yirancrazy.minimall.api.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import com.yirancrazy.minimall.api.fallback.OrderFeignFallbackFactory;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Order Feign 客户端，调用Order服务接口
 * @Version: 1.1
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
}