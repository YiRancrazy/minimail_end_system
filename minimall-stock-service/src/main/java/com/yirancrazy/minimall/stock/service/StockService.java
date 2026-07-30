package com.yirancrazy.minimall.stock.service;

/**
* 库存领域服务接口，定义 SKU 维度的库存预占、释放与可用量查询三项能力，
 *               供 C 端查询接口与订单服务内部调用；实现方负责校验库存是否存在与是否充足。
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