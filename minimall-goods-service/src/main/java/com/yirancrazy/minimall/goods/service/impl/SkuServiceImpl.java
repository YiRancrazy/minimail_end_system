package com.yirancrazy.minimall.goods.service.impl;

import com.yirancrazy.minimall.goods.constant.SkuCodeEnum;

import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.goods.dto.SkuCreateDTO;
import com.yirancrazy.minimall.goods.entity.SkuPO;
import com.yirancrazy.minimall.goods.manager.SkuManager;
import com.yirancrazy.minimall.goods.service.SkuService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品领域服务实现，负责 SKU 业务规则校验、异常转换与对 SkuManager 的调用编排。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Service
public class SkuServiceImpl implements SkuService {

    private final SkuManager skuManager;

    public SkuServiceImpl(SkuManager skuManager) {
        this.skuManager = skuManager;
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
     * 创建 SKU 记录，并对价格、库存等字段做缺省值兜底后落库。
     *
     * @param dto 待保存的 SKU 信息
     * @return 新建 SKU 的主键 ID
     */
    @Override
    public Long create(SkuCreateDTO dto) {
        SkuPO sku = new SkuPO();
        sku.setSpuId(dto.getSpuId());
        sku.setSkuName(dto.getSkuName());
        sku.setPrice(dto.getPrice() != null ? dto.getPrice() : BigDecimal.ZERO);
        sku.setStock(dto.getStock() != null ? dto.getStock() : 0);
        skuManager.save(sku);
        return sku.getId();
    }
}