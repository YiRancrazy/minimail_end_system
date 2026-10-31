package com.yirancrazy.minimall.api.feign;

import com.yirancrazy.minimall.api.dto.stock.StockAdjustDto;
import com.yirancrazy.minimall.api.dto.stock.StockAvailabilityDto;
import com.yirancrazy.minimall.api.dto.stock.StockAvailabilityQueryDto;
import com.yirancrazy.minimall.api.dto.stock.StockBatchReserveDto;
import com.yirancrazy.minimall.api.dto.stock.StockInitDto;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDto;
import com.yirancrazy.minimall.api.fallback.StockFeignFallbackFactory;
import com.yirancrazy.minimall.common.result.Result;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供库存预占、释放、初始化、调整和可售量查询内部接口.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@FeignClient(name = "minimall-stock-service", fallbackFactory = StockFeignFallbackFactory.class)
public interface StockFeignClient {
  /**
   * 为订单预占单个 SKU 库存.
   *
   * @param dto SKU、数量和订单号
   * @return 包装后的预占结果
   */
  @PostMapping("/internal/stock/reserve")
  Result<Boolean> reserve(@RequestBody StockReserveDto dto);

  /**
   * 批量预占订单内多个 SKU 的库存.
   *
   * @param dto 多组 SKU 预占明细
   * @return 包装后的批量预占结果
   */
  @PostMapping("/internal/stock/reserve-batch")
  Result<Boolean> reserveBatch(@RequestBody StockBatchReserveDto dto);

  /**
   * 释放订单已预占的单个 SKU 库存.
   *
   * @param dto SKU、数量和订单号
   * @return 包装后的释放结果
   */
  @PostMapping("/internal/stock/release")
  Result<Boolean> release(@RequestBody StockReserveDto dto);

  /**
   * 为指定 SKU 和商家初始化库存.
   *
   * @param dto SKU、商家和初始库存量
   * @return 初始化成功或系统错误
   */
  @PostMapping("/internal/stock/init")
  Result<Void> initStock(@RequestBody StockInitDto dto);

  /**
   * 按调整数量修正商家 SKU 库存并记录原因.
   *
   * @param dto SKU、商家、调整数量和原因
   * @return 调整成功或系统错误
   */
  @PostMapping("/internal/stock/adjust")
  Result<Void> adjustStock(@RequestBody StockAdjustDto dto);

  /**
   * 批量查询 SKU 当前可售库存.
   *
   * @param dto 待查询 SKU ID 列表
   * @return 包装后的 SKU 可售库存快照
   */
  @PostMapping("/internal/stock/availability")
  Result<List<StockAvailabilityDto>> queryAvailability(@RequestBody StockAvailabilityQueryDto dto);
}
