package com.yirancrazy.minimall.stock.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.stock.dto.StockStatisticsDto;
import com.yirancrazy.minimall.stock.entity.StockPo;
import com.yirancrazy.minimall.stock.manager.StockManager;
import com.yirancrazy.minimall.stock.mapper.StockMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 基于 StockMapper 实现库存聚合查询和原子增减操作。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Manager
public class StockManagerImpl extends ServiceImpl<StockMapper, StockPo> implements StockManager {

  /**
   * 查询库存汇总指标，未在应用层二次修正聚合值。
   *
   * @return 库存聚合结果
   */
  @Override
  public StockStatisticsDto statistics() {
    return baseMapper.statistics();
  }

  /**
   * 通过条件更新扣减可用量并增加预占量。
   *
   * @param skuId SKU ID
   * @param quantity 预占数量
   * @return 实际更新行数；0 表示 SKU 不存在或库存不足
   */
  @Override
  public int deductAvailable(Long skuId, long quantity) {
    return baseMapper.deductAvailable(skuId, quantity);
  }

  /**
   * 通过条件更新减少预占量并恢复可用量。
   *
   * @param skuId SKU ID
   * @param quantity 释放数量
   * @return 实际更新行数；0 表示 SKU 不存在或预占量不足
   */
  @Override
  public int restoreReserved(Long skuId, long quantity) {
    return baseMapper.restoreReserved(skuId, quantity);
  }

  /**
   * 通过原子 SQL 增加指定 SKU 的可用量。
   *
   * @param skuId SKU ID
   * @param quantity 增加数量
   * @return 实际更新行数；0 表示 SKU 不存在
   */
  @Override
  public int increaseAvailable(Long skuId, long quantity) {
    return baseMapper.increaseAvailable(skuId, quantity);
  }
}
