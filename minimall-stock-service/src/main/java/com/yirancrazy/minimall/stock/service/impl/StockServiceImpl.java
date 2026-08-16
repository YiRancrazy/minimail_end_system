package com.yirancrazy.minimall.stock.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.util.CursorUtils;
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
    private static final int QUERY_MAX_ROWS = 200;

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
     * 预占指定 SKU 的库存：原子扣减可用数量并等额增加预占数量。库存记录不存在时抛出
     * STOCK_NOT_FOUND，可用数量不足或并发下扣减未命中时抛出 STOCK_INSUFFICIENT。
     *
     * @param skuId    SKU 标识
     * @param quantity 预占数量，必须 > 0
     * @return 库存记录更新成功返回 true
     * @throws BizException 当 quantity 非法、库存不存在或库存不足时
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reserve(Long skuId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BizException(CommonCode.PARAM_INVALID, "预占数量必须大于0");
        }
        StockPO s = getStock(skuId);
        // 原子扣减：条件内嵌可用量校验，避免读-判-写的超卖窗口
        if (stockMapper.deductAvailable(skuId, quantity.longValue()) <= 0) {
            throw new BizException(StockCodeEnum.STOCK_INSUFFICIENT);
        }
        s.setAvailable(s.getAvailable() - quantity.longValue());
        s.setReserved(s.getReserved() + quantity.longValue());

        recordJournal(skuId, -quantity.longValue(), StockJournalTypeEnum.RESERVE, null, null);
        checkAlert(s);
        return true;
    }

    /**
     * 释放指定 SKU 的预占库存：原子扣减预占数量并等额回补可用数量。库存记录不存在、
     * 预占数量不足以释放或数量非法时直接返回 false，不抛出异常，保证回滚链路幂等安全。
     *
     * @param skuId    SKU 标识
     * @param quantity 释放数量，必须 > 0
     * @return 库存记录更新成功返回 true，否则返回 false
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean release(Long skuId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return false;
        }
        // 原子释放：未命中（记录不存在或预占不足）按失败处理
        if (stockMapper.restoreReserved(skuId, quantity.longValue()) <= 0) {
            return false;
        }

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
     * 商家视角查询可用库存，先校验库存归属当前商家，防止越权读取其他商家 SKU。
     * @param skuId SKU 标识
     * @param merchantId 商家ID（来自网关 X-Merchant-Id）
     * @return 该 SKU 的可用库存数量
     * @throws BizException 库存归属与商家不匹配时
     */
    @Override
    public long query(Long skuId, Long merchantId) {
        StockPO s = stockManager.getOne(
            Wrappers.lambdaQuery(StockPO.class).eq(StockPO::getSkuId, skuId));
        if (s == null) {
            return 0L;
        }
        checkMerchantOwnership(s, merchantId);
        return s.getAvailable();
    }

    /**
     * 设置库存预警阈值。库存记录不存在时自动创建（初始可用 0）。
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
        StockPO po = getOrCreateStock(skuId);
        po.setAlertThreshold(threshold);
        stockManager.updateById(po);
        log.info("threshold set, skuId={}, threshold={}", skuId, threshold);
    }

    /**
     * 商家视角设置预警阈值，校验库存归属；记录不存在时以该商家归属创建。
     * @param skuId 商品SKU ID
     * @param threshold 预警阈值，必须 >= 0
     * @param merchantId 商家ID（来自网关 X-Merchant-Id）
     * @throws BizException 当阈值非法或库存归属不匹配时
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setThreshold(Long skuId, Long threshold, Long merchantId) {
        if (threshold == null || threshold < 0) {
            throw new BizException(StockCodeEnum.THRESHOLD_INVALID);
        }
        StockPO po = getOrCreateMerchantStock(skuId, merchantId);
        po.setAlertThreshold(threshold);
        stockManager.updateById(po);
        log.info("threshold set, skuId={}, merchantId={}, threshold={}",
            skuId, merchantId, threshold);
    }

    /**
     * 手动调整库存数量。库存记录不存在时自动创建（初始可用 0），保证商家首次入库可用。
     * 负向调整不允许把可用库存调成负值。
     * @param skuId 商品SKU ID
     * @param quantity 调整数量，正数增加、负数扣减，不能为0
     * @param reason 调整原因
     * @throws BizException 当调整数量为0或负向调整超过可用库存时
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustStock(Long skuId, Long quantity, String reason) {
        if (quantity == null || quantity == 0) {
            throw new BizException(StockCodeEnum.ADJUST_QUANTITY_ZERO);
        }
        StockPO po = getOrCreateStock(skuId);
        // 负向调整下限校验：可用库存不得被调成负数
        if (po.getAvailable() + quantity < 0) {
            throw new BizException(StockCodeEnum.STOCK_INSUFFICIENT);
        }
        po.setAvailable(po.getAvailable() + quantity);
        if (!stockManager.updateById(po)) {
            throw new BizException(StockCodeEnum.STOCK_NOT_FOUND);
        }

        recordJournal(skuId, quantity, StockJournalTypeEnum.ADJUST, reason, null);
        checkAlert(po);
        log.info("stock adjusted, skuId={}, quantity={}", skuId, quantity);
    }

    /**
     * 商家视角手动调整库存，校验库存归属；记录不存在时以该商家归属创建（首次入库）。
     * @param skuId 商品SKU ID
     * @param quantity 调整数量，正数增加、负数扣减，不能为0
     * @param reason 调整原因
     * @param merchantId 商家ID（来自网关 X-Merchant-Id）
     * @throws BizException 当调整数量为0、负向调整超库存或库存归属不匹配时
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustStock(Long skuId, Long quantity, String reason, Long merchantId) {
        if (quantity == null || quantity == 0) {
            throw new BizException(StockCodeEnum.ADJUST_QUANTITY_ZERO);
        }
        StockPO po = getOrCreateMerchantStock(skuId, merchantId);
        // 负向调整下限校验：可用库存不得被调成负数
        if (po.getAvailable() + quantity < 0) {
            throw new BizException(StockCodeEnum.STOCK_INSUFFICIENT);
        }
        po.setAvailable(po.getAvailable() + quantity);
        if (!stockManager.updateById(po)) {
            throw new BizException(StockCodeEnum.STOCK_NOT_FOUND);
        }

        recordJournal(skuId, quantity, StockJournalTypeEnum.ADJUST, reason, null);
        checkAlert(po);
        log.info("stock adjusted, skuId={}, merchantId={}, quantity={}",
            skuId, merchantId, quantity);
    }

    /**
     * 查询指定SKU的库存流水记录，最多返回 200 条。
     * @param skuId 商品SKU ID
     * @return 库存流水列表，按ID降序
     */
    @Override
    public List<StockJournalPO> queryJournal(Long skuId) {
        return journalManager.list(
            Wrappers.lambdaQuery(StockJournalPO.class)
                .eq(StockJournalPO::getSkuId, skuId)
                .orderByDesc(StockJournalPO::getId)
                .last("LIMIT " + QUERY_MAX_ROWS));
    }

    /**
     * 商家视角查询库存流水，先校验库存归属当前商家，防止越权读取其他商家流水。
     * @param skuId 商品SKU ID
     * @param merchantId 商家ID（来自网关 X-Merchant-Id）
     * @return 库存流水列表，按ID降序
     * @throws BizException 当库存不存在或归属不匹配时
     */
    @Override
    public List<StockJournalPO> queryJournal(Long skuId, Long merchantId) {
        checkMerchantOwnership(getStock(skuId), merchantId);
        return journalManager.list(
            Wrappers.lambdaQuery(StockJournalPO.class)
                .eq(StockJournalPO::getSkuId, skuId)
                .orderByDesc(StockJournalPO::getId)
                .last("LIMIT " + QUERY_MAX_ROWS));
    }

    /**
     * 平台分页查询全平台库存，可选按 SKU 过滤或仅查预警库存，按ID降序返回。
     * @param dto 分页查询入参
     * @return 库存分页结果
     */
    @Override
    public CursorPageVO<StockPO> page(StockPageDTO dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        int limit = dto.getLimit();
        LambdaQueryWrapper<StockPO> wrapper = Wrappers.lambdaQuery(StockPO.class);
        wrapper.lt(lastId != null, StockPO::getId, lastId);
        if (dto.getSkuId() != null) {
            wrapper.eq(StockPO::getSkuId, dto.getSkuId());
        }
        if (Boolean.TRUE.equals(dto.getLowStockOnly())) {
            wrapper.isNotNull(StockPO::getAlertThreshold)
                .apply("available <= alert_threshold");
        }
        wrapper.orderByDesc(StockPO::getId);
        wrapper.last("LIMIT " + (limit + 1));
        List<StockPO> records = stockManager.list(wrapper);
        return CursorPageVO.of(records, limit, StockPO::getId);
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
     * 商家视角导出库存流水，先校验库存归属当前商家，防止越权导出其他商家流水。
     * @param skuId SKU标识
     * @param merchantId 商家ID（来自网关 X-Merchant-Id）
     * @return 库存流水列表，按ID降序，最多 10000 行
     * @throws BizException 库存不存在或归属不匹配时
     */
    @Override
    public List<StockJournalPO> exportJournal(Long skuId, Long merchantId) {
        checkMerchantOwnership(getStock(skuId), merchantId);
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
        StockPO to = stockManager.getOne(
            Wrappers.lambdaQuery(StockPO.class).eq(StockPO::getSkuId, dto.getToSkuId()));
        if (to == null) {
            throw new BizException(StockCodeEnum.STOCK_TRANSFER_TARGET_NOT_FOUND);
        }
        // 源扣减与目标增加均走原子 SQL，任一未命中即回滚，避免并发下源库存被重复调拨
        if (stockMapper.deductAvailable(dto.getFromSkuId(), dto.getQuantity()) <= 0) {
            throw new BizException(StockCodeEnum.STOCK_TRANSFER_INSUFFICIENT);
        }
        if (stockMapper.increaseAvailable(dto.getToSkuId(), dto.getQuantity()) <= 0) {
            throw new BizException(StockCodeEnum.STOCK_TRANSFER_TARGET_NOT_FOUND);
        }
        from.setAvailable(from.getAvailable() - dto.getQuantity());
        to.setAvailable(to.getAvailable() + dto.getQuantity());

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
    public CursorPageVO<StockTransferPO> transferPage(StockTransferPageDTO dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        int limit = dto.getLimit();
        LambdaQueryWrapper<StockTransferPO> wrapper = Wrappers.lambdaQuery(StockTransferPO.class);
        wrapper.lt(lastId != null, StockTransferPO::getId, lastId);
        if (dto.getFromSkuId() != null) {
            wrapper.eq(StockTransferPO::getFromSkuId, dto.getFromSkuId());
        }
        if (dto.getToSkuId() != null) {
            wrapper.eq(StockTransferPO::getToSkuId, dto.getToSkuId());
        }
        wrapper.orderByDesc(StockTransferPO::getId);
        wrapper.last("LIMIT " + (limit + 1));
        List<StockTransferPO> records = transferManager.list(wrapper);
        return CursorPageVO.of(records, limit, StockTransferPO::getId);
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
    public CursorPageVO<StockCountTaskPO> countTaskPage(StockCountTaskPageDTO dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        int limit = dto.getLimit();
        LambdaQueryWrapper<StockCountTaskPO> wrapper =
            Wrappers.lambdaQuery(StockCountTaskPO.class);
        wrapper.lt(lastId != null, StockCountTaskPO::getId, lastId);
        if (dto.getSkuId() != null) {
            wrapper.eq(StockCountTaskPO::getSkuId, dto.getSkuId());
        }
        if (dto.getStatus() != null) {
            wrapper.eq(StockCountTaskPO::getStatus, dto.getStatus());
        }
        wrapper.orderByDesc(StockCountTaskPO::getId);
        wrapper.last("LIMIT " + (limit + 1));
        List<StockCountTaskPO> records = countTaskManager.list(wrapper);
        return CursorPageVO.of(records, limit, StockCountTaskPO::getId);
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
        Long actualQuantity = dto.getActualQuantity();
        Long diffQuantity = actualQuantity - task.getExpectedQuantity();
        String remark = dto.getRemark() != null ? dto.getRemark() : task.getRemark();
        // 条件更新（PENDING->COMPLETED 原子迁移）未命中说明任务已被并发方完成，
        // 此时不得再次入账盘点差异，避免差异被重复调整
        if (countTaskManager.completeIfStatus(id, StockCountTaskStatusEnum.PENDING.intCode(),
            StockCountTaskStatusEnum.COMPLETED.intCode(), actualQuantity, diffQuantity, remark) == 0) {
            throw new BizException(StockCodeEnum.STOCK_COUNT_TASK_NOT_PENDING);
        }
        task.setActualQuantity(actualQuantity);
        task.setDiffQuantity(diffQuantity);
        task.setStatus(StockCountTaskStatusEnum.COMPLETED.intCode());

        if (diffQuantity != 0) {
            adjustStock(task.getSkuId(), diffQuantity, "盘点差异调整");
        }
        log.info("count task completed, id={}, skuId={}, diff={}",
            id, task.getSkuId(), diffQuantity);
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
        // 条件更新（PENDING->CANCELLED 原子迁移），并发下完成/取消互斥，未命中说明已被并发方处理
        if (countTaskManager.cancelIfStatus(id, StockCountTaskStatusEnum.PENDING.intCode(),
            StockCountTaskStatusEnum.CANCELLED.intCode()) == 0) {
            throw new BizException(StockCodeEnum.STOCK_COUNT_TASK_NOT_PENDING);
        }
        task.setStatus(StockCountTaskStatusEnum.CANCELLED.intCode());
        log.info("count task cancelled, id={}, operator={}", id, operatorId);
    }

    /**
     * 查询异常库存记录（available &lt; 0 或 reserved &lt; 0），最多返回 200 条。
     * @return 异常库存列表
     */
    @Override
    public List<StockPO> listAbnormalStock() {
        return stockManager.list(
            Wrappers.lambdaQuery(StockPO.class)
                .lt(StockPO::getAvailable, 0)
                .or()
                .lt(StockPO::getReserved, 0)
                .last("LIMIT " + QUERY_MAX_ROWS));
    }

    /**
     * 纠正指定SKU的库存至指定值，记录CORRECT类型流水。
     * @param skuId SKU标识
     * @param correctQuantity 纠正后的目标可用库存值，必须 &gt;= 0
     * @param reason 纠正原因
     * @throws BizException 当库存不存在或纠正数量非法时
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void correctStock(Long skuId, Long correctQuantity, String reason) {
        if (correctQuantity == null || correctQuantity < 0) {
            throw new BizException(StockCodeEnum.STOCK_CORRECT_QUANTITY_INVALID);
        }
        StockPO po = getStock(skuId);
        long oldAvailable = po.getAvailable();
        long delta = correctQuantity - oldAvailable;
        po.setAvailable(correctQuantity);
        stockManager.updateById(po);

        recordJournal(skuId, delta, StockJournalTypeEnum.CORRECT, reason, null);
        log.info("stock corrected, skuId={}, from={}, to={}", skuId, oldAvailable, correctQuantity);
    }

    private StockPO getStock(Long skuId) {
        StockPO po = stockManager.getOne(
            Wrappers.lambdaQuery(StockPO.class).eq(StockPO::getSkuId, skuId));
        if (po == null) {
            throw new BizException(StockCodeEnum.STOCK_NOT_FOUND);
        }
        return po;
    }

    /**
     * 查询库存记录；不存在则创建初始记录（可用 0）。仅用于商家调整/设阈值这类
     * 允许"首次建立库存"的入口；订单预占等场景仍用 getStock 强制要求记录存在。
     * ponytail: 简单 select-then-insert，商家手动操作低频，并发冲突概率可忽略；
     * 若后续出现并发建记录冲突，再引入 skuId 唯一索引 + 冲突重查。
     */
    private StockPO getOrCreateStock(Long skuId) {
        StockPO po = stockManager.getOne(
            Wrappers.lambdaQuery(StockPO.class).eq(StockPO::getSkuId, skuId));
        if (po != null) {
            return po;
        }
        po = new StockPO();
        po.setSkuId(skuId);
        po.setAvailable(0L);
        po.setReserved(0L);
        stockManager.save(po);
        return po;
    }

    /**
     * 商家视角获取库存记录：存在时校验归属，不存在时以该商家归属创建初始记录。
     * 存量 merchant_id 为空的历史记录视为无归属，不允许商家认领（由运营脚本回填）。
     */
    private StockPO getOrCreateMerchantStock(Long skuId, Long merchantId) {
        StockPO po = stockManager.getOne(
            Wrappers.lambdaQuery(StockPO.class).eq(StockPO::getSkuId, skuId));
        if (po != null) {
            checkMerchantOwnership(po, merchantId);
            return po;
        }
        po = new StockPO();
        po.setSkuId(skuId);
        po.setMerchantId(merchantId);
        po.setAvailable(0L);
        po.setReserved(0L);
        stockManager.save(po);
        return po;
    }

    /**
     * 校验库存归属：无归属（存量数据未回填）或归属商家与当前商家不一致时，
     * 统一按 SKU 不存在抛错，避免向越权商家泄露 SKU 存在性与归属信息。
     */
    private void checkMerchantOwnership(StockPO po, Long merchantId) {
        if (po.getMerchantId() == null || !po.getMerchantId().equals(merchantId)) {
            throw new BizException(StockCodeEnum.STOCK_NOT_FOUND);
        }
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
