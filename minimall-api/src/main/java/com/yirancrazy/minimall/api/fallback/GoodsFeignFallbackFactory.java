package com.yirancrazy.minimall.api.fallback;

import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.feign.GoodsFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品服务 Feign 降级工厂，在商品服务不可用时返回受控降级结果。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Slf4j
@Component
public class GoodsFeignFallbackFactory implements FallbackFactory<GoodsFeignClient> {
    @Override
    public GoodsFeignClient create(Throwable cause) {
        log.warn("goods-service unreachable, returning sentinel sku: {}", cause.getMessage());
        return id -> new SkuSnapshotDTO(-1L, -1L, "unknown", BigDecimal.ZERO, 0);
    }
}