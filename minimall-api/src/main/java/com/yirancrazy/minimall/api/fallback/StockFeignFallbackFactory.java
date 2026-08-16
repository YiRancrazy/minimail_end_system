package com.yirancrazy.minimall.api.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDTO;
import com.yirancrazy.minimall.api.feign.StockFeignClient;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockFeign Feign 降级工厂，处理StockFeign服务调用失败降级
 * @Version: 1.1
 * @DateTime: 2026/08/02
 */
@Slf4j
@Component
public class StockFeignFallbackFactory implements FallbackFactory<StockFeignClient> {
    @Override
    public StockFeignClient create(Throwable cause) {
        log.warn("stock-service unreachable: {}", cause.getMessage());
        return new StockFeignClient() {
            @Override
            public Result<Boolean> reserve(StockReserveDTO dto) {
                return Result.fail(CommonCode.SYS_ERROR, "库存服务不可用");
            }

            @Override
            public Result<Boolean> release(StockReserveDTO dto) {
                return Result.fail(CommonCode.SYS_ERROR, "库存服务不可用");
            }
        };
    }
}
