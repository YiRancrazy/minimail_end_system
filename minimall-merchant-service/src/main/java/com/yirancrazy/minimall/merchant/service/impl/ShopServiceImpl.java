package com.yirancrazy.minimall.merchant.service.impl;

import com.yirancrazy.minimall.merchant.constant.ShopCodeEnum;

import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.merchant.entity.ShopPO;
import com.yirancrazy.minimall.merchant.manager.ShopManager;
import com.yirancrazy.minimall.merchant.service.ShopService;
import org.springframework.stereotype.Service;

@Service
public class ShopServiceImpl implements ShopService {

    private final ShopManager shopManager;

    public ShopServiceImpl(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    @Override
    public ShopPO getById(Long id) {
        ShopPO s = shopManager.getById(id);
        if (s == null) {
            throw new BizException(ShopCodeEnum.SHOP_NOT_FOUND);
        }
        return s;
    }

    @Override
    public Long create(ShopPO shop) {
        shopManager.save(shop);
        return shop.getId();
    }

    @Override
    public boolean update(Long id, ShopPO shop) {
        shop.setId(id);
        return shopManager.updateById(shop);
    }

    @Override
    public boolean delete(Long id) {
        return shopManager.removeById(id);
    }
}