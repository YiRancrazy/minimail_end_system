package com.yirancrazy.minimall.api.fallback;

import com.yirancrazy.minimall.api.dto.pay.PayCreateDTO;
import com.yirancrazy.minimall.api.feign.PayFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 支付服务 Feign 降级工厂，在支付服务不可用时返回受控降级结果。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Slf4j
@Component
public class PayFeignFallbackFactory implements FallbackFactory<PayFeignClient> {
    @Override
    public PayFeignClient create(Throwable cause) {
        log.warn("pay-service unreachable: {}", cause.getMessage());
        return (PayCreateDTO dto) -> -1L;
    }
}