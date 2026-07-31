package com.yirancrazy.minimall.api.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.yirancrazy.minimall.api.dto.pay.PayCreateDTO;
import com.yirancrazy.minimall.api.fallback.PayFeignFallbackFactory;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayFeignClient description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public interface PayFeignClient {
    @PostMapping("/internal/pay/create")
    Long create(@RequestBody PayCreateDTO dto);

    @PostMapping("/internal/pay/callback")
    Boolean callback(@RequestParam("payId") Long payId);
}