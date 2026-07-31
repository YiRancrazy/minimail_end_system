package com.yirancrazy.minimall.api.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.fallback.GoodsFeignFallbackFactory;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: GoodsFeignClient description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public interface GoodsFeignClient {
    @GetMapping("/internal/goods/sku/{id}")
    SkuSnapshotDTO skuSnapshot(@PathVariable("id") Long id);
}