package com.yirancrazy.minimall.goods.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDTO;
import com.yirancrazy.minimall.api.dto.merchant.ShopSnapshotDTO;
import com.yirancrazy.minimall.api.dto.stock.StockAdjustDTO;
import com.yirancrazy.minimall.api.dto.stock.StockInitDTO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.goods.constant.AuditDecisionEnum;
import com.yirancrazy.minimall.goods.constant.SkuCodeEnum;
import com.yirancrazy.minimall.goods.constant.SpuCodeEnum;
import com.yirancrazy.minimall.goods.constant.SpuStatusEnum;
import com.yirancrazy.minimall.goods.dto.SkuItemDTO;
import com.yirancrazy.minimall.goods.dto.SpuCreateDTO;
import com.yirancrazy.minimall.goods.dto.SpuPageDTO;
import com.yirancrazy.minimall.goods.dto.SpuUpdateDTO;
import com.yirancrazy.minimall.goods.entity.SkuPO;
import com.yirancrazy.minimall.goods.entity.SpuAuditRecordPO;
import com.yirancrazy.minimall.goods.entity.SpuPO;
import com.yirancrazy.minimall.goods.gateway.GoodsRemoteGateway;
import com.yirancrazy.minimall.goods.manager.SkuManager;
import com.yirancrazy.minimall.goods.manager.SpuAuditRecordManager;
import com.yirancrazy.minimall.goods.manager.SpuManager;
import com.yirancrazy.minimall.goods.search.SpuDocument;
import com.yirancrazy.minimall.goods.search.SpuSearchService;
import com.yirancrazy.minimall.goods.service.SpuService;
import com.yirancrazy.minimall.goods.vo.SkuVO;
import com.yirancrazy.minimall.goods.vo.SpuAuditRecordVO;
import com.yirancrazy.minimall.goods.vo.SpuVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SpuServiceImpl 服务实现，实现相关业务逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Service
public class SpuServiceImpl implements SpuService {

    private final SpuManager spuManager;
    private final SpuAuditRecordManager spuAuditRecordManager;
    private final SpuSearchService spuSearchService;
    private final SkuManager skuManager;
    private final GoodsRemoteGateway goodsRemoteGateway;
    private final MinioUtil minioUtil;
    private final TransactionTemplate transactionTemplate;


    private static final String SHOP_STATUS_ACTIVE = "ACTIVE";

    /**
     * 构造 SpuServiceImpl 实例。
     * @param spuManager spuManager 参数
     * @param spuAuditRecordManager spuAuditRecordManager 参数
     * @param spuSearchService spuSearchService 参数
     * @param skuManager skuManager 参数
     * @param goodsRemoteGateway goodsRemoteGateway 参数
     * @param minioUtil minioUtil 参数
     * @param transactionTemplate transactionTemplate 参数
     */
    public SpuServiceImpl(SpuManager spuManager,
                          SpuAuditRecordManager spuAuditRecordManager,
                          SpuSearchService spuSearchService,
                          SkuManager skuManager,
                          GoodsRemoteGateway goodsRemoteGateway,
                          MinioUtil minioUtil,
                          TransactionTemplate transactionTemplate) {
        this.spuManager = spuManager;
        this.spuAuditRecordManager = spuAuditRecordManager;
        this.spuSearchService = spuSearchService;
        this.skuManager = skuManager;
        this.goodsRemoteGateway = goodsRemoteGateway;
        this.minioUtil = minioUtil;
        this.transactionTemplate = transactionTemplate;
    }


    /**
     * 处理requireActive店铺。
     * @param shopId 店铺 ID
     * @param merchantId 商户 ID
     * @throws BizException 业务校验失败时抛出
     */
    private void requireActiveShop(Long shopId, Long merchantId) {
        if (shopId == null) {
            throw new BizException(SpuCodeEnum.SPU_SHOP_INVALID);
        }
        Result<ShopSnapshotDTO> result = goodsRemoteGateway.shopSnapshot(shopId);
        ShopSnapshotDTO snap = result == null ? null : result.getData();
        if (snap == null || snap.getShopId() == null || snap.getShopId() <= 0
            || !merchantId.equals(snap.getMerchantId())
            || !SHOP_STATUS_ACTIVE.equals(snap.getStatus())) {
            throw new BizException(SpuCodeEnum.SPU_SHOP_INVALID);
        }
    }


