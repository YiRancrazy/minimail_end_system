package com.yirancrazy.minimall.merchant.service;

import com.yirancrazy.minimall.merchant.dto.ShopCreateDTO;
import com.yirancrazy.minimall.merchant.dto.ShopUpdateDTO;
import com.yirancrazy.minimall.merchant.entity.ShopPO;

/**
* 店铺领域服务接口，定义店铺聚合根的查询、创建、更新与删除等业务契约。
 */
public interface ShopService {
    /**
     * 根据ID查询店铺。
     * @param id 店铺ID
     * @return 店铺PO
     */
    ShopPO getById(Long id);

    /**
     * 创建店铺。
     * @param dto 店铺创建DTO
     * @return 店铺ID
     */
    Long create(ShopCreateDTO dto);

    /**
     * 更新店铺。
     * @param id 店铺ID
     * @param dto 店铺更新DTO
     * @return 更新是否成功
     */
    boolean update(Long id, ShopUpdateDTO dto);

    /**
     * 删除店铺。
     * @param id 店铺ID
     * @return 删除是否成功
     */
    boolean delete(Long id);
}