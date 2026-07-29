package com.yirancrazy.minimall.merchant.service;

import com.yirancrazy.minimall.merchant.entity.ShopPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 店铺领域服务接口，定义店铺聚合根的查询、创建、更新与删除等业务契约。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
public interface ShopService {
    ShopPO getById(Long id);

    Long create(ShopPO shop);

    boolean update(Long id, ShopPO shop);

    boolean delete(Long id);
}