    /**
     * 获取相关数据。
     * @param id id 参数
     * @return 处理结果
     */
    public SpuPO getById(Long id) {
        SpuPO s = spuManager.getById(id);
        if (s == null) {
            throw new BizException(SpuCodeEnum.SPU_NOT_FOUND);
        }
        return s;
    }


    /**
     * 获取相关数据。
     * @param id id 参数
     * @param merchantId merchantId 参数
     * @return 处理结果
     */
    public SpuPO getById(Long id, Long merchantId) {
        SpuPO s = getById(id);
        checkOwner(s, merchantId);
        return s;
    }


    /**
     * 校验Owner。
     * @param existing existing
     * @param merchantId 商户 ID
     * @throws BizException 业务校验失败时抛出
     */
    private void checkOwner(SpuPO existing, Long merchantId) {
        if (existing.getMerchantId() != null && !existing.getMerchantId().equals(merchantId)) {
            throw new BizException(SpuCodeEnum.SPU_NOT_FOUND);
        }
    }


    /**
     * 创建业务数据。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 业务数值
     */
    @Override
    public Long create(Long merchantId, SpuCreateDTO dto) {
        // 远程店铺校验必须在事务外完成，避免 RPC 占用数据库事务
        requireActiveShop(dto.getShopId(), merchantId);
        SpuPO po = new SpuPO();
        po.setSpuNo(UUID.randomUUID().toString().replace("-", ""));
        po.setShopId(dto.getShopId());
        po.setMerchantId(merchantId);
        po.setCategoryId(dto.getCategoryId());
        po.setTitle(dto.getTitle());
        po.setSubtitle(dto.getSubtitle());
        po.setMainImageUrl(dto.getMainImageUrl());
        po.setStatus(SpuStatusEnum.DRAFT.statusValue());
        Long spuId = transactionTemplate.execute(status -> {
            spuManager.save(po);
            List<SkuPO> createdSkus = saveSkus(merchantId, po.getId(), dto.getSkus());
            afterCommit(() -> createdSkus.forEach(sku -> initStockBestEffort(sku, merchantId)));
            return po.getId();
        });
        log.info("spu 已创建，spuId={}，merchantId={}", spuId, merchantId);
        return spuId;
    }


    /**
     * 创建Skus。
     * @param merchantId 商户 ID
     * @param spuId SPU ID
     * @param items 明细列表
     * @return 结果列表
     */
    private List<SkuPO> saveSkus(Long merchantId, Long spuId, List<SkuItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        List<SkuPO> skus = items.stream().map(it -> {
            SkuPO sku = new SkuPO();
            sku.setSpuId(spuId);
            sku.setMerchantId(merchantId);
            sku.setSkuName(it.getSkuName() == null || it.getSkuName().isBlank() ? "-" : it.getSkuName());
            sku.setPrice(it.getPrice() != null ? it.getPrice() : BigDecimal.ZERO);
            sku.setStock(it.getStock() != null ? it.getStock() : 0);
            return sku;
        }).collect(Collectors.toList());
        skuManager.saveBatch(skus);
        return skus;
    }


    /**
     * 获取相关数据。
     * @param id id 参数
     * @param merchantId merchantId 参数
     * @return 处理结果
     */
    @Override
    public SpuVO getDetail(Long id, Long merchantId) {
        SpuPO po = getById(id, merchantId);
        SpuVO vo = resolveMainImage(SpuVO.from(po));
        List<SkuPO> skus = skuManager.list(
            Wrappers.lambdaQuery(SkuPO.class).eq(SkuPO::getSpuId, id));
        vo.setSkus(skus.stream().map(SkuVO::from).collect(Collectors.toList()));
        return vo;
    }


