package com.yirancrazy.minimall.cart.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.cart.entity.CartItemPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: CartItem数据访问层接口，定义CartItem表操作契约
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public interface CartItemManager extends IService<CartItemPO> {

    /**
     * 根据用户ID和SKU ID查询购物车条目。
     * @param userId 用户ID，来自网关X-User-Id可信头
     * @param skuId SKU ID
     * @return 购物车条目；不存在返回 null
     */
    CartItemPO getByUserIdAndSkuId(Long userId, Long skuId);
}