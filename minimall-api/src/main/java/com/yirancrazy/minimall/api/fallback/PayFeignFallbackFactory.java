package com.yirancrazy.minimall.api.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.pay.PayCreateDTO;
import com.yirancrazy.minimall.api.feign.PayFeignClient;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayFeign Feign 降级工厂，处理PayFeign服务调用失败降级
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class PayFeignFallbackFactory implements FallbackFactory<PayFeignClient> {
    @Override
    public PayFeignClient create(Throwable cause) {
        log.warn("pay-service unreachable: {}", cause.getMessage());
        return new PayFeignClient() {
            @Override
            public Long create(PayCreateDTO dto) {
                return -1L;
            }

            @Override
            public Boolean callback(Long payId) {
                return false;
            }
        };
    }
}