    /**
     * 转换或解析MainImage。
     * @param vo vo
     * @return 视图对象
     */
    private SpuVO resolveMainImage(SpuVO vo) {
        vo.setMainImageUrl(minioUtil.resolvePublicUrl(vo.getMainImageUrl()));
        return vo;
    }


    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    @Override
    public CursorPageVO<SpuVO> page(SpuPageDTO dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        int limit = dto.getLimit();
        List<SpuPO> records = spuManager.list(Wrappers.lambdaQuery(SpuPO.class)
            .eq(dto.getMerchantId() != null, SpuPO::getMerchantId, dto.getMerchantId())
            .eq(dto.getStatus() != null, SpuPO::getStatus, dto.getStatus())
            .like(dto.getTitle() != null && !dto.getTitle().isBlank(),
                SpuPO::getTitle, dto.getTitle())
            .lt(lastId != null, SpuPO::getId, lastId)
            .orderByDesc(SpuPO::getId)
            .last("LIMIT " + (limit + 1)));
        Map<Long, List<SkuPO>> skuMap = batchSkus(records.stream()
            .map(SpuPO::getId).collect(Collectors.toList()));
        return CursorPageVO.of(records, limit, SpuPO::getId).map(po -> {
            SpuVO vo = resolveMainImage(SpuVO.from(po));
            vo.setSkus(skuMap.getOrDefault(po.getId(), Collections.emptyList()).stream()
                .map(SkuVO::from).collect(Collectors.toList()));
            return vo;
        });
    }


    /**
     * 处理batchSkus。
     * @param spuIds SPUIds
     * @return 结果列表
     */
    private Map<Long, List<SkuPO>> batchSkus(List<Long> spuIds) {
        List<Long> ids = spuIds.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return skuManager.list(Wrappers.lambdaQuery(SkuPO.class).in(SkuPO::getSpuId, ids)).stream()
            .collect(Collectors.groupingBy(SkuPO::getSpuId));
    }


