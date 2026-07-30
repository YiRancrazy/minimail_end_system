package com.yirancrazy.minimall.merchant.service;

import com.yirancrazy.minimall.merchant.dto.ShopCreateDTO;
import com.yirancrazy.minimall.merchant.dto.ShopUpdateDTO;
import com.yirancrazy.minimall.merchant.entity.ShopPO;

/**
* 店铺领域服务接口，定义店铺聚合根的查询、创建、更新与删除等业务契约。
 */
public interface ShopService {
    ShopPO getById(Long id);

    Long create(ShopCreateDTO dto);

    boolean update(Long id, ShopUpdateDTO dto);

    boolean delete(Long id);
}