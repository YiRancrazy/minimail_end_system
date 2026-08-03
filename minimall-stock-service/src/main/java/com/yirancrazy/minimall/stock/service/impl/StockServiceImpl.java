package com.yirancrazy.minimall.stock.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.stock.constant.StockCodeEnum;
import com.yirancrazy.minimall.stock.constant.StockCountTaskStatusEnum;
import com.yirancrazy.minimall.stock.constant.StockJournalTypeEnum;
import com.yirancrazy.minimall.stock.dto.StockCountTaskCompleteDTO;
import com.yirancrazy.minimall.stock.dto.StockCountTaskCreateDTO;
import com.yirancrazy.minimall.stock.dto.StockCountTaskPageDTO;
import com.yirancrazy.minimall.stock.dto.StockPageDTO;
import com.yirancrazy.minimall.stock.dto.StockTransferDTO;
import com.yirancrazy.minimall.stock.dto.StockTransferPageDTO;
import com.yirancrazy.minimall.stock.entity.StockCountTaskPO;
import com.yirancrazy.minimall.stock.entity.StockJournalPO;
import com.yirancrazy.minimall.stock.entity.StockPO;
import com.yirancrazy.minimall.stock.entity.StockTransferPO;
import com.yirancrazy.minimall.stock.manager.StockCountTaskManager;
import com.yirancrazy.minimall.stock.manager.StockJournalManager;
import com.yirancrazy.minimall.stock.manager.StockManager;
import com.yirancrazy.minimall.stock.manager.StockTransferManager;
import com.yirancrazy.minimall.stock.mapper.StockMapper;
import com.yirancrazy.minimall.stock.service.StockService;
import com.yirancrazy.minimall.stock.vo.StockStatisticsVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 库存领域服务实现，实现Stock相关业务逻辑
 * @Version: 1.1
 * @DateTime: 2026/08/03
 */
@Slf4j
@Service
public class StockServiceImpl implements StockService {

    private static final int EXPORT_MAX_ROWS = 10000;

    private final StockManager stockManager;
    private final StockJournalManager journalManager;
    private final StockMapper stockMapper;
    private final StockTransferManager transferManager;
    private final StockCountTaskManager countTaskManager;

    public StockServiceImpl(StockManager stockManager, StockJournalManager journalManager,
                            StockMapper stockMapper, StockTransferManager transferManager,
                            StockCountTaskManager countTaskManager) {
        this.stockManager = stockManager;
        this.journalManager = journalManager;
        this.stockMapper = stockMapper;
        this.transferManager = transferManager;
        this.countTaskManager = countTaskManager;
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
        return journalManager.list(
            Wrappers.lambdaQuery(StockJournalPO.class)
                .eq(StockJournalPO::getSkuId, skuId)
                .orderByDesc(StockJournalPO::getId));
    }

    /**
     * 平台分页查询全平台库存，可选按 SKU 过滤或仅查预警库存，按ID降序返回。
     * @param dto 分页查询入参
     * @return 库存分页结果
     */
    @Override
    public IPage<StockPO> page(StockPageDTO dto) {
        Page<StockPO> page = new Page<>(dto.getPageNo(), dto.getPageSize());
        LambdaQueryWrapper<StockPO> wrapper = Wrappers.lambdaQuery(StockPO.class);
        if (dto.getSkuId() != null) {
            wrapper.eq(StockPO::getSkuId, dto.getSkuId());
        }
        if (Boolean.TRUE.equals(dto.getLowStockOnly())) {
            wrapper.isNotNull(StockPO::getAlertThreshold)
                .apply("available <= alert_threshold");
        }
        wrapper.orderByDesc(StockPO::getId);
        return stockManager.page(page, wrapper);
    }

    /**
     * 全平台库存统计聚合，委托 Mapper 聚合后计算预警比例；无库存记录时返回零值。
     * @return 库存统计VO
     */
    @Override
    public StockStatisticsVO platformStatistics() {
        StockStatisticsVO vo = stockMapper.statistics();
        if (vo == null) {
            return new StockStatisticsVO(0L, 0L, 0L, 0L, BigDecimal.ZERO);
        }
        long total = vo.getTotalSkuCount() == null ? 0L : vo.getTotalSkuCount();
        long alert = vo.getAlertSkuCount() == null ? 0L : vo.getAlertSkuCount();
        BigDecimal ratio = total == 0L
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(alert).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        vo.setAlertRatio(ratio);
        return vo;
    }

