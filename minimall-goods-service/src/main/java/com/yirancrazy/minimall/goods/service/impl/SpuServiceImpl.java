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
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDTO;
import com.yirancrazy.minimall.api.dto.merchant.ShopSnapshotDTO;
import com.yirancrazy.minimall.api.feign.MerchantFeignClient;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.goods.constant.AuditDecisionEnum;
import com.yirancrazy.minimall.goods.constant.SpuCodeEnum;
import com.yirancrazy.minimall.goods.constant.SpuStatusEnum;
import com.yirancrazy.minimall.goods.dto.SkuItemDTO;
import com.yirancrazy.minimall.goods.dto.SpuCreateDTO;
import com.yirancrazy.minimall.goods.dto.SpuPageDTO;
import com.yirancrazy.minimall.goods.dto.SpuUpdateDTO;
import com.yirancrazy.minimall.goods.entity.SkuPO;
import com.yirancrazy.minimall.goods.entity.SpuAuditRecordPO;
import com.yirancrazy.minimall.goods.entity.SpuPO;
import com.yirancrazy.minimall.goods.manager.SkuManager;
import com.yirancrazy.minimall.goods.manager.SpuAuditRecordManager;
import com.yirancrazy.minimall.goods.manager.SpuManager;
import com.yirancrazy.minimall.goods.search.SpuDocument;
import com.yirancrazy.minimall.goods.search.SpuSearchService;
import com.yirancrazy.minimall.goods.service.SpuService;
import com.yirancrazy.minimall.goods.vo.SkuVO;
import com.yirancrazy.minimall.goods.vo.SpuVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品领域服务实现，实现Spu相关业务逻辑，含状态流转与审核闭环
 * @Version: 1.2
 * @DateTime: 2026/08/02
 */
@Slf4j
@Service
public class SpuServiceImpl implements SpuService {

    private final SpuManager spuManager;
    private final SpuAuditRecordManager spuAuditRecordManager;
    private final SpuSearchService spuSearchService;
    private final SkuManager skuManager;
    private final MerchantFeignClient merchantFeignClient;
    private final MinioUtil minioUtil;

    /** 店铺营业中状态 alias，跨服务通过 MerchantFeignClient 快照透传 */
    private static final String SHOP_STATUS_ACTIVE = "ACTIVE";

    public SpuServiceImpl(SpuManager spuManager,
                          SpuAuditRecordManager spuAuditRecordManager,
                          SpuSearchService spuSearchService,
                          SkuManager skuManager,
                          MerchantFeignClient merchantFeignClient,
                          MinioUtil minioUtil) {
        this.spuManager = spuManager;
        this.spuAuditRecordManager = spuAuditRecordManager;
        this.spuSearchService = spuSearchService;
        this.skuManager = skuManager;
        this.merchantFeignClient = merchantFeignClient;
        this.minioUtil = minioUtil;
    }

    /**
     * 发布前置校验：店铺必须存在、归属当前商家且状态为营业中，否则拒绝创建/送审。
     * Feign 降级（merchant-service 不可用）时快照返回哨兵值，此处 fail-closed，宁可拒绝不可放行。
     * @param shopId 目标店铺 ID
     * @param merchantId 请求方商家 ID
     */
    private void requireActiveShop(Long shopId, Long merchantId) {
        if (shopId == null) {
            throw new BizException(SpuCodeEnum.SPU_SHOP_INVALID);
        }
        Result<ShopSnapshotDTO> result = merchantFeignClient.shopSnapshot(shopId);
        ShopSnapshotDTO snap = result == null ? null : result.getData();
        if (snap == null || snap.getShopId() == null || snap.getShopId() <= 0
            || !merchantId.equals(snap.getMerchantId())
            || !SHOP_STATUS_ACTIVE.equals(snap.getStatus())) {
            throw new BizException(SpuCodeEnum.SPU_SHOP_INVALID);
        }
    }

    /**
     * 按主键查询 SPU，不存在时抛出 SPU_NOT_FOUND 业务异常。
     * @param id SPU 主键 ID
     * @return 已存在的 SPU 实体
     */
    @Override
    public SpuPO getById(Long id) {
        SpuPO s = spuManager.getById(id);
        if (s == null) {
            throw new BizException(SpuCodeEnum.SPU_NOT_FOUND);
        }
        return s;
    }

    /**
     * 按主键查询 SPU 并校验归属商家，供商家端使用；非本人商品按"不存在"返回，避免越权信息泄露。
     * @param id SPU 主键 ID
     * @param merchantId 商家ID，来自可信Header
     * @return 属于该商家的 SPU 实体
     */
    @Override
    public SpuPO getById(Long id, Long merchantId) {
        SpuPO s = getById(id);
        checkOwner(s, merchantId);
        return s;
    }

    /**
     * 归属校验：实体归属商家与请求商家不一致时抛出 SPU_NOT_FOUND，防越权的同时不暴露资源存在性。
     * @param existing 已查出的 SPU 实体
     * @param merchantId 请求方商家ID
     */
    private void checkOwner(SpuPO existing, Long merchantId) {
        if (existing.getMerchantId() != null && !existing.getMerchantId().equals(merchantId)) {
            throw new BizException(SpuCodeEnum.SPU_NOT_FOUND);
        }
    }

