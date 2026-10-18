package com.yirancrazy.minimall.stock.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDTO;
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
import com.yirancrazy.minimall.stock.dto.StockStatisticsDTO;
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
import com.yirancrazy.minimall.stock.service.StockService;
import com.yirancrazy.minimall.stock.vo.StockCountTaskVO;
import com.yirancrazy.minimall.stock.vo.StockJournalVO;
import com.yirancrazy.minimall.stock.vo.StockStatisticsVO;
import com.yirancrazy.minimall.stock.vo.StockTransferVO;
import com.yirancrazy.minimall.stock.vo.StockVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockServiceImpl 服务实现，实现相关业务逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Service
public class StockServiceImpl implements StockService {

    private static final int EXPORT_MAX_ROWS = 10000;
    private static final int QUERY_MAX_ROWS = 200;
    private static final int MAX_RESERVE_BATCH_SIZE = 100;

    private final StockManager stockManager;
    private final StockJournalManager journalManager;
    private final StockTransferManager transferManager;
    private final StockCountTaskManager countTaskManager;

    /**
     * 构造 StockServiceImpl 实例。
     * @param stockManager stockManager 参数
     * @param journalManager journalManager 参数
     * @param transferManager transferManager 参数
     * @param countTaskManager countTaskManager 参数
     */
    public StockServiceImpl(StockManager stockManager, StockJournalManager journalManager,
                            StockTransferManager transferManager,
                            StockCountTaskManager countTaskManager) {
        this.stockManager = stockManager;
        this.journalManager = journalManager;
        this.transferManager = transferManager;
        this.countTaskManager = countTaskManager;
    }


    /**
     * 执行 reserve 方法。
     * @param skuId skuId 参数
     * @param quantity quantity 参数
     * @return 是否处理成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reserve(Long skuId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BizException(CommonCode.PARAM_INVALID, "预占数量必须大于0");
        }
        StockPO s = getStock(skuId);
        // 原子扣减：条件内嵌可用量校验，避免读-判-写的超卖窗口
        if (stockManager.deductAvailable(skuId, quantity.longValue()) <= 0) {
            throw new BizException(StockCodeEnum.STOCK_INSUFFICIENT);
        }
        s.setAvailable(s.getAvailable() - quantity.longValue());
        s.setReserved(s.getReserved() + quantity.longValue());

        recordJournal(skuId, -quantity.longValue(), StockJournalTypeEnum.RESERVE, null, null);
        checkAlert(s);
        return true;
    }


    /**
     * 执行 reserveBatch 方法。
     * @param items items 参数
     * @return 是否处理成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reserveBatch(List<StockReserveDTO> items) {
        if (items == null || items.isEmpty()) {
            throw new BizException(CommonCode.PARAM_INVALID, "预占明细不能为空");
        }
        Map<Long, Long> quantityBySku = new LinkedHashMap<>(items.size() * 2);
        for (StockReserveDTO item : items) {
            if (item == null || item.getSkuId() == null || item.getQuantity() == null
                || item.getQuantity() <= 0) {
                throw new BizException(CommonCode.PARAM_INVALID, "预占数量必须大于0");
            }
            if (!quantityBySku.containsKey(item.getSkuId())
                && quantityBySku.size() >= MAX_RESERVE_BATCH_SIZE) {
                throw new BizException(CommonCode.PARAM_INVALID, "单次最多预占100个SKU");
            }
            long merged = quantityBySku.getOrDefault(item.getSkuId(), 0L) + item.getQuantity();
            if (merged > Integer.MAX_VALUE) {
                throw new BizException(CommonCode.PARAM_INVALID, "预占数量超出范围");
            }
            quantityBySku.put(item.getSkuId(), merged);
        }
        for (Map.Entry<Long, Long> entry : quantityBySku.entrySet()) {
            reserve(entry.getKey(), entry.getValue().intValue());
        }
        return true;
    }


    /**
     * 执行 release 方法。
     * @param skuId skuId 参数
     * @param quantity quantity 参数
     * @param orderNo orderNo 参数
     * @return 是否处理成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean release(Long skuId, Integer quantity, String orderNo) {
        if (quantity == null || quantity <= 0) {
            return false;
        }
        if (orderNo != null && !orderNo.isBlank()) {
            try {
                recordJournal(skuId, quantity.longValue(), StockJournalTypeEnum.RELEASE, null, orderNo);
            }
            catch (DuplicateKeyException e) {
                log.info("库存释放已记录，skuId={}，orderNo={}", skuId, orderNo);
                return true;
            }
        }
        // 原子释放：未命中（记录不存在或预占不足）按失败处理
        if (stockManager.restoreReserved(skuId, quantity.longValue()) <= 0) {
            if (orderNo != null && !orderNo.isBlank()) {
                throw new BizException(StockCodeEnum.STOCK_INSUFFICIENT);
            }
            return false;
        }

        if (orderNo == null || orderNo.isBlank()) {
            recordJournal(skuId, quantity.longValue(), StockJournalTypeEnum.RELEASE, null, orderNo);
        }
        return true;
    }


    /**
     * 初始化相关资源。
     * @param skuId skuId 参数
     * @param merchantId merchantId 参数
     * @param initialQuantity initialQuantity 参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void initStock(Long skuId, Long merchantId, Integer initialQuantity) {
        if (skuId == null) {
            throw new BizException(CommonCode.PARAM_INVALID, "SKU ID不能为空");
        }
        StockPO po = stockManager.getOne(
            Wrappers.lambdaQuery(StockPO.class).eq(StockPO::getSkuId, skuId));
        if (po != null) {
            return;
        }
        long initQty = initialQuantity == null || initialQuantity < 0 ? 0L : initialQuantity.longValue();
        po = new StockPO();
        po.setSkuId(skuId);
        po.setMerchantId(merchantId);
        po.setAvailable(initQty);
        po.setReserved(0L);
        stockManager.save(po);
        if (initQty > 0) {
            recordJournal(skuId, initQty, StockJournalTypeEnum.ADJUST, "商品创建初始库存", null);
        }
        log.info("库存已初始化，skuId={}，merchantId={}，initial={}", skuId, merchantId, initQty);
    }


    /**
     * 执行 query 方法。
     * @param skuId skuId 参数
     * @return 业务数值
     */
    @Override
    public long query(Long skuId) {
        StockPO s = stockManager.getOne(
            Wrappers.lambdaQuery(StockPO.class).eq(StockPO::getSkuId, skuId));
        return s == null ? 0L : s.getAvailable();
    }