    /**
     * 导出指定SKU的库存流水，最多 10000 行，按ID降序。
     * @param skuId SKU标识
     * @return 库存流水列表
     */
    @Override
    public List<StockJournalPO> exportJournal(Long skuId) {
        return journalManager.list(
            Wrappers.lambdaQuery(StockJournalPO.class)
                .eq(StockJournalPO::getSkuId, skuId)
                .orderByDesc(StockJournalPO::getId)
                .last("LIMIT " + EXPORT_MAX_ROWS));
    }

    /**
     * 跨商家库存调拨：扣减源SKU库存、增加目标SKU库存、记录调拨流水与调拨记录。
     * @param dto 调拨入参，含源/目标SKU、数量、原因、操作人
     * @throws BizException 源/目标SKU相同、源库存不足或目标不存在时
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transfer(StockTransferDTO dto) {
        if (dto.getFromSkuId().equals(dto.getToSkuId())) {
            throw new BizException(StockCodeEnum.STOCK_TRANSFER_SAME_SKU);
        }
        StockPO from = getStock(dto.getFromSkuId());
        if (from.getAvailable() < dto.getQuantity()) {
            throw new BizException(StockCodeEnum.STOCK_TRANSFER_INSUFFICIENT);
        }
        StockPO to = stockManager.getOne(
            Wrappers.lambdaQuery(StockPO.class).eq(StockPO::getSkuId, dto.getToSkuId()));
        if (to == null) {
            throw new BizException(StockCodeEnum.STOCK_TRANSFER_TARGET_NOT_FOUND);
        }
        from.setAvailable(from.getAvailable() - dto.getQuantity());
        to.setAvailable(to.getAvailable() + dto.getQuantity());
        stockManager.updateById(from);
        stockManager.updateById(to);

        recordJournal(dto.getFromSkuId(), -dto.getQuantity(),
            StockJournalTypeEnum.TRANSFER_OUT, dto.getReason(), null);
        recordJournal(dto.getToSkuId(), dto.getQuantity(),
            StockJournalTypeEnum.TRANSFER_IN, dto.getReason(), null);

        StockTransferPO transfer = new StockTransferPO();
        transfer.setFromSkuId(dto.getFromSkuId());
        transfer.setToSkuId(dto.getToSkuId());
        transfer.setQuantity(dto.getQuantity());
        transfer.setReason(dto.getReason());
        transfer.setOperatorId(dto.getOperatorId());
        transferManager.save(transfer);

        checkAlert(from);
        checkAlert(to);
        log.info("stock transfer, from={}, to={}, quantity={}",
            dto.getFromSkuId(), dto.getToSkuId(), dto.getQuantity());
    }

    /**
     * 分页查询调拨记录，可选按源/目标SKU过滤，按ID降序返回。
     * @param dto 分页查询入参
     * @return 调拨记录分页结果
     */
    @Override
    public IPage<StockTransferPO> transferPage(StockTransferPageDTO dto) {
        Page<StockTransferPO> page = new Page<>(dto.getPageNo(), dto.getPageSize());
        LambdaQueryWrapper<StockTransferPO> wrapper = Wrappers.lambdaQuery(StockTransferPO.class);
        if (dto.getFromSkuId() != null) {
            wrapper.eq(StockTransferPO::getFromSkuId, dto.getFromSkuId());
        }
        if (dto.getToSkuId() != null) {
            wrapper.eq(StockTransferPO::getToSkuId, dto.getToSkuId());
        }
        wrapper.orderByDesc(StockTransferPO::getId);
        return transferManager.page(page, wrapper);
    }

