package com.yirancrazy.minimall.merchant.service.impl;

import org.springframework.stereotype.Service;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.merchant.constant.ShopCodeEnum;
import com.yirancrazy.minimall.merchant.dto.ShopCreateDTO;
import com.yirancrazy.minimall.merchant.dto.ShopUpdateDTO;
import com.yirancrazy.minimall.merchant.entity.ShopPO;
import com.yirancrazy.minimall.merchant.manager.ShopManager;
import com.yirancrazy.minimall.merchant.service.ShopService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商户领域服务实现，实现Shop相关业务逻辑
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Service
public class ShopServiceImpl implements ShopService {

    private final ShopManager shopManager;

    public ShopServiceImpl(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    /**
     * 根据主键 ID 查询店铺，未找到时抛出店铺不存在业务异常。
     *
     * @param id 店铺主键 ID
     * @return 店铺实体对象
     */
    @Override
    public ShopPO getById(Long id) {
        ShopPO s = shopManager.getById(id);
        if (s == null) {
            throw new BizException(ShopCodeEnum.SHOP_NOT_FOUND);
        }
        return s;
    }

    /**
     * 新增店铺记录，并返回持久化后的主键 ID。
     *
     * @param dto 待创建的店铺信息
     * @return 新建店铺的主键 ID
     */
    @Override
    public Long create(ShopCreateDTO dto) {
        ShopPO shop = new ShopPO();
        shop.setShopName(dto.getShopName());
        shop.setLicenseNo(dto.getLicenseNo());
        shop.setStatus(dto.getStatus());
        shopManager.save(shop);
        return shop.getId();
    }

    /**
     * 根据主键 ID 更新店铺信息。
     *
     * @param id 店铺主键 ID
     * @param dto 待更新的店铺信息
     * @return 是否更新成功
     */
    @Override
    public boolean update(Long id, ShopUpdateDTO dto) {
        ShopPO shop = new ShopPO();
        shop.setId(id);
        shop.setShopName(dto.getShopName());
        shop.setLicenseNo(dto.getLicenseNo());
        shop.setStatus(dto.getStatus());
        return shopManager.updateById(shop);
    }

    /**
     * 根据主键 ID 逻辑删除店铺记录。
     *
     * @param id 店铺主键 ID
     * @return 是否删除成功
     */
    @Override
    public boolean delete(Long id) {
        return shopManager.removeById(id);
    }
}