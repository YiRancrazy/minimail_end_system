package com.yirancrazy.minimall.stock.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.stock.constant.StockCodeEnum;
import com.yirancrazy.minimall.stock.constant.StockJournalTypeEnum;
import com.yirancrazy.minimall.stock.entity.StockJournalPO;
import com.yirancrazy.minimall.stock.entity.StockPO;
import com.yirancrazy.minimall.stock.manager.StockManager;
import com.yirancrazy.minimall.stock.mapper.StockJournalMapper;
import com.yirancrazy.minimall.stock.service.StockService;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 库存领域服务实现，实现Stock相关业务逻辑
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Slf4j
@Service
public class StockServiceImpl implements StockService {

    private final StockManager stockManager;
    private final StockJournalMapper journalMapper;

    public StockServiceImpl(StockManager stockManager, StockJournalMapper journalMapper) {
        this.stockManager = stockManager;
        this.journalMapper = journalMapper;
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
        StockPO s = getStock(skuId);
        if (s.getAvailable() < quantity.longValue()) {
            throw new BizException(StockCodeEnum.STOCK_INSUFFICIENT);
        }
        s.setAvailable(s.getAvailable() - quantity.longValue());
        s.setReserved(s.getReserved() + quantity.longValue());
        stockManager.updateById(s);

        recordJournal(skuId, -quantity.longValue(), StockJournalTypeEnum.RESERVE, null, null);
        checkAlert(s);
        return true;
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
        if (s == null || s.getReserved() < quantity.longValue()) {
            return false;
        }
        s.setReserved(s.getReserved() - quantity.longValue());
        s.setAvailable(s.getAvailable() + quantity.longValue());
        stockManager.updateById(s);

        recordJournal(skuId, quantity.longValue(), StockJournalTypeEnum.RELEASE, null, null);
        return true;
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

    /**
     * 设置库存预警阈值。
     * @param skuId 商品SKU ID
     * @param threshold 预警阈值，必须 >= 0
     * @throws BizException 当阈值非法时
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setThreshold(Long skuId, Long threshold) {
        if (threshold == null || threshold < 0) {
            throw new BizException(StockCodeEnum.THRESHOLD_INVALID);
        }
        StockPO po = getStock(skuId);
        po.setAlertThreshold(threshold);
        stockManager.updateById(po);
        log.info("threshold set, skuId={}, threshold={}", skuId, threshold);
    }

    /**
     * 手动调整库存数量。
     * @param skuId 商品SKU ID
     * @param quantity 调整数量，正数增加、负数扣减，不能为0
     * @param reason 调整原因
     * @throws BizException 当调整数量为0时
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustStock(Long skuId, Long quantity, String reason) {
        if (quantity == null || quantity == 0) {
            throw new BizException(StockCodeEnum.ADJUST_QUANTITY_ZERO);
        }
        StockPO po = getStock(skuId);
        po.setAvailable(po.getAvailable() + quantity);
        stockManager.updateById(po);

        recordJournal(skuId, quantity, StockJournalTypeEnum.ADJUST, reason, null);
        checkAlert(po);
        log.info("stock adjusted, skuId={}, quantity={}", skuId, quantity);
    }

    /**
     * 查询指定SKU的库存流水记录。
     * @param skuId 商品SKU ID
     * @return 库存流水列表，按ID降序
     */
    @Override
    public List<StockJournalPO> queryJournal(Long skuId) {
        return journalMapper.selectList(
            Wrappers.lambdaQuery(StockJournalPO.class)
                .eq(StockJournalPO::getSkuId, skuId)
                .orderByDesc(StockJournalPO::getId));
    }

    private StockPO getStock(Long skuId) {
        StockPO po = stockManager.getOne(
            Wrappers.lambdaQuery(StockPO.class).eq(StockPO::getSkuId, skuId));
        if (po == null) {
            throw new BizException(StockCodeEnum.STOCK_NOT_FOUND);
        }
        return po;
    }

    private void recordJournal(Long skuId, Long quantity, StockJournalTypeEnum type,
                               String reason, String orderNo) {
        StockJournalPO journal = new StockJournalPO();
        journal.setSkuId(skuId);
        journal.setQuantity(quantity);
        journal.setType(type.intCode());
        journal.setReason(reason);
        journal.setOrderNo(orderNo);
        journalMapper.insert(journal);
    }

    private void checkAlert(StockPO po) {
        if (po.getAlertThreshold() != null && po.getAvailable() <= po.getAlertThreshold()) {
            log.warn("stock alert, skuId={}, available={}, threshold={}",
                po.getSkuId(), po.getAvailable(), po.getAlertThreshold());
        }
    }
}