    /**
     * 下发库存盘点任务，查询当前可用库存作为期望数量。
     * @param dto 创建入参，含SKU与操作人
     * @return 盘点任务ID
     * @throws BizException SKU库存不存在时
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCountTask(StockCountTaskCreateDTO dto) {
        StockPO po = getStock(dto.getSkuId());
        StockCountTaskPO task = new StockCountTaskPO();
        task.setSkuId(dto.getSkuId());
        task.setExpectedQuantity(po.getAvailable());
        task.setStatus(StockCountTaskStatusEnum.PENDING.intCode());
        task.setRemark(dto.getRemark());
        task.setOperatorId(dto.getOperatorId());
        countTaskManager.save(task);
        log.info("count task created, id={}, skuId={}, expected={}",
            task.getId(), dto.getSkuId(), po.getAvailable());
        return task.getId();
    }

    /**
     * 分页查询盘点任务，可选按SKU和状态过滤，按ID降序返回。
     * @param dto 分页查询入参
     * @return 盘点任务分页结果
     */
    @Override
    public IPage<StockCountTaskPO> countTaskPage(StockCountTaskPageDTO dto) {
        Page<StockCountTaskPO> page = new Page<>(dto.getPageNo(), dto.getPageSize());
        LambdaQueryWrapper<StockCountTaskPO> wrapper =
            Wrappers.lambdaQuery(StockCountTaskPO.class);
        if (dto.getSkuId() != null) {
            wrapper.eq(StockCountTaskPO::getSkuId, dto.getSkuId());
        }
        if (dto.getStatus() != null) {
            wrapper.eq(StockCountTaskPO::getStatus, dto.getStatus());
        }
        wrapper.orderByDesc(StockCountTaskPO::getId);
        return countTaskManager.page(page, wrapper);
    }

    /**
     * 完成盘点任务，计算差异并在有差异时调整库存。
     * @param id 任务ID
     * @param dto 完成入参，含实际盘点数量
     * @throws BizException 任务不存在或非待盘点状态时
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeCountTask(Long id, StockCountTaskCompleteDTO dto) {
        StockCountTaskPO task = countTaskManager.getById(id);
        if (task == null) {
            throw new BizException(StockCodeEnum.STOCK_COUNT_TASK_NOT_FOUND);
        }
        if (task.getStatus() != StockCountTaskStatusEnum.PENDING.intCode()) {
            throw new BizException(StockCodeEnum.STOCK_COUNT_TASK_NOT_PENDING);
        }
        task.setActualQuantity(dto.getActualQuantity());
        task.setDiffQuantity(dto.getActualQuantity() - task.getExpectedQuantity());
        if (dto.getRemark() != null) {
            task.setRemark(dto.getRemark());
        }
        task.setStatus(StockCountTaskStatusEnum.COMPLETED.intCode());
        countTaskManager.updateById(task);

        if (task.getDiffQuantity() != 0) {
            adjustStock(task.getSkuId(), task.getDiffQuantity(), "盘点差异调整");
        }
        log.info("count task completed, id={}, skuId={}, diff={}",
            id, task.getSkuId(), task.getDiffQuantity());
    }

    /**
     * 取消盘点任务，校验状态为PENDING后推进为CANCELLED。
     * @param id 任务ID
     * @param operatorId 操作人ID
     * @throws BizException 任务不存在或非待盘点状态时
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelCountTask(Long id, Long operatorId) {
        StockCountTaskPO task = countTaskManager.getById(id);
        if (task == null) {
            throw new BizException(StockCodeEnum.STOCK_COUNT_TASK_NOT_FOUND);
        }
        if (task.getStatus() != StockCountTaskStatusEnum.PENDING.intCode()) {
            throw new BizException(StockCodeEnum.STOCK_COUNT_TASK_NOT_PENDING);
        }
        task.setStatus(StockCountTaskStatusEnum.CANCELLED.intCode());
        countTaskManager.updateById(task);
        log.info("count task cancelled, id={}, operator={}", id, operatorId);
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
        journalManager.save(journal);
    }

    private void checkAlert(StockPO po) {
        if (po.getAlertThreshold() != null && po.getAvailable() <= po.getAlertThreshold()) {
            log.warn("stock alert, skuId={}, available={}, threshold={}",
                po.getSkuId(), po.getAvailable(), po.getAlertThreshold());
        }
    }
}
