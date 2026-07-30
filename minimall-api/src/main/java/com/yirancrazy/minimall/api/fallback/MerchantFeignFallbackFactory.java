package com.yirancrazy.minimall.api.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.merchant.ShopSnapshotDTO;
import com.yirancrazy.minimall.api.feign.MerchantFeignClient;

/**
* 商家服务 Feign 降级工厂，在商家服务不可用时返回受控降级结果。
 */
@Slf4j
@Component
public class MerchantFeignFallbackFactory implements FallbackFactory<MerchantFeignClient> {
    @Override
    public MerchantFeignClient create(Throwable cause) {
        log.warn("merchant-service unreachable, returning sentinel shop: {}", cause.getMessage());
        return id -> new ShopSnapshotDTO(-1L, "unknown", "DOWN");
    }
}