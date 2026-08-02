package com.yirancrazy.minimall.api.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDTO;
import com.yirancrazy.minimall.api.feign.StockFeignClient;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockFeign Feign 降级工厂，处理StockFeign服务调用失败降级
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Slf4j
@Component
public class StockFeignFallbackFactory implements FallbackFactory<StockFeignClient> {
    @Override
    public StockFeignClient create(Throwable cause) {
        log.warn("stock-service unreachable: {}", cause.getMessage());
        return new StockFeignClient() {
            @Override
            public Boolean reserve(StockReserveDTO dto) {
                return false;
            }

            @Override
            public Boolean release(StockReserveDTO dto) {
                return false;
            }
        };
    }
}