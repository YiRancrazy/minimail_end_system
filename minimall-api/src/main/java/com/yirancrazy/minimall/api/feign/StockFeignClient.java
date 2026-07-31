package com.yirancrazy.minimall.api.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDTO;
import com.yirancrazy.minimall.api.fallback.StockFeignFallbackFactory;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockFeignClient description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public interface StockFeignClient {
    @PostMapping("/internal/stock/reserve")
    Boolean reserve(@RequestBody StockReserveDTO dto);
}