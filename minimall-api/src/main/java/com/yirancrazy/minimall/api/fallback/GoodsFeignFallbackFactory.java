package com.yirancrazy.minimall.api.fallback;

import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.feign.GoodsFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class GoodsFeignFallbackFactory implements FallbackFactory<GoodsFeignClient> {
    @Override
    public GoodsFeignClient create(Throwable cause) {
        log.warn("goods-service unreachable, returning sentinel sku: {}", cause.getMessage());
        return id -> new SkuSnapshotDTO(-1L, -1L, "unknown", BigDecimal.ZERO, 0);
    }
}