package com.yirancrazy.minimall.goods.service.impl;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.goods.constant.SkuCodeEnum;
import com.yirancrazy.minimall.goods.constant.SpuCodeEnum;
import com.yirancrazy.minimall.goods.dto.SkuCreateDTO;
import com.yirancrazy.minimall.goods.dto.SkuPageDTO;
import com.yirancrazy.minimall.goods.dto.SkuUpdateDTO;
import com.yirancrazy.minimall.goods.entity.SkuPO;
import com.yirancrazy.minimall.goods.entity.SpuPO;
import com.yirancrazy.minimall.goods.manager.SkuManager;
import com.yirancrazy.minimall.goods.manager.SpuManager;
import com.yirancrazy.minimall.goods.service.SkuService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品领域服务实现，实现Sku相关业务逻辑
 * @Version: 1.2
 * @DateTime: 2026/08/02
 */
@Service
public class SkuServiceImpl implements SkuService {

    private final SkuManager skuManager;
    private final SpuManager spuManager;

    public SkuServiceImpl(SkuManager skuManager, SpuManager spuManager) {
        this.skuManager = skuManager;
        this.spuManager = spuManager;
    }

    /**
     * 按主键查询 SKU，不存在时抛出 SKU_NOT_FOUND 业务异常。
     *
     * @param id SKU 主键 ID
     * @return 已存在的 SKU 实体
     */
    @Override
    public SkuPO getById(Long id) {
        SkuPO s = skuManager.getById(id);
        if (s == null) {
            throw new BizException(SkuCodeEnum.SKU_NOT_FOUND);
        }
        return s;
    }

    /**
     * 按主键查询 SKU 并校验归属商家，供商家端使用；非本人商品按"不存在"返回，避免越权信息泄露。
     * @param id SKU 主键 ID
     * @param merchantId 商家ID，来自可信Header
     * @return 属于该商家的 SKU 实体
     */
    @Override
    public SkuPO getById(Long id, Long merchantId) {
        SkuPO s = getById(id);
        checkOwner(s, merchantId);
        return s;
    }

    /**
     * 归属校验：SKU 归属商家与请求商家不一致时抛出 SKU_NOT_FOUND，防越权的同时不暴露资源存在性。
     * @param existing 已查出的 SKU 实体
     * @param merchantId 请求方商家ID
     */
    private void checkOwner(SkuPO existing, Long merchantId) {
        if (existing.getMerchantId() != null && !existing.getMerchantId().equals(merchantId)) {
            throw new BizException(SkuCodeEnum.SKU_NOT_FOUND);
        }
    }

    /**
     * 创建 SKU 记录，归属商家ID来自可信Header；父 SPU 不存在或非本人商品时按"不存在"处理，
     * 防止商家在他人 SPU 下挂载 SKU。价格、库存等字段做缺省值兜底后落库。
     *
     * @param merchantId 商家ID，来自可信Header
     * @param dto 待保存的 SKU 信息
     * @return 新建 SKU 的主键 ID
     */
    @Override
    public Long create(Long merchantId, SkuCreateDTO dto) {
        SpuPO spu = spuManager.getById(dto.getSpuId());
        if (spu == null || spu.getMerchantId() == null || !spu.getMerchantId().equals(merchantId)) {
            throw new BizException(SpuCodeEnum.SPU_NOT_FOUND);
        }
        SkuPO sku = new SkuPO();
        sku.setSpuId(dto.getSpuId());
        sku.setMerchantId(merchantId);
        sku.setSkuName(dto.getSkuName());
        sku.setPrice(dto.getPrice() != null ? dto.getPrice() : BigDecimal.ZERO);
        sku.setStock(dto.getStock() != null ? dto.getStock() : 0);
        skuManager.save(sku);
        return sku.getId();
    }

    /**
     * 游标分页查询 SKU，按归属商家过滤，skuName 非空时按 like 模糊匹配，按 ID 倒序。
     *
     * @param dto 游标分页查询入参
     * @return SKU 游标分页结果
     */
    @Override
    public CursorPageVO<SkuPO> page(SkuPageDTO dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        int limit = dto.getLimit();
        List<SkuPO> records = skuManager.list(Wrappers.lambdaQuery(SkuPO.class)
            .eq(dto.getMerchantId() != null, SkuPO::getMerchantId, dto.getMerchantId())
            .lt(lastId != null, SkuPO::getId, lastId)
            .like(dto.getSkuName() != null && !dto.getSkuName().isBlank(),
                SkuPO::getSkuName, dto.getSkuName())
            .orderByDesc(SkuPO::getId)
            .last("LIMIT " + (limit + 1)));
        return CursorPageVO.of(records, limit, SkuPO::getId);
    }

    /**
     * 按主键更新 SKU，不存在或非本人商品时抛出 SKU_NOT_FOUND。
     *
     * @param id SKU 主键 ID
     * @param merchantId 商家ID，来自可信Header
     * @param dto 待更新的 SKU 信息
     * @return 更新是否成功
     */
    @Override
    public boolean update(Long id, Long merchantId, SkuUpdateDTO dto) {
        SkuPO existing = skuManager.getById(id);
        if (existing == null) {
            throw new BizException(SkuCodeEnum.SKU_NOT_FOUND);
        }
        checkOwner(existing, merchantId);
        existing.setSkuName(dto.getSkuName());
        if (dto.getPrice() != null) {
            existing.setPrice(dto.getPrice());
        }
        if (dto.getStock() != null) {
            existing.setStock(dto.getStock());
        }
        return skuManager.updateById(existing);
    }

    /**
     * 按主键删除 SKU，不存在或非本人商品时抛出 SKU_NOT_FOUND。
     *
     * @param id SKU 主键 ID
     * @param merchantId 商家ID，来自可信Header
     * @return 删除是否成功
     */
    @Override
    public boolean delete(Long id, Long merchantId) {
        SkuPO existing = skuManager.getById(id);
        if (existing == null) {
            throw new BizException(SkuCodeEnum.SKU_NOT_FOUND);
        }
        checkOwner(existing, merchantId);
        return skuManager.removeById(id);
    }
}