package com.yirancrazy.minimall.api.fallback;

import java.math.BigDecimal;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.feign.GoodsFeignClient;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: GoodsFeign Feign 降级工厂，处理GoodsFeign服务调用失败降级
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class GoodsFeignFallbackFactory implements FallbackFactory<GoodsFeignClient> {
    @Override
    public GoodsFeignClient create(Throwable cause) {
        log.warn("goods-service unreachable, returning sentinel sku: {}", cause.getMessage());
        return id -> new SkuSnapshotDTO(-1L, -1L, "unknown", BigDecimal.ZERO, 0);
    }
}