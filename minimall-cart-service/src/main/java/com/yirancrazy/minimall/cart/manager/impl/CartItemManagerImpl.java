package com.yirancrazy.minimall.cart.manager.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.cart.entity.CartItemPo;
import com.yirancrazy.minimall.cart.manager.CartItemManager;
import com.yirancrazy.minimall.cart.mapper.CartItemMapper;
import com.yirancrazy.minimall.common.annotation.Manager;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 使用 MyBatis-Plus 和 CartItemMapper 实现购物车条目持久化。查询通过用户和 SKU
 *     条件的 lambda wrapper 完成，upsert 委托给 Mapper 的数据库原子 SQL；此处不重复购物车业务规则。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Manager
public class CartItemManagerImpl extends ServiceImpl<CartItemMapper, CartItemPo>
        implements CartItemManager {

    /**
     * 按用户和 SKU 唯一键读取一条购物车记录.
     *
     * @param userId 购物车所属用户
     * @param skuId 购物车中保存的 SKU
     * @return 匹配记录；不存在时返回 null
     */
    @Override
    public CartItemPo getByUserIdAndSkuId(Long userId, Long skuId) {
        return getOne(
                Wrappers.lambdaQuery(CartItemPo.class)
                        .eq(CartItemPo::getUserId, userId)
                        .eq(CartItemPo::getSkuId, skuId));
    }

    /**
     * 执行 Mapper 的插入或数量累加语句.
     *
     * @param id 新记录主键
     * @param userId 购物车所属用户
     * @param skuId 待插入或合并的 SKU
     * @param quantity 待累加数量
     * @param selected 仅在插入时使用的勾选编码
     * @return 数据库返回的影响行数
     */
    @Override
    public int upsertQuantity(Long id, Long userId, Long skuId, Integer quantity, Integer selected) {
        return baseMapper.upsertQuantity(id, userId, skuId, quantity, selected);
    }
}
