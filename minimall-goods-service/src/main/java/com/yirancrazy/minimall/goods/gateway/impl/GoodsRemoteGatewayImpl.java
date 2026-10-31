package com.yirancrazy.minimall.goods.gateway.impl;

import com.yirancrazy.minimall.api.dto.merchant.ShopSnapshotDto;
import com.yirancrazy.minimall.api.dto.stock.StockAdjustDto;
import com.yirancrazy.minimall.api.dto.stock.StockAvailabilityDto;
import com.yirancrazy.minimall.api.dto.stock.StockAvailabilityQueryDto;
import com.yirancrazy.minimall.api.dto.stock.StockInitDto;
import com.yirancrazy.minimall.api.feign.MerchantFeignClient;
import com.yirancrazy.minimall.api.feign.StockFeignClient;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.goods.gateway.GoodsRemoteGateway;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 通过 MerchantFeignClient 和 StockFeignClient 实现店铺及库存远程调用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Component
@RequiredArgsConstructor
public class GoodsRemoteGatewayImpl implements GoodsRemoteGateway {

  private final MerchantFeignClient merchantFeignClient;
  private final StockFeignClient stockFeignClient;

  /**
   * 通过 MerchantFeignClient 查询店铺快照，用于商品发布前归属与状态校验。
   *
   * @param shopId 店铺主键，必须属于当前商家且处于营业状态
   * @return 远程服务统一响应；调用方需检查 code 和 data
   */
  @Override
  public Result<ShopSnapshotDto> shopSnapshot(Long shopId) {
    return merchantFeignClient.shopSnapshot(shopId);
  }

  /**
   * 通过 StockFeignClient 为新建 SKU 初始化权威库存。
   *
   * @param dto 请求参数
   * @return 远程服务统一响应；调用方需检查 code 和 data
   */
  @Override
  public Result<Void> initStock(StockInitDto dto) {
    return stockFeignClient.initStock(dto);
  }

  /**
   * 通过 StockFeignClient 按增量调整指定商家 SKU 的库存。
   *
   * @param dto 请求参数
   * @return 远程服务统一响应；调用方需检查 code 和 data
   */
  @Override
  public Result<Void> adjustStock(StockAdjustDto dto) {
    return stockFeignClient.adjustStock(dto);
  }

  /**
   * 通过 StockFeignClient 批量查询 SKU 权威可用库存。
   *
   * @param dto 请求参数
   * @return 远程服务统一响应；调用方需检查 code 和 data
   */
  @Override
  public Result<List<StockAvailabilityDto>> queryAvailability(StockAvailabilityQueryDto dto) {
    return stockFeignClient.queryAvailability(dto);
  }
}
