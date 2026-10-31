package com.yirancrazy.minimall.stock.controller.v1;

import com.yirancrazy.minimall.api.dto.stock.StockAdjustDto;
import com.yirancrazy.minimall.api.dto.stock.StockAvailabilityDto;
import com.yirancrazy.minimall.api.dto.stock.StockAvailabilityQueryDto;
import com.yirancrazy.minimall.api.dto.stock.StockBatchReserveDto;
import com.yirancrazy.minimall.api.dto.stock.StockInitDto;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDto;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.stock.service.StockService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供订单等内部调用方使用的库存预占、释放、初始化、调整和可用量接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/internal/stock")
public class InternalStockControllerV1 {

  private final StockService stockService;

  /**
   * 注入库存领域服务。
   *
   * @param stockService 库存业务服务
   */
  public InternalStockControllerV1(StockService stockService) {
    this.stockService = stockService;
  }

  /**
   * 接收单 SKU 预占请求，校验请求体后调用库存服务。
   *
   * @param dto SKU、预占数量和订单号
   * @return 预占成功标识的统一响应
   */
  @PostMapping("/reserve")
  public Result<Boolean> reserve(@Valid @RequestBody StockReserveDto dto) {
    return Result.success(
        stockService.reserve(dto.getSkuId(), dto.getQuantity(), dto.getOrderNo()));
  }

  /**
   * 接收多 SKU 预占请求，并交由服务层合并明细和执行事务。
   *
   * @param dto 预占明细集合
   * @return 批量预占成功标识的统一响应
   */
  @PostMapping("/reserve-batch")
  public Result<Boolean> reserveBatch(@Valid @RequestBody StockBatchReserveDto dto) {
    return Result.success(stockService.reserveBatch(dto.getItems()));
  }

  /**
   * 接收释放请求，校验订单号后调用幂等释放逻辑。
   *
   * @param dto SKU、释放数量和订单号
   * @return 释放成功标识的统一响应
   */
  @PostMapping("/release")
  public Result<Boolean> release(@Valid @RequestBody StockReserveDto dto) {
    return Result.success(
        stockService.release(dto.getSkuId(), dto.getQuantity(), dto.getOrderNo()));
  }

  /**
   * 接收 SKU 初始库存创建请求；已存在库存时由服务层保持原值。
   *
   * @param dto SKU、商户和初始数量
   * @return 无业务数据的成功响应
   */
  @PostMapping("/init")
  public Result<Void> init(@Valid @RequestBody StockInitDto dto) {
    stockService.initStock(dto.getSkuId(), dto.getMerchantId(), dto.getInitialQuantity());
    return Result.success();
  }

  /**
   * 接收内部库存增量调整请求，并委托服务层记录流水和检查告警。
   *
   * @param dto SKU、调整数量、原因和商户信息
   * @return 无业务数据的成功响应
   */
  @PostMapping("/adjust")
  public Result<Void> adjust(@Valid @RequestBody StockAdjustDto dto) {
    stockService.adjustStock(
        dto.getSkuId(), dto.getQuantity(), dto.getReason(), dto.getMerchantId());
    return Result.success();
  }

  /**
   * 批量查询去重后的 SKU 可用库存，供内部调用方构建立即购买快照。
   *
   * @param dto 需要查询的 SKU ID 列表
   * @return 按有效输入顺序返回的库存快照统一响应
   */
  @PostMapping("/availability")
  public Result<List<StockAvailabilityDto>> availability(
      @Valid @RequestBody StockAvailabilityQueryDto dto) {
    return Result.success(stockService.queryAvailability(dto.getSkuIds()));
  }
}
