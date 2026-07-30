package com.yirancrazy.minimall.stock.service;

/**
* 库存领域服务接口，定义 SKU 维度的库存预占、释放与可用量查询三项能力，
 *               供 C 端查询接口与订单服务内部调用；实现方负责校验库存是否存在与是否充足。
 */
public interface StockService {
    boolean reserve(Long skuId, Integer quantity);

    boolean release(Long skuId, Integer quantity);

    long query(Long skuId);
}