    /**
     * 更新业务数据。
     * @param id id 参数
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 是否处理成功
     */
    @Override
    public boolean update(Long id, Long merchantId, SpuUpdateDTO dto) {
        SpuPO existing = spuManager.getById(id);
        if (existing == null) {
            throw new BizException(SpuCodeEnum.SPU_NOT_FOUND);
        }
        checkOwner(existing, merchantId);
        // 更换店铺时同样要求新店铺存在、归属当前商家且营业中；远程校验在事务外完成
        if (dto.getShopId() != null) {
            requireActiveShop(dto.getShopId(), merchantId);
            existing.setShopId(dto.getShopId());
        }
        if (dto.getCategoryId() != null) {
            existing.setCategoryId(dto.getCategoryId());
        }
        if (dto.getTitle() != null) {
            existing.setTitle(dto.getTitle());
        }
        if (dto.getSubtitle() != null) {
            existing.setSubtitle(dto.getSubtitle());
        }
        if (dto.getMainImageUrl() != null) {
            existing.setMainImageUrl(dto.getMainImageUrl());
        }
        // 编辑展示字段必须重新审核：置回待审使原在售商品经 syncToEs 从前台搜索下架
        if (existing.getStatus() == null
            || existing.getStatus() != SpuStatusEnum.DRAFT.statusValue()) {
            existing.setStatus(SpuStatusEnum.PENDING_AUDIT.statusValue());
        }
        // SKU 差异与库存远程调整在事务外准备，事务内只落库，避免 RPC 占用数据库事务
        SkuSyncPlan plan = dto.getSkus() != null
            ? prepareSkuSync(merchantId, id, dto.getSkus())
            : SkuSyncPlan.empty();
        try {
            boolean ok = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
                if (!spuManager.updateById(existing)) {
                    return false;
                }
                applySkuSync(plan);
                afterCommit(() -> {
                    plan.createdSkus().forEach(sku -> initStockBestEffort(sku, merchantId));
                    syncToEs(existing);
                });
                return true;
            }));
            if (!ok) {
                rollbackStockAdjustments(plan.compensations());
            }
            return ok;
        }
        catch (RuntimeException e) {
            rollbackStockAdjustments(plan.compensations());
            throw e;
        }
    }


    /**
     * 处理prepareSKUSync。
     * @param merchantId 商户 ID
     * @param spuId SPU ID
     * @param items 明细列表
     * @return 处理结果
     */
    private SkuSyncPlan prepareSkuSync(Long merchantId, Long spuId, List<SkuItemDTO> items) {
        List<SkuPO> existing = skuManager.list(
            Wrappers.lambdaQuery(SkuPO.class).eq(SkuPO::getSpuId, spuId));
        Map<Long, SkuPO> existingById = existing.stream()
            .filter(s -> s.getId() != null)
            .collect(Collectors.toMap(SkuPO::getId, Function.identity()));
        Set<Long> keepIds = new HashSet<>();
        List<SkuPO> toCreate = new ArrayList<>();
        List<SkuPO> toUpdate = new ArrayList<>();
        List<StockAdjustDTO> compensations = new ArrayList<>();
        for (SkuItemDTO it : items) {
            SkuPO sku = existingById.get(it.getId());
            if (sku == null) {
                sku = new SkuPO();
                sku.setSpuId(spuId);
                sku.setMerchantId(merchantId);
                toCreate.add(sku);
            }
            else {
                keepIds.add(it.getId());
                toUpdate.add(sku);
            }
            sku.setSkuName(it.getSkuName() == null || it.getSkuName().isBlank() ? "-" : it.getSkuName());
            sku.setPrice(it.getPrice() != null ? it.getPrice() : BigDecimal.ZERO);
            if (sku.getId() != null && it.getStock() != null
                && !it.getStock().equals(sku.getStock())) {
                long currentStock = sku.getStock() == null ? 0L : sku.getStock().longValue();
                long delta = it.getStock().longValue() - currentStock;
                try {
                    adjustStock(sku.getId(), merchantId, delta, "商品规格库存调整");
                    compensations.add(new StockAdjustDTO(
                        sku.getId(), merchantId, -delta, "商品规格库存调整回滚"));
                    sku.setStock(it.getStock());
                }
                catch (RuntimeException e) {
                    rollbackStockAdjustments(compensations);
                    throw e;
                }
            }
            if (sku.getId() == null) {
                sku.setStock(it.getStock() != null ? it.getStock() : 0);
            }
        }
        List<Long> toDelete = existing.stream().map(SkuPO::getId)
            .filter(id -> id != null && !keepIds.contains(id)).toList();
        return new SkuSyncPlan(
            List.copyOf(toCreate), List.copyOf(toUpdate), toDelete, List.copyOf(compensations));
    }


    /**
     * 执行SKUSync逻辑。
     * @param plan plan
     * @throws BizException 业务校验失败时抛出
     */
    private void applySkuSync(SkuSyncPlan plan) {
        if (!plan.toCreate().isEmpty()) {
            skuManager.saveBatch(plan.toCreate());
        }
        if (!plan.toUpdate().isEmpty() && !skuManager.updateBatchById(plan.toUpdate())) {
            throw new BizException(SkuCodeEnum.STOCK_ADJUST_FAILED);
        }
        if (!plan.toDelete().isEmpty()) {
            skuManager.removeByIds(plan.toDelete());
        }
    }


    /**
     * @Author: yirancrazy@gmail.com
     * @Description: SkuSyncPlan 记录对象，承载不可变数据。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     **/
    private record SkuSyncPlan(List<SkuPO> toCreate, List<SkuPO> toUpdate, List<Long> toDelete,
                               List<StockAdjustDTO> compensations) {


        /**
         * 处理empty。
         * @return 处理结果
         */
        static SkuSyncPlan empty() {
            return new SkuSyncPlan(List.of(), List.of(), List.of(), List.of());
        }


        /**
         * 创建dSkus。
         * @return 结果列表
         */
        List<SkuPO> createdSkus() {
            return toCreate;
        }
    }


    /**
     * 更新库存。
     * @param skuId SKU ID
     * @param merchantId 商户 ID
     * @param delta delta
     * @param reason 原因
     * @throws BizException 业务校验失败时抛出
     */
    private void adjustStock(Long skuId, Long merchantId, long delta, String reason) {
        Result<Void> result = goodsRemoteGateway.adjustStock(
            new StockAdjustDTO(skuId, merchantId, delta, reason));
        if (result == null || !CommonCode.SUCCESS.equals(result.getCode())) {
            throw new BizException(SkuCodeEnum.STOCK_ADJUST_FAILED);
        }
    }


    /**
     * 处理rollback库存Adjustments。
     * @param adjustments adjustments
     */
    private void rollbackStockAdjustments(List<StockAdjustDTO> adjustments) {
        for (StockAdjustDTO adjustment : adjustments) {
            try {
                goodsRemoteGateway.adjustStock(adjustment);
            }
            catch (Exception e) {
                log.error("回滚库存调整失败，skuId={}，quantity={}",
                    adjustment.getSkuId(), adjustment.getQuantity(), e);
            }
        }
    }


    /**
     * 初始化库存BestEffort。
     * @param sku SKU
     * @param merchantId 商户 ID
     */
    private void initStockBestEffort(SkuPO sku, Long merchantId) {
        try {
            Result<Void> r = goodsRemoteGateway.initStock(
                new StockInitDTO(sku.getId(), merchantId, sku.getStock()));
            if (r == null || !CommonCode.SUCCESS.equals(r.getCode())) {
                log.warn("初始化库存失败，skuId={}，msg={}", sku.getId(),
                    r == null ? "无响应" : r.getMessage());
            }
        }
        catch (Exception e) {
            log.warn("初始化库存异常，skuId={}", sku.getId(), e);
        }
    }


    /**
     * 删除业务数据。
     * @param id id 参数
     * @param merchantId merchantId 参数
     * @return 是否处理成功
     */
    @Override
    public boolean delete(Long id, Long merchantId) {
        SpuPO existing = spuManager.getById(id);
        if (existing == null) {
            throw new BizException(SpuCodeEnum.SPU_NOT_FOUND);
        }
        checkOwner(existing, merchantId);
        boolean ok = spuManager.removeById(id);
        if (ok) {
            // 搜索引擎是只读镜像，删除失败不阻断主链路删除（残留文档由索引重建清理）
            try {
                spuSearchService.deleteById(id);
            }
            catch (Exception e) {
                log.error("从 ES 删除 spu 失败，spuId={}", id, e);
            }
        }
        log.info("spu 已删除，spuId={}", id);
        return ok;
    }


    /**
     * 执行 onShelf 方法。
     * @param id id 参数
     * @param merchantId merchantId 参数
     * @return 是否处理成功
     */
    @Override
    public boolean onShelf(Long id, Long merchantId) {
        SpuPO existing = spuManager.getById(id);
        if (existing == null) {
            throw new BizException(SpuCodeEnum.SPU_NOT_FOUND);
        }
        checkOwner(existing, merchantId);
        // 送审即发布前置，重新校验绑定店铺仍营业中，店铺被冻结/停业则禁止送审
        requireActiveShop(existing.getShopId(), merchantId);
        Integer s = existing.getStatus();
        if (s == null
            || (s != SpuStatusEnum.DRAFT.statusValue()
                && s != SpuStatusEnum.OFF_SHELF.statusValue()
                && s != SpuStatusEnum.REJECTED.statusValue())) {
            throw new BizException(SpuCodeEnum.SPU_STATUS_INVALID);
        }
        existing.setStatus(SpuStatusEnum.PENDING_AUDIT.statusValue());
        boolean ok = spuManager.updateById(existing);
        if (ok) {
            syncToEs(existing);
        }
        log.info("spu 已提交审核，spuId={}，ok={}", id, ok);
        return ok;
    }


    /**
     * 执行 offShelf 方法。
     * @param id id 参数
     * @param merchantId merchantId 参数
     * @return 是否处理成功
     */
    @Override
    public boolean offShelf(Long id, Long merchantId) {
        SpuPO existing = spuManager.getById(id);
        if (existing == null) {
            throw new BizException(SpuCodeEnum.SPU_NOT_FOUND);
        }
        checkOwner(existing, merchantId);
        if (existing.getStatus() == null
            || existing.getStatus() != SpuStatusEnum.ON_SALE.statusValue()) {
            throw new BizException(SpuCodeEnum.SPU_STATUS_INVALID);
        }
        existing.setStatus(SpuStatusEnum.OFF_SHELF.statusValue());
        boolean ok = spuManager.updateById(existing);
        if (ok) {
            syncToEs(existing);
        }
        log.info("spu 已下架，spuId={}，ok={}", id, ok);
        return ok;
    }


    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    public CursorPageVO<SpuPO> pagePending(SpuPageDTO dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        int limit = dto.getLimit();
        List<SpuPO> records = spuManager.list(Wrappers.lambdaQuery(SpuPO.class)
            .eq(SpuPO::getStatus, SpuStatusEnum.PENDING_AUDIT.statusValue())
            .lt(lastId != null, SpuPO::getId, lastId)
            .orderByDesc(SpuPO::getId)
            .last("LIMIT " + (limit + 1)));
        return CursorPageVO.of(records, limit, SpuPO::getId);
    }


    /**
     * 执行 pendingVO 方法。
     * @param dto dto 参数
     * @return 处理结果
     */
    @Override
    public CursorPageVO<SpuVO> pendingVO(SpuPageDTO dto) {
        return pagePending(dto).map(SpuVO::from);
    }


    /**
     * 执行 detailVO 方法。
     * @param spuId spuId 参数
     * @return 处理结果
     */
    @Override
    public SpuVO detailVO(Long spuId) {
        return SpuVO.from(getById(spuId));
    }


    /**
     * 执行 approve 方法。
     * @param spuId spuId 参数
     * @param auditorId auditorId 参数
     * @return 是否处理成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approve(Long spuId, Long auditorId) {
        SpuPO existing = spuManager.getById(spuId);
        if (existing == null) {
            throw new BizException(SpuCodeEnum.SPU_NOT_FOUND);
        }
        if (existing.getStatus() == null
            || existing.getStatus() != SpuStatusEnum.PENDING_AUDIT.statusValue()) {
            throw new BizException(SpuCodeEnum.SPU_NOT_PENDING_AUDIT);
        }
        existing.setStatus(SpuStatusEnum.ON_SALE.statusValue());
        existing.setPublishAt(LocalDateTime.now());
        boolean ok = spuManager.updateById(existing);
        if (ok) {
            afterCommit(() -> syncToEs(existing));
        }
        saveAuditRecord(spuId, auditorId, AuditDecisionEnum.APPROVE, null);
        log.info("spu 审核通过，spuId={}，auditorId={}", spuId, auditorId);
        return ok;
    }


    /**
     * 执行 reject 方法。
     * @param spuId spuId 参数
     * @param auditorId auditorId 参数
     * @param reason reason 参数
     * @return 是否处理成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reject(Long spuId, Long auditorId, String reason) {
        SpuPO existing = spuManager.getById(spuId);
        if (existing == null) {
            throw new BizException(SpuCodeEnum.SPU_NOT_FOUND);
        }
        if (existing.getStatus() == null
            || existing.getStatus() != SpuStatusEnum.PENDING_AUDIT.statusValue()) {
            throw new BizException(SpuCodeEnum.SPU_NOT_PENDING_AUDIT);
        }
        existing.setStatus(SpuStatusEnum.REJECTED.statusValue());
        boolean ok = spuManager.updateById(existing);
        if (ok) {
            afterCommit(() -> syncToEs(existing));
        }
        saveAuditRecord(spuId, auditorId, AuditDecisionEnum.REJECT, reason);
        log.info("spu 审核拒绝，spuId={}，auditorId={}，reason={}", spuId, auditorId, reason);
        return ok;
    }


    /**
     * 查询列表数据。
     * @param spuId spuId 参数
     * @return 结果列表
     */
    public List<SpuAuditRecordPO> listAuditRecords(Long spuId) {
        return spuAuditRecordManager.list(
            Wrappers.lambdaQuery(SpuAuditRecordPO.class)
                .eq(SpuAuditRecordPO::getSpuId, spuId)
                .orderByDesc(SpuAuditRecordPO::getAuditAt));
    }


    /**
     * 查询列表数据。
     * @param spuId spuId 参数
     * @return 结果列表
     */
    @Override
    public List<SpuAuditRecordVO> listAuditRecordsVO(Long spuId) {
        return listAuditRecords(spuId).stream().map(SpuAuditRecordVO::from).toList();
    }

    /**
     * 创建审核记录。
     * @param spuId SPU ID
     * @param auditorId auditor ID
     * @param decision decision
     * @param reason 原因
     */
    private void saveAuditRecord(Long spuId, Long auditorId, AuditDecisionEnum decision,
                                 String reason) {
        SpuAuditRecordPO record = new SpuAuditRecordPO();
        record.setSpuId(spuId);
        record.setAuditorId(auditorId);
        record.setDecision(decision.getCode());
        record.setReason(reason);
        record.setAuditAt(LocalDateTime.now());
        spuAuditRecordManager.save(record);
    }


    /**
     * 处理afterCommit。
     * @param action action
     */
    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            /**
             * 执行 afterCommit 方法。
             */
            public void afterCommit() {
                action.run();
            }
        });
    }


    /**
     * 执行 refreshEsDocument 方法。
     * @param spuId spuId 参数
     */
    public void refreshEsDocument(Long spuId) {
        SpuPO spu = spuManager.getById(spuId);
        if (spu == null) {
            // SPU 已删除时 ES 文档由 delete 流程清理，SKU 侧刷新直接忽略
            log.warn("刷新 ES 文档时未找到 spu，spuId={}", spuId);
            return;
        }
        syncToEs(spu);
    }


    /**
     * 执行 rebuildAllEsDocuments 方法。
     */
    public void rebuildAllEsDocuments() {
        List<SpuPO> onSaleList = spuManager.list(
            Wrappers.lambdaQuery(SpuPO.class)
                .eq(SpuPO::getStatus, SpuStatusEnum.ON_SALE.statusValue()));
        log.info("开始重建 ES 文档，count={}", onSaleList.size());
        onSaleList.forEach(this::syncToEs);
        log.info("重建 ES 文档完成，count={}", onSaleList.size());
    }

    /**
     * 处理sync转换Es。
     * @param po 持久化对象
     */
    private void syncToEs(SpuPO po) {
        // 搜索引擎是只读镜像，同步失败不阻断主链路写入（ES 恢复后由后续写操作补齐）
        try {
            SpuDocument doc = new SpuDocument();
            doc.setSpuId(po.getId());
            doc.setTitle(po.getTitle());
            doc.setCategoryId(po.getCategoryId());
            doc.setMerchantId(po.getMerchantId());
            doc.setSaleStatus(po.getStatus());
            doc.setMainImage(po.getMainImageUrl());
            doc.setCreateTime(po.getCreateTime().atZone(ZoneId.systemDefault()).toInstant());
            // 价格区间聚合该 SPU 全部 SKU 的 min/max（元→分），供 ES 价格过滤使用，null 价格跳过
            LongSummaryStatistics stats = skuManager.list(
                Wrappers.lambdaQuery(SkuPO.class).eq(SkuPO::getSpuId, po.getId())).stream()
                .map(SkuPO::getPrice)
                .filter(Objects::nonNull)
                .mapToLong(p -> p.movePointRight(2).longValueExact())
                .summaryStatistics();
            doc.setMinPrice(stats.getCount() == 0 ? null : stats.getMin());
            doc.setMaxPrice(stats.getCount() == 0 ? null : stats.getMax());
            spuSearchService.sync(doc);
        }
        catch (Exception e) {
            log.error("同步 spu 到 ES 失败，spuId={}", po.getId(), e);
        }
    }


    /**
     * 查询列表数据。
     * @param spuIds spuIds 参数
     * @return 结果映射
     */
    @Override
    public Map<Long, SpuSnapshotDTO> listSnapshots(List<Long> spuIds) {
        List<Long> ids = spuIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return spuManager.list(Wrappers.lambdaQuery(SpuPO.class).in(SpuPO::getId, ids)).stream()
            .collect(Collectors.toMap(SpuPO::getId,
                spu -> new SpuSnapshotDTO(spu.getId(), spu.getTitle(), spu.getMainImageUrl())));
    }


    /**
     * 获取相关数据。
     * @param id id 参数
     * @return 处理结果
     */
    @Override
    public SpuSnapshotDTO getSnapshot(Long id) {
        SpuPO spu = spuManager.getById(id);
        if (spu == null) {
            return new SpuSnapshotDTO(id, null, null);
        }
        return new SpuSnapshotDTO(spu.getId(), spu.getTitle(), spu.getMainImageUrl());
    }
}
