package com.yirancrazy.minimall.stock.service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 库存领域服务接口，定义Stock相关业务契约
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public interface StockService {
    /**
     * 预占库存。
     * @param skuId 商品SKU ID
     * @param quantity 预占数量
     * @return 预占是否成功
     */
    boolean reserve(Long skuId, Integer quantity);

    /**
     * 释放库存。
     * @param skuId 商品SKU ID
     * @param quantity 释放数量
     * @return 释放是否成功
     */
    boolean release(Long skuId, Integer quantity);

    /**
     * 查询可用库存。
     * @param skuId 商品SKU ID
     * @return 可用库存数量
     */
    long query(Long skuId);
}