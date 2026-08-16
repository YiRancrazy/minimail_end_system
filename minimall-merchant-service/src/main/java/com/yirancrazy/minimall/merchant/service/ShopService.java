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
     * 根据ID查询店铺，校验店铺归属商家，非本人名下店铺按不存在处理。
     * @param merchantId 商家账号 ID，为空表示内部调用跳过归属校验
     * @param id 店铺ID
     * @return 店铺PO
     * @throws com.yirancrazy.minimall.common.exception.BizException 店铺不存在或非本人名下
     */
    ShopPO getById(Long merchantId, Long id);

    /**
     * 分页查询当前商家名下的店铺列表。
     * @param merchantId 商家账号 ID
     * @param cursor 游标
     * @param limit 每页数量
     * @return 店铺列表
     */
    java.util.List<ShopPO> list(Long merchantId, String cursor, Integer limit);

    /**
     * 创建店铺，归属绑定到当前商家。
     * @param merchantId 商家账号 ID
     * @param dto 店铺创建DTO
     * @return 店铺ID
     */
    Long create(Long merchantId, ShopCreateDTO dto);

    /**
     * 更新店铺，校验店铺归属商家，非本人名下店铺按不存在处理。
     * @param merchantId 商家账号 ID
     * @param id 店铺ID
     * @param dto 店铺更新DTO
     * @return 更新是否成功
     * @throws com.yirancrazy.minimall.common.exception.BizException 店铺不存在或非本人名下
     */
    boolean update(Long merchantId, Long id, ShopUpdateDTO dto);

    /**
     * 删除店铺，校验店铺归属商家，非本人名下店铺按不存在处理。
     * @param merchantId 商家账号 ID
     * @param id 店铺ID
     * @return 删除是否成功
     * @throws com.yirancrazy.minimall.common.exception.BizException 店铺不存在或非本人名下
     */
    boolean delete(Long merchantId, Long id);
}