package com.yirancrazy.minimall.goods.service;

import com.yirancrazy.minimall.goods.dto.SkuCreateDTO;
import com.yirancrazy.minimall.goods.entity.SkuPO;

/**
* 商品领域服务接口，定义 SKU 的查询与创建业务入口，由 SkuServiceImpl 实现。
 */
public interface SkuService {
    /**
     * 根据ID查询SKU。
     * @param id SKU ID
     * @return SKU PO
     */
    SkuPO getById(Long id);

    /**
     * 创建SKU。
     * @param dto SKU创建DTO
     * @return SKU ID
     */
    Long create(SkuCreateDTO dto);
}