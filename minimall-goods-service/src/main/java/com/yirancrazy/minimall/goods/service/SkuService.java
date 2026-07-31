package com.yirancrazy.minimall.goods.service;

import com.yirancrazy.minimall.goods.dto.SkuCreateDTO;
import com.yirancrazy.minimall.goods.entity.SkuPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品领域服务接口，定义Sku相关业务契约
 * @Version: 1.0
 * @DateTime: 2026/07/31
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