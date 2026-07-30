package com.yirancrazy.minimall.goods.service;

import com.yirancrazy.minimall.goods.dto.SkuCreateDTO;
import com.yirancrazy.minimall.goods.entity.SkuPO;

/**
* 商品领域服务接口，定义 SKU 的查询与创建业务入口，由 SkuServiceImpl 实现。
 */
public interface SkuService {
    SkuPO getById(Long id);

    Long create(SkuCreateDTO dto);
}