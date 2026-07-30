package com.yirancrazy.minimall.api.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDTO;
import com.yirancrazy.minimall.api.feign.StockFeignClient;

/**
* 库存服务 Feign 降级工厂，在库存服务不可用时返回受控降级结果（按当前实现：reserve 返回 false 哨兵值）。
 */
@Slf4j
@Component
public class StockFeignFallbackFactory implements FallbackFactory<StockFeignClient> {
    @Override
    public StockFeignClient create(Throwable cause) {
        log.warn("stock-service unreachable: {}", cause.getMessage());
        return (StockReserveDTO dto) -> false;
    }
}