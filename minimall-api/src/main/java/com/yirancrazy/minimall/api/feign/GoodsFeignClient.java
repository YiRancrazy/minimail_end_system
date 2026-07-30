package com.yirancrazy.minimall.api.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.fallback.GoodsFeignFallbackFactory;

/**
* 商品服务 Feign 客户端，提供跨服务 SKU 快照查询与上下架能力。
 */
@FeignClient(value = "minimall-goods-service", fallbackFactory = GoodsFeignFallbackFactory.class)
public interface GoodsFeignClient {
    @GetMapping("/internal/goods/sku/{id}")
    SkuSnapshotDTO skuSnapshot(@PathVariable("id") Long id);
}