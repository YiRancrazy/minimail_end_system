package com.yirancrazy.minimall.merchant.service;

import com.yirancrazy.minimall.merchant.entity.ShopPO;

public interface ShopService {
    ShopPO getById(Long id);

    Long create(ShopPO shop);

    boolean update(Long id, ShopPO shop);

    boolean delete(Long id);
}