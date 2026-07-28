package com.yirancrazy.minimall.stock.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.stock.entity.StockPO;
import com.yirancrazy.minimall.stock.manager.StockManager;
import com.yirancrazy.minimall.stock.service.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockServiceImpl implements StockService {

    private final StockManager stockManager;

    public StockServiceImpl(StockManager stockManager) {
        this.stockManager = stockManager;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reserve(Long skuId, Integer quantity) {
        StockPO s = stockManager.getOne(
            Wrappers.lambdaQuery(StockPO.class).eq(StockPO::getSkuId, skuId));
        if (s == null) {
            throw new BizException("18001", "STOCK_NOT_FOUND", "SKU " + skuId + " 库存不存在");
        }
        if (s.getAvailable() < quantity) {
            throw new BizException("18002", "STOCK_INSUFFICIENT", "库存不足");
        }
        s.setAvailable(s.getAvailable() - quantity);
        s.setReserved(s.getReserved() + quantity);
        return stockManager.updateById(s);
    }

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

    @Override
    public long query(Long skuId) {
        StockPO s = stockManager.getOne(
            Wrappers.lambdaQuery(StockPO.class).eq(StockPO::getSkuId, skuId));
        return s == null ? 0L : s.getAvailable();
    }
}