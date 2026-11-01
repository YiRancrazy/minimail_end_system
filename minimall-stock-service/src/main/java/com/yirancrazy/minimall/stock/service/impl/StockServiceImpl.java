package com.yirancrazy.minimall.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.api.dto.stock.StockAvailabilityDto;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDto;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.stock.constant.StockCodeEnum;
import com.yirancrazy.minimall.stock.constant.StockCountTaskStatusEnum;
import com.yirancrazy.minimall.stock.constant.StockJournalTypeEnum;
import com.yirancrazy.minimall.stock.dto.StockCountTaskCompleteDto;
import com.yirancrazy.minimall.stock.dto.StockCountTaskCreateDto;
import com.yirancrazy.minimall.stock.dto.StockCountTaskPageDto;
import com.yirancrazy.minimall.stock.dto.StockPageDto;
import com.yirancrazy.minimall.stock.dto.StockStatisticsDto;
import com.yirancrazy.minimall.stock.dto.StockTransferDto;
import com.yirancrazy.minimall.stock.dto.StockTransferPageDto;
import com.yirancrazy.minimall.stock.entity.StockCountTaskPo;
import com.yirancrazy.minimall.stock.entity.StockJournalPo;
import com.yirancrazy.minimall.stock.entity.StockPo;
import com.yirancrazy.minimall.stock.entity.StockTransferPo;
import com.yirancrazy.minimall.stock.manager.StockCountTaskManager;
import com.yirancrazy.minimall.stock.manager.StockJournalManager;
import com.yirancrazy.minimall.stock.manager.StockManager;
import com.yirancrazy.minimall.stock.manager.StockTransferManager;
import com.yirancrazy.minimall.stock.service.StockService;
import com.yirancrazy.minimall.stock.vo.StockCountTaskVo;
import com.yirancrazy.minimall.stock.vo.StockJournalVo;
import com.yirancrazy.minimall.stock.vo.StockStatisticsVo;
import com.yirancrazy.minimall.stock.vo.StockTransferVo;
import com.yirancrazy.minimall.stock.vo.StockVo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 编排库存原子变更、流水、调拨和盘点事务，并实施商户归属与幂等规则。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Service
public class StockServiceImpl implements StockService {

    private static final int EXPORT_MAX_ROWS = 10000;
    private static final int QUERY_MAX_ROWS = 200;
    private static final int MAX_RESERVE_BATCH_SIZE = 100;
    private static final int MAX_AVAILABILITY_BATCH_SIZE = 500;

    private final StockManager stockManager;
    private final StockJournalManager journalManager;
    private final StockTransferManager transferManager;
    private final StockCountTaskManager countTaskManager;

    /**
     * 注入库存、流水、调拨和盘点任务四类持久化管理器。
     *
     * @param stockManager 库存持久化管理器
     * @param journalManager 库存流水持久化管理器
     * @param transferManager 库存调拨记录持久化管理器
     * @param countTaskManager 盘点任务持久化管理器
     */
    public StockServiceImpl(
            StockManager stockManager,
            StockJournalManager journalManager,
            StockTransferManager transferManager,
            StockCountTaskManager countTaskManager) {
        this.stockManager = stockManager;
        this.journalManager = journalManager;
        this.transferManager = transferManager;
        this.countTaskManager = countTaskManager;
    }

