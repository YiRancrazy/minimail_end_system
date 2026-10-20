package com.yirancrazy.minimall.goods.service;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SKU库存缓存同步服务。
 * @Version: 1.0
 * @DateTime: 2026/10/20
 **/
public interface SkuStockSyncService {

    /**
     * 将权威库存同步到商品库存缓存。
     * @return 更新的SKU数量
     */
    int syncStockCache();
}