    /**
     * 创建 SPU，生成业务编号并初始化为草稿状态后落库；携带的 SKU 清单一并批量保存，
     * 否则列表页商品将无图片与库存展示。
     * @param merchantId 商家ID，来自可信Header
     * @param dto 待保存的 SPU 信息
     * @return 新建 SPU 的主键 ID
     */
    @Override
    public Long create(Long merchantId, SpuCreateDTO dto) {
        // 发布前提：店铺存在、归属当前商家且营业中，无店铺商家无法发布商品
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
        spuManager.save(po);
        saveSkus(merchantId, po.getId(), dto.getSkus());
        log.info("spu created, spuId={}, merchantId={}", po.getId(), merchantId);
        return po.getId();
    }

    /**
     * 批量保存创建时携带的 SKU，空清单直接忽略；价格/库存缺省值兜底，规格描述为空时占位 "-"。
     * @param merchantId 商家ID，来自可信Header
     * @param spuId 新建 SPU 的主键
     * @param items 创建入参携带的 SKU 清单，允许为空
     */
    private void saveSkus(Long merchantId, Long spuId, List<SkuItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return;
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
    }

    /**
     * 查询 SPU 详情并装配其 SKU 列表，供商家端编辑页回显。
     * @param id SPU 主键 ID
     * @param merchantId 商家ID，来自可信Header
     * @return SPU 视图，含 skus
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
     * 主图 objectKey 转为可访问 URL，商家端展示用，空值返回 null。
     * @param vo 待处理的 SPU 视图
     * @return 处理后的 SPU 视图
     */
    private SpuVO resolveMainImage(SpuVO vo) {
        vo.setMainImageUrl(minioUtil.resolvePublicUrl(vo.getMainImageUrl()));
        return vo;
    }

    /**
     * 游标分页查询 SPU，按 merchantId/status 等值、title like 过滤，按 ID 倒序，并批量装配 SKU。
     * @param dto 游标分页查询入参
     * @return SPU 游标分页结果，含 skus
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
     * 批量查询多个 SPU 下的 SKU，一次 IN 查询替代逐 SPU 的 N 次请求。
     * @param spuIds SPU 主键集合，允许为空
     * @return spuId -> SKU 列表
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
     * 按主键更新 SPU，字段为空表示不更新对应列，不存在或非本人商品时抛出 SPU_NOT_FOUND。
     * 已发布（ON_SALE/OFF_SHELF）或已驳回（REJECTED）商品编辑后必须重新送审：因
     * SpuUpdateDTO 全部可编辑字段（类目/标题/副标题/主图）均影响前台展示、无法精确圈定
     * 审核无关字段，故采用稳妥规则——非 DRAFT 状态 update 一律置回 PENDING_AUDIT；
     * 原在售商品经 syncToEs 覆盖 ES 镜像 saleStatus，即刻退出前台搜索结果，待平台再次
     * 审核通过后才恢复上架。
     * @param id SPU 主键 ID
     * @param merchantId 商家ID，来自可信Header
     * @param dto 待更新的 SPU 信息
     * @return 更新是否成功
     */
    @Override
    public boolean update(Long id, Long merchantId, SpuUpdateDTO dto) {
        SpuPO existing = spuManager.getById(id);
        if (existing == null) {
            throw new BizException(SpuCodeEnum.SPU_NOT_FOUND);
        }
        checkOwner(existing, merchantId);
        // 更换店铺时同样要求新店铺存在、归属当前商家且营业中
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
        boolean ok = spuManager.updateById(existing);
        if (ok) {
            // skus 非空时先同步 SKU 再刷 ES，保证价格区间聚合到最新 SKU
            if (dto.getSkus() != null) {
                syncSkus(merchantId, id, dto.getSkus());
            }
            syncToEs(existing);
        }
        return ok;
    }

