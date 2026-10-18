package com.yirancrazy.minimall.cart.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.cart.entity.CartItemPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CartItemManager 管理器接口，封装数据访问能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface CartItemManager extends IService<CartItemPO> {


    /**
     * 获取相关数据。
     * @param userId userId 参数
     * @param skuId skuId 参数
     * @return 处理结果
     */
    CartItemPO getByUserIdAndSkuId(Long userId, Long skuId);

    /**
     * 执行 upsertQuantity 方法。
     * @param id id 参数
     * @param userId userId 参数
     * @param skuId skuId 参数
     * @param quantity quantity 参数
     * @param selected selected 参数
     * @return 业务数值
     */
    int upsertQuantity(Long id, Long userId, Long skuId, Integer quantity, Integer selected);
}
