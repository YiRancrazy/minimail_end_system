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
 * @Description: StockFeignFallbackFactory 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Component
public class StockFeignFallbackFactory implements FallbackFactory<StockFeignClient> {
    /**
     * 创建业务数据。
     * @param cause cause 参数
     * @return 处理结果
     */
    @Override
    public StockFeignClient create(Throwable cause) {
        log.warn("stock-service 不可达: {}", cause.getMessage());
        return new StockFeignClient() {
            /**
             * 执行 reserve 方法。
             * @param dto dto 参数
             * @return 统一响应结果
             */
            @Override
            public Result<Boolean> reserve(StockReserveDTO dto) {
                return Result.fail(CommonCode.SYS_ERROR, "库存服务不可用");
            }

            /**
             * 执行 reserveBatch 方法。
             * @param dto dto 参数
             * @return 统一响应结果
             */
            @Override
            public Result<Boolean> reserveBatch(StockBatchReserveDTO dto) {
                return Result.fail(CommonCode.SYS_ERROR, "库存服务不可用");
            }

            /**
             * 执行 release 方法。
             * @param dto dto 参数
             * @return 统一响应结果
             */
            @Override
            public Result<Boolean> release(StockReserveDTO dto) {
                return Result.fail(CommonCode.SYS_ERROR, "库存服务不可用");
            }

            /**
             * 初始化相关资源。
             * @param dto dto 参数
             * @return 统一响应结果
             */
            @Override
            public Result<Void> initStock(StockInitDTO dto) {
                log.warn("stock-service 不可达，初始化库存已跳过，skuId={}", dto.getSkuId());
                return Result.fail(CommonCode.SYS_ERROR, "库存服务不可用");
            }

            /**
             * 执行 adjustStock 方法。
             * @param dto dto 参数
             * @return 统一响应结果
             */
            @Override
            public Result<Void> adjustStock(StockAdjustDTO dto) {
                return Result.fail(CommonCode.SYS_ERROR, "库存服务不可用");
            }
        };
    }
}
