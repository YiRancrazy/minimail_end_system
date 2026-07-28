package com.yirancrazy.minimall.api.fallback;

import com.yirancrazy.minimall.api.dto.merchant.ShopSnapshotDTO;
import com.yirancrazy.minimall.api.feign.MerchantFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MerchantFeignFallbackFactory implements FallbackFactory<MerchantFeignClient> {
    @Override
    public MerchantFeignClient create(Throwable cause) {
        log.warn("merchant-service unreachable, returning sentinel shop: {}", cause.getMessage());
        return id -> new ShopSnapshotDTO(-1L, "unknown", "DOWN");
    }
}