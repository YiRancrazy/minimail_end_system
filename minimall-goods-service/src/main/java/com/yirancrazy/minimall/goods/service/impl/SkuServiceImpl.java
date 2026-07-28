package com.yirancrazy.minimall.goods.service.impl;

import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.goods.entity.SkuPO;
import com.yirancrazy.minimall.goods.manager.SkuManager;
import com.yirancrazy.minimall.goods.service.SkuService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class SkuServiceImpl implements SkuService {

    private final SkuManager skuManager;

    public SkuServiceImpl(SkuManager skuManager) {
        this.skuManager = skuManager;
    }

    @Override
    public SkuPO getById(Long id) {
        SkuPO s = skuManager.getById(id);
        if (s == null) {
            throw new BizException("13001", "SKU_NOT_FOUND", "SKU 不存在");
        }
        return s;
    }

    @Override
    public Long create(SkuPO sku) {
        if (sku.getPrice() == null) {
            sku.setPrice(BigDecimal.ZERO);
        }
        if (sku.getStock() == null) {
            sku.setStock(0);
        }
        skuManager.save(sku);
        return sku.getId();
    }
}