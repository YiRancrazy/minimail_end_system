package com.yirancrazy.minimall.stock.service;

import com.yirancrazy.minimall.stock.entity.StockJournalPO;
import java.util.List;

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

    /**
     * 设置库存预警阈值。
     * @param skuId 商品SKU ID
     * @param threshold 预警阈值，必须 >= 0
     * @throws com.yirancrazy.minimall.common.exception.BizException 当阈值非法时
     */
    void setThreshold(Long skuId, Long threshold);

    /**
     * 手动调整库存数量。
     * @param skuId 商品SKU ID
     * @param quantity 调整数量，正数增加、负数扣减，不能为0
     * @param reason 调整原因
     * @throws com.yirancrazy.minimall.common.exception.BizException 当调整数量为0时
     */
    void adjustStock(Long skuId, Long quantity, String reason);

    /**
     * 查询指定SKU的库存流水记录。
     * @param skuId 商品SKU ID
     * @return 库存流水列表，按ID降序
     */
    List<StockJournalPO> queryJournal(Long skuId);
}