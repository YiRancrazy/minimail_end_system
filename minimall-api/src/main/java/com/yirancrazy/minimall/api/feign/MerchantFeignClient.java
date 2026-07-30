package com.yirancrazy.minimall.api.feign;

import com.yirancrazy.minimall.api.dto.merchant.ShopSnapshotDTO;
import com.yirancrazy.minimall.api.fallback.MerchantFeignFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
* 商家服务 Feign 客户端，提供跨服务店铺快照查询能力。
 */
@FeignClient(value = "minimall-merchant-service", fallbackFactory = MerchantFeignFallbackFactory.class)
public interface MerchantFeignClient {
    @GetMapping("/internal/merchant/shop/{id}")
    ShopSnapshotDTO shopSnapshot(@PathVariable("id") Long id);
}