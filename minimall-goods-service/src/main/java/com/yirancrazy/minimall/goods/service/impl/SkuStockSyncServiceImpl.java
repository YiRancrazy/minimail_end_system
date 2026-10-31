package com.yirancrazy.minimall.goods.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.api.dto.stock.StockAvailabilityDto;
import com.yirancrazy.minimall.api.dto.stock.StockAvailabilityQueryDto;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.goods.entity.SkuPo;
import com.yirancrazy.minimall.goods.gateway.GoodsRemoteGateway;
import com.yirancrazy.minimall.goods.manager.SkuManager;
import com.yirancrazy.minimall.goods.service.SkuStockSyncService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 分批读取商品侧 SKU 缓存，从库存服务获取权威可用量并回写 t_sku.stock。
 * @Version: 1.0
 * @DateTime: 2026/10/20
 */
@Slf4j
@Service
public class SkuStockSyncServiceImpl implements SkuStockSyncService {

  private static final int MAX_BATCH_SIZE = 500;
  private static final int MAX_MISSING_SAMPLES = 10;

  private final SkuManager skuManager;
  private final GoodsRemoteGateway goodsRemoteGateway;
  private final int batchSize;

  /**
   * 注入SKU 数据访问管理器、店铺与库存远程调用网关、单批同步数量，构造时限制在 1 到 500，构造 SkuStockSyncServiceImpl 实例。
   *
   * @param skuManager SKU 数据访问管理器
   * @param goodsRemoteGateway 店铺与库存远程调用网关
   * @param batchSize 单批同步数量，构造时限制在 1 到 500
   */
  public SkuStockSyncServiceImpl(
      SkuManager skuManager,
      GoodsRemoteGateway goodsRemoteGateway,
      @Value("${minimall.goods.stock-sync.batch-size:200}") int batchSize) {
    this.skuManager = skuManager;
    this.goodsRemoteGateway = goodsRemoteGateway;
    this.batchSize = Math.max(1, Math.min(batchSize, MAX_BATCH_SIZE));
  }

  /**
   * 按主键游标分批扫描 SKU，调用库存服务获取权威可用量并批量回写缓存；缺失库存按零处理，远程或批量更新失败时抛出业务异常。
   *
   * @return 本次实际更新库存缓存的 SKU 数量
   * @throws BizException 库存服务无响应、返回失败或批量更新失败时抛出
   */
  @Override
  public int syncStockCache() {
    int updated = 0;
    int missing = 0;
    List<Long> missingSamples = new ArrayList<>(MAX_MISSING_SAMPLES);
    long lastId = 0L;
    while (true) {
      List<SkuPo> skuBatch =
          skuManager.list(
              Wrappers.lambdaQuery(SkuPo.class)
                  .gt(SkuPo::getId, lastId)
                  .orderByAsc(SkuPo::getId)
                  .last("LIMIT " + batchSize));
      if (skuBatch.isEmpty()) {
        break;
      }
      lastId = skuBatch.get(skuBatch.size() - 1).getId();
      List<Long> skuIds = skuBatch.stream().map(SkuPo::getId).toList();
      Result<List<StockAvailabilityDto>> response =
          goodsRemoteGateway.queryAvailability(new StockAvailabilityQueryDto(skuIds));
      if (response == null || !CommonCode.SUCCESS.equals(response.getCode())) {
        String message = response == null ? "库存服务无响应" : response.getMessage();
        throw new BizException(CommonCode.SYS_ERROR, message);
      }
      Map<Long, Long> availableBySku = new LinkedHashMap<>(skuBatch.size() * 2);
      if (response.getData() != null) {
        for (StockAvailabilityDto stock : response.getData()) {
          availableBySku.put(stock.getSkuId(), stock.getAvailable());
        }
      }
      List<SkuPo> changed = new ArrayList<>(skuBatch.size());
      for (SkuPo sku : skuBatch) {
        Long available = availableBySku.get(sku.getId());
        if (available == null) {
          missing++;
          if (missingSamples.size() < MAX_MISSING_SAMPLES) {
            missingSamples.add(sku.getId());
          }
          available = 0L;
        }
        int cacheStock = toCacheStock(available);
        if (!Objects.equals(sku.getStock(), cacheStock)) {
          sku.setStock(cacheStock);
          changed.add(sku);
        }
      }
      if (!changed.isEmpty() && !skuManager.updateBatchById(changed)) {
        throw new BizException(CommonCode.SYS_ERROR, "库存缓存批量更新失败");
      }
      updated += changed.size();
      if (skuBatch.size() < batchSize) {
        break;
      }
    }
    if (missing > 0) {
      log.warn("库存缓存同步发现缺少权威库存，count={}，skuIds={}", missing, missingSamples);
    }
    return updated;
  }

  private int toCacheStock(long available) {
    if (available <= 0) {
      return 0;
    }
    if (available > Integer.MAX_VALUE) {
      log.warn("权威库存超过商品缓存范围，将缓存值限制为Integer.MAX_VALUE，available={}", available);
      return Integer.MAX_VALUE;
    }
    return (int) available;
  }
}
