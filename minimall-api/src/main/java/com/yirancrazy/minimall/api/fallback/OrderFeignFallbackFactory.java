package com.yirancrazy.minimall.api.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.feign.OrderFeignClient;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderFeign Feign 降级工厂，处理OrderFeign服务调用失败降级
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Slf4j
@Component
public class OrderFeignFallbackFactory implements FallbackFactory<OrderFeignClient> {
    @Override
    public OrderFeignClient create(Throwable cause) {
        log.warn("order-service unreachable: {}", cause.getMessage());
        return new OrderFeignClient() {
            @Override
            public String status(Long id) {
                return "DOWN";
            }

            @Override
            public void pay(Long id) {
                log.warn("order pay fallback, orderId={} skipped", id);
            }

            @Override
            public void refundCallback(Long id, boolean success) {
                log.warn("order refund-callback fallback, orderId={}, success={} skipped", id, success);
            }
        };
    }
}