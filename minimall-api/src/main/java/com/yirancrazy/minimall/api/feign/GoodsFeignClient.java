package com.yirancrazy.minimall.api.feign;

import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.fallback.GoodsFeignFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "minimall-goods-service", fallbackFactory = GoodsFeignFallbackFactory.class)
public interface GoodsFeignClient {
    @GetMapping("/internal/goods/sku/{id}")
    SkuSnapshotDTO skuSnapshot(@PathVariable("id") Long id);
}