    /**
     * 执行 query 方法。
     * @param skuId skuId 参数
     * @param merchantId merchantId 参数
     * @return 业务数值
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
     * 设置相关数据。
     * @param skuId skuId 参数
     * @param threshold threshold 参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void setThreshold(Long skuId, Long threshold) {
        if (threshold == null || threshold < 0) {
            throw new BizException(StockCodeEnum.THRESHOLD_INVALID);
        }
        StockPO po = getOrCreateStock(skuId);
        po.setAlertThreshold(threshold);
        if (!stockManager.updateById(po)) {
            throw new BizException(StockCodeEnum.STOCK_CONCURRENT_UPDATE);
        }
        log.info("库存阈值已设置，skuId={}，threshold={}", skuId, threshold);
    }


    /**
     * 设置相关数据。
     * @param skuId skuId 参数
     * @param threshold threshold 参数
     * @param merchantId merchantId 参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void setThreshold(Long skuId, Long threshold, Long merchantId) {
        if (threshold == null || threshold < 0) {
            throw new BizException(StockCodeEnum.THRESHOLD_INVALID);
        }
        StockPO po = getOrCreateMerchantStock(skuId, merchantId);
        po.setAlertThreshold(threshold);
        if (!stockManager.updateById(po)) {
            throw new BizException(StockCodeEnum.STOCK_CONCURRENT_UPDATE);
        }
        log.info("库存阈值已设置，skuId={}，merchantId={}，threshold={}",
            skuId, merchantId, threshold);
    }


    /**
     * 执行 adjustStock 方法。
     * @param skuId skuId 参数
     * @param quantity quantity 参数
     * @param reason reason 参数
     */
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
            throw new BizException(StockCodeEnum.STOCK_CONCURRENT_UPDATE);
        }

        recordJournal(skuId, quantity, StockJournalTypeEnum.ADJUST, reason, null);
        checkAlert(po);
        log.info("库存已调整，skuId={}，quantity={}", skuId, quantity);
    }


    /**
     * 执行 adjustStock 方法。
     * @param skuId skuId 参数
     * @param quantity quantity 参数
     * @param reason reason 参数
     * @param merchantId merchantId 参数
     */
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
            throw new BizException(StockCodeEnum.STOCK_CONCURRENT_UPDATE);
        }

        recordJournal(skuId, quantity, StockJournalTypeEnum.ADJUST, reason, null);
        checkAlert(po);
        log.info("库存已调整，skuId={}，merchantId={}，quantity={}",
            skuId, merchantId, quantity);
    }


    /**
     * 执行 queryJournal 方法。
     * @param skuId skuId 参数
     * @return 结果列表
     */
    public List<StockJournalPO> queryJournal(Long skuId) {
        return journalManager.list(
            Wrappers.lambdaQuery(StockJournalPO.class)
                .eq(StockJournalPO::getSkuId, skuId)
                .orderByDesc(StockJournalPO::getId)
                .last("LIMIT " + QUERY_MAX_ROWS));
    }


    /**
     * 执行 queryJournal 方法。
     * @param skuId skuId 参数
     * @param merchantId merchantId 参数
     * @return 结果列表
     */
    public List<StockJournalPO> queryJournal(Long skuId, Long merchantId) {
        checkMerchantOwnership(getStock(skuId), merchantId);
        return journalManager.list(
            Wrappers.lambdaQuery(StockJournalPO.class)
                .eq(StockJournalPO::getSkuId, skuId)
                .orderByDesc(StockJournalPO::getId)
                .last("LIMIT " + QUERY_MAX_ROWS));
    }


    /**
     * 执行 queryJournalVO 方法。
     * @param skuId skuId 参数
     * @param merchantId merchantId 参数
     * @return 结果列表
     */
    @Override
    public List<StockJournalVO> queryJournalVO(Long skuId, Long merchantId) {
        return queryJournal(skuId, merchantId).stream().map(StockJournalVO::from).toList();
    }


    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
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
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    @Override
    public CursorPageVO<StockVO> pageVO(StockPageDTO dto) {
        return page(dto).map(StockVO::from);
    }


    /**
     * 执行 platformStatistics 方法。
     * @return 处理结果
     */
    @Override
    public StockStatisticsVO platformStatistics() {
        StockStatisticsDTO result = stockManager.statistics();
        if (result == null) {
            return new StockStatisticsVO(0L, 0L, 0L, 0L, BigDecimal.ZERO);
        }
        long total = result.getTotalSkuCount() == null ? 0L : result.getTotalSkuCount();
        long alert = result.getAlertSkuCount() == null ? 0L : result.getAlertSkuCount();
        BigDecimal ratio = total == 0L
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(alert).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        return new StockStatisticsVO(result.getTotalSkuCount(), result.getTotalAvailable(),
            result.getTotalReserved(), result.getAlertSkuCount(), ratio);
    }


    /**
     * 执行 exportJournal 方法。
     * @param skuId skuId 参数
     * @return 结果列表
     */
    public List<StockJournalPO> exportJournal(Long skuId) {
        return journalManager.list(
            Wrappers.lambdaQuery(StockJournalPO.class)
                .eq(StockJournalPO::getSkuId, skuId)
                .orderByDesc(StockJournalPO::getId)
                .last("LIMIT " + EXPORT_MAX_ROWS));
    }


    /**
     * 执行 exportJournal 方法。
     * @param skuId skuId 参数
     * @param merchantId merchantId 参数
     * @return 结果列表
     */
    public List<StockJournalPO> exportJournal(Long skuId, Long merchantId) {
        checkMerchantOwnership(getStock(skuId), merchantId);
        return journalManager.list(
            Wrappers.lambdaQuery(StockJournalPO.class)
                .eq(StockJournalPO::getSkuId, skuId)
                .orderByDesc(StockJournalPO::getId)
                .last("LIMIT " + EXPORT_MAX_ROWS));
    }


    /**
     * 执行 exportJournalVO 方法。
     * @param skuId skuId 参数
     * @param merchantId merchantId 参数
     * @return 结果列表
     */
    @Override
    public List<StockJournalVO> exportJournalVO(Long skuId, Long merchantId) {
        return exportJournal(skuId, merchantId).stream().map(StockJournalVO::from).toList();
    }


    /**
     * 执行 transfer 方法。
     * @param dto dto 参数
     */
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
        if (stockManager.deductAvailable(dto.getFromSkuId(), dto.getQuantity()) <= 0) {
            throw new BizException(StockCodeEnum.STOCK_TRANSFER_INSUFFICIENT);
        }
        if (stockManager.increaseAvailable(dto.getToSkuId(), dto.getQuantity()) <= 0) {
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
        log.info("库存已调拨，from={}，to={}，quantity={}",
            dto.getFromSkuId(), dto.getToSkuId(), dto.getQuantity());
    }


    /**
     * 执行 transferPage 方法。
     * @param dto dto 参数
     * @return 处理结果
     */
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
     * 执行 transferPageVO 方法。
     * @param dto dto 参数
     * @return 处理结果
     */
    @Override
    public CursorPageVO<StockTransferVO> transferPageVO(StockTransferPageDTO dto) {
        return transferPage(dto).map(StockTransferVO::from);
    }


    /**
     * 创建业务数据。
     * @param dto dto 参数
     * @return 业务数值
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
        log.info("盘点任务已创建，id={}，skuId={}，expected={}",
            task.getId(), dto.getSkuId(), po.getAvailable());
        return task.getId();
    }


    /**
     * 统计业务数据。
     * @param dto dto 参数
     * @return 处理结果
     */
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
     * 统计业务数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    @Override
    public CursorPageVO<StockCountTaskVO> countTaskPageVO(StockCountTaskPageDTO dto) {
        return countTaskPage(dto).map(StockCountTaskVO::from);
    }


    /**
     * 执行 completeCountTask 方法。
     * @param id id 参数
     * @param dto dto 参数
     */
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
        log.info("盘点任务已完成，id={}，skuId={}，diff={}",
            id, task.getSkuId(), diffQuantity);
    }


    /**
     * 执行 cancelCountTask 方法。
     * @param id id 参数
     * @param operatorId operatorId 参数
     */
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
        log.info("盘点任务已取消，id={}，operator={}", id, operatorId);
    }


    /**
     * 查询列表数据。
     * @return 结果列表
     */
    public List<StockPO> listAbnormalStock() {
        return stockManager.list(
            Wrappers.lambdaQuery(StockPO.class)
                .lt(StockPO::getAvailable, 0)
                .or()
                .lt(StockPO::getReserved, 0)
                .last("LIMIT " + QUERY_MAX_ROWS));
    }


    /**
     * 查询列表数据。
     * @return 结果列表
     */
    @Override
    public List<StockVO> listAbnormalStockVO() {
        return listAbnormalStock().stream().map(StockVO::from).toList();
    }


    /**
     * 执行 correctStock 方法。
     * @param skuId skuId 参数
     * @param correctQuantity correctQuantity 参数
     * @param reason reason 参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void correctStock(Long skuId, Long correctQuantity, String reason) {
        if (correctQuantity == null || correctQuantity < 0) {
            throw new BizException(StockCodeEnum.STOCK_CORRECT_QUANTITY_INVALID);
        }
        StockPO po = getStock(skuId);
        long oldAvailable = po.getAvailable();
        long delta = correctQuantity - oldAvailable;
        po.setAvailable(correctQuantity);
        if (!stockManager.updateById(po)) {
            throw new BizException(StockCodeEnum.STOCK_CONCURRENT_UPDATE);
        }

        recordJournal(skuId, delta, StockJournalTypeEnum.CORRECT, reason, null);
        log.info("库存已修正，skuId={}，from={}，to={}", skuId, oldAvailable, correctQuantity);
    }


    /**
     * 获取库存。
     * @param skuId SKU ID
     * @return 查询结果
     * @throws BizException 业务校验失败时抛出
     */
    private StockPO getStock(Long skuId) {
        StockPO po = stockManager.getOne(
            Wrappers.lambdaQuery(StockPO.class).eq(StockPO::getSkuId, skuId));
        if (po == null) {
            throw new BizException(StockCodeEnum.STOCK_NOT_FOUND);
        }
        return po;
    }


    /**
     * 获取Or创建库存。
     * @param skuId SKU ID
     * @return 查询结果
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
     * 获取Or创建商户库存。
     * @param skuId SKU ID
     * @param merchantId 商户 ID
     * @return 查询结果
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
     * 校验商户Ownership。
     * @param po 持久化对象
     * @param merchantId 商户 ID
     * @throws BizException 业务校验失败时抛出
     */
    private void checkMerchantOwnership(StockPO po, Long merchantId) {
        if (po.getMerchantId() == null || !po.getMerchantId().equals(merchantId)) {
            throw new BizException(StockCodeEnum.STOCK_NOT_FOUND);
        }
    }


    /**
     * 处理记录Journal。
     * @param skuId SKU ID
     * @param quantity 数量
     * @param type 类型
     * @param reason 原因
     * @param orderNo 订单编号
     * @throws BizException 业务校验失败时抛出
     */
    private void recordJournal(Long skuId, Long quantity, StockJournalTypeEnum type,
                               String reason, String orderNo) {
        StockJournalPO journal = new StockJournalPO();
        journal.setSkuId(skuId);
        journal.setQuantity(quantity);
        journal.setType(type.intCode());
        journal.setReason(reason);
        journal.setOrderNo(orderNo);
        if (!journalManager.save(journal)) {
            throw new BizException(StockCodeEnum.STOCK_JOURNAL_SAVE_FAIL);
        }
    }


    /**
     * 校验Alert。
     * @param po 持久化对象
     */
    private void checkAlert(StockPO po) {
        if (po.getAlertThreshold() != null && po.getAvailable() <= po.getAlertThreshold()) {
            log.warn("库存告警，skuId={}，available={}，threshold={}",
                po.getSkuId(), po.getAvailable(), po.getAlertThreshold());
        }
    }
}
