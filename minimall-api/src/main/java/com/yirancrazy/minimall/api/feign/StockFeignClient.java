package com.yirancrazy.minimall.api.feign;

import com.yirancrazy.minimall.api.dto.stock.StockReserveDTO;
import com.yirancrazy.minimall.api.fallback.StockFeignFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 库存服务 Feign 客户端，提供跨服务库存预占、释放与查询能力。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@FeignClient(value = "minimall-stock-service", fallbackFactory = StockFeignFallbackFactory.class)
public interface StockFeignClient {
    @PostMapping("/internal/stock/reserve")
    Boolean reserve(@RequestBody StockReserveDTO dto);
}