package com.yirancrazy.minimall.goods.service.impl;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.goods.constant.SpuCodeEnum;
import com.yirancrazy.minimall.goods.constant.SpuStatusEnum;
import com.yirancrazy.minimall.goods.dto.SpuCreateDTO;
import com.yirancrazy.minimall.goods.dto.SpuPageDTO;
import com.yirancrazy.minimall.goods.dto.SpuUpdateDTO;
import com.yirancrazy.minimall.goods.entity.SpuPO;
import com.yirancrazy.minimall.goods.manager.SpuManager;
import com.yirancrazy.minimall.goods.service.SpuService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品领域服务实现，实现Spu相关业务逻辑，含状态流转校验
 * @Version: 1.0
 * @DateTime: 2026/08/02
 */
@Slf4j
@Service
public class SpuServiceImpl implements SpuService {

    private final SpuManager spuManager;

    public SpuServiceImpl(SpuManager spuManager) {
        this.spuManager = spuManager;
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
     * 分页查询 SPU，按 merchantId/status 等值、title like 过滤。
     * @param dto 分页查询入参
     * @return SPU 分页结果
     */
    @Override
    public IPage<SpuPO> page(SpuPageDTO dto) {
        Page<SpuPO> page = new Page<>(dto.getPageNo(), dto.getPageSize());
        return spuManager.page(page, Wrappers.lambdaQuery(SpuPO.class)
            .eq(dto.getMerchantId() != null, SpuPO::getMerchantId, dto.getMerchantId())
            .eq(dto.getStatus() != null, SpuPO::getStatus, dto.getStatus())
            .like(dto.getTitle() != null && !dto.getTitle().isBlank(),
                SpuPO::getTitle, dto.getTitle()));
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
        return spuManager.updateById(existing);
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
     * 上架 SPU，仅草稿/下架/驳回状态可上架，否则抛出 SPU_STATUS_INVALID。
     * @param id SPU 主键 ID
     * @return 上架是否成功
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
        existing.setStatus(SpuStatusEnum.ON_SALE.statusValue());
        existing.setPublishAt(LocalDateTime.now());
        boolean ok = spuManager.updateById(existing);
        log.info("spu on shelf, spuId={}, ok={}", id, ok);
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
        log.info("spu off shelf, spuId={}, ok={}", id, ok);
        return ok;
    }
}
