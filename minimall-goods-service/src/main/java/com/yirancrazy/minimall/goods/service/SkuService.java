package com.yirancrazy.minimall.goods.service;

import com.yirancrazy.minimall.goods.dto.SkuCreateDTO;
import com.yirancrazy.minimall.goods.entity.SkuPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品领域服务接口，定义 SKU 的查询与创建业务入口，由 SkuServiceImpl 实现。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
public interface SkuService {
    SkuPO getById(Long id);

    Long create(SkuCreateDTO dto);
}