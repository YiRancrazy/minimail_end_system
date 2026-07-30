package com.yirancrazy.minimall.api.fallback;

import java.math.BigDecimal;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.feign.GoodsFeignClient;

/**
* 商品服务 Feign 降级工厂，在商品服务不可用时返回受控降级结果。
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