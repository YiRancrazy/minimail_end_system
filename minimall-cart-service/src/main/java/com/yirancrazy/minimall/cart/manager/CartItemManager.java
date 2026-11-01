package com.yirancrazy.minimall.cart.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.cart.entity.CartItemPo;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义 MyBatis-Plus 管理器层暴露的购物车条目持久化操作。该接口支持按用户和 SKU
 *     唯一键查询，以及避免 read-check-write 竞争的原子 upsert；业务校验仍留在 CartService。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface CartItemManager extends IService<CartItemPo> {

    /**
     * 按用户和 SKU 唯一键查找购物车记录.
     *
     * @param userId 购物车所属用户
     * @param skuId 购物车中保存的 SKU
     * @return 匹配的持久化记录；不存在有效记录时返回 null
     */
    CartItemPo getByUserIdAndSkuId(Long userId, Long skuId);

    /**
     * 原子插入购物车记录，或向已有用户和 SKU 记录累加数量.
     *
     * @param id 仅在插入新记录时使用的主键
     * @param userId 购物车所属用户
     * @param skuId 待插入或合并的 SKU
     * @param quantity 待累加数量，由数据库语句执行上限控制
     * @param selected 仅在插入时使用的勾选编码
     * @return 数据库操作影响的行数
     */
    int upsertQuantity(Long id, Long userId, Long skuId, Integer quantity, Integer selected);
}