    /**
     * 按 id 增改、未出现者删除的 SKU 全量同步：已存在 SKU 更新价格/库存/规格，
     * 无 id 的 SKU 新建，原 SKU 未出现在清单中则删除，防止编辑页改库存不落库。
     * @param merchantId 商家ID，来自可信Header
     * @param spuId SPU 主键 ID
     * @param items 编辑后 SKU 全量清单
     */
    private void syncSkus(Long merchantId, Long spuId, List<SkuItemDTO> items) {
        List<SkuPO> existing = skuManager.list(
            Wrappers.lambdaQuery(SkuPO.class).eq(SkuPO::getSpuId, spuId));
        Map<Long, SkuPO> existingById = existing.stream()
            .filter(s -> s.getId() != null)
            .collect(Collectors.toMap(SkuPO::getId, Function.identity()));
        Set<Long> keepIds = new HashSet<>();
        List<SkuPO> toCreate = new ArrayList<>();
        List<SkuPO> toUpdate = new ArrayList<>();
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
            sku.setStock(it.getStock() != null ? it.getStock() : 0);
        }
        if (!toCreate.isEmpty()) {
            skuManager.saveBatch(toCreate);
        }
        if (!toUpdate.isEmpty()) {
            skuManager.updateBatchById(toUpdate);
        }
        List<Long> toDelete = existing.stream().map(SkuPO::getId)
            .filter(id -> id != null && !keepIds.contains(id)).collect(Collectors.toList());
        if (!toDelete.isEmpty()) {
            skuManager.removeByIds(toDelete);
        }
    }

    /**
     * 按主键删除 SPU，不存在或非本人商品时抛出 SPU_NOT_FOUND；删除成功后同步清理 ES 镜像文档。
     * @param id SPU 主键 ID
     * @param merchantId 商家ID，来自可信Header
     * @return 删除是否成功
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
                log.error("delete spu from ES failed, spuId={}", id, e);
            }
        }
        log.info("spu deleted, spuId={}", id);
        return ok;
    }

    /**
     * 商家提交上架审核，仅草稿/下架/驳回状态可提交，提交后进入待审核，由平台审核通过后才会上架。
     * @param id SPU 主键 ID
     * @param merchantId 商家ID，来自可信Header
     * @return 提交是否成功
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
        log.info("spu submit audit, spuId={}, ok={}", id, ok);
        return ok;
    }

    /**
     * 下架 SPU，仅在售状态可下架，否则抛出 SPU_STATUS_INVALID。
     * @param id SPU 主键 ID
     * @param merchantId 商家ID，来自可信Header
     * @return 下架是否成功
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
        log.info("spu off shelf, spuId={}, ok={}", id, ok);
        return ok;
    }

    /**
     * 平台游标分页查询待审核 SPU，按 ID 倒序。
     * @param dto 游标分页查询入参
     * @return 待审核 SPU 游标分页结果
     */
    @Override
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
     * 平台审核通过，仅待审核状态可通过，通过后置为在售并记录审核日志。
     * @param spuId SPU 主键 ID
     * @param auditorId 审核员账号ID
     * @return 审核是否成功
     */
    @Override
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
            syncToEs(existing);
        }
        saveAuditRecord(spuId, auditorId, AuditDecisionEnum.APPROVE, null);
        log.info("spu approved, spuId={}, auditorId={}", spuId, auditorId);
        return ok;
    }

    /**
     * 平台审核驳回，仅待审核状态可驳回，驳回后置为驳回并记录审核日志。
     * @param spuId SPU 主键 ID
     * @param auditorId 审核员账号ID
     * @param reason 驳回原因
     * @return 审核是否成功
     */
    @Override
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
            syncToEs(existing);
        }
        saveAuditRecord(spuId, auditorId, AuditDecisionEnum.REJECT, reason);
        log.info("spu rejected, spuId={}, auditorId={}, reason={}", spuId, auditorId, reason);
        return ok;
    }

    /**
     * 查询指定 SPU 的审核记录列表，按审核时间倒序。
     * @param spuId SPU 主键 ID
     * @return 审核记录列表
     */
    @Override
    public List<SpuAuditRecordPO> listAuditRecords(Long spuId) {
        return spuAuditRecordManager.list(
            Wrappers.lambdaQuery(SpuAuditRecordPO.class)
                .eq(SpuAuditRecordPO::getSpuId, spuId)
                .orderByDesc(SpuAuditRecordPO::getAuditAt));
    }

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
     * 重新同步 SPU 文档到 ES：SKU 新增/改价/删除后父 SPU 价格区间可能变化，需刷新镜像。
     * @param spuId SPU 主键 ID
     */
    @Override
    public void refreshEsDocument(Long spuId) {
        SpuPO spu = spuManager.getById(spuId);
        if (spu == null) {
            // SPU 已删除时 ES 文档由 delete 流程清理，SKU 侧刷新直接忽略
            log.warn("spu not found when refreshing ES document, spuId={}", spuId);
            return;
        }
        syncToEs(spu);
    }

    /**
     * 全量重灌 ES 索引：遍历全部在售 SPU 重新同步文档，用于索引重建后补齐数据。
     * 单条失败由 syncToEs 内部捕获并记录，不阻断其余 SPU 同步。
     */
    @Override
    public void rebuildAllEsDocuments() {
        List<SpuPO> onSaleList = spuManager.list(
            Wrappers.lambdaQuery(SpuPO.class)
                .eq(SpuPO::getStatus, SpuStatusEnum.ON_SALE.statusValue()));
        log.info("rebuild ES documents start, count={}", onSaleList.size());
        onSaleList.forEach(this::syncToEs);
        log.info("rebuild ES documents done, count={}", onSaleList.size());
    }

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
            log.error("sync spu to ES failed, spuId={}", po.getId(), e);
        }
    }

    /**
     * 批量查询 SPU 快照：一次 IN 查询替代逐 SPU 的 N 次请求；不存在的 SPU 不放入结果，由调用方兜底降级。
     * @param spuIds SPU 主键集合，允许为空
     * @return spuId -> SPU 快照，空入参返回空 Map
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
}