    /**
     * 在单个事务内预占库存并记录负向库存流水。
     *
     * @param skuId SKU ID
     * @param quantity 预占数量，必须大于 0
     * @param orderNo 订单号，不能为空
     * @return 预占成功后返回 true
     * @throws BizException 参数无效、库存记录不存在或条件扣减未命中时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reserve(Long skuId, Integer quantity, String orderNo) {
        requireOrderNo(orderNo);
        if (quantity == null || quantity <= 0) {
            throw new BizException(CommonCode.PARAM_INVALID, "预占数量必须大于0");
        }
        StockPo s = getStock(skuId);
        // 原子扣减：条件内嵌可用量校验，避免读-判-写的超卖窗口
        if (stockManager.deductAvailable(skuId, quantity.longValue()) <= 0) {
            throw new BizException(StockCodeEnum.STOCK_INSUFFICIENT);
        }
        s.setAvailable(s.getAvailable() - quantity.longValue());
        s.setReserved(s.getReserved() + quantity.longValue());

        recordJournal(skuId, -quantity.longValue(), StockJournalTypeEnum.RESERVE, null, orderNo);
        checkAlert(s);
        return true;
    }

    /**
     * 校验并合并批量预占明细，再复用单 SKU 预占逻辑在事务内逐项执行。
     *
     * @param items 预占明细，最多 100 个不同 SKU，同一 SKU 必须使用相同订单号
     * @return 全部明细预占成功后返回 true
     * @throws BizException 明细为空、数量无效、订单号为空、SKU 数超限或任一库存不足时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reserveBatch(List<StockReserveDto> items) {
        if (items == null || items.isEmpty()) {
            throw new BizException(CommonCode.PARAM_INVALID, "预占明细不能为空");
        }
        Map<Long, Long> quantityBySku = new LinkedHashMap<>(items.size() * 2);
        Map<Long, String> orderNoBySku = new LinkedHashMap<>(items.size() * 2);
        for (StockReserveDto item : items) {
            if (item == null
                    || item.getSkuId() == null
                    || item.getQuantity() == null
                    || item.getQuantity() <= 0) {
                throw new BizException(CommonCode.PARAM_INVALID, "预占数量必须大于0");
            }
            requireOrderNo(item.getOrderNo());
            if (!quantityBySku.containsKey(item.getSkuId())
                    && quantityBySku.size() >= MAX_RESERVE_BATCH_SIZE) {
                throw new BizException(CommonCode.PARAM_INVALID, "单次最多预占100个SKU");
            }
            String existingOrderNo = orderNoBySku.putIfAbsent(item.getSkuId(), item.getOrderNo());
            if (existingOrderNo != null && !existingOrderNo.equals(item.getOrderNo())) {
                throw new BizException(CommonCode.PARAM_INVALID, "同一SKU的订单号必须一致");
            }
            long merged = quantityBySku.getOrDefault(item.getSkuId(), 0L) + item.getQuantity();
            if (merged > Integer.MAX_VALUE) {
                throw new BizException(CommonCode.PARAM_INVALID, "预占数量超出范围");
            }
            quantityBySku.put(item.getSkuId(), merged);
        }
        for (Map.Entry<Long, Long> entry : quantityBySku.entrySet()) {
            reserve(entry.getKey(), entry.getValue().intValue(), orderNoBySku.get(entry.getKey()));
        }
        return true;
    }

    /**
     * 先写释放流水实现重复请求识别，再原子恢复预占量。
     *
     * @param skuId SKU ID
     * @param quantity 释放数量
     * @param orderNo 订单号，不能为空
     * @return 释放成功或命中重复流水时返回 true；数量不大于 0 时返回 false
     * @throws BizException 订单号为空或预占量不足时抛出；异常会回滚已写流水
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean release(Long skuId, Integer quantity, String orderNo) {
        requireOrderNo(orderNo);
        if (quantity == null || quantity <= 0) {
            return false;
        }
        try {
            recordJournal(skuId, quantity.longValue(), StockJournalTypeEnum.RELEASE, null, orderNo);
        } catch (DuplicateKeyException e) {
            log.info("库存释放已记录，skuId={}，orderNo={}", skuId, orderNo);
            return true;
        }
        // 原子释放：未命中（记录不存在或预占不足）按失败处理
        if (stockManager.restoreReserved(skuId, quantity.longValue()) <= 0) {
            throw new BizException(StockCodeEnum.STOCK_INSUFFICIENT);
        }
        return true;
    }

    /**
     * 校验订单号存在且包含非空白字符。
     *
     * @param orderNo 库存变动关联的订单号
     * @throws BizException 订单号为 null、空串或仅含空白字符时抛出
     */
    private void requireOrderNo(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new BizException(CommonCode.PARAM_INVALID, "订单号不能为空");
        }
    }

    /**
     * 幂等创建 SKU 初始库存；已有记录时直接返回，不覆盖库存或归属。
     *
     * @param skuId SKU ID
     * @param merchantId SKU 所属商户 ID
     * @param initialQuantity 初始可用量；null 或负数按 0 处理
     * @throws BizException SKU ID 为空时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void initStock(Long skuId, Long merchantId, Integer initialQuantity) {
        if (skuId == null) {
            throw new BizException(CommonCode.PARAM_INVALID, "SKU ID不能为空");
        }
        StockPo po =
                stockManager.getOne(Wrappers.lambdaQuery(StockPo.class).eq(StockPo::getSkuId, skuId));
        if (po != null) {
            return;
        }
        final long initQty =
                initialQuantity == null || initialQuantity < 0 ? 0L : initialQuantity.longValue();
        po = new StockPo();
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
     * 查询 SKU 的可用量，不校验商户归属。
     *
     * @param skuId SKU ID
     * @return 可用量；库存记录不存在时返回 0
     */
    @Override
    public long query(Long skuId) {
        StockPo s =
                stockManager.getOne(Wrappers.lambdaQuery(StockPo.class).eq(StockPo::getSkuId, skuId));
        return s == null ? 0L : s.getAvailable();
    }

    /**
     * 查询指定商户名下 SKU 的可用量。
     *
     * @param skuId SKU ID
     * @param merchantId 当前商户 ID
     * @return 可用量；库存记录不存在时返回 0
     * @throws BizException 库存记录存在但不属于该商户时抛出
     */
    @Override
    public long query(Long skuId, Long merchantId) {
        StockPo s =
                stockManager.getOne(Wrappers.lambdaQuery(StockPo.class).eq(StockPo::getSkuId, skuId));
        if (s == null) {
            return 0L;
        }
        checkMerchantOwnership(s, merchantId);
        return s.getAvailable();
    }

    /**
     * 查询最多 500 个去重后的 SKU 可用量，并保持有效输入的出现顺序。
     *
     * @param skuIds SKU ID 列表
     * @return 库存快照列表；空输入或全部为 null 时返回空列表，缺失记录按 0 返回
     */
    @Override
    public List<StockAvailabilityDto> queryAvailability(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return List.of();
        }
        List<Long> normalizedIds =
                skuIds.stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .limit(MAX_AVAILABILITY_BATCH_SIZE)
                        .toList();
        if (normalizedIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> availableBySku = new LinkedHashMap<>(normalizedIds.size() * 2);
        List<StockPo> stocks =
                stockManager.list(Wrappers.lambdaQuery(StockPo.class).in(StockPo::getSkuId, normalizedIds));
        for (StockPo stock : stocks) {
            availableBySku.put(stock.getSkuId(), stock.getAvailable());
        }
        List<StockAvailabilityDto> result = new ArrayList<>(normalizedIds.size());
        for (Long skuId : normalizedIds) {
            result.add(new StockAvailabilityDto(skuId, availableBySku.getOrDefault(skuId, 0L)));
        }
        return result;
    }

    /**
     * 设置 SKU 告警阈值；库存记录不存在时先创建零库存记录。
     *
     * @param skuId SKU ID
     * @param threshold 告警阈值，必须大于等于 0
     * @throws BizException 阈值无效或条件更新未命中时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void setThreshold(Long skuId, Long threshold) {
        if (threshold == null || threshold < 0) {
            throw new BizException(StockCodeEnum.THRESHOLD_INVALID);
        }
        StockPo po = getOrCreateStock(skuId);
        po.setAlertThreshold(threshold);
        if (!stockManager.updateById(po)) {
            throw new BizException(StockCodeEnum.STOCK_CONCURRENT_UPDATE);
        }
        log.info("库存阈值已设置，skuId={}，threshold={}", skuId, threshold);
    }

    /**
     * 校验商户归属后设置 SKU 告警阈值；不存在记录时按指定商户创建。
     *
     * @param skuId SKU ID
     * @param threshold 告警阈值，必须大于等于 0
     * @param merchantId 当前商户 ID
     * @throws BizException 阈值无效、已有库存不属于该商户或条件更新未命中时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void setThreshold(Long skuId, Long threshold, Long merchantId) {
        if (threshold == null || threshold < 0) {
            throw new BizException(StockCodeEnum.THRESHOLD_INVALID);
        }
        StockPo po = getOrCreateMerchantStock(skuId, merchantId);
        po.setAlertThreshold(threshold);
        if (!stockManager.updateById(po)) {
            throw new BizException(StockCodeEnum.STOCK_CONCURRENT_UPDATE);
        }
        log.info("库存阈值已设置，skuId={}，merchantId={}，threshold={}", skuId, merchantId, threshold);
    }

    /**
     * 按增量调整可用量，禁止负库存，并记录调整流水和告警日志。
     *
     * @param skuId SKU ID
     * @param quantity 调整增量，不能为 0
     * @param reason 调整原因
     * @throws BizException 调整量为空或 0、调整后为负或条件更新未命中时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void adjustStock(Long skuId, Long quantity, String reason) {
        if (quantity == null || quantity == 0) {
            throw new BizException(StockCodeEnum.ADJUST_QUANTITY_ZERO);
        }
        StockPo po = getOrCreateStock(skuId);
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
     * 在商户归属校验后按增量调整可用量，并记录调整流水和告警日志。
     *
     * @param skuId SKU ID
     * @param quantity 调整增量，不能为 0
     * @param reason 调整原因
     * @param merchantId 当前商户 ID
     * @throws BizException 调整量无效、商户不匹配、调整后为负或条件更新未命中时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void adjustStock(Long skuId, Long quantity, String reason, Long merchantId) {
        if (quantity == null || quantity == 0) {
            throw new BizException(StockCodeEnum.ADJUST_QUANTITY_ZERO);
        }
        StockPo po = getOrCreateMerchantStock(skuId, merchantId);
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
        log.info("库存已调整，skuId={}，merchantId={}，quantity={}", skuId, merchantId, quantity);
    }

    /**
     * 查询指定 SKU 最近 200 条流水，不执行商户归属校验。
     *
     * @param skuId SKU ID
     * @return 按流水 ID 倒序排列的库存流水
     */
    public List<StockJournalPo> queryJournal(Long skuId) {
        return journalManager.list(
                Wrappers.lambdaQuery(StockJournalPo.class)
                        .eq(StockJournalPo::getSkuId, skuId)
                        .orderByDesc(StockJournalPo::getId)
                        .last("LIMIT " + QUERY_MAX_ROWS));
    }

    /**
     * 校验库存归属后查询指定商户 SKU 最近 200 条流水。
     *
     * @param skuId SKU ID
     * @param merchantId 当前商户 ID
     * @return 按流水 ID 倒序排列的库存流水
     * @throws BizException SKU 不存在或不属于该商户时抛出
     */
    public List<StockJournalPo> queryJournal(Long skuId, Long merchantId) {
        checkMerchantOwnership(getStock(skuId), merchantId);
        return journalManager.list(
                Wrappers.lambdaQuery(StockJournalPo.class)
                        .eq(StockJournalPo::getSkuId, skuId)
                        .orderByDesc(StockJournalPo::getId)
                        .last("LIMIT " + QUERY_MAX_ROWS));
    }

    /**
     * 查询商户可见流水并转换为接口视图。
     *
     * @param skuId SKU ID
     * @param merchantId 当前商户 ID
     * @return 最多 200 条库存流水视图
     * @throws BizException SKU 不存在或不属于该商户时抛出
     */
    @Override
    public List<StockJournalVo> queryJournalVo(Long skuId, Long merchantId) {
        return queryJournal(skuId, merchantId).stream().map(StockJournalVo::from).toList();
    }

    /**
     * 使用 ID 游标查询库存记录，支持 SKU 和低库存条件过滤。
     *
     * @param dto 游标、分页大小和筛选条件
     * @return 按库存 ID 倒序排列的游标分页结果
     */
    public CursorPageVo<StockPo> page(StockPageDto dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        final int limit = dto.getLimit();
        LambdaQueryWrapper<StockPo> wrapper = Wrappers.lambdaQuery(StockPo.class);
        wrapper.lt(lastId != null, StockPo::getId, lastId);
        if (dto.getSkuId() != null) {
            wrapper.eq(StockPo::getSkuId, dto.getSkuId());
        }
        if (Boolean.TRUE.equals(dto.getLowStockOnly())) {
            wrapper.isNotNull(StockPo::getAlertThreshold).apply("available <= alert_threshold");
        }
        wrapper.orderByDesc(StockPo::getId);
        wrapper.last("LIMIT " + (limit + 1));
        List<StockPo> records = stockManager.list(wrapper);
        return CursorPageVo.of(records, limit, StockPo::getId);
    }

    /**
     * 使用 ID 游标查询库存并转换为接口视图。
     *
     * @param dto 游标、分页大小和筛选条件
     * @return 按库存 ID 倒序排列的库存视图分页结果
     */
    @Override
    public CursorPageVo<StockVo> pageVo(StockPageDto dto) {
        return page(dto).map(StockVo::from);
    }

    /**
     * 将库存聚合结果转换为平台统计视图，并计算两位小数的告警 SKU 占比。
     *
     * @return 统计视图；SQL 聚合结果为空时返回全零数据
     */
    @Override
    public StockStatisticsVo platformStatistics() {
        StockStatisticsDto result = stockManager.statistics();
        if (result == null) {
            return new StockStatisticsVo(0L, 0L, 0L, 0L, BigDecimal.ZERO);
        }
        long total = result.getTotalSkuCount() == null ? 0L : result.getTotalSkuCount();
        long alert = result.getAlertSkuCount() == null ? 0L : result.getAlertSkuCount();
        BigDecimal ratio =
                total == 0L
                        ? BigDecimal.ZERO
                        : BigDecimal.valueOf(alert).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        return new StockStatisticsVo(
                result.getTotalSkuCount(),
                result.getTotalAvailable(),
                result.getTotalReserved(),
                result.getAlertSkuCount(),
                ratio);
    }

    /**
     * 导出指定 SKU 最近 10000 条流水，不执行商户归属校验。
     *
     * @param skuId SKU ID
     * @return 按流水 ID 倒序排列的库存流水
     */
    public List<StockJournalPo> exportJournal(Long skuId) {
        return journalManager.list(
                Wrappers.lambdaQuery(StockJournalPo.class)
                        .eq(StockJournalPo::getSkuId, skuId)
                        .orderByDesc(StockJournalPo::getId)
                        .last("LIMIT " + EXPORT_MAX_ROWS));
    }

    /**
     * 校验商户归属后导出指定 SKU 最近 10000 条流水。
     *
     * @param skuId SKU ID
     * @param merchantId 当前商户 ID
     * @return 按流水 ID 倒序排列的库存流水
     * @throws BizException SKU 不存在或不属于该商户时抛出
     */
    public List<StockJournalPo> exportJournal(Long skuId, Long merchantId) {
        checkMerchantOwnership(getStock(skuId), merchantId);
        return journalManager.list(
                Wrappers.lambdaQuery(StockJournalPo.class)
                        .eq(StockJournalPo::getSkuId, skuId)
                        .orderByDesc(StockJournalPo::getId)
                        .last("LIMIT " + EXPORT_MAX_ROWS));
    }

    /**
     * 导出商户可见流水并转换为接口视图。
     *
     * @param skuId SKU ID
     * @param merchantId 当前商户 ID
     * @return 最多 10000 条库存流水视图
     * @throws BizException SKU 不存在或不属于该商户时抛出
     */
    @Override
    public List<StockJournalVo> exportJournalVo(Long skuId, Long merchantId) {
        return exportJournal(skuId, merchantId).stream().map(StockJournalVo::from).toList();
    }

    /**
     * 在单事务内原子扣减源库存、增加目标库存，并保存两条流水和一条调拨记录。
     *
     * @param dto 源、目标 SKU、数量及操作审计信息
     * @throws BizException 源目标相同、目标库存不存在或源库存不足时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void transfer(StockTransferDto dto) {
        if (dto.getFromSkuId().equals(dto.getToSkuId())) {
            throw new BizException(StockCodeEnum.STOCK_TRANSFER_SAME_SKU);
        }
        final StockPo from = getStock(dto.getFromSkuId());
        StockPo to =
                stockManager.getOne(
                        Wrappers.lambdaQuery(StockPo.class).eq(StockPo::getSkuId, dto.getToSkuId()));
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

        recordJournal(
                dto.getFromSkuId(),
                -dto.getQuantity(),
                StockJournalTypeEnum.TRANSFER_OUT,
                dto.getReason(),
                null);
        recordJournal(
                dto.getToSkuId(),
                dto.getQuantity(),
                StockJournalTypeEnum.TRANSFER_IN,
                dto.getReason(),
                null);

        StockTransferPo transfer = new StockTransferPo();
        transfer.setFromSkuId(dto.getFromSkuId());
        transfer.setToSkuId(dto.getToSkuId());
        transfer.setQuantity(dto.getQuantity());
        transfer.setReason(dto.getReason());
        transfer.setOperatorId(dto.getOperatorId());
        transferManager.save(transfer);

        checkAlert(from);
        checkAlert(to);
        log.info(
                "库存已调拨，from={}，to={}，quantity={}", dto.getFromSkuId(), dto.getToSkuId(), dto.getQuantity());
    }

    /**
     * 使用 ID 游标查询调拨记录，支持源 SKU 和目标 SKU 过滤。
     *
     * @param dto 游标、分页大小和筛选条件
     * @return 按调拨记录 ID 倒序排列的游标分页结果
     */
    public CursorPageVo<StockTransferPo> transferPage(StockTransferPageDto dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        final int limit = dto.getLimit();
        LambdaQueryWrapper<StockTransferPo> wrapper = Wrappers.lambdaQuery(StockTransferPo.class);
        wrapper.lt(lastId != null, StockTransferPo::getId, lastId);
        if (dto.getFromSkuId() != null) {
            wrapper.eq(StockTransferPo::getFromSkuId, dto.getFromSkuId());
        }
        if (dto.getToSkuId() != null) {
            wrapper.eq(StockTransferPo::getToSkuId, dto.getToSkuId());
        }
        wrapper.orderByDesc(StockTransferPo::getId);
        wrapper.last("LIMIT " + (limit + 1));
        List<StockTransferPo> records = transferManager.list(wrapper);
        return CursorPageVo.of(records, limit, StockTransferPo::getId);
    }

    /**
     * 使用 ID 游标查询调拨记录并转换为接口视图。
     *
     * @param dto 游标、分页大小和筛选条件
     * @return 按调拨记录 ID 倒序排列的视图分页结果
     */
    @Override
    public CursorPageVo<StockTransferVo> transferPageVo(StockTransferPageDto dto) {
        return transferPage(dto).map(StockTransferVo::from);
    }

    /**
     * 读取 SKU 当前可用量作为账面量，创建待盘点任务。
     *
     * @param dto SKU、备注和操作人信息
     * @return 新建盘点任务 ID
     * @throws BizException SKU 库存记录不存在时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCountTask(StockCountTaskCreateDto dto) {
        StockPo po = getStock(dto.getSkuId());
        StockCountTaskPo task = new StockCountTaskPo();
        task.setSkuId(dto.getSkuId());
        task.setExpectedQuantity(po.getAvailable());
        task.setStatus(StockCountTaskStatusEnum.PENDING.intCode());
        task.setRemark(dto.getRemark());
        task.setOperatorId(dto.getOperatorId());
        countTaskManager.save(task);
        log.info("盘点任务已创建，id={}，skuId={}，expected={}", task.getId(), dto.getSkuId(), po.getAvailable());
        return task.getId();
    }

    /**
     * 使用 ID 游标查询盘点任务，支持 SKU 和状态过滤。
     *
     * @param dto 游标、分页大小和筛选条件
     * @return 按任务 ID 倒序排列的游标分页结果
     */
    public CursorPageVo<StockCountTaskPo> countTaskPage(StockCountTaskPageDto dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        final int limit = dto.getLimit();
        LambdaQueryWrapper<StockCountTaskPo> wrapper = Wrappers.lambdaQuery(StockCountTaskPo.class);
        wrapper.lt(lastId != null, StockCountTaskPo::getId, lastId);
        if (dto.getSkuId() != null) {
            wrapper.eq(StockCountTaskPo::getSkuId, dto.getSkuId());
        }
        if (dto.getStatus() != null) {
            wrapper.eq(StockCountTaskPo::getStatus, dto.getStatus());
        }
        wrapper.orderByDesc(StockCountTaskPo::getId);
        wrapper.last("LIMIT " + (limit + 1));
        List<StockCountTaskPo> records = countTaskManager.list(wrapper);
        return CursorPageVo.of(records, limit, StockCountTaskPo::getId);
    }

    /**
     * 使用 ID 游标查询盘点任务并转换为接口视图。
     *
     * @param dto 游标、分页大小和筛选条件
     * @return 按任务 ID 倒序排列的视图分页结果
     */
    @Override
    public CursorPageVo<StockCountTaskVo> countTaskPageVo(StockCountTaskPageDto dto) {
        return countTaskPage(dto).map(StockCountTaskVo::from);
    }

    /**
     * 原子完成待盘点任务，并在存在差异时于同一事务内调整库存。
     *
     * @param id 盘点任务 ID
     * @param dto 实盘数量和非空时覆盖原备注的新备注
     * @throws BizException 任务不存在、状态非待盘点或条件更新未命中时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void completeCountTask(Long id, StockCountTaskCompleteDto dto) {
        StockCountTaskPo task = countTaskManager.getById(id);
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
        if (countTaskManager.completeIfStatus(
                        id,
                        StockCountTaskStatusEnum.PENDING.intCode(),
                        StockCountTaskStatusEnum.COMPLETED.intCode(),
                        actualQuantity,
                        diffQuantity,
                        remark)
                == 0) {
            throw new BizException(StockCodeEnum.STOCK_COUNT_TASK_NOT_PENDING);
        }
        task.setActualQuantity(actualQuantity);
        task.setDiffQuantity(diffQuantity);
        task.setStatus(StockCountTaskStatusEnum.COMPLETED.intCode());

        if (diffQuantity != 0) {
            adjustStock(task.getSkuId(), diffQuantity, "盘点差异调整");
        }
        log.info("盘点任务已完成，id={}，skuId={}，diff={}", id, task.getSkuId(), diffQuantity);
    }

    /**
     * 原子取消待盘点任务，不调整库存数量。
     *
     * @param id 盘点任务 ID
     * @param operatorId 操作人 ID，仅写入日志
     * @throws BizException 任务不存在、状态非待盘点或条件更新未命中时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelCountTask(Long id, Long operatorId) {
        StockCountTaskPo task = countTaskManager.getById(id);
        if (task == null) {
            throw new BizException(StockCodeEnum.STOCK_COUNT_TASK_NOT_FOUND);
        }
        if (task.getStatus() != StockCountTaskStatusEnum.PENDING.intCode()) {
            throw new BizException(StockCodeEnum.STOCK_COUNT_TASK_NOT_PENDING);
        }
        // 条件更新（PENDING->CANCELLED 原子迁移），并发下完成/取消互斥，未命中说明已被并发方处理
        if (countTaskManager.cancelIfStatus(
                        id,
                        StockCountTaskStatusEnum.PENDING.intCode(),
                        StockCountTaskStatusEnum.CANCELLED.intCode())
                == 0) {
            throw new BizException(StockCodeEnum.STOCK_COUNT_TASK_NOT_PENDING);
        }
        task.setStatus(StockCountTaskStatusEnum.CANCELLED.intCode());
        log.info("盘点任务已取消，id={}，operator={}", id, operatorId);
    }

    /**
     * 查询可用量或预占量小于 0 的异常库存，单次最多返回 200 条。
     *
     * @return 异常库存持久化对象列表
     */
    public List<StockPo> listAbnormalStock() {
        return stockManager.list(
                Wrappers.lambdaQuery(StockPo.class)
                        .lt(StockPo::getAvailable, 0)
                        .or()
                        .lt(StockPo::getReserved, 0)
                        .last("LIMIT " + QUERY_MAX_ROWS));
    }

    /**
     * 查询异常库存并转换为接口视图。
     *
     * @return 最多 200 条异常库存视图
     */
    @Override
    public List<StockVo> listAbnormalStockVo() {
        return listAbnormalStock().stream().map(StockVo::from).toList();
    }

    /**
     * 将可用量纠正为目标值，并按目标值与旧值的差值记录纠正流水。
     *
     * @param skuId SKU ID
     * @param correctQuantity 目标可用量，必须大于等于 0
     * @param reason 纠正原因
     * @throws BizException 目标值为空或为负、库存不存在或条件更新未命中时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void correctStock(Long skuId, Long correctQuantity, String reason) {
        if (correctQuantity == null || correctQuantity < 0) {
            throw new BizException(StockCodeEnum.STOCK_CORRECT_QUANTITY_INVALID);
        }
        StockPo po = getStock(skuId);
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
     * 查询 SKU 库存，并要求记录必须存在。
     *
     * @param skuId SKU ID
     * @return 库存持久化对象
     * @throws BizException SKU 库存记录不存在时抛出
     */
    private StockPo getStock(Long skuId) {
        StockPo po =
                stockManager.getOne(Wrappers.lambdaQuery(StockPo.class).eq(StockPo::getSkuId, skuId));
        if (po == null) {
            throw new BizException(StockCodeEnum.STOCK_NOT_FOUND);
        }
        return po;
    }

    /**
     * 查询 SKU 库存；不存在时创建可用量和预占量均为 0 的记录。
     *
     * @param skuId SKU ID
     * @return 已有或新建的库存持久化对象
     */
    private StockPo getOrCreateStock(Long skuId) {
        StockPo po =
                stockManager.getOne(Wrappers.lambdaQuery(StockPo.class).eq(StockPo::getSkuId, skuId));
        if (po != null) {
            return po;
        }
        po = new StockPo();
        po.setSkuId(skuId);
        po.setAvailable(0L);
        po.setReserved(0L);
        stockManager.save(po);
        return po;
    }

    /**
     * 查询指定商户的 SKU 库存；已有记录的归属不匹配时拒绝访问，不存在时创建零库存记录。
     *
     * @param skuId SKU ID
     * @param merchantId 当前商户 ID
     * @return 已有或新建的商户库存持久化对象
     * @throws BizException 已有库存未绑定商户或不属于该商户时抛出
     */
    private StockPo getOrCreateMerchantStock(Long skuId, Long merchantId) {
        StockPo po =
                stockManager.getOne(Wrappers.lambdaQuery(StockPo.class).eq(StockPo::getSkuId, skuId));
        if (po != null) {
            checkMerchantOwnership(po, merchantId);
            return po;
        }
        po = new StockPo();
        po.setSkuId(skuId);
        po.setMerchantId(merchantId);
        po.setAvailable(0L);
        po.setReserved(0L);
        stockManager.save(po);
        return po;
    }

    /**
     * 校验库存记录已绑定商户，且绑定值与当前商户一致。
     *
     * @param po 库存持久化对象
     * @param merchantId 当前商户 ID
     * @throws BizException 库存未绑定商户或不属于该商户时抛出
     */
    private void checkMerchantOwnership(StockPo po, Long merchantId) {
        if (po.getMerchantId() == null || !po.getMerchantId().equals(merchantId)) {
            throw new BizException(StockCodeEnum.STOCK_NOT_FOUND);
        }
    }

    /**
     * 构建并持久化一条库存流水，保存失败时终止当前事务。
     *
     * @param skuId SKU ID
     * @param quantity 有符号变动数量
     * @param type 流水业务类型
     * @param reason 变动原因
     * @param orderNo 关联订单号；人工调整时可为空
     * @throws BizException 流水保存失败时抛出
     */
    private void recordJournal(
            Long skuId, Long quantity, StockJournalTypeEnum type, String reason, String orderNo) {
        StockJournalPo journal = new StockJournalPo();
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
     * 当库存可用量不高于已配置阈值时输出告警日志。
     *
     * @param po 已完成本次库存变更的持久化对象
     */
    private void checkAlert(StockPo po) {
        if (po.getAlertThreshold() != null && po.getAvailable() <= po.getAlertThreshold()) {
            log.warn(
                    "库存告警，skuId={}，available={}，threshold={}",
                    po.getSkuId(),
                    po.getAvailable(),
                    po.getAlertThreshold());
        }
    }
}
