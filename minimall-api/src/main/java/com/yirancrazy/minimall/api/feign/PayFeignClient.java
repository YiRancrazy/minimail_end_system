package com.yirancrazy.minimall.api.feign;

import com.yirancrazy.minimall.api.dto.pay.PayCreateDTO;
import com.yirancrazy.minimall.api.fallback.PayFeignFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "minimall-pay-service", fallbackFactory = PayFeignFallbackFactory.class)
public interface PayFeignClient {
    @PostMapping("/internal/pay/create")
    Long create(@RequestBody PayCreateDTO dto);

    @PostMapping("/internal/pay/callback")
    Boolean callback(@RequestParam("payId") Long payId);
}