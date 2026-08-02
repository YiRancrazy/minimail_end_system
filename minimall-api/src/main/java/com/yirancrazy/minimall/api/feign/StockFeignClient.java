package com.yirancrazy.minimall.api.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDTO;
import com.yirancrazy.minimall.api.fallback.StockFeignFallbackFactory;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Stock Feign 客户端，调用Stock服务接口
 * @Version: 1.2
 * @DateTime: 2026/08/02
 */
@FeignClient(name = "minimall-stock-service", fallbackFactory = StockFeignFallbackFactory.class)
public interface StockFeignClient {
    @PostMapping("/internal/stock/reserve")
    Result<Boolean> reserve(@RequestBody StockReserveDTO dto);

    /**
     * Release previously reserved stock for a SKU.
     * @param dto the release request, reusing the reserve DTO shape
     * @return true if released successfully
     */
    @PostMapping("/internal/stock/release")
    Result<Boolean> release(@RequestBody StockReserveDTO dto);
}
