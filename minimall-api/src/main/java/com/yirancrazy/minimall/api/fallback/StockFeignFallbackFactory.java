package com.yirancrazy.minimall.api.fallback;

import com.yirancrazy.minimall.api.dto.stock.StockAdjustDto;
import com.yirancrazy.minimall.api.dto.stock.StockAvailabilityDto;
import com.yirancrazy.minimall.api.dto.stock.StockAvailabilityQueryDto;
import com.yirancrazy.minimall.api.dto.stock.StockBatchReserveDto;
import com.yirancrazy.minimall.api.dto.stock.StockInitDto;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDto;
import com.yirancrazy.minimall.api.feign.StockFeignClient;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 在库存服务不可用时统一返回系统错误，避免预占或扣减被误判成功.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Component
public class StockFeignFallbackFactory implements FallbackFactory<StockFeignClient> {
  /**
   * 为库存远程调用异常创建降级客户端.
   *
   * @param cause 库存远程调用异常
   * @return 库存降级客户端
   */
  @Override
  public StockFeignClient create(Throwable cause) {
    log.warn("stock-service 不可达: {}", cause.getMessage());
    return new StockFeignClient() {
      /**
       * 库存服务不可用时拒绝单个 SKU 预占.
       *
       * @param dto SKU 预占参数
       * @return 系统错误响应
       */
      @Override
      public Result<Boolean> reserve(StockReserveDto dto) {
        return Result.fail(CommonCode.SYS_ERROR, "库存服务不可用");
      }

      /**
       * 库存服务不可用时拒绝批量 SKU 预占.
       *
       * @param dto 批量预占参数
       * @return 系统错误响应
       */
      @Override
      public Result<Boolean> reserveBatch(StockBatchReserveDto dto) {
        return Result.fail(CommonCode.SYS_ERROR, "库存服务不可用");
      }

      /**
       * 库存服务不可用时拒绝释放预占，调用方应重试.
       *
       * @param dto SKU 释放参数
       * @return 系统错误响应
       */
      @Override
      public Result<Boolean> release(StockReserveDto dto) {
        return Result.fail(CommonCode.SYS_ERROR, "库存服务不可用");
      }

      /**
       * 库存服务不可用时拒绝初始化并记录 SKU ID.
       *
       * @param dto 库存初始化参数
       * @return 系统错误响应
       */
      @Override
      public Result<Void> initStock(StockInitDto dto) {
        log.warn("stock-service 不可达，初始化库存已跳过，skuId={}", dto.getSkuId());
        return Result.fail(CommonCode.SYS_ERROR, "库存服务不可用");
      }

      /**
       * 库存服务不可用时拒绝库存调整.
       *
       * @param dto 库存调整参数
       * @return 系统错误响应
       */
      @Override
      public Result<Void> adjustStock(StockAdjustDto dto) {
        return Result.fail(CommonCode.SYS_ERROR, "库存服务不可用");
      }

      /**
       * 库存服务不可用时返回系统错误，避免调用方误用空库存.
       *
       * @param dto 待查询 SKU ID 列表
       * @return 系统错误响应
       */
      @Override
      public Result<List<StockAvailabilityDto>> queryAvailability(StockAvailabilityQueryDto dto) {
        return Result.fail(CommonCode.SYS_ERROR, "库存服务不可用");
      }
    };
  }
}
