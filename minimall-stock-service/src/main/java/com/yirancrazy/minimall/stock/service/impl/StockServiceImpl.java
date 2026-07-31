package com.yirancrazy.minimall.stock.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.stock.constant.StockCodeEnum;
import com.yirancrazy.minimall.stock.entity.StockPO;
import com.yirancrazy.minimall.stock.manager.StockManager;
import com.yirancrazy.minimall.stock.service.StockService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 库存领域服务实现，实现Stock相关业务逻辑
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class StockServiceImpl implements StockService {

    private final StockManager stockManager;

    public StockServiceImpl(StockManager stockManager) {
        this.stockManager = stockManager;
    }

    /**
     * 预占指定 SKU 的库存：扣减可用数量并等额增加预占数量。库存记录不存在时抛出
     * STOCK_NOT_FOUND，可用数量不足时抛出 STOCK_INSUFFICIENT。
     *
     * @param skuId    SKU 标识
     * @param quantity 预占数量
     * @return 库存记录更新成功返回 true
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reserve(Long skuId, Integer quantity) {
        StockPO s = stockManager.getOne(
            Wrappers.lambdaQuery(StockPO.class).eq(StockPO::getSkuId, skuId));
        if (s == null) {
            throw new BizException(StockCodeEnum.STOCK_NOT_FOUND);
        }
        if (s.getAvailable() < quantity) {
            throw new BizException(StockCodeEnum.STOCK_INSUFFICIENT);
        }
        s.setAvailable(s.getAvailable() - quantity);
        s.setReserved(s.getReserved() + quantity);
        return stockManager.updateById(s);
    }

    /**
     * 释放指定 SKU 的预占库存：扣减预占数量并等额回补可用数量。库存记录不存在或
     * 预占数量不足以释放时直接返回 false，不抛出异常，保证回滚链路幂等安全。
     *
     * @param skuId    SKU 标识
     * @param quantity 释放数量
     * @return 库存记录更新成功返回 true，否则返回 false
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean release(Long skuId, Integer quantity) {
        StockPO s = stockManager.getOne(
            Wrappers.lambdaQuery(StockPO.class).eq(StockPO::getSkuId, skuId));
        if (s == null || s.getReserved() < quantity) {
            return false;
        }
        s.setReserved(s.getReserved() - quantity);
        s.setAvailable(s.getAvailable() + quantity);
        return stockManager.updateById(s);
    }

    /**
     * 查询指定 SKU 的当前可用库存数量，库存记录不存在时返回 0 而非抛出异常。
     *
     * @param skuId SKU 标识
     * @return 该 SKU 的可用库存数量
     */
    @Override
    public long query(Long skuId) {
        StockPO s = stockManager.getOne(
            Wrappers.lambdaQuery(StockPO.class).eq(StockPO::getSkuId, skuId));
        return s == null ? 0L : s.getAvailable();
    }
}