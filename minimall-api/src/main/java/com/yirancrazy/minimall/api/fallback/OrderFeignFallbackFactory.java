package com.yirancrazy.minimall.api.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.feign.OrderFeignClient;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderFeign Feign 降级工厂，处理OrderFeign服务调用失败降级
 * @Version: 1.2
 * @DateTime: 2026/08/02
 */
@Slf4j
@Component
public class OrderFeignFallbackFactory implements FallbackFactory<OrderFeignClient> {
    @Override
    public OrderFeignClient create(Throwable cause) {
        log.warn("order-service unreachable: {}", cause.getMessage());
        return new OrderFeignClient() {
            @Override
            public Result<Integer> status(Long id) {
                return Result.success(null);
            }

            @Override
            public Result<Long> merchantId(String ref) {
                log.warn("order merchant resolve fallback, ref={} skipped", ref);
                return Result.success(null);
            }

            @Override
            public Result<Void> pay(Long id) {
                log.warn("order pay fallback, orderId={} skipped", id);
                return Result.success(null);
            }

            @Override
            public Result<Void> payByOrderNo(String orderNo) {
                log.warn("order pay-by-order-no fallback, orderNo={} skipped", orderNo);
                return Result.success(null);
            }

            @Override
            public Result<Void> refundCallback(Long id, boolean success) {
                log.warn("order refund-callback fallback, orderId={}, success={} skipped", id, success);
                return Result.success(null);
            }
        };
    }
}
