package com.yirancrazy.minimall.api.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.stock.StockAdjustDTO;
import com.yirancrazy.minimall.api.dto.stock.StockBatchReserveDTO;
import com.yirancrazy.minimall.api.dto.stock.StockInitDTO;
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
        log.warn("stock-service 不可达: {}", cause.getMessage());
        return new StockFeignClient() {
            @Override
            public Result<Boolean> reserve(StockReserveDTO dto) {
                return Result.fail(CommonCode.SYS_ERROR, "库存服务不可用");
            }

            @Override
            public Result<Boolean> reserveBatch(StockBatchReserveDTO dto) {
                return Result.fail(CommonCode.SYS_ERROR, "库存服务不可用");
            }

            @Override
            public Result<Boolean> release(StockReserveDTO dto) {
                return Result.fail(CommonCode.SYS_ERROR, "库存服务不可用");
            }

            @Override
            public Result<Void> initStock(StockInitDTO dto) {
                log.warn("stock-service 不可达，初始化库存已跳过，skuId={}", dto.getSkuId());
                return Result.fail(CommonCode.SYS_ERROR, "库存服务不可用");
            }

            @Override
            public Result<Void> adjustStock(StockAdjustDTO dto) {
                return Result.fail(CommonCode.SYS_ERROR, "库存服务不可用");
            }
        };
    }
}
