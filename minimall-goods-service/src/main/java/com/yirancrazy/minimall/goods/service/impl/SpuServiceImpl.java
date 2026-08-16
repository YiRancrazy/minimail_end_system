package com.yirancrazy.minimall.goods.service.impl;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.goods.constant.AuditDecisionEnum;
import com.yirancrazy.minimall.goods.constant.SpuCodeEnum;
import com.yirancrazy.minimall.goods.constant.SpuStatusEnum;
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

    public SpuServiceImpl(SpuManager spuManager,
                          SpuAuditRecordManager spuAuditRecordManager,
                          SpuSearchService spuSearchService,
                          SkuManager skuManager) {
        this.spuManager = spuManager;
        this.spuAuditRecordManager = spuAuditRecordManager;
        this.spuSearchService = spuSearchService;
        this.skuManager = skuManager;
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
     * 创建 SPU，生成业务编号并初始化为草稿状态后落库。
     * @param merchantId 商家ID，来自可信Header
     * @param dto 待保存的 SPU 信息
     * @return 新建 SPU 的主键 ID
     */
    @Override
    public Long create(Long merchantId, SpuCreateDTO dto) {
        SpuPO po = new SpuPO();
        po.setSpuNo(UUID.randomUUID().toString().replace("-", ""));
        po.setMerchantId(merchantId);
        po.setCategoryId(dto.getCategoryId());
        po.setTitle(dto.getTitle());
        po.setSubtitle(dto.getSubtitle());
        po.setMainImageUrl(dto.getMainImageUrl());
        po.setStatus(SpuStatusEnum.DRAFT.statusValue());
        spuManager.save(po);
        log.info("spu created, spuId={}, merchantId={}", po.getId(), merchantId);
        return po.getId();
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
            SpuVO vo = SpuVO.from(po);
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
        boolean ok = spuManager.updateById(existing);
        if (ok) {
            syncToEs(existing);
        }
        return ok;
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
            doc.setCreateTime(po.getCreateTime());
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
}
