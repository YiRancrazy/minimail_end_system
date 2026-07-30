package com.yirancrazy.minimall.api.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.pay.PayCreateDTO;
import com.yirancrazy.minimall.api.feign.PayFeignClient;

/**
* 支付服务 Feign 降级工厂，在支付服务不可用时返回受控降级结果。
 */
@Slf4j
@Component
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