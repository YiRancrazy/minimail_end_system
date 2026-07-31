package com.yirancrazy.minimall.api.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.yirancrazy.minimall.api.dto.merchant.ShopSnapshotDTO;
import com.yirancrazy.minimall.api.fallback.MerchantFeignFallbackFactory;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantFeignClient description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public interface MerchantFeignClient {
    @GetMapping("/internal/merchant/shop/{id}")
    ShopSnapshotDTO shopSnapshot(@PathVariable("id") Long id);
}