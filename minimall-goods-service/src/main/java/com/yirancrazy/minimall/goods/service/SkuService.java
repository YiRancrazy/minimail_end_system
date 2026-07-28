package com.yirancrazy.minimall.goods.service;

import com.yirancrazy.minimall.goods.entity.SkuPO;

public interface SkuService {
    SkuPO getById(Long id);

    Long create(SkuPO sku);
}