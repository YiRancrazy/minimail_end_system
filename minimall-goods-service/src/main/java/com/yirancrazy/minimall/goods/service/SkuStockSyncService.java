package com.yirancrazy.minimall.goods.service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义将库存服务权威可用量分批同步到商品库存展示缓存的业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/20
 */
public interface SkuStockSyncService {

  /**
   * 分批读取 SKU，将库存服务权威可用量同步到商品侧库存缓存。
   *
   * @return 本次实际更新的 SKU 数量
   * @throws BizException 库存服务失败或缓存批量更新失败时抛出
   */
  int syncStockCache();
}
