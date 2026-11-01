package com.yirancrazy.minimall.goods.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDto;
import com.yirancrazy.minimall.api.dto.merchant.ShopSnapshotDto;
import com.yirancrazy.minimall.api.dto.stock.StockAdjustDto;
import com.yirancrazy.minimall.api.dto.stock.StockInitDto;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.goods.constant.AuditDecisionEnum;
import com.yirancrazy.minimall.goods.constant.SkuCodeEnum;
import com.yirancrazy.minimall.goods.constant.SpuCodeEnum;
import com.yirancrazy.minimall.goods.constant.SpuStatusEnum;
import com.yirancrazy.minimall.goods.dto.SkuItemDto;
import com.yirancrazy.minimall.goods.dto.SpuCreateDto;
import com.yirancrazy.minimall.goods.dto.SpuPageDto;
import com.yirancrazy.minimall.goods.dto.SpuUpdateDto;
import com.yirancrazy.minimall.goods.entity.SkuPo;
import com.yirancrazy.minimall.goods.entity.SpuAuditRecordPo;
import com.yirancrazy.minimall.goods.entity.SpuPo;
import com.yirancrazy.minimall.goods.gateway.GoodsRemoteGateway;
import com.yirancrazy.minimall.goods.manager.SkuManager;
import com.yirancrazy.minimall.goods.manager.SpuAuditRecordManager;
import com.yirancrazy.minimall.goods.manager.SpuManager;
import com.yirancrazy.minimall.goods.search.SpuDocument;
import com.yirancrazy.minimall.goods.search.SpuSearchService;
import com.yirancrazy.minimall.goods.service.SpuService;
import com.yirancrazy.minimall.goods.vo.SkuVo;
import com.yirancrazy.minimall.goods.vo.SpuAuditRecordVo;
import com.yirancrazy.minimall.goods.vo.SpuVo;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 维护 SPU 生命周期和审核日志，协调店铺校验、SKU 批量同步、库存调整及 ES 镜像。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
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
     * 注入SPU 数据访问管理器、商品审核记录数据访问管理器、Elasticsearch SPU 索引服务、SKU 数据访问管理器、店铺与库存远程调用网关、MinIO 对象键到公网 URL
     *   的解析工具、SPU 与 SKU 原子写入使用的编程式事务模板，构造 SpuServiceImpl 实例。
     *
     * @param spuManager SPU 数据访问管理器
     * @param spuAuditRecordManager 商品审核记录数据访问管理器
     * @param spuSearchService Elasticsearch SPU 索引服务
     * @param skuManager SKU 数据访问管理器
     * @param goodsRemoteGateway 店铺与库存远程调用网关
     * @param minioUtil MinIO 对象键到公网 URL 的解析工具
     * @param transactionTemplate SPU 与 SKU 原子写入使用的编程式事务模板
     */
    public SpuServiceImpl(
            SpuManager spuManager,
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
     * 查询店铺快照并校验店铺存在、归属当前商家且处于 ACTIVE 状态。
     *
     * @param shopId 店铺主键，必须属于当前商家且处于营业状态
     * @param merchantId 当前商家 ID，用于商品归属校验
     */
    private void requireActiveShop(Long shopId, Long merchantId) {
        if (shopId == null) {
            throw new BizException(SpuCodeEnum.SPU_SHOP_INVALID);
        }
        Result<ShopSnapshotDto> result = goodsRemoteGateway.shopSnapshot(shopId);
        ShopSnapshotDto snap = result == null ? null : result.getData();
        if (snap == null
                || snap.getShopId() == null
                || snap.getShopId() <= 0
                || !merchantId.equals(snap.getMerchantId())
                || !SHOP_STATUS_ACTIVE.equals(snap.getStatus())) {
            throw new BizException(SpuCodeEnum.SPU_SHOP_INVALID);
        }
    }

    /**
     * 按主键读取 SPU，不存在时抛出 SPU_NOT_FOUND。
     *
     * @param id SPU 主键
     * @return 匹配的 SPU 持久化对象
     */
    public SpuPo getById(Long id) {
        SpuPo s = spuManager.getById(id);
        if (s == null) {
            throw new BizException(SpuCodeEnum.SPU_NOT_FOUND);
        }
        return s;
    }

    /**
     * 读取 SPU 并校验商家归属；不存在或越权统一抛出 SPU_NOT_FOUND。
     *
     * @param id SPU 主键
     * @param merchantId 当前商家 ID，用于商品归属校验
     * @return 匹配的 SPU 持久化对象
     */
    public SpuPo getById(Long id, Long merchantId) {
        SpuPo s = getById(id);
        checkOwner(s, merchantId);
        return s;
    }

    /**
     * 校验记录的商家归属；越权访问按记录不存在处理。
     *
     * @param existing 已持久化的商品记录
     * @param merchantId 当前商家 ID，用于商品归属校验
     */
    private void checkOwner(SpuPo existing, Long merchantId) {
        if (existing.getMerchantId() != null && !existing.getMerchantId().equals(merchantId)) {
            throw new BizException(SpuCodeEnum.SPU_NOT_FOUND);
        }
    }

    /**
     * 事务外校验店铺后，在同一事务内保存 SPU 与 SKU，并在提交后初始化库存。
     *
     * @param merchantId 当前商家 ID，用于商品归属校验
     * @param dto SPU 创建参数
     * @return 新建记录的主键
     */
    @Override
    public Long create(Long merchantId, SpuCreateDto dto) {
        // 远程店铺校验必须在事务外完成，避免 RPC 占用数据库事务
        requireActiveShop(dto.getShopId(), merchantId);
        SpuPo po = new SpuPo();
        po.setSpuNo(UUID.randomUUID().toString().replace("-", ""));
        po.setShopId(dto.getShopId());
        po.setMerchantId(merchantId);
        po.setCategoryId(dto.getCategoryId());
        po.setTitle(dto.getTitle());
        po.setSubtitle(dto.getSubtitle());
        po.setMainImageUrl(dto.getMainImageUrl());
        po.setStatus(SpuStatusEnum.DRAFT.statusValue());
        Long spuId =
                transactionTemplate.execute(
                        status -> {
                            spuManager.save(po);
                            List<SkuPo> createdSkus = saveSkus(merchantId, po.getId(), dto.getSkus());
                            afterCommit(() -> createdSkus.forEach(sku -> initStockBestEffort(sku, merchantId)));
                            return po.getId();
                        });
        log.info("spu 已创建，spuId={}，merchantId={}", spuId, merchantId);
        return spuId;
    }

    /**
     * 将 SPU 明细转换为 SKU 实体并批量保存，空白规格名使用占位符。
     *
     * @param merchantId 当前商家 ID，用于商品归属校验
     * @param spuId SPU 主键
     * @param items 提交的 SKU 明细列表
     * @return 匹配的 SKU 持久化对象列表；无数据时返回空列表
     */
    private List<SkuPo> saveSkus(Long merchantId, Long spuId, List<SkuItemDto> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        List<SkuPo> skus =
                items.stream()
                        .map(
                                it -> {
                                    SkuPo sku = new SkuPo();
                                    sku.setSpuId(spuId);
                                    sku.setMerchantId(merchantId);
                                    sku.setSkuName(
                                            it.getSkuName() == null || it.getSkuName().isBlank() ? "-" : it.getSkuName());
                                    sku.setPrice(it.getPrice() != null ? it.getPrice() : BigDecimal.ZERO);
                                    sku.setStock(it.getStock() != null ? it.getStock() : 0);
                                    return sku;
                                })
                        .collect(Collectors.toList());
        skuManager.saveBatch(skus);
        return skus;
    }

    /**
     * 读取商家自有 SPU，解析主图 URL 并组装 SKU 列表。
     *
     * @param id SPU 主键
     * @param merchantId 当前商家 ID，用于商品归属校验
     * @return 转换后的 SPU 视图对象
     */
    @Override
    public SpuVo getDetail(Long id, Long merchantId) {
        SpuPo po = getById(id, merchantId);
        SpuVo vo = resolveMainImage(SpuVo.from(po));
        List<SkuPo> skus = skuManager.list(Wrappers.lambdaQuery(SkuPo.class).eq(SkuPo::getSpuId, id));
        vo.setSkus(skus.stream().map(SkuVo::from).collect(Collectors.toList()));
        return vo;
    }

    /**
     * 将 SPU 视图中的 MinIO 对象键解析为公网图片 URL。
     *
     * @param vo 待解析主图 URL 的 SPU 视图对象
     * @return 转换后的 SPU 视图对象
     */
    private SpuVo resolveMainImage(SpuVo vo) {
        vo.setMainImageUrl(minioUtil.resolvePublicUrl(vo.getMainImageUrl()));
        return vo;
    }

    /**
     * 按商家、状态和标题游标分页查询 SPU，批量组装 SKU 并解析主图 URL。
     *
     * @param dto SPU 游标分页或状态过滤参数
     * @return 按主键游标分页的结果；记录项为转换后的 SPU 视图对象
     */
    @Override
    public CursorPageVo<SpuVo> page(SpuPageDto dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        int limit = dto.getLimit();
        List<SpuPo> records =
                spuManager.list(
                        Wrappers.lambdaQuery(SpuPo.class)
                                .eq(dto.getMerchantId() != null, SpuPo::getMerchantId, dto.getMerchantId())
                                .eq(dto.getStatus() != null, SpuPo::getStatus, dto.getStatus())
                                .like(
                                        dto.getTitle() != null && !dto.getTitle().isBlank(),
                                        SpuPo::getTitle,
                                        dto.getTitle())
                                .lt(lastId != null, SpuPo::getId, lastId)
                                .orderByDesc(SpuPo::getId)
                                .last("LIMIT " + (limit + 1)));
        Map<Long, List<SkuPo>> skuMap =
                batchSkus(records.stream().map(SpuPo::getId).collect(Collectors.toList()));
        return CursorPageVo.of(records, limit, SpuPo::getId)
                .map(
                        po -> {
                            SpuVo vo = resolveMainImage(SpuVo.from(po));
                            vo.setSkus(
                                    skuMap.getOrDefault(po.getId(), Collections.emptyList()).stream()
                                            .map(SkuVo::from)
                                            .collect(Collectors.toList()));
                            return vo;
                        });
    }

    /**
     * 按 SPU 主键批量查询 SKU 并按父 SPU 分组。
     *
     * @param spuIds 待查询的 SPU 主键列表，空列表返回空映射
     * @return 以 SPU 主键为键的 SKU 分组映射；无数据时返回空映射
     */
    private Map<Long, List<SkuPo>> batchSkus(List<Long> spuIds) {
        List<Long> ids = spuIds.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return skuManager.list(Wrappers.lambdaQuery(SkuPo.class).in(SkuPo::getSpuId, ids)).stream()
                .collect(Collectors.groupingBy(SkuPo::getSpuId));
    }

    /**
     * 事务外准备库存调整和 SKU 差异，事务内原子写入；成功后同步 ES 镜像。
     *
     * @param id SPU 主键
     * @param merchantId 当前商家 ID，用于商品归属校验
     * @param dto SPU 增量更新参数
     * @return 操作实际影响记录时为 true，否则为 false
     */
    @Override
    public boolean update(Long id, Long merchantId, SpuUpdateDto dto) {
        SpuPo existing = spuManager.getById(id);
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
        if (existing.getStatus() == null || existing.getStatus() != SpuStatusEnum.DRAFT.statusValue()) {
            existing.setStatus(SpuStatusEnum.PENDING_AUDIT.statusValue());
        }
        // SKU 差异与库存远程调整在事务外准备，事务内只落库，避免 RPC 占用数据库事务
        SkuSyncPlan plan =
                dto.getSkus() != null ? prepareSkuSync(merchantId, id, dto.getSkus()) : SkuSyncPlan.empty();
        try {
            boolean ok =
                    Boolean.TRUE.equals(
                            transactionTemplate.execute(
                                    status -> {
                                        if (!spuManager.updateById(existing)) {
                                            return false;
                                        }
                                        applySkuSync(plan);
                                        afterCommit(
                                                () -> {
                                                    plan.createdSkus().forEach(sku -> initStockBestEffort(sku, merchantId));
                                                    syncToEs(existing);
                                                });
                                        return true;
                                    }));
            if (!ok) {
                rollbackStockAdjustments(plan.compensations());
            }
            return ok;
        } catch (RuntimeException e) {
            rollbackStockAdjustments(plan.compensations());
            throw e;
        }
    }

    /**
     * 在事务外计算 SKU 新增、更新、删除差异，并预先提交库存增量及补偿指令。
     *
     * @param merchantId 当前商家 ID，用于商品归属校验
     * @param spuId SPU 主键
     * @param items 提交的 SKU 明细列表
     * @return 本次 SPU 编辑需要的 SKU 变更与库存补偿计划
     */
    private SkuSyncPlan prepareSkuSync(Long merchantId, Long spuId, List<SkuItemDto> items) {
        List<SkuPo> existing =
                skuManager.list(Wrappers.lambdaQuery(SkuPo.class).eq(SkuPo::getSpuId, spuId));
        Map<Long, SkuPo> existingById =
                existing.stream()
                        .filter(s -> s.getId() != null)
                        .collect(Collectors.toMap(SkuPo::getId, Function.identity()));
        Set<Long> keepIds = new HashSet<>();
        List<SkuPo> toCreate = new ArrayList<>();
        List<SkuPo> toUpdate = new ArrayList<>();
        List<StockAdjustDto> compensations = new ArrayList<>();
        for (SkuItemDto it : items) {
            SkuPo sku = existingById.get(it.getId());
            if (sku == null) {
                sku = new SkuPo();
                sku.setSpuId(spuId);
                sku.setMerchantId(merchantId);
                toCreate.add(sku);
            } else {
                keepIds.add(it.getId());
                toUpdate.add(sku);
            }
            sku.setSkuName(it.getSkuName() == null || it.getSkuName().isBlank() ? "-" : it.getSkuName());
            sku.setPrice(it.getPrice() != null ? it.getPrice() : BigDecimal.ZERO);
            if (sku.getId() != null && it.getStock() != null && !it.getStock().equals(sku.getStock())) {
                long currentStock = sku.getStock() == null ? 0L : sku.getStock().longValue();
                long delta = it.getStock().longValue() - currentStock;
                try {
                    adjustStock(sku.getId(), merchantId, delta, "商品规格库存调整");
                    compensations.add(new StockAdjustDto(sku.getId(), merchantId, -delta, "商品规格库存调整回滚"));
                    sku.setStock(it.getStock());
                } catch (RuntimeException e) {
                    rollbackStockAdjustments(compensations);
                    throw e;
                }
            }
            if (sku.getId() == null) {
                sku.setStock(it.getStock() != null ? it.getStock() : 0);
            }
        }
        List<Long> toDelete =
                existing.stream()
                        .map(SkuPo::getId)
                        .filter(id -> id != null && !keepIds.contains(id))
                        .toList();
        return new SkuSyncPlan(
                List.copyOf(toCreate), List.copyOf(toUpdate), toDelete, List.copyOf(compensations));
    }

    /**
     * 在事务内执行 SKU 批量新增、更新和删除。
     *
     * @param plan 已准备的 SKU 新增、更新、删除和库存补偿计划
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
     * @Description: 封装一次 SPU 编辑中待新增、更新、删除的 SKU 及库存补偿指令。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     */
    private record SkuSyncPlan(
            List<SkuPo> toCreate,
            List<SkuPo> toUpdate,
            List<Long> toDelete,
            List<StockAdjustDto> compensations) {

        /**
         * 返回不包含 SKU 差异和库存补偿的空计划。
         *
         * @return 本次 SPU 编辑需要的 SKU 变更与库存补偿计划
         */
        static SkuSyncPlan empty() {
            return new SkuSyncPlan(List.of(), List.of(), List.of(), List.of());
        }

        /**
         * 返回本次计划中待新增的 SKU 列表。
         *
         * @return 匹配的 SKU 持久化对象列表；无数据时返回空列表
         */
        List<SkuPo> createdSkus() {
            return toCreate;
        }
    }

    /**
     * 调用库存服务调整库存；远程无响应或返回失败时抛出业务异常。
     *
     * @param skuId SKU 主键
     * @param merchantId 当前商家 ID，用于商品归属校验
     * @param delta 本次库存调整增量
     * @param reason 驳回或库存调整原因
     */
    private void adjustStock(Long skuId, Long merchantId, long delta, String reason) {
        Result<Void> result =
                goodsRemoteGateway.adjustStock(new StockAdjustDto(skuId, merchantId, delta, reason));
        if (result == null || !CommonCode.SUCCESS.equals(result.getCode())) {
            throw new BizException(SkuCodeEnum.STOCK_ADJUST_FAILED);
        }
    }

    /**
     * 按补偿列表逐项反向调整库存；单项失败不阻断其余补偿。
     *
     * @param adjustments 失败时需要反向提交的库存调整列表
     */
    private void rollbackStockAdjustments(List<StockAdjustDto> adjustments) {
        for (StockAdjustDto adjustment : adjustments) {
            try {
                goodsRemoteGateway.adjustStock(adjustment);
            } catch (Exception e) {
                log.error(
                        "回滚库存调整失败，skuId={}，quantity={}", adjustment.getSkuId(), adjustment.getQuantity(), e);
            }
        }
    }

    /**
     * 尽力调用库存服务初始化 SKU 库存，失败仅记录日志，不阻断商品主链路。
     *
     * @param sku 待初始化或刷新的 SKU 记录
     * @param merchantId 当前商家 ID，用于商品归属校验
     */
    private void initStockBestEffort(SkuPo sku, Long merchantId) {
        try {
            Result<Void> r =
                    goodsRemoteGateway.initStock(new StockInitDto(sku.getId(), merchantId, sku.getStock()));
            if (r == null || !CommonCode.SUCCESS.equals(r.getCode())) {
                log.warn("初始化库存失败，skuId={}，msg={}", sku.getId(), r == null ? "无响应" : r.getMessage());
            }
        } catch (Exception e) {
            log.warn("初始化库存异常，skuId={}", sku.getId(), e);
        }
    }

    /**
     * 删除商家自有 SPU，并尽力清理 Elasticsearch 文档。
     *
     * @param id SPU 主键
     * @param merchantId 当前商家 ID，用于商品归属校验
     * @return 操作实际影响记录时为 true，否则为 false
     */
    @Override
    public boolean delete(Long id, Long merchantId) {
        SpuPo existing = spuManager.getById(id);
        if (existing == null) {
            throw new BizException(SpuCodeEnum.SPU_NOT_FOUND);
        }
        checkOwner(existing, merchantId);
        boolean ok = spuManager.removeById(id);
        if (ok) {
            // 搜索引擎是只读镜像，删除失败不阻断主链路删除（残留文档由索引重建清理）
            try {
                spuSearchService.deleteById(id);
            } catch (Exception e) {
                log.error("从 ES 删除 spu 失败，spuId={}", id, e);
            }
        }
        log.info("spu 已删除，spuId={}", id);
        return ok;
    }

    /**
     * 校验商家归属、店铺状态和 SPU 状态后提交审核，并同步搜索镜像。
     *
     * @param id SPU 主键
     * @param merchantId 当前商家 ID，用于商品归属校验
     * @return 操作实际影响记录时为 true，否则为 false
     */
    @Override
    public boolean onShelf(Long id, Long merchantId) {
        SpuPo existing = spuManager.getById(id);
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
     * 校验商家归属和当前状态后将 SPU 下架，并同步搜索镜像。
     *
     * @param id SPU 主键
     * @param merchantId 当前商家 ID，用于商品归属校验
     * @return 操作实际影响记录时为 true，否则为 false
     */
    @Override
    public boolean offShelf(Long id, Long merchantId) {
        SpuPo existing = spuManager.getById(id);
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
     * 按主键倒序分页查询待审核 SPU。
     *
     * @param dto SPU 游标分页或状态过滤参数
     * @return 按主键游标分页的结果；记录项为匹配的 SPU 持久化对象
     */
    public CursorPageVo<SpuPo> pagePending(SpuPageDto dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        int limit = dto.getLimit();
        List<SpuPo> records =
                spuManager.list(
                        Wrappers.lambdaQuery(SpuPo.class)
                                .eq(SpuPo::getStatus, SpuStatusEnum.PENDING_AUDIT.statusValue())
                                .lt(lastId != null, SpuPo::getId, lastId)
                                .orderByDesc(SpuPo::getId)
                                .last("LIMIT " + (limit + 1)));
        return CursorPageVo.of(records, limit, SpuPo::getId);
    }

    /**
     * 分页查询待审核 SPU 并转换为视图对象。
     *
     * @param dto SPU 游标分页或状态过滤参数
     * @return 按主键游标分页的结果；记录项为转换后的 SPU 视图对象
     */
    @Override
    public CursorPageVo<SpuVo> pendingVo(SpuPageDto dto) {
        return pagePending(dto).map(SpuVo::from);
    }

    /**
     * 按主键读取 SPU 并转换为平台审核视图。
     *
     * @param spuId SPU 主键
     * @return 转换后的 SPU 视图对象
     */
    @Override
    public SpuVo detailVo(Long spuId) {
        return SpuVo.from(getById(spuId));
    }

    /**
     * 事务内将待审 SPU 置为在售、写入发布时间并保存审核记录；提交后同步 ES。
     *
     * @param spuId SPU 主键
     * @param auditorId 平台审核人用户 ID
     * @return 操作实际影响记录时为 true，否则为 false
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approve(Long spuId, Long auditorId) {
        SpuPo existing = spuManager.getById(spuId);
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
     * 事务内将待审 SPU 置为驳回并保存审核记录；提交后同步 ES。
     *
     * @param spuId SPU 主键
     * @param auditorId 平台审核人用户 ID
     * @param reason 驳回或库存调整原因
     * @return 操作实际影响记录时为 true，否则为 false
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reject(Long spuId, Long auditorId, String reason) {
        SpuPo existing = spuManager.getById(spuId);
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
     * 按 SPU 主键查询审核记录，按审核时间倒序返回。
     *
     * @param spuId SPU 主键
     * @return 商品审核记录列表；无数据时返回空列表
     */
    public List<SpuAuditRecordPo> listAuditRecords(Long spuId) {
        return spuAuditRecordManager.list(
                Wrappers.lambdaQuery(SpuAuditRecordPo.class)
                        .eq(SpuAuditRecordPo::getSpuId, spuId)
                        .orderByDesc(SpuAuditRecordPo::getAuditAt));
    }

    /**
     * 按 SPU 主键查询审核记录并转换为视图对象。
     *
     * @param spuId SPU 主键
     * @return 商品审核记录视图对象列表；无数据时返回空列表
     */
    @Override
    public List<SpuAuditRecordVo> listAuditRecordsVo(Long spuId) {
        return listAuditRecords(spuId).stream().map(SpuAuditRecordVo::from).toList();
    }

    /**
     * 保存商品审核决策、审核人、时间和原因。
     *
     * @param spuId SPU 主键
     * @param auditorId 平台审核人用户 ID
     * @param decision 审核决策枚举
     * @param reason 驳回或库存调整原因
     */
    private void saveAuditRecord(
            Long spuId, Long auditorId, AuditDecisionEnum decision, String reason) {
        SpuAuditRecordPo record = new SpuAuditRecordPo();
        record.setSpuId(spuId);
        record.setAuditorId(auditorId);
        record.setDecision(decision.getCode());
        record.setReason(reason);
        record.setAuditAt(LocalDateTime.now());
        spuAuditRecordManager.save(record);
    }

    /**
     * 在事务同步生效时注册提交后回调，否则立即执行回调。
     *
     * @param action 事务提交后执行的回调动作
     */
    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    /**
                     * 在事务同步生效时注册提交后回调，否则立即执行回调。
                     */
                    public void afterCommit() {
                        action.run();
                    }
                });
    }

    /**
     * 读取指定 SPU 并同步搜索文档；记录已删除时记录告警并忽略。
     *
     * @param spuId SPU 主键
     */
    public void refreshEsDocument(Long spuId) {
        SpuPo spu = spuManager.getById(spuId);
        if (spu == null) {
            // SPU 已删除时 ES 文档由 delete 流程清理，SKU 侧刷新直接忽略
            log.warn("刷新 ES 文档时未找到 spu，spuId={}", spuId);
            return;
        }
        syncToEs(spu);
    }

    /**
     * 扫描全部在售 SPU，逐个重建 Elasticsearch 文档。
     */
    public void rebuildAllEsDocuments() {
        List<SpuPo> onSaleList =
                spuManager.list(
                        Wrappers.lambdaQuery(SpuPo.class)
                                .eq(SpuPo::getStatus, SpuStatusEnum.ON_SALE.statusValue()));
        log.info("开始重建 ES 文档，count={}", onSaleList.size());
        onSaleList.forEach(this::syncToEs);
        log.info("重建 ES 文档完成，count={}", onSaleList.size());
    }

    /**
     * 将 SPU 主数据、在售状态和 SKU 价格区间同步到 Elasticsearch；失败仅记录日志。
     *
     * @param po 待转换的持久化对象
     */
    private void syncToEs(SpuPo po) {
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
            LongSummaryStatistics stats =
                    skuManager
                            .list(Wrappers.lambdaQuery(SkuPo.class).eq(SkuPo::getSpuId, po.getId()))
                            .stream()
                            .map(SkuPo::getPrice)
                            .filter(Objects::nonNull)
                            .mapToLong(p -> p.movePointRight(2).longValueExact())
                            .summaryStatistics();
            doc.setMinPrice(stats.getCount() == 0 ? null : stats.getMin());
            doc.setMaxPrice(stats.getCount() == 0 ? null : stats.getMax());
            spuSearchService.sync(doc);
        } catch (Exception e) {
            log.error("同步 spu 到 ES 失败，spuId={}", po.getId(), e);
        }
    }

    /**
     * 批量读取 SPU 快照，仅返回已存在记录并以主键建映射。
     *
     * @param spuIds 待查询的 SPU 主键列表，空列表返回空映射
     * @return 以 SPU 主键为键的快照映射；不存在的记录不包含
     */
    @Override
    public Map<Long, SpuSnapshotDto> listSnapshots(List<Long> spuIds) {
        List<Long> ids =
                spuIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return spuManager.list(Wrappers.lambdaQuery(SpuPo.class).in(SpuPo::getId, ids)).stream()
                .collect(
                        Collectors.toMap(
                                SpuPo::getId,
                                spu -> new SpuSnapshotDto(spu.getId(), spu.getTitle(), spu.getMainImageUrl())));
    }

    /**
     * 读取单个 SPU 快照；记录不存在时返回仅含主键的快照对象。
     *
     * @param id SPU 主键
     * @return SPU 标题和主图快照
     */
    @Override
    public SpuSnapshotDto getSnapshot(Long id) {
        SpuPo spu = spuManager.getById(id);
        if (spu == null) {
            return new SpuSnapshotDto(id, null, null);
        }
        return new SpuSnapshotDto(spu.getId(), spu.getTitle(), spu.getMainImageUrl());
    }
}
