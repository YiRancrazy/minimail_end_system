package com.yirancrazy.minimall.merchant.service;

import com.yirancrazy.minimall.merchant.dto.ShopCreateDTO;
import com.yirancrazy.minimall.merchant.dto.ShopUpdateDTO;
import com.yirancrazy.minimall.merchant.entity.ShopPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商户领域服务接口，定义Shop相关业务契约
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public interface ShopService {
    /**
     * 根据ID查询店铺。
     * @param id 店铺ID
     * @return 店铺PO
     */
    ShopPO getById(Long id);

    /**
     * 分页查询店铺列表。
     * @param merchantId 商家账号 ID
     * @param cursor 游标
     * @param limit 每页数量
     * @return 店铺列表
     */
    java.util.List<ShopPO> list(Long merchantId, String cursor, Integer limit);

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