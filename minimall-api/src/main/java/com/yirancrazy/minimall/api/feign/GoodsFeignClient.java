package com.yirancrazy.minimall.api.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.fallback.GoodsFeignFallbackFactory;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Goods Feign 客户端，调用Goods服务接口
 * @Version: 1.1
 * @DateTime: 2026/08/02
 */
@FeignClient(name = "minimall-goods-service", fallbackFactory = GoodsFeignFallbackFactory.class)
public interface GoodsFeignClient {
    @GetMapping("/internal/goods/sku/{id}")
    SkuSnapshotDTO skuSnapshot(@PathVariable("id") Long id);
}