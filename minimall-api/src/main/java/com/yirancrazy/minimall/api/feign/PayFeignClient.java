package com.yirancrazy.minimall.api.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.yirancrazy.minimall.api.dto.pay.PayCreateDTO;
import com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO;
import com.yirancrazy.minimall.api.fallback.PayFeignFallbackFactory;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Pay Feign 客户端，调用Pay服务接口
 * @Version: 1.3
 * @DateTime: 2026/08/02
 */
@FeignClient(name = "minimall-pay-service", fallbackFactory = PayFeignFallbackFactory.class)
public interface PayFeignClient {
    @PostMapping("/internal/pay/create")
    Result<Long> create(@RequestBody PayCreateDTO dto);

    /**
     * Create a refund for an existing payment.
     * @param dto the refund creation request
     * @return true if the refund was created and processed successfully
     */
    @PostMapping("/internal/pay/refund")
    Result<Boolean> refund(@RequestBody RefundCreateDTO dto);
}
