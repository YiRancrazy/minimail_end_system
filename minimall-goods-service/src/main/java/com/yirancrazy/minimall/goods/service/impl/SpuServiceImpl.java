package com.yirancrazy.minimall.goods.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
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
import com.yirancrazy.minimall.goods.entity.SpuAuditRecordPO;
import com.yirancrazy.minimall.goods.entity.SpuPO;
import com.yirancrazy.minimall.goods.manager.SpuAuditRecordManager;
import com.yirancrazy.minimall.goods.manager.SpuManager;
import com.yirancrazy.minimall.goods.search.SpuDocument;
import com.yirancrazy.minimall.goods.search.SpuSearchService;
import com.yirancrazy.minimall.goods.service.SpuService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品领域服务实现，实现Spu相关业务逻辑，含状态流转与审核闭环
 * @Version: 1.1
 * @DateTime: 2026/08/02
 */
@Slf4j
@Service
public class SpuServiceImpl implements SpuService {

    private final SpuManager spuManager;
    private final SpuAuditRecordManager spuAuditRecordManager;
    private final SpuSearchService spuSearchService;

    public SpuServiceImpl(SpuManager spuManager,
                          SpuAuditRecordManager spuAuditRecordManager,
                          SpuSearchService spuSearchService) {
        this.spuManager = spuManager;
        this.spuAuditRecordManager = spuAuditRecordManager;
        this.spuSearchService = spuSearchService;
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
     * 游标分页查询 SPU，按 merchantId/status 等值、title like 过滤，按 ID 倒序。
     * @param dto 游标分页查询入参
     * @return SPU 游标分页结果
     */
    @Override
    public CursorPageVO<SpuPO> page(SpuPageDTO dto) {
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
        return CursorPageVO.of(records, limit, SpuPO::getId);
    }

    /**
     * 按主键更新 SPU，字段为空表示不更新对应列，不存在时抛出 SPU_NOT_FOUND。
     * @param id SPU 主键 ID
     * @param dto 待更新的 SPU 信息
     * @return 更新是否成功
     */
    @Override
    public boolean update(Long id, SpuUpdateDTO dto) {
        SpuPO existing = spuManager.getById(id);
        if (existing == null) {
            throw new BizException(SpuCodeEnum.SPU_NOT_FOUND);
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
        boolean ok = spuManager.updateById(existing);
        if (ok) {
            syncToEs(existing);
        }
        return ok;
    }

    /**
     * 按主键删除 SPU，不存在时抛出 SPU_NOT_FOUND。
     * @param id SPU 主键 ID
     * @return 删除是否成功
     */
    @Override
    public boolean delete(Long id) {
        SpuPO existing = spuManager.getById(id);
        if (existing == null) {
            throw new BizException(SpuCodeEnum.SPU_NOT_FOUND);
        }
        log.info("spu deleted, spuId={}", id);
        return spuManager.removeById(id);
    }

    /**
     * 商家提交上架审核，仅草稿/下架/驳回状态可提交，提交后进入待审核，由平台审核通过后才会上架。
     * @param id SPU 主键 ID
     * @return 提交是否成功
     */
    @Override
    public boolean onShelf(Long id) {
        SpuPO existing = spuManager.getById(id);
        if (existing == null) {
            throw new BizException(SpuCodeEnum.SPU_NOT_FOUND);
        }
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
     * @return 下架是否成功
     */
    @Override
    public boolean offShelf(Long id) {
        SpuPO existing = spuManager.getById(id);
        if (existing == null) {
            throw new BizException(SpuCodeEnum.SPU_NOT_FOUND);
        }
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

    private void syncToEs(SpuPO po) {
        SpuDocument doc = new SpuDocument();
        doc.setSpuId(po.getId());
        doc.setTitle(po.getTitle());
        doc.setCategoryId(po.getCategoryId());
        doc.setMerchantId(po.getMerchantId());
        doc.setSaleStatus(po.getStatus());
        doc.setMainImage(po.getMainImageUrl());
        doc.setCreateTime(po.getCreateTime());
        spuSearchService.sync(doc);
    }
}
